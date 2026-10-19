package dev.arnagpal.netherorder.mixin;

import dev.arnagpal.netherorder.WorkerThreads;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;

@Mixin(Util.class)
public abstract class UtilMixin {

    // swap the main worker pool (the one worldgen uses) for one we can resize later
    @Inject(method = "method_28122", at = @At("HEAD"), cancellable = true)
    private static void netherOrder$resizableWorkers(String name, CallbackInfoReturnable<ExecutorService> cir) {
        if (name.equals("Main")) {
            cir.setReturnValue(WorkerThreads.createMain(name));
        }
    }
}
