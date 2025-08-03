package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.PacketId;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PacketType {
    private static Map<Byte, Class<? extends Packet>> map = new HashMap<>();

    private byte packetId;
    private Class<? extends Packet> packetClass;

    static {
    	try {
    		final List<Class<?>> packetsToMap = IOService.getClassesOnClasspath();
    		for (Class<?> clazz : packetsToMap) {
    			// If not streamable at all dont bother
    			if (!IOService.isStreamableClass(clazz) || !clazz.getSuperclass().equals(Packet.class))
    				continue;
    			
    			Packet packet = (Packet)clazz.getDeclaredConstructor().newInstance();
    			PacketId id = packet.getClass().getAnnotation(PacketId.class);
    			if(id == null) {
    				log.error("[PacketMapper] **CRITICAL** No @PacketId declared for class {}. Skipping ", packet);
    				continue;
    			}
    			if(PacketType.map.get(id.packetId())!=null) {
    				log.error("[PacketMapper] **CRITICAL** Duplicate packet mapping for packetId {} -  Classes {} and {}", id.packetId(), PacketType.map.get(id.packetId()), packet.getClass());
    			}else {
    				log.info("[PacketMapper] Mapped packetId {} to packet class {}", id.packetId(), packet.getClass());

        			PacketType.map.put(id.packetId(), packet.getClass());
    			}
    		}
    	}catch(Exception e) {
    		log.error("[PacketMapper] **CRITICAL** Failed to load packet types from classpath. Reason: {}", e);
    		System.exit(-1);
    	}
    	
    }

    private PacketType(byte entityTypeId, Class<? extends Packet> packetClass) {
        this.packetId = entityTypeId;
        this.packetClass = packetClass;
    }

    public byte getPacketId() {
        return this.packetId;
    }

    public Class<? extends Packet> getPacketClass() {
        return this.packetClass;
    }

    public static Class<? extends Packet> valueOf(byte value) {
        return PacketType.map.get(Byte.valueOf(value));
    }

    public static Class<? extends Packet> valueOf(int value) {
        return PacketType.map.get(Byte.valueOf((byte) value));
    }
    
    public static Entry<Byte, Class<? extends Packet>> valueOf(Class<?> packetClass) {
        return PacketType.map.entrySet().stream().filter(packet->packet.getValue().equals(packetClass)).findAny().orElse(null);
    }
}
