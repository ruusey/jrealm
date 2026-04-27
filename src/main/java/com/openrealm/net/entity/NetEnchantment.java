package com.openrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.openrealm.net.Streamable;
import com.openrealm.net.core.SerializableFieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Streamable
public class NetEnchantment extends SerializableFieldType<NetEnchantment> {
    private byte statId;
    private byte deltaValue;
    private byte pixelX;
    private byte pixelY;
    private int pixelColor;

    @Override
    public int write(NetEnchantment value, DataOutputStream stream) throws Exception {
        final NetEnchantment v = value == null ? new NetEnchantment() : value;
        stream.writeByte(v.statId);
        stream.writeByte(v.deltaValue);
        stream.writeByte(v.pixelX);
        stream.writeByte(v.pixelY);
        stream.writeInt(v.pixelColor);
        return 8;
    }

    @Override
    public NetEnchantment read(DataInputStream stream) throws Exception {
        final NetEnchantment v = new NetEnchantment();
        v.statId = stream.readByte();
        v.deltaValue = stream.readByte();
        v.pixelX = stream.readByte();
        v.pixelY = stream.readByte();
        v.pixelColor = stream.readInt();
        return v;
    }
}
