package com.theneuron.pricer.mock;

import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public final class UUIDSupplierQueued implements Supplier<UUID> {

    private final Queue<UUID> queue;

    public UUIDSupplierQueued(Queue<UUID> queue) {
        this.queue = queue;
    }

    public final UUID putRandom() {
        UUID random = UUID.randomUUID();
        log.debug("putin: {}", random);
        queue.add(random);
        return random;
    }

    public final void clear() {
        log.debug("clearing size: {}", queue.size());
        queue.clear();
    }

    @Override
    public UUID get() {
        UUID result = queue.poll();
        log.debug("got: {}", result);
        return result;
    }
}
