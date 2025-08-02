package com.jrealm.net.server.packet;

import java.util.Arrays;
import java.util.List;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@NoArgsConstructor
@AllArgsConstructor
public class MoveItemPacket extends Packet {
	private static final List<Integer> EQUIPMENT_IDX = Arrays.asList(0, 1, 2, 3);
	private static final List<Integer> INV_IDX1 = Arrays.asList(4, 5, 6, 7, 8, 9, 10, 11);
	// private static final List<Integer> INV_IDX2
	// =Arrays.asList(12,13,14,15,16,17,18,19);
	private static final List<Integer> GROUND_LOOT_IDX = Arrays.asList(20, 21, 22, 23, 24, 25, 26, 27);

	@SerializableField(order = 0, type = SerializableLong.class)
	private long playerId;
	@SerializableField(order = 1, type = SerializableByte.class)
	private byte targetSlotIndex;
	@SerializableField(order = 2, type = SerializableByte.class)
	private byte fromSlotIndex;
	@SerializableField(order = 3, type = SerializableBoolean.class)
	private boolean drop;
	@SerializableField(order = 4, type = SerializableBoolean.class)
	private boolean consume;
	
	public static MoveItemPacket from(long playerId, byte targetSlot, byte fromSlot, boolean drop, boolean consume)
			throws Exception {
		MoveItemPacket packet = new MoveItemPacket(playerId, targetSlot, fromSlot, drop, consume);
		return packet;
	}

	public static boolean isInv1(int index) {
		return INV_IDX1.contains(index);
	}

	public static boolean isEquipment(int index) {
		return EQUIPMENT_IDX.contains(index);
	}

	public static boolean isGroundLoot(int index) {
		return GROUND_LOOT_IDX.contains(index);
	}

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte) 12;
	}
}
