package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
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
	private Boolean[] selection;
	@SerializableField(order = 2, type = NetGameItemRef.class, isCollection = true)
	private NetGameItemRef[] itemRefs;

	@Override
	public NetInventorySelection read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetInventorySelection value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}

	public static NetInventorySelection fromPlayer(Player player, Boolean[] selectedSlots) {
		return NetInventorySelection.builder().playerId(player.getId()).selection(selectedSlots).build();
	}

	public GameItem[] getGameItems() {
		final GameItem[] items = new GameItem[itemRefs.length];
		for (int i = 0; i < this.itemRefs.length; i++) {
			items[i] = GameItem.fromGameItemRef(this.itemRefs[i]);
		}
		return items;
	}

}
