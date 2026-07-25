# Add project specific ProGuard rules here.
# Keep public API of framework module.
-keep class com.template.framework.api.model.** { *; }
-keep class com.template.framework.api.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses