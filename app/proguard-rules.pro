# ProGuard / R8 Rules for Teman Kontrol

# Keep Kotlinx Serialization routes for Compose Navigation
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    static ** Companion;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class id.temankontrol.app.navigation.** { *; }
-keep interface id.temankontrol.app.navigation.** { *; }

# Room Database & Entities
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class id.temankontrol.app.data.local.entity.** { *; }
-keep class id.temankontrol.app.data.local.dao.** { *; }

# Hilt & Dependency Injection
-dontwarn com.google.errorprone.annotations.**
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Coroutines
-dontwarn kotlinx.coroutines.**