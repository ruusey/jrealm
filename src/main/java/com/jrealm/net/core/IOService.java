package com.jrealm.net.core;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.NetConstants;
import com.jrealm.net.Packet;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableIntArray;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableLongArray;
import com.jrealm.net.core.nettypes.SerializableShort;
import com.jrealm.net.core.nettypes.SerializableShortArray;
import com.jrealm.net.core.nettypes.SerializableString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOService {
	private static final Lookup lookup = MethodHandles.lookup();
	public static Map<Class<?>, List<PacketMappingInformation>> MAPPING_DATA = new HashMap<>();

	public static <T> T read(Class<? extends Packet> clazz, DataInputStream stream) throws Exception {
		return readStream(clazz, stream);
	}
	
	public static void write(Packet packet, DataOutputStream stream) throws Exception {
    	final ByteArrayOutputStream byteStream0 = new ByteArrayOutputStream();
    	final DataOutputStream stream0 = new DataOutputStream(byteStream0);
		writeStream(packet, stream0);
    	final ByteArrayOutputStream byteStreamFinal = new ByteArrayOutputStream();
    	final DataOutputStream streamfinal = new DataOutputStream(byteStreamFinal);
		addHeader(packet, byteStream0.toByteArray().length, streamfinal);
		streamfinal.write(byteStream0.toByteArray());
		stream.write(byteStreamFinal.toByteArray());
	}
	
	public static void writeStream(Object packet,  DataOutputStream stream0) throws Exception {
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(packet.getClass());

		for (PacketMappingInformation info : mappingInfo) {
			final SerializableFieldType<?> serializer = info.getSerializer();
			if (serializer instanceof SerializableBoolean) {

				final Boolean fieldVal = (Boolean) info.getPropertyHandle().get(packet);
				((SerializableBoolean) serializer).write(fieldVal, stream0);

			} else if (serializer instanceof SerializableShortArray) {

				final short[] fieldVal = (short[]) info.getPropertyHandle().get(packet);
				((SerializableShortArray) serializer).write(convertShortArray(fieldVal), stream0);

			} else if (serializer instanceof SerializableShort) {

				final Short fieldVal = (Short) info.getPropertyHandle().get(packet);
				((SerializableShort) serializer).write(fieldVal, stream0);

			} else if (serializer instanceof SerializableIntArray) {
				
				final int[] fieldVal = (int[]) info.getPropertyHandle().get(packet);
				((SerializableIntArray) serializer).write(convertIntArray(fieldVal), stream0);

			} else if (serializer instanceof SerializableInt) {

				final Integer fieldVal = (Integer) info.getPropertyHandle().get(packet);
				((SerializableInt) serializer).write(fieldVal, stream0);
			}else if (serializer instanceof SerializableLong) {

				final Long fieldVal = (Long) info.getPropertyHandle().get(packet);
				((SerializableLong) serializer).write(fieldVal, stream0);
				
			}else if (serializer instanceof SerializableByte) {

				final Byte fieldVal = (Byte) info.getPropertyHandle().get(packet);
				((SerializableByte) serializer).write(fieldVal, stream0);
				
			}else if (serializer instanceof SerializableString) {

				final String fieldVal = (String) info.getPropertyHandle().get(packet);
				((SerializableString) serializer).write(fieldVal, stream0);
				
			}else if (serializer instanceof SerializableLongArray) {

				final long[] fieldVal = (long[]) info.getPropertyHandle().get(packet);
				((SerializableLongArray) serializer).write(convertLongArray(fieldVal), stream0);
			}
		}
	}
	
	public static <T> T readStream(Class<?> clazz, DataInputStream stream) throws Exception{
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(clazz);
		
		Object packet = clazz.getDeclaredConstructor().newInstance();
		if(packet instanceof Packet) {
			((Packet)packet).setId(PacketType.valueOf(clazz).getPacketId());
		}
		
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
			}else if (serializer instanceof SerializableLong) {

				final Long fieldVal = ((SerializableLong) serializer).read(stream);
				info.getPropertyHandle().set(packet, fieldVal);

			}else if (serializer instanceof SerializableByte) {

				final Byte fieldVal = ((SerializableByte) serializer).read(stream);
				info.getPropertyHandle().set(packet, fieldVal);

			}else if (serializer instanceof SerializableString) {

				final String fieldVal = ((SerializableString) serializer).read(stream);
				info.getPropertyHandle().set(packet, fieldVal);

			}else if (serializer instanceof SerializableLongArray) {
				
				final Long[] fieldVal = ((SerializableLongArray) serializer).read(stream);
				info.getPropertyHandle().set(packet, convertLongArray(fieldVal));

			}
		}
		return (T) packet;
	}
	
    public static void addHeader(Packet packet, int dataSize, DataOutputStream stream) throws Exception {
        stream.writeByte(packet.getId());
        stream.writeInt(dataSize + NetConstants.PACKET_HEADER_SIZE);
    }
    
    public static byte removeHeader(DataInputStream stream) throws Exception {
    	byte packetId = stream.readByte();
    	int len = stream.readInt();
    	return packetId;
    }
    
	public static long[] convertLongArray(Long[] in) {
		final long[] intArr = new long[in.length];
		for (int i = 0; i < in.length; i++) {
			intArr[i] = in[i];
		}
		return intArr;
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
	
	public static Long[] convertLongArray(long[] in) {
		final Long[] intArr = new Long[in.length];
		for (int i = 0; i < in.length; i++) {
			intArr[i] = in[i];
		}
		return intArr;
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
		packetsToMap.addAll(getClassesInPackage("com.jrealm.net.client.packet"));
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
				// Sort the properties to be mapped using the order provided in the annotation handles
				// cases where the implementor wants to write class fields out of sequential order
				Collections.sort(mappingForClass, new Comparator<PacketMappingInformation>() {
				    @Override
				    public int compare(PacketMappingInformation info0, PacketMappingInformation info1) {
				        return info0.getOrder()-info1.getOrder();
				    }
				});
				MAPPING_DATA.put(clazz, mappingForClass);
			}
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
