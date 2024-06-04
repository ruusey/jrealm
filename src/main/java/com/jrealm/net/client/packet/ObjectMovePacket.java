package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.GameObject;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ObjectMovePacket extends Packet {

    private ObjectMovement[] movements;

    public ObjectMovePacket() {

    }

    public ObjectMovePacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            log.error("Failed to parse ObjectMove packet, Reason: {}", e);
        }
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");

        this.addHeader(stream);
        stream.writeInt(this.movements.length);
        for (ObjectMovement movement : this.movements) {
            movement.write(stream);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        if (dis == null || dis.available() < 5)
            throw new IllegalStateException("No Packet data available to read from DataInputStream");

        final int movementsSize = dis.readInt();
        this.movements = new ObjectMovement[movementsSize];
        for (int i = 0; i < movementsSize; i++) {
            this.movements[i] = new ObjectMovement().read(dis);
        }

    }

    public ObjectMovePacket getMoveDiff(ObjectMovePacket newMove) throws Exception {
        List<ObjectMovement> moveDiff = new ArrayList<>();
        for (ObjectMovement movement : newMove.getMovements()) {
            if (!this.containsMovement(movement)) {
                moveDiff.add(movement);
            }
        }

        return moveDiff.size() == 0 ? null : ObjectMovePacket.from(moveDiff.toArray(new ObjectMovement[0]));
    }

    public boolean containsMovement(ObjectMovement movement) {
        for (ObjectMovement thisMovement : this.getMovements()) {
            if (thisMovement != null && thisMovement.equals(movement)) {
                return true;
            }
        }
        return false;
    }

    public static ObjectMovePacket from(GameObject[] objects) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);

        stream.writeInt(objects.length);
        for (GameObject obj : objects) {
            new ObjectMovement(obj).write(stream);
        }

        return new ObjectMovePacket(PacketType.OBJECT_MOVE.getPacketId(), byteStream.toByteArray());
    }

    public boolean equals(ObjectMovePacket other) {
        if (other == null)
            return false;
        for (ObjectMovement movement : other.getMovements()) {
            if (!this.containsMovement(movement)) {
                return false;
            }
        }
        return true;
    }

    public static ObjectMovePacket from(ObjectMovement[] objects) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);

        stream.writeInt(objects.length);
        for (ObjectMovement obj : objects) {
            obj.write(stream);
        }

        return new ObjectMovePacket(PacketType.OBJECT_MOVE.getPacketId(), byteStream.toByteArray());
    }
}
