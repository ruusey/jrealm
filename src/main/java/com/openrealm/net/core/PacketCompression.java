package com.openrealm.net.core;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Transparent packet compression using zlib deflate.
 * Packets over the size threshold are compressed, signaled by
 * setting the high bit (0x80) on the packet ID byte.
 *
 * Format:
 *   Uncompressed: [packetId][4-byte length][payload]
 *   Compressed:   [packetId | 0x80][4-byte compressed length][deflated payload]
 */
public class PacketCompression {
    public static final int COMPRESSION_THRESHOLD = 128;
    public static final byte COMPRESSION_FLAG = (byte) 0x80;

    private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(() -> new Deflater(Deflater.BEST_SPEED));
    private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);
    private static final ThreadLocal<byte[]> COMPRESS_BUF = ThreadLocal.withInitial(() -> new byte[8192]);

    public static boolean isCompressed(byte packetId) {
        return (packetId & COMPRESSION_FLAG) != 0;
    }

    public static byte getRealPacketId(byte packetId) {
        return (byte) (packetId & 0x7F);
    }

    public static byte markCompressed(byte packetId) {
        return (byte) (packetId | COMPRESSION_FLAG);
    }

    /**
     * Compress a serialized frame. Returns the original frame if below threshold
     * or if compression doesn't save space.
     */
    public static byte[] compressFrame(byte[] frame) {
        if (frame.length <= COMPRESSION_THRESHOLD + 5) {
            return frame;
        }
        // Extract header parts
        byte packetId = frame[0];
        int payloadLen = frame.length - 5;

        // Compress just the payload
        Deflater deflater = DEFLATER.get();
        deflater.reset();
        deflater.setInput(frame, 5, payloadLen);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(payloadLen);
        byte[] buf = COMPRESS_BUF.get();
        while (!deflater.finished()) {
            int count = deflater.deflate(buf);
            baos.write(buf, 0, count);
        }
        byte[] compressed = baos.toByteArray();

        // Only use compression if it actually saves space
        if (compressed.length >= payloadLen) {
            return frame;
        }

        // Build compressed frame: [packetId | 0x80][4-byte total length][4-byte original payload size][compressed payload]
        int compressedFrameLen = 5 + 4 + compressed.length; // header + original size + compressed data
        byte[] result = new byte[compressedFrameLen];
        result[0] = markCompressed(packetId);
        int totalLen = compressedFrameLen;
        result[1] = (byte) ((totalLen >> 24) & 0xFF);
        result[2] = (byte) ((totalLen >> 16) & 0xFF);
        result[3] = (byte) ((totalLen >> 8) & 0xFF);
        result[4] = (byte) (totalLen & 0xFF);
        // Original payload size (for inflate buffer allocation)
        result[5] = (byte) ((payloadLen >> 24) & 0xFF);
        result[6] = (byte) ((payloadLen >> 16) & 0xFF);
        result[7] = (byte) ((payloadLen >> 8) & 0xFF);
        result[8] = (byte) (payloadLen & 0xFF);
        System.arraycopy(compressed, 0, result, 9, compressed.length);
        return result;
    }

    /**
     * Decompress a payload from a compressed packet.
     * @param compressedPayload the bytes after the 5-byte header
     * @return the original uncompressed payload
     */
    public static byte[] decompressPayload(byte[] compressedPayload) throws Exception {
        // First 4 bytes are original payload size
        int originalSize = ((compressedPayload[0] & 0xFF) << 24)
                         | ((compressedPayload[1] & 0xFF) << 16)
                         | ((compressedPayload[2] & 0xFF) << 8)
                         |  (compressedPayload[3] & 0xFF);

        byte[] result = new byte[originalSize];
        Inflater inflater = INFLATER.get();
        inflater.reset();
        inflater.setInput(compressedPayload, 4, compressedPayload.length - 4);
        int inflated = inflater.inflate(result);
        if (inflated != originalSize) {
            throw new Exception("Decompression size mismatch: expected " + originalSize + " got " + inflated);
        }
        return result;
    }
}
