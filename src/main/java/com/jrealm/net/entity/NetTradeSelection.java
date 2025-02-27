package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Player;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Streamable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetTradeSelection extends SerializableFieldType<NetTradeSelection> {

	@SerializableField(order = 0, type = NetInventorySelection.class)
	private NetInventorySelection player0Selection;
	@SerializableField(order = 1, type = NetInventorySelection.class)
	private NetInventorySelection player1Selection;

	@Override
	public NetTradeSelection read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetTradeSelection value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}

	public static NetTradeSelection getTradeSelection(Player p0, Player p1, Boolean[] p0Selection,
			Boolean[] p1Selection) {
		final NetInventorySelection p0Inv = NetInventorySelection.fromPlayer(p0, p0Selection);
		final NetInventorySelection p1Inv = NetInventorySelection.fromPlayer(p1, p1Selection);
		return new NetTradeSelection(p0Inv, p1Inv);
	}

	public void applyUpdate(NetTradeSelection playerSelection) {
		this.player0Selection = playerSelection.getPlayer0Selection();
		this.player1Selection = playerSelection.getPlayer1Selection();
	}

	public void applyUpdate(NetInventorySelection playerSelection) {
		if (playerSelection.getPlayerId() == player0Selection.getPlayerId()) {
			player0Selection.setSelection(playerSelection.getSelection());
		} else {
			player1Selection.setSelection(playerSelection.getSelection());
		}
	}
}
