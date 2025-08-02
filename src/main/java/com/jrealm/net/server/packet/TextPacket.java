package com.jrealm.net.server.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class TextPacket extends Packet {
	@SerializableField(order = 0, type = SerializableString.class)
    private String from;
	@SerializableField(order = 1, type = SerializableString.class)
    private String to;
	@SerializableField(order = 2, type = SerializableString.class)
    private String message;

    public static TextPacket from(String from, String to, String text) throws Exception {
    	final TextPacket read = new TextPacket();
    	read.setFrom(from);
    	read.setTo(to);
    	read.setMessage(text);
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

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte) 4;
	}
}
