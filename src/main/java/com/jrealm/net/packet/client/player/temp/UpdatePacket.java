package com.jrealm.net.packet.client.player.temp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.server.temp.Packet;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
public class UpdatePacket extends Packet{
	private long playerId;
	private Stats stats;
	private GameItem[] inventory;
	
	
	public UpdatePacket(byte id, byte[] data) {
		super(id, data);
		try {
			final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
			this.readData(dis);
		}catch(Exception e) {
			log.error("Failed to build Stats Packet. Reason: {}", e.getMessage());
		}
	}
	
	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if(this.getId()<1 || this.getData()== null || this.getData().length<5) throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		this.addHeader(stream);
		stream.writeLong(this.playerId);
		
		if(this.stats!=null) {
			this.stats.write(stream);
		}
		int invSize = 0;
		if(this.inventory!=null) {
			invSize = this.inventory.length;
			stream.writeInt(invSize);
		}else {
			stream.writeInt(invSize);

		}

		for(int i = 0; i<invSize; i++) {
			this.inventory[i].write(stream);
		}

	}
	
	@Override
	public void readData(DataInputStream stream) throws Exception {
		if(stream==null || stream.available()<5) throw new IllegalStateException("No Packet data available to read from DataInputStream");
		this.playerId = stream.readLong();
		this.stats = new Stats().read(stream);
		int invSize = stream.readInt();
		
		if(invSize>0) {
			this.inventory = new GameItem[invSize];

			for(int i = 0; i < invSize; i++) {
				this.inventory[i] = new GameItem().read(stream);
			}
		}else {
			this.inventory = new GameItem[20];
		}
			
	}
	
	public UpdatePacket fromPlayer(Player player) throws Exception {
		DataOutputStream stream = new DataOutputStream( new ByteArrayOutputStream());
		this.addHeader(stream);
		stream.writeLong(this.playerId);
		
		if(this.stats!=null) {
			this.stats.write(stream);
		}
		
		if(this.inventory!=null) {
			stream.writeInt(this.inventory.length);
		}else {
			stream.writeInt(0);

		}
		return null;
	}
	
}
