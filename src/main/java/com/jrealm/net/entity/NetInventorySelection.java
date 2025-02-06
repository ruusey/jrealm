package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Player;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableBoolean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Streamable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetInventorySelection extends SerializableFieldType<NetInventorySelection> {
	
	@SerializableField(order = 0, type = SerializableLong.class)
	private long playerId;
	@SerializableField(order = 1, type = SerializableBoolean.class, isCollection = true)
	private boolean[] selection;

	@Override
	public NetInventorySelection read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetInventorySelection value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}
	
	
	public static NetInventorySelection fromPlayer(Player player, boolean[] selectedSlots) {
		return NetInventorySelection.builder().playerId(player.getId()).selection(selectedSlots).build();	
	}

}
