-verbose
-dontwarn android.opengl.**
-dontwarn com.badlogic.gdx.jnigen.**
-dontwarn com.badlogic.gdx.utils.**

-keep class com.badlogic.gdx.controllers.** { *; }
-keep class com.badlogic.gdx.backends.android.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.ironbow.stickmanarcher.data.** { *; }
-keepclassmembers class com.ironbow.stickmanarcher.data.** {
    <init>(...);
    <fields>;
    <methods>;
}
