package com.jrealm.net.server.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Streamable
public class TextPacket extends Packet {
	@SerializableField(order = 0, type = SerializableString.class)
    private String from;
	@SerializableField(order = 1, type = SerializableString.class)
    private String to;
	@SerializableField(order = 2, type = SerializableString.class)
    private String message;

    public TextPacket() {

    }

    public TextPacket(byte packetId, byte[] data) {
        super(packetId, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            log.error("Failed to create Text Packet. Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final TextPacket read = IOService.readPacket(getClass(), data);
    	this.from = read.getFrom();
    	this.to = read.getTo();
    	this.message = read.getMessage();
    	this.setId(PacketType.TEXT.getPacketId());
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
    }

    public static TextPacket from(String from, String to, String text) throws Exception {
    	final TextPacket read = new TextPacket();
    	read.setFrom(from);
    	read.setTo(to);
    	read.setMessage(text);
    	read.setId(PacketType.TEXT.getPacketId());
        return read;
    }

    public static TextPacket create(String from, String to, String text) {
        TextPacket created = null;
        try {
            created = from(from, to, text);
        } catch (Exception e) {
            log.error("Failed to create Text Packet. Reason: {}", e);
        }
        return created;
    }

    public TextPacket clone() {
        try {
            return TextPacket.from(this.from, this.to, this.message);
        } catch (Exception e) {
            return null;
        }
    }
}
