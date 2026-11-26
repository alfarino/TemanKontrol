# ProGuard / R8 Rules for Teman Kontrol

# ===================================================================
# General & Attributes
# ===================================================================
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, SourceFile, LineNumberTable

# ===================================================================
# Room Database, DAOs, Entities, and Generated Implementations
# ===================================================================
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep class *_Impl {
    <init>();
    <init>(...);
    *;
}
-keep class id.temankontrol.app.data.local.** { *; }
-keep interface id.temankontrol.app.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ===================================================================
# Kotlinx Serialization & Navigation Compose Routes
# ===================================================================
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
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
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}
-keep class * implements kotlinx.serialization.KSerializer {
    <init>(...);
    *;
}
-keep class id.temankontrol.app.navigation.** { *; }
-keep interface id.temankontrol.app.navigation.** { *; }
-keep class id.temankontrol.app.domain.model.** { *; }
-keep class id.temankontrol.app.data.backup.** { *; }
-keep class id.temankontrol.app.data.reminder.** { *; }
-keep class id.temankontrol.shared.dto.** { *; }
-keep class id.temankontrol.composeapp.** { *; }
-keep class androidx.biometric.** { *; }

# Ktor / OkHttp / Coroutines reflection safety
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn kotlinx.atomicfu.**

# ===================================================================
# Hilt / Dagger Dependency Injection
# ===================================================================
-dontwarn com.google.errorprone.annotations.**
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.Module class * { *; }
-keep class id.temankontrol.app.di.** { *; }

# ===================================================================
# Coroutines & DataStore
# ===================================================================
-dontwarn kotlinx.coroutines.**
-keep class androidx.datastore.** { *; }

# ===================================================================
# Jetpack Compose Runtime
# ===================================================================
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
