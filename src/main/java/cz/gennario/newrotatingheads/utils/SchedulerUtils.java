package cz.gennario.newrotatingheads.utils;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SchedulerUtils {

    private static boolean isFolia = false;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static void runTask(Plugin plugin, Runnable runnable) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTask(Plugin plugin, Location location, Runnable runnable) {
        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, location, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTask(Plugin plugin, Entity entity, Runnable runnable) {
        if (isFolia) {
            entity.getScheduler().execute(plugin, runnable, null, 1L);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTaskLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (task) -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runTaskLater(Plugin plugin, Location location, Runnable runnable, long delayTicks) {
        if (isFolia) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, (task) -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        if (isFolia) {
            entity.getScheduler().runDelayed(plugin, (task) -> runnable.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runTaskTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> runnable.run(), delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    public static void runTaskTimer(Plugin plugin, Location location, Runnable runnable, long delayTicks,
            long periodTicks) {
        if (isFolia) {
            Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, (task) -> runnable.run(), delayTicks,
                    periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    public static void runTaskTimer(Plugin plugin, Entity entity, Runnable runnable, long delayTicks,
            long periodTicks) {
        if (isFolia) {
            entity.getScheduler().runAtFixedRate(plugin, (task) -> runnable.run(), null, delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    public static void runTaskAsync(Plugin plugin, Runnable runnable) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (task) -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static void runTaskLaterAsync(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, (task) -> runnable.run(), delayTicks * 50,
                    TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
        }
    }

    public static void runTaskTimerAsync(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> runnable.run(), delayTicks * 50,
                    periodTicks * 50, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
        }
    }
}
