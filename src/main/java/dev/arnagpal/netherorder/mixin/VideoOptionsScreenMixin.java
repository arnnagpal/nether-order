package dev.arnagpal.netherorder.mixin;

import dev.arnagpal.netherorder.WorkerThreads;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.options.DoubleOption;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VideoOptionsScreen.class)
public abstract class VideoOptionsScreenMixin {

    @Unique
    private static final DoubleOption WORKER_THREADS = new DoubleOption("nether-order.workerThreads", 1, WorkerThreads.MAX, 1.0f,
            options -> (double) WorkerThreads.get(),
            (options, value) -> WorkerThreads.set((int) Math.round(value)),
            (options, option) -> {
                // read through the option so mods that hold changes until release (sodium) still show the live value
                int threads = (int) Math.round(option.get(options));
                return new LiteralText("Worker Threads: " + threads + (threads == WorkerThreads.DEFAULT ? " (default)" : ""));
            });

    @Shadow
    private ButtonListWidget list;

    // one extra row at the bottom of video settings
    @Inject(method = "init", at = @At("TAIL"))
    private void netherOrder$addThreadSlider(CallbackInfo ci) {
        list.addSingleOptionEntry(WORKER_THREADS);
    }
}
