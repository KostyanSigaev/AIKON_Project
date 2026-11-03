package com.aikon.filemanager;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.aikon.core.ui.list.AikonListAdapter;
import com.aikon.core.ui.list.AikonListItem;
import com.aikon.core.ui.list.AikonListView;
import com.aikon.core.ui.AikonLoadingOverlay;
import com.aikon.core.ui.AikonContextMenu;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Одна страница файлового менеджера — содержимое одной папки.
 * Обычный режим: полный список (иконка, имя, размер и дата), долгий тап — меню файла.
 * Режим выбора папки (копирование/перемещение): простой список только из папок.
 */
public class DirectoryFragment extends Fragment {

    private static final String ARG_PATH = "path";

    static DirectoryFragment newInstance(File dir) {
        DirectoryFragment fragment = new DirectoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, dir.getAbsolutePath());
        fragment.setArguments(args);
        return fragment;
    }

    private static final class Node {
        final File file;
        final boolean parentLink; // пункт "На уровень выше"

        Node(File file, boolean parentLink) {
            this.file = file;
            this.parentLink = parentLink;
        }
    }

    private static final class LoadResult {
        final List<AikonListItem<Node>> items;
        final boolean unreadable;
        final boolean empty;

        LoadResult(List<AikonListItem<Node>> items, boolean unreadable, boolean empty) {
            this.items = items;
            this.unreadable = unreadable;
            this.empty = empty;
        }
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private File dir;
    private AikonListView listView;
    private TextView messageView;
    private List<AikonListItem<Node>> currentItems = Collections.emptyList();
    /** Отсекает результаты устаревших загрузок (быстрая навигация, повторный reload). */
    private volatile int generation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dir = new File(requireArguments().getString(ARG_PATH, "/"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_directory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.list_files);
        messageView = view.findViewById(R.id.text_message);
        reload();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        generation++;
        listView = null;
        messageView = null;
    }

    private MainActivity host() {
        return (MainActivity) requireActivity();
    }

    // ---------- Загрузка списка ----------

    void reload() {
        if (listView == null) return;
        final int gen = ++generation;
        final boolean simple = host().isTransferActive();
        final Context appContext = requireContext().getApplicationContext();

        AikonLoadingOverlay.show();
        FileIo.EXECUTOR.execute(() -> {
            if (gen != generation) { // страницу уже закрыли или перезагрузили
                AikonLoadingOverlay.hide();
                return;
            }
            LoadResult result = build(appContext, simple);
            mainHandler.post(() -> {
                if (gen == generation && listView != null) apply(result, simple);
                AikonLoadingOverlay.hide();
            });
        });
    }

    private LoadResult build(Context ctx, boolean simple) {
        File[] children = dir.listFiles();
        List<AikonListItem<Node>> items = new ArrayList<>();

        File parent = dir.getParentFile();
        if (parent != null) {
            String title = ctx.getString(R.string.fm_parent_dir);
            Node node = new Node(parent, true);
            if (simple) {
                items.add(AikonListItem.simple(title, node));
            } else {
                items.add(AikonListItem.full(title, parent.getAbsolutePath(),
                        FileIconResolver.parentIcon(ctx), node));
            }
        }

        List<File> dirs = new ArrayList<>();
        List<File> files = new ArrayList<>();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) dirs.add(child);
                else files.add(child);
            }
        }
        Comparator<File> byName = (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        Collections.sort(dirs, byName);
        Collections.sort(files, byName);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        int shown = 0;
        for (File d : dirs) {
            addEntry(items, ctx, d, simple, dateFormat);
            shown++;
        }
        if (!simple) { // при выборе папки файлы не нужны
            for (File f : files) {
                addEntry(items, ctx, f, false, dateFormat);
                shown++;
            }
        }
        return new LoadResult(items, children == null, children != null && shown == 0);
    }

    private static void addEntry(List<AikonListItem<Node>> items, Context ctx, File file,
                                 boolean simple, DateFormat dateFormat) {
        Node node = new Node(file, false);
        if (simple) {
            items.add(AikonListItem.simple(file.getName(), node));
            return;
        }
        String date = dateFormat.format(new Date(FileOperations.creationTime(file)));
        String description = file.isDirectory()
                ? ctx.getString(R.string.fm_folder) + " · " + date
                : Formatter.formatShortFileSize(ctx, file.length()) + " · " + date;
        items.add(AikonListItem.full(file.getName(), description,
                FileIconResolver.resolve(ctx, file), node));
    }

    private void apply(LoadResult result, boolean simple) {
        currentItems = result.items;

        AikonListAdapter.OnItemLongClickListener<Node> longClick = null;
        if (!simple) longClick = this::onItemLongClick; // в режиме выбора папки меню у элементов нет
        listView.setAdapter(new AikonListAdapter<>(result.items, this::onItemClick, longClick));

        if (result.unreadable) {
            messageView.setText(R.string.fm_cannot_read);
            messageView.setVisibility(View.VISIBLE);
        } else if (result.empty) {
            messageView.setText(R.string.fm_empty_folder);
            messageView.setVisibility(View.VISIBLE);
        } else {
            messageView.setVisibility(View.GONE);
        }
    }

    // ---------- Клики ----------

    private void onItemClick(AikonListItem<Node> item, int position) {
        Node node = item.getPayload();
        if (node.parentLink) {
            host().openDirectory(node.file);
        } else {
            openItem(node.file);
        }
    }

    private void onItemLongClick(AikonListItem<Node> item, int position) {
        Node node = item.getPayload();
        if (node.parentLink) {
            showDirectoryMenu();
        } else {
            showItemMenu(node.file);
        }
    }

    private void openItem(File file) {
        if (file.isDirectory()) {
            host().openDirectory(file);
        } else {
            FileIntents.view(requireActivity(), file, false);
        }
    }

    // ---------- Меню ----------

    /**
     * Вызывается из MainActivity по левой кнопке панели и физической клавише MENU.
     * В режиме выбора папки — "Сюда"/"Отмена"; иначе — меню выделенного элемента,
     * а если выделения нет (или выделен пункт "На уровень выше") — меню текущей папки.
     */
    void onMenuRequested() {
        if (listView == null) return;
        if (host().isTransferActive()) {
            showTransferMenu();
            return;
        }
        Node focused = focusedNode();
        if (focused != null && !focused.parentLink) {
            showItemMenu(focused.file);
        } else {
            showDirectoryMenu();
        }
    }

    @Nullable
    private Node focusedNode() {
        View child = listView.getFocusedChild();
        if (child == null) return null;
        int position = listView.getChildAdapterPosition(child);
        if (position == RecyclerView.NO_POSITION || position >= currentItems.size()) return null;
        return currentItems.get(position).getPayload();
    }

    private void showTransferMenu() {
        AikonContextMenu menu = new AikonContextMenu(requireContext());
        menu.addItem(getString(R.string.fm_menu_here), () -> host().completeTransfer(dir));
        menu.addItem(getString(R.string.fm_menu_cancel), () -> host().cancelTransfer());
        menu.show(listView);
    }

    private void showItemMenu(File file) {
        boolean isFile = !file.isDirectory();
        AikonContextMenu menu = new AikonContextMenu(requireContext());
        menu.addItem(getString(R.string.fm_menu_open), () -> openItem(file));
        if (isFile) {
            menu.addItem(getString(R.string.fm_menu_open_with),
                    () -> FileIntents.view(requireActivity(), file, true));
        }
        menu.addItem(getString(R.string.fm_menu_copy_to), () -> host().beginTransfer(false, file));
        menu.addItem(getString(R.string.fm_menu_move), () -> host().beginTransfer(true, file));
        if (isFile) {
            menu.addItem(getString(R.string.fm_menu_send),
                    () -> FileIntents.send(requireActivity(), file));
        }
        menu.addItem(getString(R.string.fm_menu_create_folder), this::promptCreateFolder);
        menu.addItem(getString(R.string.fm_menu_properties), () -> showProperties(file));
        menu.addItem(getString(R.string.fm_menu_delete), () -> confirmDelete(file));
        menu.show(listView);
    }

    private void showDirectoryMenu() {
        AikonContextMenu menu = new AikonContextMenu(requireContext());
        menu.addItem(getString(R.string.fm_menu_create_folder), this::promptCreateFolder);
        menu.addItem(getString(R.string.fm_menu_properties), () -> showProperties(dir));
        menu.show(listView);
    }

    // ---------- Удаление (системный диалог — позже заменим окном из core) ----------

    private void confirmDelete(File file) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.fm_delete_title)
                .setMessage(getString(R.string.fm_delete_message, file.getName()))
                .setPositiveButton(R.string.fm_btn_delete, (d, w) -> doDelete(file))
                .setNegativeButton(R.string.fm_btn_cancel, null)
                .show();
    }

    private void doDelete(File file) {
        final Context app = requireContext().getApplicationContext();
        AikonLoadingOverlay.runAsync(() -> {
            String message;
            try {
                FileOperations.delete(file);
                message = app.getString(R.string.fm_toast_deleted);
            } catch (IOException e) {
                message = app.getString(R.string.fm_toast_error, String.valueOf(e.getMessage()));
            }
            final String toastText = message;
            mainHandler.post(() -> {
                Toast.makeText(app, toastText, Toast.LENGTH_SHORT).show();
                if (isAdded()) reload();
            });
        });
    }

    // ---------- Создание папки (системный диалог — позже заменим окном из core) ----------

    private void promptCreateFolder() {
        final EditText input = new EditText(requireContext());
        input.setHint(R.string.fm_new_folder_hint);
        input.setSingleLine(true);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.fm_new_folder_title)
                .setView(input)
                .setPositiveButton(R.string.fm_btn_create,
                        (d, w) -> createFolder(input.getText().toString().trim()))
                .setNegativeButton(R.string.fm_btn_cancel, null)
                .show();
    }

    private void createFolder(String name) {
        Context ctx = requireContext();
        if (name.isEmpty() || name.contains("/") || name.equals(".") || name.equals("..")) {
            Toast.makeText(ctx, R.string.fm_toast_invalid_name, Toast.LENGTH_SHORT).show();
            return;
        }
        File target = new File(dir, name);
        if (target.exists()) {
            Toast.makeText(ctx, R.string.fm_toast_exists, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!target.mkdir()) {
            Toast.makeText(ctx, R.string.fm_toast_mkdir_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        reload();
    }

    // ---------- Свойства ----------

    private void showProperties(File file) {
        if (!file.isDirectory()) {
            showPropertiesDialog(file, null);
            return;
        }
        // Размер папки считается рекурсивно — это может быть долго, поэтому в фоне, под окном загрузки
        AikonLoadingOverlay.runAsync(() -> {
            long[] stats = FileOperations.stats(file);
            mainHandler.post(() -> {
                if (isAdded()) showPropertiesDialog(file, stats);
            });
        });
    }

    private void showPropertiesDialog(File file, @Nullable long[] folderStats) {
        Context ctx = requireContext();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        StringBuilder text = new StringBuilder();
        text.append(getString(R.string.fm_prop_name, file.getName())).append('\n');
        text.append(getString(R.string.fm_prop_path, file.getAbsolutePath())).append('\n');
        text.append(getString(R.string.fm_prop_type, file.isDirectory()
                ? getString(R.string.fm_type_folder)
                : typeLabel(file))).append('\n');

        if (folderStats != null) {
            text.append(getString(R.string.fm_prop_size, Formatter.formatFileSize(ctx, folderStats[0]))).append('\n');
            text.append(getString(R.string.fm_prop_contents, folderStats[1], folderStats[2])).append('\n');
        } else {
            text.append(getString(R.string.fm_prop_size, Formatter.formatFileSize(ctx, file.length()))).append('\n');
        }

        text.append(getString(R.string.fm_prop_modified, dateFormat.format(new Date(file.lastModified())))).append('\n');
        text.append(getString(R.string.fm_prop_created,
                dateFormat.format(new Date(FileOperations.creationTime(file))))).append('\n');

        int accessRes = file.canWrite() ? R.string.fm_access_rw
                : file.canRead() ? R.string.fm_access_r : R.string.fm_access_none;
        text.append(getString(R.string.fm_prop_access, getString(accessRes)));

        new AlertDialog.Builder(ctx)
                .setTitle(R.string.fm_prop_title)
                .setMessage(text.toString())
                .setPositiveButton(R.string.fm_btn_ok, null)
                .show();
    }

    private String typeLabel(File file) {
        String mime = FileIntents.mimeOf(file);
        return "*/*".equals(mime) ? getString(R.string.fm_type_unknown) : mime;
    }
}
