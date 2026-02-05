package cz.gennario.newrotatingheads.cooldown;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cooldowns implements Listener {

    /*
    This was added because on 1.21.1 any spam of interactions could lag the server
     */

    // Map to store player cooldowns with UUID as the key and cooldown timestamp as the value
    public static final ConcurrentHashMap<UUID, Long> ClickCooldown = new ConcurrentHashMap<>();
    public static final long COOLDOWN_TIME = 500;  // Cooldown time in milliseconds

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // When a player joins, set their cooldown timestamp to current time minus COOLDOWN_TIME
        ClickCooldown.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() - COOLDOWN_TIME);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        // Remove player from the cooldown map when they leave
        ClickCooldown.remove(event.getPlayer().getUniqueId());
    }

    // Method to check if a player is within cooldown
    public static boolean isCooldown(UUID playerId) {
        long lastClickTime = ClickCooldown.getOrDefault(playerId, 0L);
        // Return true if the player is still within the cooldown period
        return (System.currentTimeMillis() - lastClickTime) < COOLDOWN_TIME;
    }

    // Method to reset a player's cooldown after their action
    public static void resetCooldown(UUID playerId) {
        ClickCooldown.put(playerId, System.currentTimeMillis());
    }
}