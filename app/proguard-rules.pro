-dontwarn io.github.libxposed.annotation.**
# LSPosed loads this name from java_init.list, not a Java call site.
-keep,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public void onModuleLoaded(...);
}
