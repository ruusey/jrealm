package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class UnloadPacket extends Packet{
	private long[] players;
	private long[] bullets;
	private long[] enemies;
	private long[] containers;
	
	public UnloadPacket() {

	}

	public UnloadPacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse LoadPacket packet, Reason: {}", e);
		}
	}

	
	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		int playersSize = dis.readInt();
		this.players = new long[playersSize];
		for(int i = 0; i< playersSize; i++) {
			this.players[i] = dis.readLong();
		}
		
		int containersSize = dis.readInt();
		this.containers = new long[containersSize];
		for(int i = 0; i< containersSize; i++) {
			this.containers[i] = dis.readLong();
		}
		
		int bulletsSize = dis.readInt();
		this.bullets = new long[bulletsSize];
		for(int i = 0; i < bulletsSize ; i++) {
			this.bullets[i] = dis.readLong();
		}
		
		int enemiesSize = dis.readInt();
		this.enemies = new long[enemiesSize];
		for(int i = 0; i < enemiesSize; i++) {
			this.enemies[i] = dis.readLong();
		}
		
	}
	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");

		this.addHeader(stream);
		stream.writeInt(this.players.length);
		for(long p : this.players) {
			stream.writeLong(p);
		}
		
		stream.writeInt(this.containers.length);
		for(long l : this.containers) {
			stream.writeLong(l);
		}
		
		stream.writeInt(this.bullets.length);
		for(long b : this.bullets) {
			stream.writeLong(b);
		}
		
		stream.writeInt(enemies.length);
		for(long e: enemies) {
			stream.writeLong(e);
		}
	}
	
	public static UnloadPacket from(Long[] players, Long[] containers, Long[] bullets, Long[] enemies) throws Exception{
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(byteStream);
		stream.writeInt(players.length);
		for(long p : players) {
			stream.writeLong(p);
		}
		
		stream.writeInt(containers.length);
		for(long l : containers) {
			stream.writeLong(l);
		}
		
		stream.writeInt(bullets.length);
		for(long b : bullets) {
			stream.writeLong(b);
		}
		
		stream.writeInt(enemies.length);
		for(long e: enemies) {
			stream.writeLong(e);
		}
		
		return new UnloadPacket(PacketType.UNLOAD.getPacketId(), byteStream.toByteArray());
	}	
}