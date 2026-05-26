# MapSyncer for Xaero World Map

服务端地图同步到 Xaero's World Map 的 Fabric 模组。它会把服务端 `.mca` 区域文件转换为 Xaero 可读取的地图缓存，并让客户端按需同步已有探索区域。

## 版本

- Minecraft：`26.1.2`
- Fabric Loader：`0.19.2`
- Fabric API：`0.149.1+26.1.2`
- Java：`25`
- Mod：`1.0.1-fabric`

本项目固定面向 Minecraft/Fabric `26.1.2`。

## 来源

基于 [RuoChennn/MapSyncer-for-XaeroWorldmap](https://github.com/RuoChennn/MapSyncer-for-XaeroWorldmap) 改写。

- 原作者：RuoChennn
- 当前 `26.1.2` 适配版本：ShanHe_YF

## 功能

- 服务端从 `.mca` 生成 Xaero World Map 缓存。
- 客户端同步缺失或过期的地图区域。
- 支持主世界、下界、末地和 Mod 维度。
- 支持时间戳/哈希增量同步、DirtyRegion 精准增量、断点续传。
- 支持同步 HUD、聊天进度降噪、可选进服自动同步。
- 服务端可配置限速和自动增量生成。

## 安装

服务端：

- Fabric Loader `0.19.2`
- Fabric API `0.149.1+26.1.2`
- MapSyncer

客户端：

- Fabric Loader `0.19.2`
- Fabric API `0.149.1+26.1.2`
- MapSyncer
- Xaero's World Map

服务端不需要安装 Xaero's World Map。

## 使用

管理员先在服务端生成缓存：

```text
/mapsyncer generate                         # 生成所有维度的服务端地图缓存
```

玩家进入服务器后同步地图：

```text
/mapsyncer sync                             # 同步当前所在维度
/mapsyncer sync all                         # 同步所有已生成缓存的维度
/mapsyncer sync <dimension>                 # 同步指定维度，例如 overworld 或 minecraft:the_nether
```

常用服务端命令：

```text
/mapsyncer generate <dimension>             # 增量生成指定维度的地图缓存
/mapsyncer generate <dimension> force       # 强制重新生成指定维度缓存，忽略已有缓存
/mapsyncer generate <dimension> <x> <z>     # 只生成指定 region 坐标的地图缓存
/mapsyncer status                           # 查看生成任务、增量更新状态和缓存统计
/mapsyncer incremental off                  # 关闭自动增量更新
/mapsyncer incremental tick [interval]      # 按 Tick 间隔执行自动增量更新
/mapsyncer incremental scheduled [hour] [minute] # 按每天固定时间执行自动增量更新
```

## 配置

服务端配置：

```text
config/mapsyncer.json
```

关键项：

- `maxConcurrentRegions`：并发转换数量。
- `maxSyncPacketSize`：单个同步包大小。
- `syncSpeedLimitKBps`：同步限速，`0` 为不限速。
- `incrementalUpdateMode`：`DISABLED`、`TICK`、`SCHEDULED`。
- `enableDirtyRegionTracking`：启用 DirtyRegion 精准增量。
- `dirtyRegionFallbackFullScan`：Dirty 队列为空时回退到时间戳扫描。
- `maxDirtyRegionsPerIncrementalRun`：每轮最多处理的 dirty region。
- `dimensionConfigs`：维度扫描模式和高度配置。

客户端配置：

```text
config/mapsyncer-client.json
```

关键项：

- `autoSyncOnJoin`：进服自动同步，默认 `false`。
- `showSyncHud`：显示同步 HUD，默认 `true`。
- `syncProgressChatIntervalPercent`：聊天栏百分比提示间隔，默认 `0`。
- `autoSyncDelaySeconds`：自动同步延迟秒数，默认 `3`。

## 路径

服务端缓存目录：

```text
server_map_cache/
```

支持的区域文件路径：

```text
<world>/dimensions/<namespace>/<dimension>/region
<world>/region
<world>/DIM-1/region
<world>/DIM1/region
```

## 构建

需要 Java 25：

```powershell
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-25' # 使用 Java 25
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"       # 临时把 Java 25 加到当前终端 PATH
.\gradlew.bat clean build --stacktrace         # 清理旧产物并重新构建
```

可安装 jar 会生成在：

```text
build/libs/mapsyncer-1.0.1-fabric.jar
```
