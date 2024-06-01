package com.jrealm.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Interface that enforces the implementation of stream write and read
 * functionality for a given object
 * 
 * @param <T> - Type of the streamable object
 */
public interface Streamable<T> {
    void write(DataOutputStream stream) throws Exception;

    T read(DataInputStream stream) throws Exception;
}
