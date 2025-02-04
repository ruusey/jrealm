package com.jrealm.net.client.packet;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.GameObject;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
public class ObjectMovePacket extends Packet {

	@SerializableField(order = 0, type = ObjectMovement.class, isCollection=true)
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
        IOService.writePacket(this, stream);
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final ObjectMovePacket read = IOService.readPacket(getClass(), data);
    	read.setId(PacketType.OBJECT_MOVE.getPacketId());
    	this.movements = read.getMovements();
    }

    
    public ObjectMovePacket getMoveDiff(ObjectMovePacket newMove) throws Exception {
    	final List<ObjectMovement> moveDiff = new ArrayList<>();
        for (final ObjectMovement movement : newMove.getMovements()) {
            if (!this.containsMovement(movement)) {
                moveDiff.add(movement);
            }
        }
        return moveDiff.size() == 0 ? null : ObjectMovePacket.from(moveDiff.toArray(new ObjectMovement[0]));
    }

    public boolean containsMovement(ObjectMovement movement) {
        for (final ObjectMovement thisMovement : this.getMovements()) {
            if (thisMovement != null && thisMovement.equals(movement)) {
                return true;
            }
        }
        return false;
    }

	public static ObjectMovePacket from(GameObject[] objects) throws Exception {
		final ObjectMovement[] results = new ObjectMovement[objects.length];
		for (int i = 0; i < objects.length; i++) {
			final ObjectMovement toWrite = new ObjectMovement(objects[i]);
			results[i] = toWrite;
		}
		final ObjectMovePacket packet = new ObjectMovePacket();
		packet.setId(PacketType.COMMAND.getPacketId());
		packet.setMovements(results);
		return packet;
	}

    public boolean equals(ObjectMovePacket other) {
        if (other == null)
            return false;
        if(this.movements==null && other.getMovements()==null) {
        	return true;
        }else if(this.movements!=null && other.getMovements()==null) {
        	return false;
        }else if(this.movements==null && other.getMovements()!=null) {
        	return false;
        }
        for (final ObjectMovement movement : other.getMovements()) {
            if (!this.containsMovement(movement)) {
                return false;
            }
        }
        return true;
    }

    public static ObjectMovePacket from(ObjectMovement[] objects) throws Exception {
    	ObjectMovePacket packet = new ObjectMovePacket();
    	packet.setMovements(objects);
    	packet.setId(PacketType.OBJECT_MOVE.getPacketId());
    	return packet;
    }
}
