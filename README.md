# MapSyncer for Xaero World Map

MapSyncer 用于把服务端已探索或已生成的地图区域同步到客户端的 Xaero World Map。普通地图同步会固定写入 Xaero 的 `mw$default`，避免再生成第二个 `mw$map` 地图；如果客户端已经存在旧的 `mw$map` 或其他历史 `mw$*` 目录，MapSyncer 会在后台把缺失的 `.zip` 地图区域迁移到 `mw$default`，保留旧目录和玩家私有路标。

## 主要功能

- 将服务端生成的 Xaero 地图缓存同步到客户端。
- 普通地图同步固定写入 `mw$default`。
- 后台迁移旧版 `mw$map` 数据到 `mw$default`。
- 支持半径同步，例如 `/mapsyncer sync radius 1000`。
- 支持公共路标同步，并按 Xaero `waypoints.txt` 文本格式安全合并。
- 支持 Voxy 同步。
- 提供客户端 GUI，普通玩家和 OP 管理界面分离。

## 版本信息

- Minecraft / Fabric：`26.1.2`
- Fabric Loader：`0.19.2`
- Fabric API：`0.150.0+26.1.2`
- Fabric Loom：`1.16-SNAPSHOT`
- Gradle Wrapper：`9.4.1`
- Java：`25`
- Mod 版本：`1.0.1-fabric`

## 安装

### 服务端

- Fabric Loader
- Fabric API
- MapSyncer

服务端不需要安装 Xaero's World Map。

### 客户端

- Fabric Loader
- Fabric API
- MapSyncer
- Xaero's World Map
- Voxy：可选，仅在使用 Voxy 同步时需要
- Xaero 路标组件：可选，仅在使用公共路标同步时需要

## 使用

### 客户端 GUI

- 普通玩家：`Sync`、`Settings`
- OP：`Sync`、`Admin`、`Settings`

GUI 中主要按钮和设置项都带有悬浮说明。客户端也可以使用 `/mapsyncer gui` 或 `/mapsyncergui` 打开界面。

## 命令速查

### 玩家命令

```text
/mapsyncer
/mapsyncer help
/mapsyncer gui
/mapsyncergui
/mapsyncer sync
/mapsyncer sync radius <blocks>
/mapsyncer sync all
/mapsyncer sync <dimension>
```

| 命令 | 作用 |
| --- | --- |
| `/mapsyncer` | 显示帮助信息 |
| `/mapsyncer help` | 显示帮助信息 |
| `/mapsyncer gui` | 打开 MapSyncer GUI |
| `/mapsyncergui` | 打开 MapSyncer GUI 的快捷命令 |
| `/mapsyncer sync` | 同步当前维度 |
| `/mapsyncer sync radius <blocks>` | 同步当前维度指定半径内的地图，例如 `/mapsyncer sync radius 1000` |
| `/mapsyncer sync all` | 同步所有已生成缓存的维度 |
| `/mapsyncer sync <dimension>` | 同步指定维度，例如 `/mapsyncer sync overworld` 或 `/mapsyncer sync minecraft:the_nether` |

`<dimension>` 支持 `overworld`、`the_nether`、`the_end`，也支持完整维度 ID，例如 `minecraft:overworld` 或其他模组维度 ID。

### OP / 服务端管理命令

```text
/mapsyncer
/mapsyncer help
/mapsyncer gui
/mapsyncer generate
/mapsyncer generate <dimension>
/mapsyncer generate <dimension> force
/mapsyncer generate <dimension> <x> <z>
/mapsyncer status
/mapsyncer incremental run
/mapsyncer incremental off
/mapsyncer incremental tick
/mapsyncer incremental tick <interval>
/mapsyncer incremental scheduled
/mapsyncer incremental scheduled <hour>
/mapsyncer incremental scheduled <hour> <minute>
```

| 命令 | 作用 |
| --- | --- |
| `/mapsyncer` / `/mapsyncer help` | 显示 OP 管理命令帮助 |
| `/mapsyncer gui` | 让当前 OP 玩家打开管理 GUI；控制台不能使用 GUI |
| `/mapsyncer generate` | 后台生成所有维度的 Xaero 地图缓存 |
| `/mapsyncer generate <dimension>` | 后台生成指定维度缓存 |
| `/mapsyncer generate <dimension> force` | 清理并强制重新生成指定维度缓存 |
| `/mapsyncer generate <dimension> <x> <z>` | 只生成指定 MCA region，例如 `/mapsyncer generate minecraft:overworld 0 0` |
| `/mapsyncer status` | 查看生成进度、增量模式和缓存统计 |
| `/mapsyncer incremental run` | 立即执行一次增量扫描 |
| `/mapsyncer incremental off` | 关闭自动增量更新 |
| `/mapsyncer incremental tick` | 按当前配置的 tick 间隔启用增量更新 |
| `/mapsyncer incremental tick <interval>` | 设置并启用按 tick 间隔执行的增量更新，例如 `200` 约等于 10 秒 |
| `/mapsyncer incremental scheduled` | 使用当前配置时间启用每日定时增量更新 |
| `/mapsyncer incremental scheduled <hour>` | 设置每日指定小时执行，分钟默认为 `0` |
| `/mapsyncer incremental scheduled <hour> <minute>` | 设置每日指定时间执行，例如 `/mapsyncer incremental scheduled 4 30` |

OP 管理命令需要 `LEVEL_OWNERS` 权限。建议首次开服后先执行 `/mapsyncer generate` 生成基础缓存，再让玩家通过 GUI 或 `/mapsyncer sync` 同步。

## 配置

### 服务端配置

配置文件：`config/mapsyncer.json`

- `enableDebugLogging`：是否启用调试日志。
- `maxConcurrentRegions`：服务端并发处理 region 的数量。
- `maxSyncPacketSize`：地图同步分包大小。
- `syncSpeedLimitKBps`：每个玩家的固定最高同步速度。
- `enableAdaptiveSyncThrottle`：是否启用基于玩家 Ping 的自适应限速。
- `adaptivePingThresholdMs` / `adaptivePingRecoverMs`：触发限速和恢复的 Ping 阈值。
- `adaptiveThrottleAdjustCooldownMs`：自适应调速冷却时间，默认 `2000ms`。
- `adaptiveMinSyncSpeedKBps`：自适应限速的最低速度。
- `adaptiveIncreaseStepKBps`：网络稳定时每次恢复速度的步进。
- `adaptiveDecreaseFactor`：网络不稳定时速度下降比例。
- `adaptiveStableRecoverSamples`：恢复速度前需要的稳定样本数量。
- `adaptiveUnlimitedCeilingKBps`：不限速时的自适应速度上限。
- `enableVoxySync`：是否允许 Voxy 同步。
- `incrementalUpdateMode`：增量更新模式，可为 `DISABLED`、`TICK`、`SCHEDULED`。
- `incrementalUpdateIntervalTicks`：tick 模式的执行间隔。
- `scheduledUpdateHour` / `scheduledUpdateMinute`：定时模式的执行时间。
- `enableDirtyRegionTracking`：是否启用脏 region 精确增量。
- `dirtyRegionFallbackFullScan`：脏 region 为空时是否回退到全量扫描。
- `maxDirtyRegionsPerIncrementalRun`：单次增量运行最多处理的脏 region 数量。
- `incrementalForceSaveBeforeScan`：增量扫描前是否强制保存区块。
- `enableRadiusSync`：是否允许半径同步。
- `maxRadiusSyncBlocks`：玩家可请求的最大同步半径。
- `radiusSyncCenterMode`：半径同步中心模式，可为 `PLAYER_POSITION`、`WORLD_SPAWN`、`FIXED`。
- `radiusSyncFixedDimension` / `radiusSyncFixedX` / `radiusSyncFixedY` / `radiusSyncFixedZ`：固定中心点配置。
- `defaultScanMode` / `defaultCaveStart`：未单独配置维度时的默认扫描模式和洞穴起始高度。
- `dimensionConfigs`：维度扫描配置列表。

### 客户端配置

配置文件：`config/mapsyncer-client.json`

- `autoSyncOnJoin`：进服后是否自动同步。
- `showSyncHud`：是否显示同步 HUD。
- `syncProgressChatIntervalPercent`：聊天栏进度提示间隔百分比，`0` 表示不按百分比提示。
- `autoSyncDelaySeconds`：自动同步延迟秒数。

### 公共路标配置

配置文件：`config/mapsyncer-public-waypoints.json`

公共路标会按 Xaero 的 `waypoints.txt` 文本格式写入：

```text
waypoint:name:initial:x:y:z:color:disabled:type:set:dimension
```

MapSyncer 只管理配置中的公共组，默认组名是 `ServerPublic`。写入时会先读取现有 `waypoints.txt`，删除公共组旧行，再追加新的公共路标；私人路标和其他组不会被覆盖。

如果路标文件被 Xaero 占用，客户端会放弃本次写入并提示稍后手动同步。

OP 也可以从自己的 Xaero 本地路标导入公共路标：

1. 在 Xaero 中正常创建一个私人路标。
2. 打开 `/mapsyncer gui`，进入 `Admin` 页。
3. 点击扫描本地路标，MapSyncer 会在后台读取当前服务器对应的 `waypoints.txt`。
4. 在列表中选择路标并添加，即可写入服务端 `config/mapsyncer-public-waypoints.json`。

导入不会修改本地私人 `waypoints.txt`。服务端会校验 OP 权限，并按“同维度同名或同维度同坐标则更新，否则追加”的规则保存；保存成功后会自动启用公共路标配置并立即向该 OP 下发最新公共路标。

## Xaero 默认地图与迁移

MapSyncer 的普通地图同步固定写入 `mw$default`，不再创建第二个 `mw$map` 地图。

如果客户端已经存在旧的 `mw$map` 或其他历史 `mw$*` 目录，MapSyncer 会在后台迁移：

- 只复制缺失的 `.zip` 地图区域。
- 保留 `caves/<layer>` 结构。
- 忽略 `.part`、`.temp` 和非 `.zip` 文件。
- 不删除旧目录。
- 不修改 `waypoints.txt`。

迁移完成后会自动触发 Xaero 地图批量重载，玩家不需要重新进入游戏。

## Voxy 同步

GUI 会自动检测客户端是否安装并启用了 Voxy。只有客户端和服务端都支持时，Voxy 同步按钮才可用。

Voxy 同步只处理当前维度，使用服务端最近一次落盘的存档数据，不会清理 NBT。刚放下的方块可能要等服务端自动保存后才会在 Voxy 中出现。

## 性能说明

- 普通地图同步写入 `mw$default`，避免重复创建地图目录。
- 公共路标使用 hash 去重和后台流式合并，减少卡顿。
- 服务端限速支持按玩家 Ping 自适应调整。
- 客户端 Xaero 地图刷新采用分批重载，减少微卡顿。
- 增量更新可配合脏 region 追踪，只处理最近变化过的区域。

## 构建

本项目需要 Java 25。Windows 下可显式指定 Java 25 后构建：

```powershell
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build --stacktrace
```

构建产物位于 `build/libs/`。

## 常见问题

### 为什么会看到两个地图？

旧客户端可能保留了 `mw$map`。新版会把普通同步写入 `mw$default`，并在后台把旧数据合并进去。合并后玩家只需要使用默认地图。

### 公共路标会覆盖我的私人路标吗？

不会。MapSyncer 只替换公共组，私人路标和其他组不会被修改。

### 没装 Xaero 路标组件还能同步地图吗？

可以。公共路标会跳过，普通地图同步不受影响。

### 服务端没装 Voxy 会怎样？

Voxy 按钮会保持不可用，普通地图同步仍然正常。

### 同步刚结束地图没有立刻刷新怎么办？

通常等待自动批量重载即可。如果仍然没有显示，可以重新打开 Xaero 地图或重新进入服务器。
