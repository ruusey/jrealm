package com.jrealm.net.client.packet;

import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.entity.GameObject;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.entity.ObjectMovement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@NoArgsConstructor
public class ObjectMovePacket extends Packet {

	@SerializableField(order = 0, type = ObjectMovement.class, isCollection=true)
    private ObjectMovement[] movements;
    
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
    	final ObjectMovePacket packet = new ObjectMovePacket();
    	packet.setMovements(objects);
    	return packet;
    }

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte)3;
	}
}
