package com.mapsyncer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapsyncer.MapSyncer;
import com.mapsyncer.mca.DimensionTypeInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mod 配置类
 *
 * <p>管理 MapSyncer for XaeroWorldMap 的服务器端配置，包括:</p>
 * <ul>
 *   <li>通用设置（调试日志、并发限制等）</li>
 *   <li>增量更新设置（更新模式、时间间隔）</li>
 *   <li>维度扫描配置（扫描模式、起始高度等）</li>
 * </ul>
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mapsyncer.json");

    public static final ServerConfig SERVER = new ServerConfig();

    /**
     * 加载配置文件
     */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ServerConfig loaded = GSON.fromJson(json, ServerConfig.class);
                if (loaded != null) {
                    SERVER.copyFrom(loaded);
                }
                MapSyncer.LOGGER.info("Loaded config from {}", CONFIG_PATH);
            } catch (IOException e) {
                MapSyncer.LOGGER.error("Failed to load config, using defaults", e);
            }
        } else {
            save();
        }
    }

    /**
     * 保存配置文件
     */
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(SERVER);
            Files.writeString(CONFIG_PATH, json);
            MapSyncer.LOGGER.info("Saved config to {}", CONFIG_PATH);
        } catch (IOException e) {
            MapSyncer.LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * 更新模式枚举
     */
    public enum UpdateMode {
        DISABLED,
        TICK,
        SCHEDULED
    }

    /**
     * 扫描模式枚举
     */
    public enum ScanMode {
        SURFACE,
        CAVE
    }

    /**
     * 维度扫描配置记录
     */
    public record DimensionScanConfig(
        String dimension,
        ScanMode scanMode,
        int caveStart,
        DimensionTypeInfo dimTypeInfo
    ) {
        public DimensionScanConfig(String dimension, ScanMode scanMode, int caveStart) {
            this(dimension, scanMode, caveStart, null);
        }

        public int getCaveLayer() {
            if (scanMode == ScanMode.SURFACE) {
                return Integer.MAX_VALUE;
            }
            if (caveStart == Integer.MAX_VALUE || caveStart == Integer.MIN_VALUE) {
                return caveStart;
            }
            return caveStart >> 4;
        }

        public int getCaveDepth(int minBuildHeight) {
            if (scanMode == ScanMode.SURFACE) {
                return 0;
            }
            return Math.max(30, caveStart - minBuildHeight);
        }

        public DimensionTypeInfo getDimensionTypeInfo() {
            if (dimTypeInfo != null) {
                return dimTypeInfo;
            }
            return DimensionTypeInfo.fromDimensionId(dimension);
        }
    }

    /**
     * 服务端配置类
     */
    public static class ServerConfig {
        // General settings
        public boolean enableDebugLogging = false;
        public int maxConcurrentRegions = 4;
        public int maxSyncPacketSize = 262144; // 256KB
        public int syncSpeedLimitKBps = 1024; // 1MB/s

        // Incremental update settings
        public UpdateMode incrementalUpdateMode = UpdateMode.DISABLED;
        public int incrementalUpdateIntervalTicks = 200; // 10 seconds
        public int scheduledUpdateHour = 4;
        public int scheduledUpdateMinute = 0;

        // Dimension scan settings
        public ScanMode defaultScanMode = ScanMode.SURFACE;
        public int defaultCaveStart = 63;
        public List<String> dimensionConfigs = getDefaultDimensionConfigStrings();

        private static List<String> getDefaultDimensionConfigStrings() {
            List<String> defaults = new ArrayList<>();
            defaults.add("minecraft:overworld|SURFACE|63|true|false|-64|384|384");
            defaults.add("minecraft:the_nether|CAVE|63|false|true|0|256|256");
            defaults.add("minecraft:the_end|SURFACE|63|false|false|0|256|256");
            return defaults;
        }

        public void copyFrom(ServerConfig other) {
            this.enableDebugLogging = other.enableDebugLogging;
            this.maxConcurrentRegions = other.maxConcurrentRegions;
            this.maxSyncPacketSize = other.maxSyncPacketSize;
            this.syncSpeedLimitKBps = other.syncSpeedLimitKBps;
            this.incrementalUpdateMode = other.incrementalUpdateMode;
            this.incrementalUpdateIntervalTicks = other.incrementalUpdateIntervalTicks;
            this.scheduledUpdateHour = other.scheduledUpdateHour;
            this.scheduledUpdateMinute = other.scheduledUpdateMinute;
            this.defaultScanMode = other.defaultScanMode;
            this.defaultCaveStart = other.defaultCaveStart;
            this.dimensionConfigs = other.dimensionConfigs;
        }

        public List<DimensionScanConfig> parseDimensionConfigs() {
            List<DimensionScanConfig> result = new ArrayList<>();
            for (String configStr : dimensionConfigs) {
                DimensionScanConfig config = parseConfigString(configStr);
                if (config != null) {
                    result.add(config);
                }
            }
            return result;
        }

        private DimensionScanConfig parseConfigString(String configStr) {
            if (configStr == null || configStr.isEmpty()) {
                return null;
            }

            String[] parts = configStr.split("\\|");
            if (parts.length < 1) {
                return null;
            }

            String dimension = parts[0];
            int caveStart = 63;
            DimensionTypeInfo dimTypeInfo = DimensionTypeInfo.fromDimensionId(dimension);

            try {
                boolean isNewFormat = parts.length > 1 &&
                    (parts[1].equalsIgnoreCase("SURFACE") || parts[1].equalsIgnoreCase("CAVE"));

                int scanModeIndex = isNewFormat ? 1 : 2;
                int caveStartIndex = isNewFormat ? 2 : 3;
                int dimTypeStartIndex = isNewFormat ? 3 : 4;

                String modeStr = parts.length > scanModeIndex ? parts[scanModeIndex] : "SURFACE";

                if (parts.length > caveStartIndex) {
                    caveStart = Integer.parseInt(parts[caveStartIndex]);
                }

                if (parts.length >= dimTypeStartIndex + 5) {
                    boolean hasSkylight = Boolean.parseBoolean(parts[dimTypeStartIndex]);
                    boolean hasCeiling = Boolean.parseBoolean(parts[dimTypeStartIndex + 1]);
                    int minY = Integer.parseInt(parts[dimTypeStartIndex + 2]);
                    int height = Integer.parseInt(parts[dimTypeStartIndex + 3]);
                    int logicalHeight = Integer.parseInt(parts[dimTypeStartIndex + 4]);
                    dimTypeInfo = new DimensionTypeInfo(hasSkylight, hasCeiling, minY, height, logicalHeight);
                }

                ScanMode mode = ScanMode.valueOf(modeStr.toUpperCase());
                return new DimensionScanConfig(dimension, mode, caveStart, dimTypeInfo);
            } catch (NumberFormatException e) {
                return new DimensionScanConfig(dimension, ScanMode.SURFACE, 63, dimTypeInfo);
            } catch (IllegalArgumentException e) {
                return new DimensionScanConfig(dimension, ScanMode.SURFACE, caveStart, dimTypeInfo);
            }
        }

        public DimensionScanConfig getConfigForDimension(String dimensionPath) {
            String normalizedPath = dimensionPath.replace("minecraft:", "").toLowerCase();

            if (normalizedPath.equals("the_nether")) {
                for (DimensionScanConfig config : parseDimensionConfigs()) {
                    String configDim = config.dimension().replace("minecraft:", "").toLowerCase();
                    if (configDim.equals("the_nether")) {
                        return config;
                    }
                }
                return new DimensionScanConfig("minecraft:the_nether", ScanMode.CAVE, 63,
                    DimensionTypeInfo.nether());
            }

            if (normalizedPath.equals("overworld")) {
                for (DimensionScanConfig config : parseDimensionConfigs()) {
                    String configDim = config.dimension().replace("minecraft:", "").toLowerCase();
                    if (configDim.equals("overworld")) {
                        return config;
                    }
                }
                return new DimensionScanConfig("minecraft:overworld", ScanMode.SURFACE, 63,
                    DimensionTypeInfo.overworld());
            }

            if (normalizedPath.equals("the_end")) {
                for (DimensionScanConfig config : parseDimensionConfigs()) {
                    String configDim = config.dimension().replace("minecraft:", "").toLowerCase();
                    if (configDim.equals("the_end")) {
                        return config;
                    }
                }
                return new DimensionScanConfig("minecraft:the_end", ScanMode.SURFACE, 63,
                    DimensionTypeInfo.theEnd());
            }

            for (DimensionScanConfig config : parseDimensionConfigs()) {
                String configDim = config.dimension();
                if (configDim.equalsIgnoreCase(dimensionPath) ||
                    configDim.equalsIgnoreCase("minecraft:" + dimensionPath) ||
                    configDim.replace("minecraft:", "").equalsIgnoreCase(dimensionPath)) {
                    return config;
                }
            }

            DimensionTypeInfo inferredDimType = DimensionTypeInfo.fromDimensionId(dimensionPath);
            return new DimensionScanConfig(dimensionPath, defaultScanMode, defaultCaveStart, inferredDimType);
        }
    }
}
