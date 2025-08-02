package com.jrealm.net.test;

import java.io.DataOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.IntFunction;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.*;

import lombok.AllArgsConstructor;
import lombok.Builder;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Streamable
@Slf4j
@PacketId(packetId = (byte)101)
public class TestPacket extends Packet {
	@SerializableField(order = 0, type = SerializableString.class)
	private String test0;

	@SerializableField(order = 1, type = SerializableString.class)
	private String test1;

	@SerializableField(order = 2, type = SerializableLong.class)
	private Long test2;

	@SerializableField(order = 3, type = SerializableShort.class)
	private Short test3;

	@SerializableField(order = 4, type = SerializableBoolean.class, isCollection = true)
	private Boolean[] test4;

	public TestPacket(byte id, byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse ObjectMove packet, Reason: {}", e);
		}
	}

	public TestPacket(byte id, byte[] data, String srcIp) {
		super(id, data, srcIp);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse ObjectMove packet, Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		final TestPacket read = IOService.readPacket(getClass(), data);
		this.test0 = read.getTest0();
		this.test1 = read.getTest1();
		this.test2 = read.getTest2();
		this.test3 = read.getTest3();
		this.test4 = read.getTest4();
		this.setId((byte) 99);
	}

	@Override
	public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
	}

	public static TestPacket generate() {
		final SecureRandom r = new SecureRandom();
		final int boolArrSize = r.nextInt(20)+10;
		final Boolean[] b = new Boolean[boolArrSize];
		final IntFunction<Boolean> randBool = (val)->{
			return r.nextBoolean();
		};
		Arrays.parallelSetAll(b, randBool);
		return TestPacket.builder().test0(UUID.randomUUID().toString()).test1(UUID.randomUUID().toString())
				.test2(r.nextLong()).test3((short) r.nextInt(Short.MAX_VALUE)).test4(b).build();
	}


}
