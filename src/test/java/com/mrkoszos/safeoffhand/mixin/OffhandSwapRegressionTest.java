package com.mrkoszos.safeoffhand.mixin;

import com.mrkoszos.safeoffhand.config.SafeOffhandConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

/** Runs the production guard; does not apply the mixin transformation or start a server. */
public final class OffhandSwapRegressionTest {
	public static void main(String[] args) {
		SafeOffhandConfig config = new SafeOffhandConfig();
		config.setHotbarSlotAllowed(8, true);
		Server server = new Server();
		swap(config, 8, server);
		check(server.packets.size() == 2, "Expected selection then swap");
		check(server.packets.get(0) instanceof ServerboundSetCarriedItemPacket selection
				&& selection.getSlot() == 8, "Checked client slot must be sent first");
		check(server.packets.get(1) instanceof ServerboundPlayerActionPacket action
				&& action.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
				"Swap must follow selection");
		check("totem".equals(server.offhand), "The totem must reach offhand");
		check("locked-diamond".equals(server.hotbar[0]), "Locked item must stay in slot 0");
		check("empty".equals(server.hotbar[8]), "Only slot 8 must be swapped");

		server = new Server();
		swap(config, 0, server);
		check(server.packets.isEmpty(), "Locked slot must emit no packets");
		check("locked-diamond".equals(server.hotbar[0]), "Blocked swap must preserve item");

		config.enabled = false;
		server = new Server();
		swap(config, 8, server);
		check(server.packets.size() == 1
				&& server.packets.get(0) instanceof ServerboundPlayerActionPacket,
				"Disabled mod must send only vanilla swap");
		check("locked-diamond".equals(server.offhand), "Disabled mod must retain vanilla server selection");
		server = new Server();
		swap(config, 0, server);
		check(server.packets.size() == 1, "Disabled mod must allow even locked client slots");
		System.out.println("Offhand regression: 4 scenarios passed");
	}

	private static void swap(SafeOffhandConfig config, int clientSlot, Server server) {
		// Model the cancellable HEAD injection: guard runs before the original send.
		boolean blocked;
		try {
			var guard = ClientCommonPacketListenerImplMixin.class.getDeclaredMethod(
					"safeoffhand$shouldBlockSwap", SafeOffhandConfig.class, int.class, Consumer.class);
			check(Modifier.isPrivate(guard.getModifiers()), "Static mixin helper must be private");
			guard.setAccessible(true);
			blocked = (boolean) guard.invoke(null, config, clientSlot, (Consumer<Packet<?>>) server::send);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Could not invoke production swap guard", e);
		}
		if (!blocked) {
			server.send(new ServerboundPlayerActionPacket(
					ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	/** Ordered receiver modelling the inspected 26.2 selected-slot and hand-swap semantics. */
	private static final class Server {
		final List<Packet<?>> packets = new ArrayList<>();
		final String[] hotbar = new String[9];
		int selectedSlot = 0;
		String offhand = "empty";

		Server() {
			hotbar[0] = "locked-diamond";
			hotbar[8] = "totem";
		}

		void send(Packet<?> packet) {
			packets.add(packet);
			if (packet instanceof ServerboundSetCarriedItemPacket selection) {
				selectedSlot = selection.getSlot();
			} else if (packet instanceof ServerboundPlayerActionPacket action
					&& action.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
				String previous = offhand;
				offhand = hotbar[selectedSlot];
				hotbar[selectedSlot] = previous;
			} else {
				throw new AssertionError("Unexpected packet: " + packet);
			}
		}
	}
}
