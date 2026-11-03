package com.aikon.filemanager;

import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

/** Операции над файлами. Все методы блокирующие — вызывать только из фонового потока. */
final class FileOperations {

    /** Попытка поместить папку внутрь самой себя. */
    static final class SelfCopyException extends IOException {}

    private static final int BUFFER_SIZE = 64 * 1024;

    private FileOperations() {}

    static void copy(File source, File targetDir) throws IOException {
        checkNotIntoItself(source, targetDir);
        copyRecursive(source, uniqueTarget(targetDir, source.getName(), source.isDirectory()));
    }

    /** Сначала быстрый rename; если не вышло (другой том) — копирование и удаление оригинала. */
    static void move(File source, File targetDir) throws IOException {
        checkNotIntoItself(source, targetDir);
        File dest = uniqueTarget(targetDir, source.getName(), source.isDirectory());
        if (source.renameTo(dest)) return;
        copyRecursive(source, dest);
        delete(source);
    }

    static void delete(File file) throws IOException {
        // Симлинк на папку удаляем как ссылку, не заходя внутрь — иначе снесли бы содержимое цели
        if (file.isDirectory() && !isSymlink(file)) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) delete(child);
            }
        }
        if (!file.delete()) throw new IOException(file.getName());
    }

    /** @return {размер в байтах, количество файлов, количество папок} (рекурсивно) */
    static long[] stats(File dir) {
        long[] result = new long[3];
        walk(dir, result);
        return result;
    }

    /**
     * Android не отдаёт время создания надёжно: на большинстве файловых систем оно
     * недоступно, и тогда возвращается время изменения.
     */
    static long creationTime(File file) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                long t = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
                        .creationTime().toMillis();
                if (t > 0) return t;
            } catch (IOException | RuntimeException ignored) {
            }
        }
        return file.lastModified();
    }

    static File uniqueTarget(File dir, String name, boolean isDirectory) {
        File candidate = new File(dir, name);
        if (!candidate.exists()) return candidate;

        int dot = name.lastIndexOf('.');
        boolean hasExt = !isDirectory && dot > 0;
        String base = hasExt ? name.substring(0, dot) : name;
        String ext = hasExt ? name.substring(dot) : "";
        for (int i = 1; ; i++) {
            candidate = new File(dir, base + " (" + i + ")" + ext);
            if (!candidate.exists()) return candidate;
        }
    }

    private static void checkNotIntoItself(File source, File targetDir) throws IOException {
        if (!source.isDirectory()) return;
        String s = source.getCanonicalPath();
        String t = targetDir.getCanonicalPath();
        if (t.equals(s) || t.startsWith(s + File.separator)) throw new SelfCopyException();
    }

    private static void copyRecursive(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.mkdirs() && !dst.isDirectory()) throw new IOException(dst.getName());
            File[] children = src.listFiles();
            if (children == null) throw new IOException(src.getName());
            for (File child : children) copyRecursive(child, new File(dst, child.getName()));
        } else {
            try (InputStream in = new FileInputStream(src);
                 OutputStream out = new FileOutputStream(dst)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            }
        }
        dst.setLastModified(src.lastModified());
    }

    private static void walk(File dir, long[] result) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                result[2]++;
                try {
                    if (isSymlink(child)) continue;
                } catch (IOException e) {
                    continue;
                }
                walk(child, result);
            } else {
                result[1]++;
                result[0] += child.length();
            }
        }
    }

    private static boolean isSymlink(File file) throws IOException {
        File parent = file.getParentFile();
        File canon = (parent == null) ? file : new File(parent.getCanonicalFile(), file.getName());
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
}
