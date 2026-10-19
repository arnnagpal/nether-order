package dev.arnagpal.netherorder;

import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class OrderedFeatures {

    // keeps a chunk we need loaded long enough for it to decorate
    public static final ChunkTicketType<ChunkPos> TICKET = ChunkTicketType.create("nether_order", Comparator.comparingLong(ChunkPos::toLong));

    // just used to wait a bit before checking a chunk again
    public static final ScheduledExecutorService RETRY = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nether-order-retry");
        thread.setDaemon(true);
        return thread;
    });

    // chunks whose dependencies are done, so the next generateChunk call goes straight through
    public static final Set<Long> READY = new HashSet<>();

    // how many chunks are waiting on each ticketed chunk, so the ticket is only removed once
    public static final Map<Long, Integer> HELD = new HashMap<>();

    private OrderedFeatures() {
    }
}
