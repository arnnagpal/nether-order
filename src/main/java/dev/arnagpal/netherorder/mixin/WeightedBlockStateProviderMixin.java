package dev.arnagpal.netherorder.mixin;

import dev.arnagpal.netherorder.Picks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.stateprovider.WeightedBlockStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(WeightedBlockStateProvider.class)
public abstract class WeightedBlockStateProviderMixin {

    // the list's starting order, the shared copy gets reshuffled on every pick
    @Unique
    private final List<BlockState> netherOrder$states = new ArrayList<>();

    @Unique
    private final List<Integer> netherOrder$weights = new ArrayList<>();

    @Inject(method = "addState", at = @At("TAIL"))
    private void netherOrder$remember(BlockState state, int weight, CallbackInfoReturnable<WeightedBlockStateProvider> cir) {
        netherOrder$states.add(state);
        netherOrder$weights.add(weight);
    }

    // pick from this chunk's own copy instead of the shared one
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void netherOrder$pickFromChunkCopy(Random random, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (netherOrder$states.isEmpty()) {
            return;
        }

        BlockState picked = Picks.pick(this, netherOrder$states, netherOrder$weights, random);
        if (picked != null) {
            cir.setReturnValue(picked);
        }
    }
}
