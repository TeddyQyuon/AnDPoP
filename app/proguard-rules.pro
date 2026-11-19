# CAPod R8 / ProGuard Release Rules
# Preserve line numbers for readable Play Console crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Room generated database implementation & entity models cleanly
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class com.example.data.** { *; }
