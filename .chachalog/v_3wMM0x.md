---
# Allowed version bumps: patch, minor, major
jahia-configuration: minor
---

The goals that `jahia-maven-plugin` binds to a Jahia core or module build declare that they are thread-safe, so a parallel build (`mvn -T`) no longer warns about them. The shared dependency cache in `~/.m2/dependency-cache` is safe for the concurrent modules of such a build: it tolerates a directory another module created first, and it writes each entry atomically, so a reader never picks up a half-written one. A build that runs without `-T` is unchanged. (#162)
