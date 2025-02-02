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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.NetConstants;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.client.packet.ObjectMovement;
import com.jrealm.net.core.converters.ShortToEnumConverter;
import com.jrealm.net.core.converters.EnumToShortConverter;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;
import com.jrealm.net.core.nettypes.SerializableString;
import com.jrealm.net.entity.NetDamage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOService {
	public static ModelMapper MAPPER = new ModelMapper();
	private static final Lookup lookup = MethodHandles.lookup();
	public static Map<Class<?>, List<PacketMappingInformation>> MAPPING_DATA = new HashMap<>();
	static {
		MAPPER.addConverter(new ShortToEnumConverter());
		MAPPER.addConverter(new EnumToShortConverter());

	}

	public static <T> T readPacket(Class<? extends Packet> clazz, byte[] data) throws Exception {
		final ByteArrayInputStream bis = new ByteArrayInputStream(data);
		final DataInputStream dis = new DataInputStream(bis);
		byte packetIdRead = removeHeader(dis);
		return readStream(clazz, dis);
	}

	public static <T> T readPacket(Class<? extends Packet> clazz, DataInputStream stream) throws Exception {
		byte packetIdRead = removeHeader(stream);
		return readStream(clazz, stream);
	}

	public static byte[] writePacket(Packet packet, DataOutputStream stream) throws Exception {
		final ByteArrayOutputStream byteStream0 = new ByteArrayOutputStream();
		final DataOutputStream stream0 = new DataOutputStream(byteStream0);
		writeStream(packet, stream0);
		final ByteArrayOutputStream byteStreamFinal = new ByteArrayOutputStream();
		final DataOutputStream streamfinal = new DataOutputStream(byteStreamFinal);
		addHeader(packet, byteStream0.toByteArray().length, streamfinal);
		streamfinal.write(byteStream0.toByteArray());
		stream.write(byteStreamFinal.toByteArray());
		stream.flush();
		return byteStreamFinal.toByteArray();
	}

	public static void writeStream(Object model, DataOutputStream stream0) throws Exception {
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(model.getClass());
		if (mappingInfo == null) {
			log.error("NO MAPPING FOR CLASS {}", model.getClass());
			return;
		}
		for (PacketMappingInformation info : mappingInfo) {

			log.info("[WRITE] Begin write mapping for MODEL {} field {}", model.getClass(), info.getPropertyHandle().varType());
			Object[] collection = null;
			int collectionLength = 1;
			if (info.isCollection()) {
				log.info("[WRITE] Field {} is a collection. Target class = {}[]", info.getPropertyHandle().varType(),
						info.getPropertyHandle().varType());
				try {
					collection = (Object[]) info.getPropertyHandle().get(model);
					log.info("[WRITE] Fetched FieldType {} as collection. Length = {}", info.getPropertyHandle().varType(),
							collection.length);

					if (collection != null && collection.length >= 0) {
						collectionLength = collection.length;
						stream0.writeInt(collectionLength);
						log.info("[WRITE] Writing collectionSize. Length = {}", collectionLength);

					} else {
						log.info("[WRITE] Writing empty collectionSize. Length = {}", 0);
						stream0.writeInt(0);
					}

				} catch (Exception e) {
					log.error("[WRITE] Failed to extract collection info frorm class {}", model.getClass());
				}
			}
			for (int i = 0; i < collectionLength; i++) {
				if (info.isCollection()) {
					log.info("[WRITE] Writing Object {} {}/{}", info.getPropertyHandle().varType(), i, collectionLength);
				}
				log.info("[WRITE] Begin write mapping for MODEL {} field {}", model.getClass(),
						info.getPropertyHandle().varType());

				final SerializableFieldType<?> serializer = info.getSerializer();
				if (serializer instanceof SerializableBoolean) {
					Boolean fieldVal = null;
					if (info.isCollection()) {
						fieldVal = (Boolean) collection[i];
					} else {
						fieldVal = (Boolean) info.getPropertyHandle().get(model);
					}
					((SerializableBoolean) serializer).write(fieldVal, stream0);

				} else if (serializer instanceof SerializableShort) {
					Short fieldVal = null;
					if (info.isCollection()) {
						fieldVal = (Short) collection[i];
					} else {
						fieldVal = (Short) info.getPropertyHandle().get(model);
					}
					((SerializableShort) serializer).write(fieldVal, stream0);

				} else if (serializer instanceof SerializableInt) {

					final Integer fieldVal = (Integer) info.getPropertyHandle().get(model);
					((SerializableInt) serializer).write(fieldVal, stream0);

				} else if (serializer instanceof SerializableLong) {
					Long fieldVal = null;
					if (info.isCollection()) {
						fieldVal = (Long) collection[i];
					} else {
						fieldVal = (Long) info.getPropertyHandle().get(model);
					}
					((SerializableLong) serializer).write(fieldVal, stream0);

				} else if (serializer instanceof SerializableByte) {

					final Byte fieldVal = (Byte) info.getPropertyHandle().get(model);
					((SerializableByte) serializer).write(fieldVal, stream0);

				} else if (serializer instanceof SerializableString) {

					final String fieldVal = (String) info.getPropertyHandle().get(model);
					((SerializableString) serializer).write(fieldVal, stream0);

				} else if (serializer instanceof SerializableFloat) {
					final Float fieldVal = (Float) info.getPropertyHandle().get(model);
					((SerializableFloat) serializer).write(fieldVal, stream0);
				}

				else {
					Object value = null;
					log.info("[WRITE] writing sub object {}", info.getPropertyHandle().varType());

					if (info.isCollection()) {
						final Object toWrite = collection[i];
						if (toWrite != null) {
							log.info("[WRITE] adding value {} to collection sub object {}", value,
									info.getPropertyHandle().varType());
							writeStream(collection[i], stream0);
						}
					} else {

						value = info.getPropertyHandle().get(model);
						if (value != null) {
							log.info("[WRITE] wrote Object to Model {}. Value {}", info.getPropertyHandle().varType(),
									value);

							writeStream(value, stream0);
						}
					}
				}
			}
		}
	}

	public static <T> T mapModel(Object model, Class<T> target) {
		return MAPPER.map(model, target);
	}

	public static <T> T readStreamRecursive(Class<?> clazz, DataInputStream stream, Object result) throws Exception {
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(clazz);
		log.info("[READ] class {} begin. CurrentRessults = {}", clazz, result);
		if (result == null) {
			final Object packet = clazz.getDeclaredConstructor().newInstance();
			if (packet instanceof Packet) {

				((Packet) packet).setId(PacketType.valueOf(clazz).getPacketId());
			}
			result = packet;
		}

		for (PacketMappingInformation info : mappingInfo) {
			log.info("[READ] Begin read mapping for MODEL {} field {}", clazz, info.getPropertyHandle().varType());

			Integer collectionLength = 1;
			Object[] collection = null;

			if (info.isCollection()) {
				log.info("[READ] Field {} is a collection. Target class = {}[]", info.getPropertyHandle().varType(),
						info.getPropertyHandle().varType());
				collectionLength = stream.readInt();
				collection = new Object[collectionLength];
			}
			final SerializableFieldType<?> serializer = info.getSerializer();
			for (int i = 0; i < collectionLength; i++) {
				if (info.isCollection()) {
					log.info("[READ] Reading object {} {}/{}", info.getPropertyHandle().varType(), i,
							collectionLength);

				}
				log.info("[READ] Begin write mapping for MODEL {} field {}", clazz,
						info.getPropertyHandle().varType());

				if (serializer instanceof SerializableBoolean) {
					final Boolean fieldVal = ((SerializableBoolean) serializer).read(stream);
					if (info.isCollection()) {
						collection[i] = fieldVal;
					} else {
						info.getPropertyHandle().set(result, fieldVal);
					}
				} else if (serializer instanceof SerializableShort) {
					final Short fieldVal = ((SerializableShort) serializer).read(stream);
					if (info.isCollection()) {
						collection[i] = fieldVal;
					} else {
						info.getPropertyHandle().set(result, fieldVal);
					}
				} else if (serializer instanceof SerializableInt) {
					final Integer fieldVal = ((SerializableInt) serializer).read(stream);
					if (info.isCollection()) {
						collection[i] = fieldVal;
					} else {
						info.getPropertyHandle().set(result, fieldVal);
					}
				} else if (serializer instanceof SerializableLong) {
					final Long fieldVal = ((SerializableLong) serializer).read(stream);
					if (info.isCollection()) {
						collection[i] = fieldVal;
					} else {
						info.getPropertyHandle().set(result, fieldVal);
					}
				} else if (serializer instanceof SerializableByte) {
					final Byte fieldVal = ((SerializableByte) serializer).read(stream);
					if (info.isCollection()) {
						collection[i] = fieldVal;
					} else {
						info.getPropertyHandle().set(result, fieldVal);
					}
				} else if (serializer instanceof SerializableString) {
					final String fieldVal = ((SerializableString) serializer).read(stream);
					if (info.isCollection()) {
						collection[i] = fieldVal;
					} else {
						info.getPropertyHandle().set(result, fieldVal);
					}
				} else if (serializer instanceof SerializableFloat) {
					final Float fieldVal = ((SerializableFloat) serializer).read(stream);
					if (info.isCollection()) {
						collection[i] = fieldVal;
					} else {
						info.getPropertyHandle().set(result, fieldVal);
					}
				} else {
					if (serializer instanceof NetDamage) {
						int h = 0;
					}
					final Object subObject = serializer.read(stream);
					log.info("[READ] reading sub object {}", subObject);
					// Object read = readStreamRecursive(subObject.getClass(), stream, subObject);
					if (subObject != null && info.isCollection()) {
						log.info("[READ] Sub Object {} Added to collection at index {}", subObject, i);

						collection[i] = subObject;
					} else if (subObject != null) {
						log.info("[READ] Sub Object {} set on {}", subObject, result);

						info.getPropertyHandle().set(result, subObject);
					}
				}
			}
			if (info.isCollection()) {
				info.getPropertyHandle().set(result,
						IOService.mapModel(collection, info.getPropertyHandle().varType()));
			}
		}
		return (T) result;
	}

	public static <T> T readStream(Class<?> clazz, DataInputStream stream) throws Exception {
		return readStreamRecursive(clazz, stream, null);
	}

	public static <T> T readStream(Class<?> clazz, byte[] stream) throws Exception {
		final ByteArrayInputStream bis = new ByteArrayInputStream(stream);
		final DataInputStream dis = new DataInputStream(bis);
		return readStreamRecursive(clazz, dis, null);
	}

	public static void addHeader(Packet packet, int dataSize, DataOutputStream stream) throws Exception {
		stream.writeByte(PacketType.valueOf(packet.getClass()).getPacketId());
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
		final List<Class<?>> packetsToMap = getClassesInPackage("com.jrealm.net.client.packet");
		packetsToMap.addAll(getClassesInPackage("com.jrealm.net.server.packet"));
		packetsToMap.addAll(getClassesInPackage("com.jrealm.net.entity"));
		packetsToMap.addAll(getClassesInPackage("com.jrealm.game.math"));

		for (Class<?> clazz : packetsToMap) {
			if (!isStreamableClass(clazz))
				continue;
			final List<PacketMappingInformation> mappingForClass = new LinkedList<>();
			final Field[] fieldsToWrite = clazz.getDeclaredFields();
			for (Field objField : fieldsToWrite) {
				objField.setAccessible(true);
				final Annotation[] annots = objField.getAnnotations();
				for (Annotation annot : annots) {
					if (annot instanceof SerializableField) {
						final SerializableField serdesAnnotation = (SerializableField) annot;
						final int order = serdesAnnotation.order();
						SerializableFieldType<?> serializer = null;
						try {
							final Lookup tempLookup = MethodHandles.privateLookupIn(clazz, lookup);
							final Class<? extends SerializableFieldType<?>> serializerType = serdesAnnotation.type();
							final boolean isCollection = serdesAnnotation.isCollection();

							serializer = serializerType.getDeclaredConstructor().newInstance();
							final VarHandle fieldHandle = tempLookup.findVarHandle(clazz, objField.getName(),
									objField.getType());

							log.info(
									"Successfully located serializable packet field in Class {}. Field: {}. Serializer: {}. isCollection: {}. Order: {}",
									clazz.getName(), objField.getName(), serializer.getClass(), isCollection, order);

							final PacketMappingInformation mappingInfo = PacketMappingInformation.builder()
									.propertyHandle(fieldHandle).order(order).serializer(serializer)
									.isCollection(isCollection).build();
							mappingForClass.add(mappingInfo);
						} catch (Exception e) {
							log.error("**[CRITICAL]** Failed parsing serializable types in packets. Reason: {}", e);
						}
					}
				}
			}
			if (mappingForClass.size() > 0) {
				// Sort the properties to be mapped using the order provided in the annotation
				// handles
				// cases where the implementor wants to write class fields out of sequential
				// order
				Collections.sort(mappingForClass, new Comparator<PacketMappingInformation>() {
					@Override
					public int compare(PacketMappingInformation info0, PacketMappingInformation info1) {
						return info0.getOrder() - info1.getOrder();
					}
				});
				MAPPING_DATA.put(clazz, mappingForClass);
			}
		}
	}

	private static boolean isStreamableClass(Class<?> clazz) {
		boolean result = false;
		for (Annotation annot : clazz.getDeclaredAnnotations()) {
			if (annot instanceof Streamable) {
				result = true;
				break;
			}
		}
		return result;
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
