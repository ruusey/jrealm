package com.jrealm.net.test;

import com.jrealm.net.Packet;

public abstract class TestService {
	
	@PacketHandler(TestPacket.class)
	public static Packet invoke(Packet ref) {
		return TestPacket.generate();
	}
}
