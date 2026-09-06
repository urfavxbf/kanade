# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# NewPipe Extractor uses Rhino/reflective JavaScript parsing for YouTube
# signature and throttling parameter handling.
-keep class org.mozilla.javascript.** { *; }
-keep class org.schabi.newpipe.extractor.** { *; }

# Preserve generic signatures used by extractor response models.
-keepattributes Signature,InnerClasses,EnclosingMethod

# If you keep the line number information, uncomment the following rules.
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile
