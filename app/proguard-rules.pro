# OkHttp / Okio (used by the Gemini client). These are optional/runtime-only deps
# that R8 warns about but the app doesn't use.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep the contact model (small, avoids any reflection surprises).
-keep class com.contactsnap.app.model.** { *; }
