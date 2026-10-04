
# Room generates classes reflectively referenced by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# kotlinx.serialization: keep generated serializers for @Serializable classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.joe.taskmanager.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.joe.taskmanager.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Gson is not used. Hilt/Dagger keep rules ship with the library.
