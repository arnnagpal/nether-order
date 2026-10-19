package dev.arnagpal.netherorder;

import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public final class Waves {

    // order the 9 mod-3 classes run in, this one keeps the dependency chains shortest
    private static final int[] RANK = rank(new int[]{0, 1, 2, 5, 3, 4, 7, 6, 8});

    private Waves() {
    }

    public static int wave(int chunkX, int chunkZ) {
        return RANK[Math.floorMod(chunkX, 3) * 3 + Math.floorMod(chunkZ, 3)];
    }

    // every chunk within 2 that has to finish decorating before this one can start
    public static List<ChunkPos> dependencies(ChunkPos pos) {
        int own = wave(pos.x, pos.z);
        List<ChunkPos> earlier = new ArrayList<>();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (wave(pos.x + dx, pos.z + dz) < own) {
                    earlier.add(new ChunkPos(pos.x + dx, pos.z + dz));
                }
            }
        }

        return earlier;
    }

    private static int[] rank(int[] order) {
        int[] rank = new int[order.length];

        for (int i = 0; i < order.length; i++) {
            rank[order[i]] = i;
        }

        return rank;
    }
}
