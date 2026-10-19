package dev.arnagpal.netherorder;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.WeightedList;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Picks {

    // one copy of each weighted list per chunk being decorated on this thread
    private static final ThreadLocal<Map<Object, WeightedList<BlockState>>> COPIES = new ThreadLocal<>();

    private Picks() {
    }

    public static void begin() {
        COPIES.set(new IdentityHashMap<>());
    }

    public static void end() {
        COPIES.remove();
    }

    // null means we aren't decorating a nether chunk, so the shared list picks like normal
    public static BlockState pick(Object provider, List<BlockState> states, List<Integer> weights, Random random) {
        Map<Object, WeightedList<BlockState>> copies = COPIES.get();
        if (copies == null) {
            return null;
        }

        // fresh copy in the starting order the first time this chunk uses the list
        WeightedList<BlockState> copy = copies.computeIfAbsent(provider, key -> {
            WeightedList<BlockState> list = new WeightedList<>();

            for (int i = 0; i < states.size(); i++) {
                list.add(states.get(i), weights.get(i));
            }

            return list;
        });

        return copy.pickRandom(random);
    }
}
