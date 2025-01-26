package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.*;
import com.jrealm.net.realm.Realm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TestPacket extends Packet {
	@SerializableField(order = 0, type = SerializableInt.class)
	private int identifier;
	@SerializableField(order = 1, type = SerializableIntArray.class)
	private int[] identifiers;
	@SerializableField(order = 2, type = SerializableShort.class)
	private short type;
	@SerializableField(order = 3, type = SerializableShortArray.class)
	private short[] types;

	public TestPacket(byte id, byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			e.printStackTrace();
			TestPacket.log.error("Failed to build Stats Packet. Reason: {}", e.getMessage());
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		final ByteArrayInputStream bis = new ByteArrayInputStream(data);
		final DataInputStream dis = new DataInputStream(bis);
		if ((dis == null) || (dis.available() < 5))
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		this.identifier = dis.readInt();
		final int idsLength = dis.readInt();
		this.identifiers = new int[idsLength];
		for (int i = 0; i < idsLength; i++) {
			this.identifiers[i] = dis.readInt();
		}

		this.type = dis.readShort();
		final int typesLength = dis.readInt();
		this.types = new short[typesLength];
		for (int i = 0; i < typesLength; i++) {
			this.types[i] = dis.readShort();
		}
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		//this.addHeader(stream);
		stream.writeInt(this.getIdentifier());
		stream.writeInt(this.identifiers.length);
		for (int i = 0; i < this.identifiers.length; i++) {
			stream.writeInt(this.identifiers[i]);
		}

		stream.writeShort(this.getType());
		stream.writeInt(this.types.length);
		for (int i = 0; i < this.types.length; i++) {
			stream.writeShort(this.types[i]);
		}
	}

	public static TestPacket fromRandom() throws Exception {
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		final DataOutputStream stream = new DataOutputStream(byteStream);

		int identifier = Realm.RANDOM.nextInt();
		stream.writeInt(identifier);
		int length = Realm.RANDOM.nextInt(10)+2;
		stream.writeInt(length);
		for (int i = 0; i < length; i++) {
			stream.writeInt(Realm.RANDOM.nextInt());
		}

		short type = (short) Realm.RANDOM.nextInt(Short.MAX_VALUE/2);
		stream.writeShort(type);
		int length0 = Realm.RANDOM.nextInt(10)+2;
		stream.writeInt(length0);
		for (int i = 0; i < length; i++) {
			stream.writeShort((short) Realm.RANDOM.nextInt(Short.MAX_VALUE/2));
		}
		return new TestPacket(PacketType.TEST_PACKET.getPacketId(), byteStream.toByteArray());
	}
}