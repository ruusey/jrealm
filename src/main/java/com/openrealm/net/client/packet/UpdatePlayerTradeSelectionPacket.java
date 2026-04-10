package com.openrealm.net.client.packet;

import com.openrealm.game.entity.Player;
import com.openrealm.game.ui.PlayerUI;
import com.openrealm.game.ui.Slots;
import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.entity.NetInventorySelection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Streamable
@Data
@AllArgsConstructor
@NoArgsConstructor
@PacketId(packetId = (byte)18)
public class UpdatePlayerTradeSelectionPacket extends Packet {
	@SerializableField(order = 0, type = NetInventorySelection.class)
	private NetInventorySelection selection;

	public static UpdatePlayerTradeSelectionPacket fromSelection(Player player, PlayerUI ui) {
		final Slots[] uiSlots = ui.getSlots(4, 12);
		final Boolean[] selected = new Boolean[uiSlots.length];
		for (int i = 0; i < uiSlots.length; i++) {
			Slots slot = uiSlots[i];
			if (slot == null)
				continue;
			if (slot.isSelected()) {
				selected[i] = true;
			}
		}
		final NetInventorySelection updatedSelection = NetInventorySelection.builder().playerId(player.getId())
				.selection(selected).build();
		return new UpdatePlayerTradeSelectionPacket(updatedSelection);
	}
}
