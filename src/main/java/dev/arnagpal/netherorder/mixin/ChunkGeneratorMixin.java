package dev.arnagpal.netherorder.mixin;

import dev.arnagpal.netherorder.Picks;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.World;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    // each nether chunk starts with its own fresh weighted lists
    @Inject(method = "generateFeatures", at = @At("HEAD"))
    private void netherOrder$freshPicks(ChunkRegion region, StructureAccessor accessor, CallbackInfo ci) {
        if (region.getWorld().getRegistryKey() == World.NETHER) {
            Picks.begin();
        }
    }

    @Inject(method = "generateFeatures", at = @At("RETURN"))
    private void netherOrder$endPicks(ChunkRegion region, StructureAccessor accessor, CallbackInfo ci) {
        Picks.end();
    }
}
