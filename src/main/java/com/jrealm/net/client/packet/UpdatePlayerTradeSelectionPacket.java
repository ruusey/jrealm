package com.jrealm.net.client.packet;

import java.io.DataOutputStream;

import com.jrealm.game.entity.Player;
import com.jrealm.game.ui.PlayerUI;
import com.jrealm.game.ui.Slots;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;
import com.jrealm.net.entity.NetInventorySelection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Streamable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePlayerTradeSelectionPacket extends Packet {
	@SerializableField(order = 0 , type = NetInventorySelection.class)
	NetInventorySelection selection;

	@Override
	public void readData(byte[] data) throws Exception {
		final UpdatePlayerTradeSelectionPacket read = IOService.readPacket(getClass(), data);
		this.selection = read.getSelection();
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		IOService.writePacket(this, stream);
	}
	
	public static UpdatePlayerTradeSelectionPacket fromSelection(Player player, PlayerUI ui) {
		final Slots[] uiSlots = ui.getSlots(4, 12);
		final boolean[] selected = new boolean[uiSlots.length];
		for(int i = 0 ; i< uiSlots.length; i++){
			Slots slot = uiSlots[i];
			
			if(slot.isSelected()) {
				selected[i]=true;
			}
		}
		final NetInventorySelection updatedSelection = NetInventorySelection.builder().playerId(player.getId()).selection(selected).build();
		return new UpdatePlayerTradeSelectionPacket(updatedSelection);
	}
}
