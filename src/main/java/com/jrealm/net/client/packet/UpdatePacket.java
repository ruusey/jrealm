package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper=true)
public class UpdatePacket extends Packet {
	private long playerId;
	private Stats stats;
	private GameItem[] inventory;
	
	public UpdatePacket() {
		
	}
	
	public UpdatePacket(byte id, byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Failed to build Stats Packet. Reason: {}", e.getMessage());
		}
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		this.addHeader(stream);
		stream.writeLong(this.playerId);

		if (this.stats != null) {
			this.stats.write(stream);
		}

		int invSize = 0;
		if (this.inventory != null) {
			invSize = this.inventory.length;
		}
		stream.writeShort(invSize);

		for (int i = 0; i < invSize; i++) {
			if(this.inventory[i]!=null) {
				this.inventory[i].write(stream);
			}else {
				stream.writeInt(-1);
			}
		}
	}
	
	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		this.playerId = dis.readLong();
		this.stats = new Stats().read(dis);
		int invSize = dis.readShort();

		if (invSize > 0) {
			this.inventory = new GameItem[invSize];

			for (int i = 0; i < invSize; i++) {
				//if(stream.available()>0) {
					this.inventory[i] = new GameItem().read(dis);
				//}
			}
		} else {
			this.inventory = new GameItem[20];
		}
	}

	public UpdatePacket fromPlayer(Player player) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		
		DataOutputStream stream = new DataOutputStream(byteStream);
		stream.writeLong(player.getId());

		if (player.getStats() != null) {
			player.getStats().write(stream);
		}

		int invSize = 0;
		if (player.getInventory() != null) {
			invSize = player.getInventory().length;
		} 
		stream.writeShort(invSize);
		

		for (int i = 0; i < invSize; i++) {
			GameItem item = player.getInventory()[i];
			if(item!=null) {
				player.getInventory()[i].write(stream);
			}else {
				stream.writeInt(-1);
			}
		}
		
		return new UpdatePacket((byte) 2, byteStream.toByteArray());
	}
}
