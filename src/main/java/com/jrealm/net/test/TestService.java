package com.jrealm.net.test;

import com.jrealm.net.Packet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestService {
	
	@PacketHandler(TestPacket.class)
	// Same as [POST] @RequestBody TestPacket request
	public static Packet testMethod(ServerConnectionManager mgr, Packet ref) {
		TestPacket recieved = (TestPacket) ref;
		//log.info("Handler received test packet {}", recieved);
		return TestPacket.generate();
	}
}
