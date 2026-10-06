# Issue #1 verification

The original fix targeted Minecraft 26.2 and Java 25; the shared fix now builds for
all nine targets listed in [Stonecutter development](multiversion.md).
No applicable AGENTS.md was found.
The issue URL could not be fetched in this session; the supplied reproduction was used.

## Inspected vanilla flow

Inspected the project's cached 26.2 Minecraft classes with JDK 25 `javap -p -c`:

- `Minecraft.handleKeybinds` updates `Inventory.setSelectedSlot` for hotbar keys
  before consuming the offhand key and directly sending `SWAP_ITEM_WITH_OFFHAND`.
- `MultiPlayerGameMode.tick` calls `ensureHasSentCarriedItem`, which uses a cached
  `carriedIndex`. The offhand input path does not call this synchronization method.
- `ServerboundPlayerActionPacket` carries action, position, direction and sequence,
  but no hotbar index. `ServerboundSetCarriedItemPacket(int)` carries the selected slot.
- `ClientCommonPacketListenerImpl.send(Packet<?>)` delegates to its own Connection.
  Connection writes immediately on its event loop or schedules writes there in order.
  Therefore the nested send from the HEAD injection precedes the original swap on
  the same connection. The selection packet is not an action packet, so it does not
  trigger another swap check.
- `ServerGamePacketListenerImpl.handleSetCarriedItem` validates and sets the server
  inventory selection. `handlePlayerAction` swaps the current main hand with offhand.
  Both dispatch onto the server thread through `PacketUtils.ensureRunningOnSameThread`.

The fix sends the checked slot unconditionally before an allowed swap; no persistent
slot cache is added. Locked slots still cancel the original send. Disabled mode
returns before any added packet or cancellation.

## Automated regression

PowerShell:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.2'
$env:GRADLE_USER_HOME = 'C:/Users/User/.gradle'
.\gradlew.bat :26.2:offhandRegressionTest :26.2:build --offline
```

The dependency-free test runs the production guard in the mixin with real packet
classes. Its ordered server model starts on index 0 with a distinguishable diamond;
client index 8 contains a totem and is the only unlocked slot. It checks selection
before swap, offhand contents and preservation of the locked item. It also covers
blocked swaps and disabled mode with both allowed and locked client slots.
`test` depends on this suite, so `check` and `build` run it too.

This test does **not** apply Mixin transformation, start Minecraft, use real
ItemStacks, or execute the actual server listener/Netty transport. Those boundaries
require the following in-game check. Removing just the added selection send (the
old sending behavior) was verified to fail with `Expected selection then swap`.

## In-game checks (not executed in this session)

1. Run the 26.2 Fabric client with this mod and connect to a vanilla 26.2 server.
   Enable the mod, lock slot 1 (index 0), unlock slot 9 (index 8). Put a diamond in
   slot 1, a totem in slot 9 and leave offhand empty.
2. Select slot 1 and press F: the diamond must stay in place, and no swap packet
   must leave. Repeat F and rapid switching to slot 9 several times: whenever a
   swap is allowed, only the totem may reach offhand.
3. For a deterministic mismatch, debug the client and break at
   `Minecraft.handleKeybinds` immediately before the offhand packet send. Start
   with server index 0 selected, then queue hotbar key 9 and F together. At the
   breakpoint verify client index 8 and server index 0 (use a server debugger at
   the inventory selection). Resume and trace the client listener's sends: slot
   selection 8 must precede swap. On the server, `handleSetCarriedItem(8)` must
   execute before the swap action. The diamond stays in slot 1; the totem reaches
   offhand. Do not suspend the client event-loop thread while tracing.
4. Disable the mod. Repeat F on slot 1: vanilla must swap the diamond. In the
   deterministic mismatch check the mod must add no slot selection packet.
5. Confirm startup has no mixin injection errors and the blocked-swap actionbar
   message still works when enabled in configuration.
