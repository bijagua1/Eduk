# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/ubuntu/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For Room
-keep class androidx.room.paging.PagingSource { *; }

# For Retrofit/Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.eduk.app.model.** { *; }
