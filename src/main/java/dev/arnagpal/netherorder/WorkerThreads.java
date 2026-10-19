package dev.arnagpal.netherorder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class WorkerThreads {

    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("nether-order.properties");
    private static final int CORES = Runtime.getRuntime().availableProcessors();

    // what mc picks on its own
    public static final int DEFAULT = MathHelper.clamp(CORES - 1, 1, 7);
    public static final int MAX = Math.max(CORES, DEFAULT);

    // the pool worldgen runs on, made once when mc starts up
    private static ResizableExecutor main;

    // the last thread count picked on the slider
    private static volatile int selected = load();

    // the resize waiting for the slider to settle
    private static ScheduledFuture<?> pending;

    private WorkerThreads() {
    }

    public static ResizableExecutor createMain(String name) {
        main = new ResizableExecutor(name, selected);
        return main;
    }

    public static int get() {
        return selected;
    }

    // dragging only changes the number, the pool resize and save wait until the slider settles
    public static synchronized void set(int threads) {
        threads = MathHelper.clamp(threads, 1, MAX);
        if (threads == selected) {
            return;
        }

        selected = threads;
        if (pending != null) {
            pending.cancel(false);
        }
        pending = OrderedFeatures.RETRY.schedule(WorkerThreads::apply, 250, TimeUnit.MILLISECONDS);
    }

    private static void apply() {
        int threads = selected;

        if (main != null) {
            main.resize(threads);
        }
        save(threads);
    }

    private static int load() {
        Properties properties = new Properties();

        if (Files.exists(CONFIG)) {
            try (Reader reader = Files.newBufferedReader(CONFIG)) {
                properties.load(reader);
            } catch (IOException ignored) {
            }
        }

        // nothing saved yet (or junk in the file) falls back to mc's own pick
        try {
            return MathHelper.clamp(Integer.parseInt(properties.getProperty("workerThreads", "")), 1, MAX);
        } catch (NumberFormatException e) {
            return DEFAULT;
        }
    }

    private static void save(int threads) {
        Properties properties = new Properties();
        properties.setProperty("workerThreads", Integer.toString(threads));

        try {
            Files.createDirectories(CONFIG.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG)) {
                properties.store(writer, "nether-order");
            }
        } catch (IOException ignored) {
        }
    }
}
