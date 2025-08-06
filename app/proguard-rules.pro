# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Gson models
-keep class com.ztftrue.music.utils.model.* {  *; }

# Keep Room entities and DAOs
-keep class com.ztftrue.music.sqlData.** { *; }
-keepclassmembers class com.ztftrue.music.sqlData.model.** { *; }

# Keep Room database class, change "AppDatabase" to your actual database class
-keep class com.ztftrue.music.sqlData.MusicDatabase { *; }
-keepclassmembers class com.ztftrue.music.sqlData.MusicDatabase { *; }


# Keep Room annotations
-keepattributes Room*Annotation*
# class com.google.common.reflect.TypeToken isn't parameterized
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

-keep class org.jaudiotagger.** {*;}


-keep class com.ztftrue.music.play.PlayService { *; }
-keep class com.ztftrue.music.play.PlayService$* { *; }

-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }