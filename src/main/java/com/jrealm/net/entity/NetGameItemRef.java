package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Streamable
public class NetGameItemRef extends SerializableFieldType<NetGameItemRef>{
	@SerializableField(order = 1, type = SerializableInt.class)
    private int itemId;
	@SerializableField(order = 2, type = SerializableInt.class)
    private int slotIdx;
	@SerializableField(order = 3, type = SerializableString.class)
    private String itemUuid;
	
}
