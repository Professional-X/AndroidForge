# XIVOX Video Player ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract *;
}
-dontwarn androidx.media3.**
