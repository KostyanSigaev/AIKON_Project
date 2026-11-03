package com.aikon.filemanager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единый фоновый поток для чтения каталогов. Намеренно НЕ закрывается через
 * shutdownNow(): задача, которая не дошла до выполнения, не вызвала бы
 * AikonLoadingOverlay.hide(), и окно загрузки зависло бы на экране.
 */
final class FileIo {
    static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private FileIo() {}
}
