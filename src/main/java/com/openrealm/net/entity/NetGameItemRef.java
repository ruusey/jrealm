package com.openrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.openrealm.net.Streamable;
import com.openrealm.net.core.IOService;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.SerializableFieldType;
import com.openrealm.net.core.nettypes.SerializableInt;
import com.openrealm.net.core.nettypes.SerializableString;

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
