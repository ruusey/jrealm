package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class MoveItemPacket extends Packet {
    private static final List<Integer> EQUIPMENT_IDX = Arrays.asList(0, 1, 2, 3);
    private static final List<Integer> INV_IDX1 = Arrays.asList(4, 5, 6, 7, 8, 9, 10, 11);
    // private static final List<Integer> INV_IDX2
    // =Arrays.asList(12,13,14,15,16,17,18,19);
    private static final List<Integer> GROUND_LOOT_IDX = Arrays.asList(20, 21, 22, 23, 24, 25, 26, 27);

    private long playerId;
    private byte targetSlotIndex;
    private byte fromSlotIndex;
    private boolean drop;
    private boolean consume;

    public MoveItemPacket() {

    }

    public MoveItemPacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            log.error("Failed to parse MoveItem packet, Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final ByteArrayInputStream bis = new ByteArrayInputStream(data);
    	final DataInputStream dis = new DataInputStream(bis);
        if (dis == null || dis.available() < 5)
            throw new IllegalStateException("No Packet data available to read from DataInputStream");
        this.playerId = dis.readLong();
        this.targetSlotIndex = dis.readByte();
        this.fromSlotIndex = dis.readByte();
        this.drop = dis.readBoolean();
        this.consume = dis.readBoolean();
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");
        this.addHeader(stream);
        stream.writeLong(this.playerId);
        stream.writeByte(this.targetSlotIndex);
        stream.writeByte(this.fromSlotIndex);
        stream.writeBoolean(this.drop);
        stream.writeBoolean(this.consume);
    }

    public static MoveItemPacket from(long playerId, byte targetSlot, byte fromSlot, boolean drop, boolean consume)
            throws Exception {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	final DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(playerId);
        dos.writeByte(targetSlot);
        dos.writeByte(fromSlot);
        dos.writeBoolean(drop);
        dos.writeBoolean(consume);
        return new MoveItemPacket(PacketType.MOVE_ITEM.getPacketId(), baos.toByteArray());
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
}
