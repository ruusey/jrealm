package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;
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

	public static TestPacket fromRandom() throws Exception {
		TestPacket test = new TestPacket();
		test.setId(PacketType.TEST_PACKET.getPacketId());
		int identifier = Realm.RANDOM.nextInt();
		test.setIdentifier(identifier);
		int length = Realm.RANDOM.nextInt(10)+2;
		int[] identifiers = new int[length];
		for (int i = 0; i < length; i++) {
			identifiers[i]=(Realm.RANDOM.nextInt());
		}
		test.setIdentifiers(identifiers);

		short type = (short) Realm.RANDOM.nextInt(Short.MAX_VALUE/2);
		test.setType(type);
		int length0 = Realm.RANDOM.nextInt(10)+2;
		short[] types = new short[length0];
		for (int i = 0; i < length0; i++) {
			types[i]=((short) Realm.RANDOM.nextInt(Short.MAX_VALUE/2));
		}
		test.setTypes(types);
		return test;
	}

	@Override
	public void readData(byte[] data) throws Exception {
    	final ByteArrayInputStream bis = new ByteArrayInputStream(data);
    	final DataInputStream dis = new DataInputStream(bis);
		TestPacket created = IOService.read(TestPacket.class, dis);
		this.identifier = created.getIdentifier();
		this.identifiers = created.getIdentifiers();
		this.type = created.getType();
		this.types = created.getTypes();
		
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		IOService.write(this, stream);
	}
}