# Keep JGit classes for reflection used by git config/transport lookups.
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn org.slf4j.**
-dontwarn javax.security.auth.**
-dontwarn java.lang.management.**