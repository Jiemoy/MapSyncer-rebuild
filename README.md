# MapSyncer for Xaero World Map

MapSyncer 用于把服务端已探索区域同步到客户端的 Xaero World Map。普通地图同步固定写入 `mw$default`，不会再生成第二个 `map` 地图。若客户端已有旧的 `mw$map` 或其他历史 `mw$*` 目录，程序会在后台合并到 `mw$default`，只复制缺失的 `.zip` 区域文件，保留旧目录，不会删除玩家的私人路标。

## 主要功能

- 同步服务端生成的 Xaero 地图缓存到客户端
- 普通地图固定写入 `mw$default`
- 后台迁移旧版 `mw$map` 数据到 `mw$default`
- 半径同步：`/mapsyncer sync radius 1000`
- 公共路标同步，按 Xaero `waypoints.txt` 文本格式安全合并
- 支持 Voxy 同步
- 提供客户端 GUI，普通玩家和 OP 界面明确分开

## 版本信息

- Minecraft / Fabric：`26.1.2`
- Fabric Loader：`0.19.2`
- Fabric API：`0.149.1+26.1.2`
- Java：`25`
- Mod 版本：`1.0.1-fabric`

## 安装

### 服务端

- Fabric Loader
- Fabric API
- MapSyncer

### 客户端

- Fabric Loader
- Fabric API
- MapSyncer
- Xaero's World Map
- Voxy（可选，仅在使用 Voxy 同步时需要）
- Xaero 路标组件（可选，仅在使用公共路标同步时需要）

服务端不需要安装 Xaero's World Map。

## 使用

### 客户端 GUI

- 普通玩家：`Sync`、`Settings`
- OP：`Sync`、`Admin`、`Settings`

GUI 中每个主要按钮和设置项都带有悬浮说明。

### 玩家同步命令

```text
/mapsyncer gui
/mapsyncer sync
/mapsyncer sync radius <blocks>
/mapsyncer sync all
/mapsyncer sync <dimension>
```

- `/mapsyncer sync`：同步当前维度
- `/mapsyncer sync radius <blocks>`：同步当前维度指定半径内的地图
- `/mapsyncer sync all`：同步所有已生成缓存的维度
- `/mapsyncer sync <dimension>`：同步指定维度，例如 `overworld` 或 `minecraft:the_nether`

### 服务端管理命令

```text
/mapsyncer generate
/mapsyncer generate <dimension>
/mapsyncer generate <dimension> force
/mapsyncer generate <dimension> <x> <z>
/mapsyncer incremental run
/mapsyncer status
/mapsyncer incremental off
/mapsyncer incremental tick [interval]
/mapsyncer incremental scheduled [hour] [minute]
```

## 配置

### 服务端配置

`config/mapsyncer.json`

- `enableVoxySync`：是否允许 Voxy 同步
- `maxSyncPacketSize`：同步分包大小
- `syncSpeedLimitKBps`：每个玩家的固定最高限速
- `enableAdaptiveSyncThrottle`：是否启用基于 Ping 的自适应限速
- `adaptivePingThresholdMs` / `adaptivePingRecoverMs`：限速和恢复阈值
- `adaptiveThrottleAdjustCooldownMs`：调速冷却时间，默认 `2000ms`
- `enableDirtyRegionTracking`：是否启用脏区块精确增量
- `enableRadiusSync`：是否允许半径同步
- `maxRadiusSyncBlocks`：玩家可请求的最大半径
- `radiusSyncCenterMode`：半径中心模式，可为 `PLAYER_POSITION`、`WORLD_SPAWN`、`FIXED`
- `radiusSyncFixedDimension` / `radiusSyncFixedX/Y/Z`：固定中心点坐标

### 客户端配置

`config/mapsyncer-client.json`

- `autoSyncOnJoin`：进服自动同步
- `showSyncHud`：显示同步 HUD
- `syncProgressChatIntervalPercent`：聊天栏进度提示间隔
- `autoSyncDelaySeconds`：自动同步延迟秒数

### 公共路标配置

`config/mapsyncer-public-waypoints.json`

公共路标会按 Xaero 的 `waypoints.txt` 文本格式写入：

```text
waypoint:name:initial:x:y:z:color:disabled:type:set:dimension
```

MapSyncer 只管理配置中的公共组，默认组名是 `ServerPublic`。写入时会先读取现有 `waypoints.txt`，删除公共组旧行，再追加新的公共路标。私人路标和其他组不会被覆盖。

如果路标文件被 Xaero 占用，客户端会放弃本次写入并提示稍后手动同步。

OP 也可以直接从自己的 Xaero 本地路标导入公共路标：

1. 在 Xaero 中正常创建一个私人路标。
2. 打开 `/mapsyncer gui`，进入 `Admin` 页。
3. 点击“扫描本地路标”，MapSyncer 会在后台读取当前服务器对应的 `waypoints.txt`。
4. 在列表中点击路标行或右侧“添加”，即可把该路标写入服务端 `config/mapsyncer-public-waypoints.json`。

导入时不会修改本地私人 `waypoints.txt`。服务端会校验 OP 权限，并按“同维度同名或同维度同坐标则更新，否则追加”的规则保存；保存成功后会自动启用公共路标配置并立即向该 OP 下发最新公共路标。

## Xaero 默认地图与迁移

MapSyncer 的普通地图同步固定写入 `mw$default`，不再创建第二个 `mw$map` 地图。

如果客户端已有旧的 `mw$map` 或其他历史 `mw$*` 目录，MapSyncer 会在后台迁移：

- 只复制缺失的 `.zip` 地图区域
- 保留 `caves/<layer>` 结构
- 忽略 `.part`、`.temp` 和非 `.zip` 文件
- 不删除旧目录
- 不修改 `waypoints.txt`

迁移完成后会自动触发 Xaero 地图批量重载，玩家不需要重进游戏。

## Voxy 同步

GUI 会自动检测客户端是否安装并启用了 Voxy。只有在客户端和服务端都支持时，Voxy 同步按钮才可用。

Voxy 同步只处理当前维度，使用服务端最近一次落盘的存档数据，不会清洗 NBT。刚放下的方块可能要等服务端自动保存后才会在 Voxy 中出现。

## 性能说明

- 普通地图同步写入 `mw$default`，避免重复创建地图目录
- 公共路标使用 hash 去重和后台流式合并，减少卡顿
- 服务端限速支持按玩家 Ping 自适应调整
- 客户端 Xaero 地图刷新采用分批重载，减少微卡顿

## 常见问题

### 为什么会看到两个地图？

旧客户端可能保留了 `mw$map`。新版会把普通同步写入 `mw$default`，并在后台把旧数据合并进去。合并后只保留一个默认地图供玩家使用。

### 公共路标会覆盖我的私人路标吗？

不会。MapSyncer 只替换公共组，私人路标不动。

### 没装 Xaero 路标还能同步地图吗？

可以。公共路标会跳过，普通地图同步不受影响。

### 服务器没装 Voxy 会怎样？

Voxy 按钮会保持不可用，普通地图同步仍然正常。
