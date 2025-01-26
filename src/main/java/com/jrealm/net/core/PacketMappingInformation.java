package com.jrealm.net.core;

import java.lang.invoke.VarHandle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PacketMappingInformation {
	private int order;
	private VarHandle propertyHandle;
	private SerializableFieldType<?> serializer;
}