package com.aikon.launcher.apps;

public final class AppCategoryFlags {
    public static final int NONE = 0;
    public static final int CAMERA = 1;
    public static final int GALLERY = 1 << 1;
    public static final int MUSIC = 1 << 2;
    public static final int RECORDER = 1 << 3;
    public static final int GAME = 1 << 4;

    private AppCategoryFlags() {}
}
