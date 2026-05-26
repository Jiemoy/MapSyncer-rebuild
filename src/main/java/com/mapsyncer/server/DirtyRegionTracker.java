package com.mapsyncer.server;

import com.mapsyncer.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DirtyRegionTracker {
    private static final Set<DirtyRegion> DIRTY_REGIONS = ConcurrentHashMap.newKeySet();

    private DirtyRegionTracker() {
    }

    public static void markDirty(ServerLevel level, BlockPos pos) {
        if (!ModConfig.SERVER.enableDirtyRegionTracking || level == null || pos == null) {
            return;
        }

        ChunkPos chunkPos = ChunkPos.containing(pos);
        Identifier dimensionId = level.dimension().identifier();
        DIRTY_REGIONS.add(new DirtyRegion(dimensionId.toString(), chunkPos.getRegionX(), chunkPos.getRegionZ()));
    }

    public static List<DirtyRegion> takeSnapshot(int maxCount) {
        if (maxCount <= 0 || DIRTY_REGIONS.isEmpty()) {
            return List.of();
        }

        List<DirtyRegion> snapshot = new ArrayList<>(Math.min(maxCount, DIRTY_REGIONS.size()));
        for (DirtyRegion region : DIRTY_REGIONS) {
            snapshot.add(region);
            if (snapshot.size() >= maxCount) {
                break;
            }
        }
        return snapshot;
    }

    public static void markProcessed(DirtyRegion region) {
        DIRTY_REGIONS.remove(region);
    }

    public static void clear() {
        DIRTY_REGIONS.clear();
    }

    public static boolean hasDirtyRegions() {
        return !DIRTY_REGIONS.isEmpty();
    }

    public static int dirtyCount() {
        return DIRTY_REGIONS.size();
    }

    public record DirtyRegion(String dimensionId, int regionX, int regionZ) {
        public boolean matches(ResourceKey<Level> dimension) {
            return dimension != null && Objects.equals(dimension.identifier().toString(), dimensionId);
        }

        public RegionScanner.RegionCoords toRegionCoords() {
            return new RegionScanner.RegionCoords(regionX, regionZ);
        }
    }
}
