# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/luck/Documents/android-sdk-macosx-2/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!cl
-optimizationpasses 5  # 指定代码的压缩级别
-allowaccessmodification #优化时允许访问并修改有修饰符的类和类的成员
-dontusemixedcaseclassnames  # 是否使用大小写混合
-dontskipnonpubliclibraryclasses  # 是否混淆第三方jar
-dontskipnonpubliclibraryclassmembers
-keepattributes SourceFile,LineNumberTable
# EnclosingMethod is required to use InnerClasses.
-keepattributes InnerClasses,EnclosingMethod
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
-keepattributes *Exceptions*
-dontpreverify  # 混淆时是否做预校验
-verbose    # 混淆时是否记录日志
-ignorewarnings  # 忽略警告，避免打包时某些警告出现
-keepattributes *Annotation*
-dontwarn **.**
-keep class *$Lambda* { <methods>; }
-keepclassmembernames public class * {
    *** lambda*(...);
}
-keepnames class * extends android.view.View
-keepnames class * extends android.app.Fragment
-keepnames class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Activity
-keepclassmembers class * implements android.os.Parcelable{
  <fields>;<init>(...);
  static ** CREATOR;
}
-keep public class * extends android.view.View{
     *** get*();
     void set*(***);
     public <init>(android.content.Context);
     public <init>(android.content.Context, android.util.AttributeSet);
     public <init>(android.content.Context, android.util.AttributeSet, int);
 }
-keepnames class com.peihua.selector.data.provider.*
-keepnames class com.peihua.selector.result.*
-keepnames class com.peihua.selector.data.Selection
-keep public class com.peihua.selector.data.Selection{
     public *** get*();
     public void set*(***);
 }
 -keep public class * extends com.peihua.selector.data.provider.IBridgeMediaLoader{
       public  <methods>;
 }