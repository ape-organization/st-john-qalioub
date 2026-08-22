package com.stjohn.qalioub.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SeatExportCache {

    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    public String store(byte[] data) {
        String token = UUID.randomUUID().toString();
        cache.put(token, data);
        return token;
    }

    public byte[] getAndRemove(String token) {
        return cache.remove(token);
    }
}
