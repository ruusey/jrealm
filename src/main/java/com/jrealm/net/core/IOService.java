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
import java.lang.reflect.Array;
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
import com.jrealm.game.util.ClasspathInspector;
import com.jrealm.net.NetConstants;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.core.converters.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
public class IOService {
	public static ModelMapper MAPPER = new ModelMapper();
	private static final Lookup lookup = MethodHandles.lookup();
	public static Map<Class<?>, List<PacketMappingInformation>> MAPPING_DATA = new HashMap<>();
	
	static {
		MAPPER.addConverter(new ShortToEffectTypeConverter());
		MAPPER.addConverter(new EffectTypeToShortConverter());
		MAPPER.addConverter(new ByteToLootTierConverter());
		MAPPER.addConverter(new LootTierToByteConverter());
	}

	public static <T> T readPacket(Class<? extends Packet> clazz, byte[] data) throws Exception {
		final ByteArrayInputStream bis = new ByteArrayInputStream(data);
		final DataInputStream dis = new DataInputStream(bis);
		final byte packetIdRead = removeHeader(dis);
		return readStream(clazz, dis);
	}

	public static <T> T readPacket(Class<? extends Packet> clazz, DataInputStream stream) throws Exception {
		final byte packetIdRead = removeHeader(stream);
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
		// stream.flush();
		return byteStreamFinal.toByteArray();
	}

	public static int writeStream(Object model, DataOutputStream stream0) throws Exception {
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(model.getClass());
		if (log.isDebugEnabled())
			log.info("[WRITE] class {} begin. ToWrite = {}", model.getClass(), model);
		if (mappingInfo == null) {
			log.error("[WRITE] NO MAPPING FOR CLASS {}", model.getClass());
			return 0;
		}
		int bytesWritten = 0;
		for (PacketMappingInformation info : mappingInfo) {
			if (log.isDebugEnabled())
				log.info("[WRITE] Begin write mapping for MODEL {} field {}", model.getClass(),
						info.getPropertyHandle().varType());
			final SerializableFieldType serializer = info.getSerializer();
			if (info.isCollection()) {
				final Object[] collection = (Object[]) info.getPropertyHandle().get(model);
				final int collectionLength = collection != null ? collection.length : 0;
				stream0.writeInt(collectionLength);
				bytesWritten += NetConstants.INT32_LENGTH;
				for (int i = 0; i < collectionLength; i++) {
					bytesWritten += serializer.write(collection[i], stream0);
				}
			} else {
				final Object obj = info.getPropertyHandle().get(model);
				bytesWritten += serializer.write(obj, stream0);
			}
		}
		return bytesWritten;
	}

	public static <T> T mapModel(Object model, Class<T> target) {
		return MAPPER.map(model, target);
	}

	// Nominate me for a nobel peace prize or somethin
	public static <T> T readStream(Class<?> clazz, DataInputStream stream, Object result) throws Exception {
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(clazz);
		if (log.isDebugEnabled())
			log.info("[READ] class {} begin. CurrentRessults = {}", clazz, result);
		if (result == null) {
			final Object packet = clazz.getDeclaredConstructor().newInstance();
			if (packet instanceof Packet) {
				((Packet) packet).setId(PacketType.valueOf(clazz).getPacketId());
			}
			result = packet;
		}

		// For each network serializable field in the class
		for (PacketMappingInformation info : mappingInfo) {
			if (log.isDebugEnabled())
				log.info("[READ] Begin read mapping for MODEL {} field {}", clazz, info.getPropertyHandle().varType());

			final SerializableFieldType<?> serializer = info.getSerializer();
			// Basic collection handling (write collection length followed by each entity)
			if (info.isCollection()) {
				if (log.isDebugEnabled())
					log.info("[READ] Field {} is a collection. Target class = {}[]", info.getPropertyHandle().varType(),
							info.getPropertyHandle().varType());
				// Read collection length. Always int32
				final int collectionLength = stream.readInt();
				final Object[] collection = (Object[]) Array
						.newInstance(info.getPropertyHandle().varType().getComponentType(), collectionLength);
				// Read each collection element
				for (int i = 0; i < collectionLength; i++) {
					final Object obj = serializer.read(stream);
					collection[i] = obj;
				}
				info.getPropertyHandle().set(result, collection);
			} else {
				final Object obj = serializer.read(stream);
				info.getPropertyHandle().set(result, obj);
			}
		}
		return (T) result;
	}

	public static <T> T readStream(Class<?> clazz, DataInputStream stream) throws Exception {
		return readStream(clazz, stream, null);
	}

	public static <T> T readStream(Class<?> clazz, byte[] stream) throws Exception {
		final ByteArrayInputStream bis = new ByteArrayInputStream(stream);
		final DataInputStream dis = new DataInputStream(bis);
		return readStream(clazz, dis, null);
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

	// Map annotated Net entity members into an in memory map
	// for runtime serdes
	public static void mapSerializableData() throws Exception {
		log.info("Loading classes to map packet data");
		final List<Class<?>> packetsToMap = IOService.getClassesOnClasspath();

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
							// Try to lookup private members within @Streamable classes
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
				// handles cases where the implementor wants to write class fields out 
				// of sequential order
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

	// Check if the class is annotated @Streamable
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
	
	// Load all @Streamable types into a list
	public static List<Class<?>> getClassesOnClasspath() throws Exception {
		final List<Class<?>> classes = new ArrayList<>();
		for (Class<?> clazz : ClasspathInspector.getAllKnownClasses()) {
			if(IOService.isStreamableClass(clazz)) {
				classes.add(clazz);
			}
		}
		return classes;
	}
}
