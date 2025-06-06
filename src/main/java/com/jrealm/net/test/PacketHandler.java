package com.jrealm.net.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jrealm.net.Packet;

@Target({ ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketHandler {
    public Class<? extends Packet> value ();
}
