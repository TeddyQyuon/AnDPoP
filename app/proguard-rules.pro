# Keep Xposed module entrypoints and JNI bridge in the FOSS build.
-keep class me.kavishdevar.librepods.utils.KotlinModule { *; }
-keep class me.kavishdevar.librepods.utils.NativeBridge { *; }
-dontwarn io.github.libxposed.**

# Keep RoomDatabase generated *_Impl constructors used reflectively by WorkManager (Glance widgets).
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# Strip verbose/debug/info logs from release builds so identifiers and packet traces are not logged.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
