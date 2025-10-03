package com.searchlight.infra.util;

import java.util.UUID;

/**
 * Utility for generating and encoding IDs.
 */
public class IdCodec {
    
    /**
     * Generate a unique ID.
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a deterministic ID from a string.
     */
    public static String generateIdFrom(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes()).toString();
    }
}
