package com.jrealm.net.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableIntArray;
import com.jrealm.net.core.nettypes.SerializableShort;
import com.jrealm.net.core.nettypes.SerializableShortArray;
import com.jrealm.net.server.packet.TestPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOService {
	private static final Lookup lookup = MethodHandles.lookup();
	public static Map<Class<?>, List<PacketMappingInformation>> MAPPING_DATA = new HashMap<>();

	public static void write(Object o, DataOutputStream stream) throws Exception {

		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(o.getClass());

		for (PacketMappingInformation info : mappingInfo) {
			final SerializableFieldType<?> serializer = info.getSerializer();
			if (serializer instanceof SerializableBoolean) {

				final Boolean fieldVal = (Boolean) info.getPropertyHandle().get(o);
				((SerializableBoolean) serializer).write(fieldVal, stream);

			} else if (serializer instanceof SerializableShortArray) {

				final short[] fieldVal = (short[]) info.getPropertyHandle().get(o);
				((SerializableShortArray) serializer).write(convertShortArray(fieldVal), stream);

			} else if (serializer instanceof SerializableShort) {

				final Short fieldVal = (Short) info.getPropertyHandle().get(o);
				((SerializableShort) serializer).write(fieldVal, stream);

			} else if (serializer instanceof SerializableIntArray) {
				
				final int[] fieldVal = (int[]) info.getPropertyHandle().get(o);
				((SerializableIntArray) serializer).write(convertIntArray(fieldVal), stream);

			} else if (serializer instanceof SerializableInt) {

				final Integer fieldVal = (Integer) info.getPropertyHandle().get(o);
				((SerializableInt) serializer).write(fieldVal, stream);
			}
		}
	}

	public static <T> T read(Class<? extends Packet> clazz, DataInputStream stream) throws Exception {
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(clazz);
		Packet packet = clazz.getDeclaredConstructor().newInstance();
		for (PacketMappingInformation info : mappingInfo) {
			final SerializableFieldType<?> serializer = info.getSerializer();
			if (serializer instanceof SerializableBoolean) {
				final Boolean fieldVal = ((SerializableBoolean) serializer).read(stream);
				info.getPropertyHandle().set(packet, fieldVal);

			} else if (serializer instanceof SerializableShortArray) {
				final Short[] fieldVal = ((SerializableShortArray) serializer).read(stream);
				info.getPropertyHandle().set(packet, convertShortArray(fieldVal));

			} else if (serializer instanceof SerializableShort) {

				final Short fieldVal = ((SerializableShort) serializer).read(stream);
				info.getPropertyHandle().set(packet, fieldVal);

			} else if (serializer instanceof SerializableIntArray) {
				final Integer[] fieldVal = ((SerializableIntArray) serializer).read(stream);
				info.getPropertyHandle().set(packet, convertIntArray(fieldVal));

			} else if (serializer instanceof SerializableInt) {

				final Integer fieldVal = ((SerializableInt) serializer).read(stream);
				info.getPropertyHandle().set(packet, fieldVal);
			}
		}
		return (T) packet;
	}
	
	public static int[] convertIntArray(Integer[] in) {
		final int[] intArr = new int[in.length];
		for (int i = 0; i < in.length; i++) {
			intArr[i] = in[i];
		}
		return intArr;
	}

	public static short[] convertShortArray(Short[] in) {
		final short[] shortArr = new short[in.length];
		for (int i = 0; i < in.length; i++) {
			shortArr[i] = in[i];
		}
		return shortArr;
	}

	public static Integer[] convertIntArray(int[] in) {
		final Integer[] intArr = new Integer[in.length];
		for (int i = 0; i < in.length; i++) {
			intArr[i] = in[i];
		}
		return intArr;
	}

	public static Short[] convertShortArray(short[] in) {
		final Short[] shortArr = new Short[in.length];
		for (int i = 0; i < in.length; i++) {
			shortArr[i] = in[i];
		}
		return shortArr;
	}

	public static void mapSerializableData() throws Exception {
		final List<Class<?>> packetsToMap = getClassesInPackage("com.jrealm.net.server.packet");
		for (Class<?> clazz : packetsToMap) {
			final List<PacketMappingInformation> mappingForClass = new LinkedList<>();
			final Field[] fieldsToWrite = clazz.getDeclaredFields();
			for (Field objField : fieldsToWrite) {
				objField.setAccessible(true);
				final Annotation[] annots = objField.getAnnotations();
				for (Annotation annot : annots) {
					if (annot instanceof SerializableField) {
						final SerializableField myAnnotation = (SerializableField) annot;
						final int order = myAnnotation.order();
						SerializableFieldType<?> serializer = null;
						try {
							final Lookup tempLookup = MethodHandles.privateLookupIn(clazz, lookup);
							serializer = myAnnotation.type().getDeclaredConstructor().newInstance();
							final VarHandle fieldHandle = tempLookup.findVarHandle(clazz, objField.getName(),
									objField.getType());

							log.info(
									"Successfully located serializable packet field in Class {}. Field: {}. Serializer: {}. Order: {}",
									clazz.getName(), objField.getName(), serializer.getClass(), order);

							final PacketMappingInformation mappingInfo = PacketMappingInformation.builder()
									.propertyHandle(fieldHandle).order(order).serializer(serializer).build();
							mappingForClass.add(mappingInfo);
						} catch (Exception e) {
							log.error("**[CRITICAL]** Failed parsing serializable types in packets. Reason: {}", e);
						}
					}
				}
			}
			if(mappingForClass.size()>0) {
				MAPPING_DATA.put(clazz, mappingForClass);
			}
		}
		testWrite();
	}

	private static void testWrite() {
		try {
			// Test 0
			// Write then Read test packet data via byte stream directly
			// then read that byte stream into the object model (full ser/des)
			// Uses ByteArrayOutputStream under hoot
			long start = System.nanoTime();
			long diff0 = 0l;
			long diff1 = 0l;

			TestPacket test = TestPacket.fromRandom();
			diff0 = (System.nanoTime() - start);
			log.info("********* Time to read/write packet to stream directly {}", diff0);
			log.info("Test Before= {}", test);

			// Test 1
			// Write the packets data using a reflection based model of pre
			// mapped fields in the packet and the type of seriazation to use
			// when sending down wire
			// Read the packet data back in using a ByteArrayStream same as Test 0
			start = System.nanoTime();
			final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			final DataOutputStream stream = new DataOutputStream(byteStream);

			final ByteArrayInputStream byteStream0 = new ByteArrayInputStream(test.getData());
			final DataInputStream stream0 = new DataInputStream(byteStream0);
			
			// Bew reflective write to stream
			IOService.write(test, stream);
			final TestPacket testAfter = IOService.read(TestPacket.class, stream0);
			diff1 = (System.nanoTime() - start);

			log.info("******** Time to write packet to stream using reflection {}", diff1);
			// Matching object data
			log.info("Test After= {}", testAfter);
			
			final double totalDiff = Math.abs(diff0-diff1)/1000000.0d;
			log.info("Difference in nanos={}. ( {}ms ) ({} x faster}", (diff0-diff1), totalDiff, (double)diff0/(double)diff1);
		} catch (Exception e) {
			log.error("Failed write. Reason: {}", e);
		}
	}

	public static List<Class<?>> getClassesInPackage(String packageName) throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);

		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			File directory = new File(resource.toURI());

			if (directory.exists()) {
				for (File file : directory.listFiles()) {
					if (file.isFile() && file.getName().endsWith(".class")) {
						String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
						Class<?> clazz = Class.forName(className);
						classes.add(clazz);
					}
				}
			}
		}
		return classes;
	}

	public static void main(String[] args) {
	}

}
