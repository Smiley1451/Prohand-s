# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, AnnotationDefault
-keep class retrofit2.** { *; }
-keep @retrofit2.http.** interface * { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep your data models (Add all packages containing DTOs/Models)
-keep class com.anand.prohands.data.** { *; }
-keepclassmembers class com.anand.prohands.data.** { *; }

# Keep ReviewApi and other network interfaces
-keep interface com.anand.prohands.network.** { *; }
