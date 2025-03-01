package com.jrealm.game.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jrealm.net.Packet;

@Target({ ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketHandlerServer {
    public Class<? extends Packet> value ();
}
