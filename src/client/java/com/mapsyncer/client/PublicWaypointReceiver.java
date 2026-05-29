package com.mapsyncer.client;

import com.mapsyncer.network.PacketHandler;
import com.mapsyncer.util.ChatUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PublicWaypointReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicWaypointReceiver.class);
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "mapsyncer-public-waypoints");
        thread.setDaemon(true);
        return thread;
    });

    private PublicWaypointReceiver() {
    }

    public static void handle(PacketHandler.PublicWaypointsPayload payload, ClientPlayNetworking.Context context) {
        Path serverDir = XaeroMapIntegrator.getCurrentServerDirectory();
        WORKER.execute(() -> applyWaypoints(payload, serverDir));
    }

    private static void applyWaypoints(PacketHandler.PublicWaypointsPayload payload, Path serverDir) {
        if (serverDir == null) {
            notifyClient("mapsyncer.waypoints.skipped");
            return;
        }

        Path waypointsFile = findWaypointsFile(serverDir);
        if (waypointsFile == null) {
            notifyClient("mapsyncer.waypoints.skipped");
            return;
        }

        try {
            Files.createDirectories(waypointsFile.getParent());
            List<String> existing = Files.exists(waypointsFile)
                    ? Files.readAllLines(waypointsFile, StandardCharsets.UTF_8)
                    : List.of();
            List<String> merged = new ArrayList<>();
            for (String line : existing) {
                if (!payload.replaceGroup() || !isManagedGroupLine(line, payload.groupName())) {
                    merged.add(line);
                }
            }
            for (PacketHandler.PublicWaypoint waypoint : payload.waypoints()) {
                merged.add(toXaeroLine(waypoint, payload.groupName()));
            }

            Path tmp = waypointsFile.resolveSibling(waypointsFile.getFileName().toString() + ".mapsyncer.tmp");
            Files.write(tmp, merged, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, waypointsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(tmp, waypointsFile, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("Applied {} public waypoints to {}", payload.waypoints().size(), waypointsFile);
            notifyClient("mapsyncer.waypoints.synced");
        } catch (Exception e) {
            LOGGER.error("Failed to apply public waypoints", e);
            notifyClient("mapsyncer.waypoints.failed");
        }
    }

    private static Path findWaypointsFile(Path worldMapServerDir) {
        Path xaeroDir = worldMapServerDir.getParent();
        if (xaeroDir == null) {
            return null;
        }
        Path gameDir = xaeroDir.getParent();
        if (gameDir == null) {
            return null;
        }

        String serverFolder = worldMapServerDir.getFileName().toString();
        Path minimapWaypoints = gameDir.resolve("minimap").resolve("Multiplayer")
                .resolve(serverFolder).resolve("waypoints.txt");
        if (Files.exists(minimapWaypoints) || Files.exists(minimapWaypoints.getParent())) {
            return minimapWaypoints;
        }

        Path legacy = gameDir.resolve("minimap").resolve(serverFolder).resolve("waypoints.txt");
        if (Files.exists(legacy) || Files.exists(legacy.getParent())) {
            return legacy;
        }

        Path worldMapWaypoints = worldMapServerDir.resolve("waypoints.txt");
        if (Files.exists(worldMapWaypoints) || Files.exists(worldMapWaypoints.getParent())) {
            return worldMapWaypoints;
        }
        return null;
    }

    private static boolean isManagedGroupLine(String line, String groupName) {
        String[] parts = line.split(":", -1);
        return parts.length >= 10 && groupName.equals(parts[parts.length - 2]);
    }

    private static String toXaeroLine(PacketHandler.PublicWaypoint waypoint, String fallbackGroup) {
        String set = sanitize(waypoint.set().isBlank() ? fallbackGroup : waypoint.set());
        return String.join(":",
                "waypoint",
                sanitize(waypoint.name()),
                sanitize(waypoint.initial()),
                String.valueOf(waypoint.x()),
                String.valueOf(waypoint.y()),
                String.valueOf(waypoint.z()),
                String.valueOf(waypoint.color()),
                String.valueOf(waypoint.disabled()),
                sanitize(waypoint.type()),
                set,
                sanitize(waypoint.dimension())
        );
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "_";
        }
        return value.replace(':', '_').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static void notifyClient(String key) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(ChatUtils.message(key));
            }
        });
    }
}
