# MapSyncer for Xaero World Map

MapSyncer 是一个 Fabric Mod，用于把服务端已经探索或预生成的地图区域转换为 Xaero's World Map 可读取的数据，并按需同步到客户端。

适合长期运行的服务器、使用 Chunky 等工具预生成过地图的服务器，或希望玩家首次进入时快速获得已有探索范围地图的场景。

## 当前版本

| 项目 | 版本 |
| --- | --- |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.2` |
| Fabric API | `0.149.1+26.1.2` |
| Java | `25` |
| Mod 版本 | `1.0.1-fabric` |

本项目已按本地官方 Fabric 模板 `E:\代码\mc服务器网站\mod\fabric-example-mod` 升级到 Minecraft `26.1.2`，没有更换目标版本。

## 功能

| 功能 | 说明 |
| --- | --- |
| 服务端地图缓存生成 | 从服务端 `.mca` 区域文件生成 Xaero World Map 兼容缓存 |
| 客户端同步 | 客户端执行命令后下载缺失或过期的地图区域 |
| 增量同步 | 通过时间戳和 CRC32 哈希比对，只同步有变化的区域 |
| 断点续传 | 同步中断后保留本地状态，下次进入服务器可继续同步 |
| 流式加载 | 客户端边接收边写入，并尝试刷新视野范围内的 Xaero 地图区域 |
| 多维度支持 | 支持主世界、下界、末地和 Mod 维度 |
| 限速发送 | 服务端可限制同步带宽，避免一次性发送过多数据 |
| 定时增量生成 | 支持按 Tick 间隔或每日固定时间刷新服务端缓存 |

## 安装

服务端需要安装：
- Fabric Loader `0.19.2` 或兼容版本
- Fabric API `0.149.1+26.1.2` 或兼容版本
- MapSyncer

客户端需要安装：
- Fabric Loader `0.19.2` 或兼容版本
- Fabric API `0.149.1+26.1.2` 或兼容版本
- MapSyncer
- Xaero's World Map

服务端不需要安装 Xaero's World Map。

## 使用流程

1. 服务端和客户端都安装 MapSyncer、Fabric Loader、Fabric API。
2. 客户端安装 Xaero's World Map。
3. 管理员在服务端执行 `/mapsyncer generate` 生成地图缓存。
4. 玩家进入服务器后执行 `/mapsyncer sync` 或 `/mapsyncer sync all` 同步地图。

服务器会在玩家加入时发送 MapSyncer 已安装通知。客户端如果检测到上次同步中断，会在聊天栏提示继续同步。

## 客户端命令

| 命令 | 说明 |
| --- | --- |
| `/mapsyncer` | 显示帮助 |
| `/mapsyncer help` | 显示帮助 |
| `/mapsyncer sync` | 同步当前维度 |
| `/mapsyncer sync <dimension>` | 同步指定维度，例如 `overworld`、`the_nether`、`minecraft:overworld` |
| `/mapsyncer sync all` | 同步所有已生成缓存的维度 |

## 服务端命令

以下命令需要 OP 权限。

| 命令 | 说明 |
| --- | --- |
| `/mapsyncer` | 显示服务端帮助 |
| `/mapsyncer generate` | 为所有维度生成地图缓存 |
| `/mapsyncer generate <dimension>` | 为指定维度增量生成缓存 |
| `/mapsyncer generate <dimension> <x> <z>` | 生成指定区域文件对应的地图缓存 |
| `/mapsyncer generate <dimension> force` | 强制重新生成指定维度缓存 |
| `/mapsyncer status` | 查看生成任务、增量更新状态和缓存统计 |
| `/mapsyncer incremental off` | 关闭自动增量更新 |
| `/mapsyncer incremental tick [interval]` | 按 Tick 间隔自动增量更新 |
| `/mapsyncer incremental scheduled [hour] [minute]` | 按每日固定时间自动增量更新 |

## 配置

配置文件位于：

```text
config/mapsyncer.json
```

主要配置项：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enableDebugLogging` | `false` | 是否启用调试日志 |
| `maxConcurrentRegions` | `4` | 同时转换的区域数量 |
| `maxSyncPacketSize` | `262144` | 单个同步包最大大小，默认 256 KiB |
| `syncSpeedLimitKBps` | `1024` | 同步限速，单位 KB/s，`0` 表示不限速 |
| `incrementalUpdateMode` | `DISABLED` | 自动增量更新模式：`DISABLED`、`TICK`、`SCHEDULED` |
| `incrementalUpdateIntervalTicks` | `200` | Tick 模式更新间隔 |
| `scheduledUpdateHour` | `4` | 定时模式小时 |
| `scheduledUpdateMinute` | `0` | 定时模式分钟 |
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

## 存档目录

Minecraft `26.1.2` 的区域文件优先从以下路径读取：

```text
<world>/dimensions/minecraft/overworld/region
<world>/dimensions/minecraft/the_nether/region
<world>/dimensions/minecraft/the_end/region
<world>/dimensions/<namespace>/<dimension>/region
```

为了兼容旧存档，也会回退检查：

```text
<world>/region
<world>/DIM-1/region
<world>/DIM1/region
```

## 构建

项目需要 Java 25。当前机器可使用：

```powershell
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean build
```

构建成功后，可用 Mod 文件位于：

```text
build/libs/mapsyncer-1.0.1-fabric.jar
```

完整路径：

```text
e:\代码\mc服务器网站\mod\服务端\MapSyncer-for-XaeroWorldmap\build\libs\mapsyncer-1.0.1-fabric.jar
```

`mapsyncer-1.0.1-fabric-sources.jar` 是源码包，不是放入 Minecraft `mods` 目录的文件。

## 项目结构

| 路径 | 说明 |
| --- | --- |
| `src/main/java/com/mapsyncer` | 公共逻辑、服务端逻辑、网络包、MCA 解析 |
| `src/client/java/com/mapsyncer/client` | 客户端命令、网络接收、Xaero 集成 |
| `src/main/resources/fabric.mod.json` | Fabric Mod 元数据 |
| `src/main/resources/assets/mapsyncer/lang` | 中英文语言文件 |

## 说明

MapSyncer 通过反射与 Xaero's World Map 的客户端逻辑集成，因此服务端可以独立运行；客户端若要显示同步后的地图，需要安装 Xaero's World Map。

本项目已从 NeoForge 迁移到 Fabric，并已在 Minecraft/Fabric `26.1.2` 环境下通过 `.\gradlew.bat clean build`。
