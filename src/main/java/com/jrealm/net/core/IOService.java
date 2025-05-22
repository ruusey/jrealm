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
import java.util.Map.Entry;

import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.NetConstants;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.core.converters.*;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.entity.NetTile;
import com.jrealm.util.ClasspathInspector;

import lombok.extern.slf4j.Slf4j;
/** 
 * @author Robert Usey
 * <br>
 * <p>IOService.class - Service  layer for reading and writing JNET
 * objects to and from and input/output streams (typically for use with sockets)
 * </p>
 * <p>
 * IOService is in conjunction with {@link Streamable} packet objects
 * to build robust socket-based communication applications in a manner similar to gRPC.
 * </p>
 * <p>
 * Packet objects are classes extending the {@link Packet}  superclass and
 * made up of {@link SerialzableField} members where a {@link SerialzableField} 
 * is simply a member that is also {@link Streamable}
 * </p>
 * <p>
 * {@link SerializableField} Packet members must have 3 properties 
 * <b> order, type, isCollection (default false)</b> where <b>order</b>
 * denotes the serialization order of the field when going down the wire
 * as bytes, <b>type</b> denotes the POJO class to map to/from bytes
 * and <b>isCollection</b> which tells JNET whether to write the object
 * as a collection of object or as a single value
 * (collections have an extra 4 byte int32 length value written at the 
 * start of serialization)
 * <b>examples:</b>
 * </p>
 * <pre>
 * <code>
 *{@link @SerializableField}(order = 0, type = NetTile.class, isCollection=true)
 * private NetTile[] tiles;
 *   
 *{@link @SerializableField}(order = 1, type = SerializableLong.class)
 * private long realmId;
 * </code>
 * </pre>
 * <p> 
 * Once you have built your packet model and any accompanying SerializableField models
 * you can read and write your object do a socket input or output stream using:
 * <b>note:</b> all {@link SerializableField} members are automatically written and read
 * to/from the stream automatically when reading/writing the containing packet
 * </p>
 * </br>
 * <b> Read Usage:</b>
 * <pre>
 *     <code>
 * final byte[] data = ...
 * final MyPacket readPacket = IOService.readPacket(MyPacket.class, data);
 *     </code>
 * </pre>
 * <b>Write Usage:</b>
 * <pre>
 *     <code> 
 * final DataOutputStream outputStream = ...
 * final MyPacket toWrite = ...
 * IOService.writePacket(toWrite, outputStream);
 *     </code>
 * </pre>
 *     
 */
@Slf4j
@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
public class IOService {
	public static ModelMapper MAPPER = new ModelMapper();
	private static final Lookup lookup = MethodHandles.lookup();
	public static Map<Class<?>, List<PacketMappingInformation>> MAPPING_DATA = new HashMap<>();
	
	static {
		try {
			registerModelConverter(new ShortToEffectTypeConverter());
			registerModelConverter(new EffectTypeToShortConverter());
			registerModelConverter(new ByteToLootTierConverter());
			registerModelConverter(new LootTierToByteConverter());
		}catch(Exception e) {
			log.error("[IOService] Failed to register custom mapper. Reason: {}", e.getMessage());
		}
	}
	
	// Register a model mapper custom converter to aid in transforming
	// byte level data structures into POJOs
	public static void registerModelConverter(AbstractConverter converter) throws Exception {
		MAPPER.addConverter(converter);
	}

	public static <T> T readPacket(Class<? extends Packet> clazz, byte[] data) throws Exception {
		final ByteArrayInputStream bis = new ByteArrayInputStream(data);
		final DataInputStream dis = new DataInputStream(bis);
		final byte packetIdRead = removeHeader(dis);
		final Packet read = ((Packet)readStream(clazz, dis));
		read.setId(packetIdRead);
		return (T) read;
	}

	public static <T> T readPacket(Class<? extends Packet> clazz, DataInputStream stream) throws Exception {
		final byte packetIdRead = removeHeader(stream);
		return readStream(clazz, stream);
	}

	public static byte[] writePacket(Packet packet, DataOutputStream stream) throws Exception {
		// Write the data bytes to a separate output stream so we know what 
		// length to include in the header. Look into preserving the original packet data
		// underlying byte array to use its length instead of calculating it at write time
		final ByteArrayOutputStream tempByteStream = new ByteArrayOutputStream();
		final DataOutputStream tempOutStream = new DataOutputStream(tempByteStream);
		writeStream(packet, tempOutStream);

		final ByteArrayOutputStream byteStreamFinal = new ByteArrayOutputStream();
		final DataOutputStream streamfinal = new DataOutputStream(byteStreamFinal);
		// Write header
		addHeader(packet, tempByteStream.toByteArray().length, streamfinal);
		// Write data
		streamfinal.write(tempByteStream.toByteArray());
		// Write result to output stream
		stream.write(byteStreamFinal.toByteArray());
		// return written bytes length
		return byteStreamFinal.toByteArray();
	}

	public static int writeStream(Object model, DataOutputStream stream0) throws Exception {
		final List<PacketMappingInformation> mappingInfo = MAPPING_DATA.get(model.getClass());
		if (log.isDebugEnabled())
			log.info("[IOService::WRITE] class {} begin. ToWrite = {}", model.getClass(), model);
		if (mappingInfo == null) {
			log.error("[IOService::WRITE] NO MAPPING FOR CLASS {}", model.getClass());
			return 0;
		}
		int bytesWritten = 0;
		for (PacketMappingInformation info : mappingInfo) {
			if (log.isDebugEnabled())
				log.info("[IOService::WRITE] Begin write mapping for MODEL {} field {}", model.getClass(),
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
			log.info("[IOService::READ] class {} begin. CurrentRessults = {}", clazz, result);
		if (result == null) {
			final Object packet = clazz.getDeclaredConstructor().newInstance();
			result = packet;
		}

		// For each network serializable field in the class
		for (PacketMappingInformation info : mappingInfo) {
			if (log.isDebugEnabled())
				log.info("[IOService::READ] Begin read mapping for MODEL {} field {}", clazz, info.getPropertyHandle().varType());

			final SerializableFieldType<?> serializer = info.getSerializer();
			// Basic collection handling (write collection length followed by each entity)
			if (info.isCollection()) {
				if (log.isDebugEnabled())
					log.info("[IOService::READ] Field {} is a collection. Target class = {}[]", info.getPropertyHandle().varType(),
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
		Entry<Byte, Class<? extends Packet>> targetPacket = PacketType.valueOf(packet.getClass());
		if(targetPacket==null) {
			System.out.println();
		}
		stream.writeByte(targetPacket.getKey());
		stream.writeInt(dataSize + NetConstants.PACKET_HEADER_SIZE);
	}

	public static byte removeHeader(DataInputStream stream) throws Exception {
		// read the first byte of the stream as packetId
		final byte packetId = stream.readByte();
		// read the next 4 bytes of the stream as signed int32
		final int len = stream.readInt();
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

	// Map annotated JNET entity members into an in memory map
	// for runtime serialization/deserialization from byte streams
	public static void mapSerializableData() throws Exception {
		log.info("[IOService::INIT] Loading classes to map packet data");
		final List<Class<?>> packetsToMap = IOService.getClassesOnClasspath();

		for (Class<?> clazz : packetsToMap) {
			// If not streamable at all dont bother
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
									"[IOService::INIT] Successfully located serializable packet field in Class {}. Field: {}. Serializer: {}. isCollection: {}. Order: {}",
									clazz.getName(), objField.getName(), serializer.getClass(), isCollection, order);

							final PacketMappingInformation mappingInfo = PacketMappingInformation.builder()
									.propertyHandle(fieldHandle).order(order).serializer(serializer)
									.isCollection(isCollection).build();
							mappingForClass.add(mappingInfo);
						} catch (Exception e) {
							log.error("[IOService::INIT] **CRITICAL** Failed parsing serializable types in packets. Reason: {}", e);
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
		log.info("[IOService::INIT] Mapping completed");
	}

	// Check if the class is annotated @Streamable
	public static boolean isStreamableClass(Class<?> clazz) {
		if(clazz==null) return false;
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
