# Night Watch uses only Android framework entry points declared in the manifest.
# R8 keeps those components automatically and removes unreachable code.

# Keep line numbers so crash stack traces from users are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
