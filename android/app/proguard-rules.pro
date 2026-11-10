# Gson reflects over DTO backing fields; preserve their names and Retrofit annotations.
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses
-keep class com.aistudymentor.data.remote.** { *; }
-keep interface com.aistudymentor.data.remote.ApiService { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Room-generated implementations and schema entities use reflected metadata at runtime.
-keep class com.aistudymentor.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Retrofit method annotations are required to construct API calls at runtime.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Tink references these compile-time Error Prone annotations; their annotations are not needed at runtime.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi