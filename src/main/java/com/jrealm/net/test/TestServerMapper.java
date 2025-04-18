package com.jrealm.net.test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class TestServerMapper {
	private Reflections classPathScanner = new Reflections("com.jrealm.net.test", Scanners.SubTypes, Scanners.MethodsAnnotated);
	private MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
	private final Map<Byte, List<Function<Packet, Packet>>> packetCallbacksServer = new HashMap<>();
	private final Map<Byte, List<MethodHandle>> userPacketCallbacksServer = new HashMap<>();

	public TestServerMapper() {
		this.registerPacketCallbacksReflection();
	}
	
	private void registerPacketCallbacksReflection() {
		log.info("Registering packet handlers using reflection");
		final MethodType mt = MethodType.methodType(Packet.class, Packet.class);

		final Set<Method> subclasses = this.classPathScanner.getMethodsAnnotatedWith(PacketHandler.class);
		for (final Method method : subclasses) {
			try {
				final PacketHandler packetToHandle = method.getDeclaredAnnotation(PacketHandler.class);
				MethodHandle handleToHandler = null;
				try {
					handleToHandler = this.publicLookup.findStatic(TestService.class, method.getName(), mt);
				} catch (Exception e) {
					handleToHandler = this.publicLookup.findStatic(TestService.class, method.getName(), mt);
				}

				if (handleToHandler != null) {
					final PacketType targetPacketType = PacketType.valueOf(packetToHandle.value());
					List<MethodHandle> existing = this.userPacketCallbacksServer.get(targetPacketType.getPacketId());
					if (existing == null) {
						existing = new ArrayList<>();
					}
					existing.add(handleToHandler);
					log.info("Added new packet handler for packet {}. Handler method: {}", targetPacketType,
							handleToHandler.toString());
					this.userPacketCallbacksServer.put(targetPacketType.getPacketId(), existing);
				}
			} catch (Exception e) {
				log.error("Failed to get MethodHandle to method {}. Reason: {}", method.getName(), e);
			}
		}
	}
}
