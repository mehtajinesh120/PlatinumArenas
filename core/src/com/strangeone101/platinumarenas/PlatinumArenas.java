package com.strangeone101.platinumarenas;

import com.strangeone101.platinumarenas.blockentity.WrapperRegistry;
import com.strangeone101.platinumarenas.region.DefaultRegionSelection;
import com.strangeone101.platinumarenas.region.IRegionSelection;
import com.strangeone101.platinumarenas.region.WorldEditRegionSelection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class PlatinumArenas extends JavaPlugin {

    public static PlatinumArenas INSTANCE;

    public static final String PREFIX = ChatColor.RED + "[" + ChatColor.GRAY + "PlatinumArenas" + ChatColor.RED + "]";

    public static final boolean DEBUG = false;

    public static UUID DEFAULT_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private IRegionSelection regionSelection;

    protected boolean ready;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        WrapperRegistry.registerAll();

        ArenaCommand.createCommands();
        getCommand("platinumarenas").setExecutor(ArenaCommand.getCommandExecutor());
        getCommand("platinumarenas").setTabCompleter(ArenaCommand.getTabCompleter());
        Bukkit.getPluginManager().registerEvents(new ArenaListener(), this);

        File folder = new File(getDataFolder(), "Arenas");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        if (!ConfigManager.setup()) {
            getLogger().warning("Internal defaults will be used due to config not being loaded!");
        }

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            regionSelection = new WorldEditRegionSelection();
        } else {
            regionSelection = new DefaultRegionSelection();
        }

        getLogger().info("PlatinumArenas Enabled!");
        getLogger().info("Loading arenas... this will be done async.");

        TimerManager.reload();
        async(ArenaIO::loadAllArenas);
    }

    @Override
    public void onDisable() {
        TimerManager.shutdown();
    }

    /**
     * Call method async
     * @param callable Method
     */
    public static void async(Callable<?> callable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(PlatinumArenas.INSTANCE);
    }

    public static void async(Runnable runnable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskAsynchronously(PlatinumArenas.INSTANCE);
    }

    public static CompletableFuture<Void> sync(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture();
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
                future.complete(null);
            }
        }.runTask(PlatinumArenas.INSTANCE);
        return future;
    }

    public IRegionSelection getRegionSelection() {
        return regionSelection;
    }

    /**
     * Whether the plugin is ready to use arenas.
     * @return True when all arenas have been loaded
     */
    public boolean isReady() {
        return ready;
    }

    public static String getMCVersion() {
        return Bukkit.getBukkitVersion().split("-", 2)[0];
    }

    public static int getIntVersion(String version) {

        if (!version.matches("\\d+\\.\\d+(\\.\\d+)?(\\.build\\.\\d+)?.+")) {
            PlatinumArenas.INSTANCE.getLogger().warning("Version not valid! Cannot parse version \"" + version + "\"");

            return 1211; //1.21.1
        }

        String[] split = version.split("\\.", 5);

        int major = Integer.parseInt(split[0]);
        int minor = 0;
        int fix = 0;

        if (split.length > 1) {
            minor = Integer.parseInt(split[1]);

            if (split.length > 2) {
                // Some server forks (e.g. Leaf) report a Bukkit version like
                // "26.2.build.117-alpha", where the 3rd segment is the literal
                // word "build" rather than a numeric patch/fix version. Guard
                // against that instead of crashing with a NumberFormatException.
                try {
                    fix = Integer.parseInt(split[2]);
                } catch (NumberFormatException ignored) {
                    fix = 0;
                }
            }
        }

        return major * 1000 + minor * 10 + fix; //1.16.4 -> 1164; 1.18 -> 1180
    }

    public static int getMCVersionInt() {
        return getIntVersion(getMCVersion());
    }

    public static void debug(String string) {
        if (DEBUG) {
            PlatinumArenas.INSTANCE.getLogger().info(string);
        }
    }
}
