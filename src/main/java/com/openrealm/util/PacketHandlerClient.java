package com.openrealm.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.openrealm.net.Packet;

@Target({ ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketHandlerClient {
    public Class<? extends Packet> value ();
}
