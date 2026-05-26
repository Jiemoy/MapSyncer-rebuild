# MapSyncer for Xaero World Map

MapSyncer 是一个 Fabric 模组，用于把服务端已经探索或预生成的地图区域转换为 Xaero's World Map 可读取的数据，并按需同步到客户端。它适合长期运行、使用 Chunky 等工具预生成地图，或希望玩家进服后快速获得已有地图范围的服务器。

## 目录

- [版本目标](#版本目标)
- [来源与署名](#来源与署名)
- [主要功能](#主要功能)
- [安装要求](#安装要求)
- [使用流程](#使用流程)
- [命令](#命令)
- [配置](#配置)
- [缓存与路径](#缓存与路径)
- [构建与产物](#构建与产物)
- [项目结构](#项目结构)
- [说明](#说明)

## 版本目标

| 项目 | 版本 |
| --- | --- |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.2` |
| Fabric API | `0.149.1+26.1.2` |
| Fabric Loom | `1.16-SNAPSHOT` |
| Java | `25` |
| Mod 版本 | `1.0.1-fabric` |

本分支固定面向 Minecraft/Fabric `26.1.2`，不更换目标版本。

## 来源与署名

本项目基于 [RuoChennn/MapSyncer-for-XaeroWorldmap](https://github.com/RuoChennn/MapSyncer-for-XaeroWorldmap) 改写。

原作者：RuoChennn  
原项目地址：https://github.com/RuoChennn/MapSyncer-for-XaeroWorldmap

当前 Minecraft/Fabric `26.1.2` 适配版本由 ShanHe_YF 制作。

## 主要功能

| 功能 | 说明 |
| --- | --- |
| 服务端地图缓存生成 | 从服务端 `.mca` 区域文件生成 Xaero's World Map 兼容缓存 |
| 客户端同步 | 玩家执行命令后下载缺失或过期的地图区域 |
| 增量更新 | 通过时间戳、CRC32 哈希和 DirtyRegion 追踪减少重复转换 |
| DirtyRegion 追踪 | 监听服务端区块方块变化，只优先重算发生变化的 region |
| 断点续传 | 同步中断后保留本地状态，下次进入服务器可继续 |
| HUD 进度 | 同步中在 HUD 显示进度，完成后短暂停留并淡出 |
| 可选自动同步 | 客户端可配置进服后延迟自动同步，默认关闭 |
| 多维度支持 | 支持主世界、下界、末地和 Mod 维度 |
| 限速发送 | 服务端可限制同步带宽，避免一次性发送过多数据 |
| 定时增量生成 | 支持按 Tick 间隔或每天固定时间刷新服务端缓存 |

## 安装要求

服务端需要安装：

- Fabric Loader `0.19.2`
- Fabric API `0.149.1+26.1.2`
- MapSyncer

客户端需要安装：

- Fabric Loader `0.19.2`
- Fabric API `0.149.1+26.1.2`
- MapSyncer
- Xaero's World Map

服务端不需要安装 Xaero's World Map。

## 使用流程

1. 服务端和客户端都安装 MapSyncer、Fabric Loader、Fabric API。
2. 客户端安装 Xaero's World Map。
3. 管理员在服务端执行 `/mapsyncer generate` 生成地图缓存。
4. 玩家进入服务器后执行 `/mapsyncer sync` 或 `/mapsyncer sync all` 同步地图。
5. 如果客户端启用了 `autoSyncOnJoin`，进服收到服务端握手后会延迟自动同步。

## 命令

客户端命令：

| 命令 | 说明 |
| --- | --- |
| `/mapsyncer` | 显示帮助 |
| `/mapsyncer help` | 显示帮助 |
| `/mapsyncer sync` | 同步当前维度 |
| `/mapsyncer sync <dimension>` | 同步指定维度，例如 `overworld`、`the_nether`、`minecraft:overworld` |
| `/mapsyncer sync all` | 同步所有已生成缓存的维度 |

服务端命令需要 OP 权限：

| 命令 | 说明 |
| --- | --- |
| `/mapsyncer generate` | 为所有维度生成地图缓存 |
| `/mapsyncer generate <dimension>` | 为指定维度增量生成缓存 |
| `/mapsyncer generate <dimension> <x> <z>` | 生成指定区域文件对应的地图缓存 |
| `/mapsyncer generate <dimension> force` | 强制重新生成指定维度缓存 |
| `/mapsyncer status` | 查看生成任务、增量更新状态和缓存统计 |
| `/mapsyncer incremental off` | 关闭自动增量更新 |
| `/mapsyncer incremental tick [interval]` | 按 Tick 间隔自动增量更新 |
| `/mapsyncer incremental scheduled [hour] [minute]` | 按每日固定时间自动增量更新 |

## 配置

服务端配置文件：

```text
config/mapsyncer.json
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enableDebugLogging` | `false` | 是否启用调试日志 |
| `maxConcurrentRegions` | `4` | 同时转换的 region 数量 |
| `maxSyncPacketSize` | `262144` | 单个同步包最大大小，默认 256 KiB |
| `syncSpeedLimitKBps` | `1024` | 同步限速，单位 KB/s，`0` 表示不限速 |
| `incrementalUpdateMode` | `DISABLED` | 自动增量更新模式：`DISABLED`、`TICK`、`SCHEDULED` |
| `incrementalUpdateIntervalTicks` | `200` | Tick 模式更新间隔 |
| `scheduledUpdateHour` | `4` | 定时模式小时 |
| `scheduledUpdateMinute` | `0` | 定时模式分钟 |
| `enableDirtyRegionTracking` | `true` | 是否启用 DirtyRegion 精准增量追踪 |
| `dirtyRegionFallbackFullScan` | `true` | Dirty 队列为空时是否回退到旧时间戳扫描 |
| `maxDirtyRegionsPerIncrementalRun` | `512` | 每轮增量最多处理的 dirty region 数量 |
| `defaultScanMode` | `SURFACE` | 未单独配置维度时的扫描模式 |
| `defaultCaveStart` | `63` | Cave 模式默认起始高度 |
| `dimensionConfigs` | 原版三维度 | 维度扫描配置列表 |

`dimensionConfigs` 格式：

```text
dimension|scan_mode|cave_start|hasSkylight|hasCeiling|minY|height|logicalHeight
```

示例：

```text
minecraft:the_nether|CAVE|63|false|true|0|256|256
```

客户端配置文件：

```text
config/mapsyncer-client.json
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `autoSyncOnJoin` | `false` | 进服检测到服务端安装 MapSyncer 后是否自动同步 |
| `showSyncHud` | `true` | 是否显示同步 HUD 进度 |
| `syncProgressChatIntervalPercent` | `0` | 聊天栏百分比提示间隔，`0` 表示关闭百分比刷屏 |
| `autoSyncDelaySeconds` | `3` | 自动同步触发延迟秒数 |

## 缓存与路径

服务端生成的 MapSyncer 缓存目录：

```text
server_map_cache/
```

Minecraft `26.1.2` 的区域文件会自动检测以下路径：

```text
<world>/dimensions/minecraft/overworld/region
<world>/dimensions/minecraft/the_nether/region
<world>/dimensions/minecraft/the_end/region
<world>/dimensions/<namespace>/<dimension>/region
```

同时兼容旧式存档路径：

```text
<world>/region
<world>/DIM-1/region
<world>/DIM1/region
```

客户端写入 Xaero's World Map 的目录由 Xaero 当前服务器目录决定，MapSyncer 会保持原有 Xaero 目录结构和写入目标。

## 构建与产物

项目需要 Java 25。当前机器可使用：

```powershell
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean build --stacktrace
```

构建成功后，可安装的 Mod 文件位于：

```text
build/libs/mapsyncer-1.0.1-fabric.jar
```

当前仓库下的完整路径为：

```text
e:\代码\mc服务器网站\mod\服务端\MapSyncer-for-XaeroWorldmap\build\libs\mapsyncer-1.0.1-fabric.jar
```

`mapsyncer-1.0.1-fabric-sources.jar` 是源码包，不是放入 Minecraft `mods` 目录的文件。

## 项目结构

| 路径 | 说明 |
| --- | --- |
| `src/main/java/com/mapsyncer` | 公共逻辑、服务端逻辑、网络包、MCA 解析 |
| `src/client/java/com/mapsyncer/client` | 客户端命令、网络接收、HUD、自动同步、Xaero 集成 |
| `src/main/resources/fabric.mod.json` | Fabric Mod 元数据 |
| `src/main/resources/mapsyncer.mixins.json` | DirtyRegion 追踪用 Mixin 配置 |
| `src/main/resources/assets/mapsyncer/lang` | 中英文语言文件 |

## 说明

MapSyncer 通过反射与 Xaero's World Map 的客户端逻辑集成，因此服务端可以独立运行；客户端若要显示同步后的地图，需要安装 Xaero's World Map。

本版本只做 Minecraft/Fabric `26.1.2` 迁移、稳定性修复和轻量体验优化，不改变原有命令语义、缓存目录、同步协议字段顺序或 Xaero 写入目标。
