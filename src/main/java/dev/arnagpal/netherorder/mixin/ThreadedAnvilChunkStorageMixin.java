package dev.arnagpal.netherorder.mixin;

import com.mojang.datafixers.util.Either;
import dev.arnagpal.netherorder.OrderedFeatures;
import dev.arnagpal.netherorder.Waves;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {

    @Shadow
    @Final
    private ServerWorld world;

    @Shadow
    @Final
    private ThreadExecutor<Runnable> mainThreadExecutor;

    @Shadow
    protected abstract ChunkHolder getCurrentChunkHolder(long pos);

    @Shadow
    protected abstract CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> generateChunk(ChunkHolder holder, ChunkStatus status);

    @Inject(method = "generateChunk", at = @At("HEAD"), cancellable = true)
    private void netherOrder$waitForEarlierWaves(ChunkHolder holder, ChunkStatus status,
                                                 CallbackInfoReturnable<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> cir) {
        // only nether decoration cares about order
        if (status != ChunkStatus.FEATURES || world.getRegistryKey() != World.NETHER) {
            return;
        }

        // second time through, the earlier waves are done so let it generate normally
        ChunkPos pos = holder.getPos();
        if (OrderedFeatures.READY.remove(pos.toLong())) {
            return;
        }

        List<ChunkPos> earlier = Waves.dependencies(pos);
        CompletableFuture<?>[] waits = new CompletableFuture<?>[earlier.size()];
        for (int i = 0; i < waits.length; i++) {
            waits[i] = netherOrder$decorated(earlier.get(i));
        }

        // once they're all decorated, run this chunk's decoration on the main thread like it normally would
        cir.setReturnValue(CompletableFuture.allOf(waits).thenComposeAsync(done -> {
            OrderedFeatures.READY.add(pos.toLong());
            return generateChunk(holder, status);
        }, mainThreadExecutor));
    }

    private CompletableFuture<?> netherOrder$decorated(ChunkPos pos) {
        // already loaded far enough to decorate, just wait on it
        ChunkHolder holder = getCurrentChunkHolder(pos.toLong());
        if (holder != null && ChunkHolder.getTargetGenerationStatus(holder.getLevel()).isAtLeast(ChunkStatus.FEATURES)) {
            return holder.createFuture(ChunkStatus.FEATURES, (ThreadedAnvilChunkStorage) (Object) this);
        }

        // not loaded, so ticket it up to decoration
        int radius = -ChunkStatus.getTargetGenerationRadius(ChunkStatus.FEATURES);
        if (OrderedFeatures.HELD.merge(pos.toLong(), 1, Integer::sum) == 1) {
            world.getChunkManager().addTicket(OrderedFeatures.TICKET, pos, radius, pos);
        }

        // the holder shows up after the next ticket update, so check back in a bit
        CompletableFuture<Void> later = new CompletableFuture<>();
        OrderedFeatures.RETRY.schedule(() -> mainThreadExecutor.execute(() -> later.complete(null)), 50, TimeUnit.MILLISECONDS);

        return later.thenComposeAsync(v -> netherOrder$decorated(pos), mainThreadExecutor).thenApplyAsync(result -> {
            // last one waiting on this chunk drops the ticket
            int held = OrderedFeatures.HELD.getOrDefault(pos.toLong(), 0);
            if (held <= 1) {
                OrderedFeatures.HELD.remove(pos.toLong());
                world.getChunkManager().removeTicket(OrderedFeatures.TICKET, pos, radius, pos);
            } else {
                OrderedFeatures.HELD.put(pos.toLong(), held - 1);
            }

            return result;
        }, mainThreadExecutor);
    }
}
