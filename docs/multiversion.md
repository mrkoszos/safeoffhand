# Stonecutter development

The mod version stays `1.0.1`. There is one shared Java/resource/test tree; no Java
files need Stonecutter version conditions for the supported targets.

```text
settings.gradle                   Stonecutter 0.9.8 + loom-back-compat 0.4.2, nine nodes
stonecutter.gradle                 active node 26.2, aggregate build/test/jar checks
build.gradle                      shared node build, dependencies, toolchains, metadata
gradle.properties                 shared mod identity and Gradle options
gradle/wrapper/                    Gradle 9.6.0
src/client/java/                  shared implementation (unchanged)
src/client/resources/             shared assets and expanded metadata/mixin Java level
src/test/java/                    shared offhand packet-order regression (unchanged)
versions/
  1.21.8/gradle.properties
  1.21.9/gradle.properties
  1.21.10/gradle.properties
  1.21.11/gradle.properties
  26.1/gradle.properties
  26.1.1/gradle.properties
  26.1.2/gradle.properties
  26.2/gradle.properties
  26.3/gradle.properties
```

Stonecutter generates each node's processed sources and outputs under
`versions/<version>/build/`; these are not independent source trees. Each client
uses its own `versions/<version>/run/` directory, keeping test worlds separate.

## Dependency matrix

All nodes pin Fabric Loader **0.19.5** and Loom **1.17.21**. `loom-back-compat`
selects the plugin mode and maps its `modImplementation`/`modApi` declarations to
ordinary configurations on unobfuscated targets. The Loom version is declared in
every node rather than inherited from the former 26.2 build.

| Minecraft | Java | Loom plugin | Fabric API | Cloth Config | Mod Menu |
|---|---:|---|---|---|---|
| 1.21.8 | 21 | fabric-loom-remap | 0.136.1+1.21.8 | 19.0.147 | 15.0.2 |
| 1.21.9 | 21 | fabric-loom-remap | 0.134.1+1.21.9 | 20.0.149 | 16.0.1 |
| 1.21.10 | 21 | fabric-loom-remap | 0.138.4+1.21.10 | 20.0.149 | 16.0.1 |
| 1.21.11 | 21 | fabric-loom-remap | 0.141.6+1.21.11 | 21.11.153 | 17.0.1 |
| 26.1 | 25 | fabric-loom | 0.155.3+26.1.2 | 26.1.154 | 18.0.2 |
| 26.1.1 | 25 | fabric-loom | 0.155.3+26.1.2 | 26.1.154 | 18.0.2 |
| 26.1.2 | 25 | fabric-loom | 0.155.3+26.1.2 | 26.1.154 | 18.0.2 |
| 26.2 | 25 | fabric-loom | 0.161.0+26.2 | 26.2.155 | 20.0.3 |
| 26.3 | 25 | fabric-loom | 0.162.0+26.3 | 26.3.159 | 21.0.0 |

The full plugin IDs are `net.fabricmc.fabric-loom-remap` and
`net.fabricmc.fabric-loom`. The older nodes use official Mojang mappings and
release remapping; 26.x uses neither mappings nor release remapping. Fabric API
`0.155.3+26.1.2` explicitly advertises compatibility with all three 26.1 releases
in its published version metadata; it was not chosen just by its filename.

Sources for the pinned compatibility choices:

- [Fabric Loom plugin modes](https://docs.fabricmc.net/develop/loom/)
- [Fabric 26.1 Java/tooling transition](https://www.fabricmc.net/2026/03/14/261.html)
- [Fabric 26.3 tooling](https://www.fabricmc.net/2026/09/15/263.html)
- [Fabric API version metadata](https://api.modrinth.com/v2/project/fabric-api/version)
- [Cloth Config version metadata](https://api.modrinth.com/v2/project/cloth-config/version)
- [Mod Menu version metadata](https://api.modrinth.com/v2/project/modmenu/version)
- [Fabric Loader metadata](https://meta.fabricmc.net/v2/versions/loader)

## Commands (PowerShell)

Run Gradle itself with Java 25. Gradle selects Java 21 or 25 toolchains for node
compilation, regression tests and client execution. Both were found locally on
the validation machine; there is no hardcoded machine-specific JDK path in the build.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.2'
# Only needed if your environment points GRADLE_USER_HOME at an unsuitable location:
$env:GRADLE_USER_HOME = 'C:/Users/User/.gradle'

.\gradlew.bat 'Set active project to 1.21.8'
.\gradlew.bat 'Set active project to 26.2'

.\gradlew.bat :1.21.8:runClient
.\gradlew.bat :26.3:runClient

.\gradlew.bat :1.21.11:build
.\gradlew.bat buildActive
.\gradlew.bat buildAll --no-parallel
.\gradlew.bat collectJars --no-parallel
.\gradlew.bat verifyJars --no-parallel

.\gradlew.bat :26.2:offhandRegressionTest
.\gradlew.bat regressionAll --no-parallel
```

`build`/`check` on every node includes its regression suite. Qualified commands
do not require changing the IDE's active version. Release jars are named
`safeoffhand-1.0.1+mc<version>.jar`; `collectJars` puts all nine release jars in
root `build/libs/`, excluding development and sources jars. `verifyJars` checks
their names, dependency/Minecraft/Java metadata, mixin Java level and class version.
`--offline` can be added after dependencies and client assets have been fetched.

## API and behavior inspection

`javap -p -c` inspection of all nine resolved game versions confirmed the same
relevant API and semantics: `Inventory.getSelectedSlot()`, the listener's
`send(Packet<?>)`, `ServerboundSetCarriedItemPacket(int)`, and
`SWAP_ITEM_WITH_OFFHAND`. The swap packet carries no hotbar index. Input updates
the local selection before the swap, while vanilla synchronizes selection through
`MultiPlayerGameMode.ensureHasSentCarriedItem`. The server applies the selection
packet before swapping its current main hand. The mod's unconditional checked-slot
send before an allowed swap therefore remains necessary on every target.

The private static mixin helper, locked-slot cancellation, disabled-mode vanilla
behavior, configuration fields and file identity, actionbar behavior and inventory
hover swaps are unchanged. No new version-dependent state or source branches exist.

## Validation

All nine nodes compiled, built release jars and passed the existing four-case
regression suite. All nine release jars passed `verifyJars`. Version switching was
tested, then the active version was restored to 26.2. `git diff --check` passed.

Development clients for 1.21.8, 1.21.11, 26.1, 26.2 and 26.3 were launched with a
temporary Java startup agent generated under ignored `build/migration/`. It checks
on the client thread that the live title screen responds, `SafeOffhandClient`
has registered its config, and the actual transformed packet listener contains
the private static guard. The probe closes successful clients gracefully. It is
not part of any release jar. Client logs and the probe source are left under
`build/migration/` for inspection. The initial offline 1.21.8 launch lacked assets;
fetching them online resolved that pre-launch failure.

The packet-order regression invokes the production helper using real packet
classes and a model server; it does not execute Minecraft server handlers or
Mixin transformation. The separate client startup checks cover transformation.

Remaining manual checks on each version: connect to a matching server, lock index
0 with a diamond, unlock index 8 with a totem, and repeat F with rapid switching.
Only the totem may enter offhand on allowed swaps; locked swaps must send nothing.
Check disabled mode, actionbar messages, Mod Menu configuration saving/reloading,
and vanilla inventory-hover swaps. For a deterministic client/server selection
mismatch, follow [the debugger steps](offhand-regression.md#in-game-checks-not-executed-in-this-session).
These world-level checks were not performed during this migration.
