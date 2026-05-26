package com.mapsyncer.client;

import com.mapsyncer.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MapSyncerScreen extends Screen {
    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 286;
    private static final int COMPLETE_VISIBLE_MS = 2000;
    private static final int ADMIN_POLL_MS = 2000;

    private Tab activeTab = Tab.SYNC;
    private Button syncCurrentButton;
    private Button syncAllButton;
    private Button autoSyncButton;
    private Button hudButton;
    private Button delayValueButton;
    private Button chatIntervalValueButton;
    private Button incrementalButton;
    private Button forceButton;
    private EditBox dimensionBox;
    private long lastAdminPoll;

    public MapSyncerScreen() {
        super(Component.translatable("mapsyncer.gui.title"));
    }

    @Override
    protected void init() {
        if (activeTab == Tab.ADMIN && !isOwner()) {
            activeTab = Tab.SYNC;
        }
        rebuildGui();
    }

    @Override
    public void tick() {
        if (activeTab == Tab.ADMIN && !isOwner()) {
            activeTab = Tab.SYNC;
            rebuildGui();
            return;
        }
        if (activeTab == Tab.ADMIN && isOwner()) {
            long now = System.currentTimeMillis();
            if (now - lastAdminPoll > ADMIN_POLL_MS) {
                AdminStatusClientState.requestNow();
                lastAdminPoll = now;
            }
        }
        updateDynamicButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawShell(graphics);
        switch (activeTab) {
            case SYNC -> drawSyncTab(graphics);
            case ADMIN -> drawAdminTab(graphics);
            case SETTINGS -> drawSettingsTab(graphics);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuildGui() {
        clearWidgets();
        int panelLeft = panelLeft();
        int panelTop = panelTop();
        int x = contentLeft();
        int y = panelTop + 30;
        int gap = 8;
        int tabCount = isOwner() ? 3 : 2;
        int tabWidth = Math.min(108, Math.max(72, (contentWidth() - gap * (tabCount - 1)) / tabCount));

        addTabButton(Tab.SYNC, x, y, tabWidth);
        x += tabWidth + gap;
        if (isOwner()) {
            addTabButton(Tab.ADMIN, x, y, tabWidth);
            x += tabWidth + gap;
        }
        addTabButton(Tab.SETTINGS, x, y, tabWidth);

        switch (activeTab) {
            case SYNC -> buildSyncWidgets(panelLeft, panelTop);
            case ADMIN -> buildAdminWidgets(panelLeft, panelTop);
            case SETTINGS -> buildSettingsWidgets(panelLeft, panelTop);
        }
        updateDynamicButtons();
    }

    private void addTabButton(Tab tab, int x, int y, int width) {
        Button button = Button.builder(tab.title(), b -> {
            activeTab = tab;
            rebuildGui();
            if (activeTab == Tab.ADMIN) {
                AdminStatusClientState.requestNow();
                lastAdminPoll = System.currentTimeMillis();
            }
        }).bounds(x, y, width, 20).build();
        button.active = activeTab != tab;
        addRenderableWidget(button);
    }

    private void buildSyncWidgets(int panelLeft, int panelTop) {
        int x = contentLeft();
        int y = panelTop + 122;
        int gap = 10;
        int buttonWidth = Math.max(1, (contentWidth() - gap) / 2);
        syncCurrentButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.sync.current"),
                b -> MapSyncerCommand.sendSyncRequest(Minecraft.getInstance(), currentDimensionId(), false)
        ).bounds(x, y, buttonWidth, 22).build());
        syncAllButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.sync.all"),
                b -> MapSyncerCommand.sendSyncRequest(Minecraft.getInstance(), "all", true)
        ).bounds(x + buttonWidth + gap, y, buttonWidth, 22).build());
    }

    private void buildAdminWidgets(int panelLeft, int panelTop) {
        int x = contentLeft();
        int y = panelTop + 94;
        int contentWidth = contentWidth();
        int gap = 8;

        dimensionBox = addRenderableWidget(new EditBox(font, x, y, Math.min(238, contentWidth), 20,
                Component.translatable("mapsyncer.gui.admin.dimension")));
        dimensionBox.setMaxLength(128);
        dimensionBox.setValue(currentDimensionId());

        if (contentWidth >= 470) {
            int buttonWidth = Math.max(1, (contentWidth - gap * 2) / 3);
            incrementalButton = addRenderableWidget(Button.builder(
                    Component.translatable("mapsyncer.gui.admin.incremental"),
                    b -> {
                        sendServerCommand("mapsyncer incremental run");
                        AdminStatusClientState.requestNow();
                        lastAdminPoll = System.currentTimeMillis();
                    }
            ).bounds(x, y + 34, buttonWidth, 22).build());

            forceButton = addRenderableWidget(Button.builder(
                    Component.translatable("mapsyncer.gui.admin.force"),
                    b -> {
                        String dimension = sanitizeDimensionInput(dimensionBox.getValue());
                        sendServerCommand("mapsyncer generate " + dimension + " force");
                        AdminStatusClientState.requestNow();
                        lastAdminPoll = System.currentTimeMillis();
                    }
            ).bounds(x + buttonWidth + gap, y + 34, buttonWidth, 22).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("mapsyncer.gui.admin.refresh"),
                    b -> {
                        AdminStatusClientState.requestNow();
                        lastAdminPoll = System.currentTimeMillis();
                    }
            ).bounds(x + (buttonWidth + gap) * 2, y + 34, buttonWidth, 22).build());
        } else {
            int buttonWidth = Math.max(1, (contentWidth - gap) / 2);
            incrementalButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.admin.incremental"),
                b -> {
                    sendServerCommand("mapsyncer incremental run");
                    AdminStatusClientState.requestNow();
                    lastAdminPoll = System.currentTimeMillis();
                }
            ).bounds(x, y + 34, buttonWidth, 22).build());

            forceButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.admin.force"),
                b -> {
                    String dimension = sanitizeDimensionInput(dimensionBox.getValue());
                    sendServerCommand("mapsyncer generate " + dimension + " force");
                    AdminStatusClientState.requestNow();
                    lastAdminPoll = System.currentTimeMillis();
                }
            ).bounds(x + buttonWidth + gap, y + 34, buttonWidth, 22).build());

            addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.admin.refresh"),
                b -> {
                    AdminStatusClientState.requestNow();
                    lastAdminPoll = System.currentTimeMillis();
                }
            ).bounds(x, y + 62, buttonWidth, 22).build());
        }

        AdminStatusClientState.requestNow();
        lastAdminPoll = System.currentTimeMillis();
    }

    private void buildSettingsWidgets(int panelLeft, int panelTop) {
        int x = contentLeft();
        int y = settingsStartY();
        int controlWidth = Math.min(130, Math.max(104, contentWidth() / 3));
        int controlX = contentRight() - controlWidth;
        int stepperWidth = Math.min(130, Math.max(122, controlWidth));
        int stepX = contentRight() - stepperWidth;
        int valueWidth = stepperWidth - 76;

        autoSyncButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            ClientConfig.VALUES.autoSyncOnJoin = !ClientConfig.VALUES.autoSyncOnJoin;
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(controlX, y, controlWidth, 20).build());

        hudButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            ClientConfig.VALUES.showSyncHud = !ClientConfig.VALUES.showSyncHud;
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(controlX, y + settingsRowGap(), controlWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            ClientConfig.VALUES.autoSyncDelaySeconds = Math.max(1, ClientConfig.VALUES.autoSyncDelaySeconds - 1);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX, y + settingsRowGap() * 2, 32, 20).build());
        delayValueButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
        }).bounds(stepX + 38, y + settingsRowGap() * 2, valueWidth, 20).build());
        delayValueButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            ClientConfig.VALUES.autoSyncDelaySeconds = Math.min(60, ClientConfig.VALUES.autoSyncDelaySeconds + 1);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX + 44 + valueWidth, y + settingsRowGap() * 2, 32, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            ClientConfig.VALUES.syncProgressChatIntervalPercent = Math.max(0,
                    ClientConfig.VALUES.syncProgressChatIntervalPercent - 5);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX, y + settingsRowGap() * 3, 32, 20).build());
        chatIntervalValueButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
        }).bounds(stepX + 38, y + settingsRowGap() * 3, valueWidth, 20).build());
        chatIntervalValueButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            ClientConfig.VALUES.syncProgressChatIntervalPercent = Math.min(100,
                    ClientConfig.VALUES.syncProgressChatIntervalPercent + 5);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX + 44 + valueWidth, y + settingsRowGap() * 3, 32, 20).build());

        updateSettingsButtonMessages();
    }

    private void drawShell(GuiGraphicsExtractor graphics) {
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xD8181C22);
        graphics.outline(left, top, panelWidth(), panelHeight(), 0xFF5BC0EB);
        graphics.centeredText(font, title, width / 2, top + 10, 0xFFFFFFFF);
        graphics.fill(left + 12, top + 58, left + panelWidth() - 12, top + 59, 0x665BC0EB);
    }

    private void drawSyncTab(GuiGraphicsExtractor graphics) {
        int x = contentLeft();
        int y = panelTop() + 72;
        boolean installed = MapPacketReceiver.isServerInstalled();
        Component serverStatus = installed
                ? Component.translatable("mapsyncer.gui.sync.server_installed", MapPacketReceiver.getServerVersion())
                : Component.translatable("mapsyncer.gui.sync.server_missing");
        graphics.text(font, Component.translatable("mapsyncer.gui.sync.dimension", currentDimensionId()), x, y, 0xFFE8F0F6, false);
        graphics.text(font, serverStatus, x, y + 16, installed ? 0xFF8CE99A : 0xFFFFC857, false);
        graphics.textWithWordWrap(font, Component.translatable("mapsyncer.gui.sync.tip"), x, y + 88, contentWidth(), 0xFFB9C3CC);

        int barY = y + 52;
        int barWidth = contentWidth();
        int percent = visibleSyncPercent();
        graphics.text(font, Component.translatable("mapsyncer.gui.sync.progress",
                SyncProgressTracker.getProcessed(), SyncProgressTracker.getTotal(), percent), x, barY - 12, 0xFFE8F0F6, false);
        graphics.fill(x, barY, x + barWidth, barY + 8, 0xFF2E3740);
        graphics.fill(x, barY, x + (barWidth * percent) / 100, barY + 8, 0xFF5BC0EB);
        graphics.outline(x, barY, barWidth, 8, 0xFF6B7785);
        if (!SyncProgressTracker.getStatus().isBlank()) {
            graphics.text(font, Component.literal(SyncProgressTracker.getStatus()), x, barY + 14, 0xFFB9C3CC, false);
        }
    }

    private void drawAdminTab(GuiGraphicsExtractor graphics) {
        int x = contentLeft();
        int y = panelTop() + 72;
        graphics.text(font, Component.translatable("mapsyncer.gui.admin.dimension_label"), x, y + 10, 0xFFE8F0F6, false);
        graphics.textWithWordWrap(font, Component.translatable("mapsyncer.gui.admin.warning"), x, y + 90, contentWidth(), 0xFFFFC857);

        int statusY = y + (contentWidth() >= 470 ? 124 : 134);
        String error = AdminStatusClientState.getLastError();
        PacketHandler.AdminStatusPayload status = AdminStatusClientState.getLastStatus();
        if (!error.isBlank()) {
            graphics.text(font, Component.translatable("mapsyncer.gui.admin.status_" + error), x, statusY, 0xFFFF6B6B, false);
            return;
        }
        if (status == null) {
            graphics.text(font, Component.translatable("mapsyncer.gui.admin.status_waiting"), x, statusY, 0xFFB9C3CC, false);
            return;
        }
        if (!status.allowed()) {
            graphics.text(font, Component.translatable("mapsyncer.gui.admin.no_permission"), x, statusY, 0xFFFF6B6B, false);
            return;
        }

        int percent = status.total() > 0 ? Math.min(100, status.processed() * 100 / status.total()) : 0;
        graphics.text(font, Component.translatable("mapsyncer.gui.admin.status_line",
                status.running() ? Component.translatable("mapsyncer.gui.admin.running") : Component.translatable("mapsyncer.gui.admin.idle"),
                status.processed(), status.total(), percent, status.status()), x, statusY, 0xFFE8F0F6, false);
        graphics.text(font, Component.translatable("mapsyncer.gui.admin.dirty", status.dirtyCount()), x, statusY + 16, 0xFFB9C3CC, false);
        graphics.text(font, Component.translatable("mapsyncer.gui.admin.cache",
                status.cacheDimensionCount(), status.cacheRegionCount(), status.cacheSizeBytes() / (1024.0 * 1024.0)), x, statusY + 32, 0xFFB9C3CC, false);
        String speedLimit = status.syncSpeedLimitKBps() <= 0
                ? Component.translatable("mapsyncer.gui.admin.unlimited").getString()
                : status.syncSpeedLimitKBps() + " KB/s";
        graphics.text(font, Component.translatable("mapsyncer.gui.admin.limit", speedLimit), x, statusY + 48, 0xFFB9C3CC, false);
        graphics.text(font, Component.translatable("mapsyncer.gui.admin.incremental_status", status.incrementalStatus()), x, statusY + 64, 0xFFB9C3CC, false);
    }

    private void drawSettingsTab(GuiGraphicsExtractor graphics) {
        int x = contentLeft();
        int y = settingsStartY();
        int textWidth = Math.max(118, contentRight() - x - Math.min(130, Math.max(104, contentWidth() / 3)) - 12);
        int rowGap = settingsRowGap();
        drawSetting(graphics, "mapsyncer.gui.settings.auto_sync", "mapsyncer.gui.settings.auto_sync.desc", x, y, textWidth);
        drawSetting(graphics, "mapsyncer.gui.settings.hud", "mapsyncer.gui.settings.hud.desc", x, y + rowGap, textWidth);
        drawSetting(graphics, "mapsyncer.gui.settings.delay", "mapsyncer.gui.settings.delay.desc", x, y + rowGap * 2, textWidth);
        drawSetting(graphics, "mapsyncer.gui.settings.chat", "mapsyncer.gui.settings.chat.desc", x, y + rowGap * 3, textWidth);
        int noteY = y + rowGap * 4 + 4;
        if (noteY + 24 < panelTop() + panelHeight()) {
            graphics.textWithWordWrap(font, Component.translatable("mapsyncer.gui.settings.server_limit_note"),
                    x, noteY, contentWidth(), 0xFFB9C3CC);
        }
    }

    private void drawSetting(GuiGraphicsExtractor graphics, String titleKey, String descKey, int x, int y, int textWidth) {
        graphics.text(font, Component.translatable(titleKey), x, y + 2, 0xFFE8F0F6, false);
        graphics.textWithWordWrap(font, Component.translatable(descKey), x, y + 16, textWidth, 0xFF9BA8B5);
    }

    private void updateDynamicButtons() {
        boolean canSync = minecraft != null && minecraft.player != null && minecraft.level != null
                && MapPacketReceiver.isServerInstalled() && !MapPacketReceiver.isSyncInProgress();
        if (syncCurrentButton != null) {
            syncCurrentButton.active = canSync;
        }
        if (syncAllButton != null) {
            syncAllButton.active = canSync;
        }
        boolean canAdminAction = isOwner() && minecraft != null && minecraft.player != null;
        if (incrementalButton != null) {
            incrementalButton.active = canAdminAction;
        }
        if (forceButton != null) {
            forceButton.active = canAdminAction && dimensionBox != null && !sanitizeDimensionInput(dimensionBox.getValue()).isBlank();
        }
        updateSettingsButtonMessages();
    }

    private void updateSettingsButtonMessages() {
        if (autoSyncButton != null) {
            autoSyncButton.setMessage(toggleLabel(ClientConfig.VALUES.autoSyncOnJoin));
        }
        if (hudButton != null) {
            hudButton.setMessage(toggleLabel(ClientConfig.VALUES.showSyncHud));
        }
        if (delayValueButton != null) {
            delayValueButton.setMessage(Component.translatable("mapsyncer.gui.settings.seconds",
                    ClientConfig.VALUES.autoSyncDelaySeconds));
        }
        if (chatIntervalValueButton != null) {
            chatIntervalValueButton.setMessage(Component.translatable("mapsyncer.gui.settings.percent",
                    ClientConfig.VALUES.syncProgressChatIntervalPercent));
        }
    }

    private Component toggleLabel(boolean enabled) {
        return Component.translatable(enabled ? "mapsyncer.gui.settings.on" : "mapsyncer.gui.settings.off");
    }

    private int visibleSyncPercent() {
        if (SyncProgressTracker.isTracking()) {
            return SyncProgressTracker.getPercent();
        }
        long completedAt = SyncProgressTracker.getCompletedAt();
        if (completedAt > 0 && System.currentTimeMillis() - completedAt <= COMPLETE_VISIBLE_MS) {
            return 100;
        }
        return 0;
    }

    private boolean isOwner() {
        return minecraft != null && minecraft.player != null
                && Commands.LEVEL_OWNERS.check(minecraft.player.permissions());
    }

    private String currentDimensionId() {
        return MapSyncerCommand.currentDimensionId(Minecraft.getInstance());
    }

    private String sanitizeDimensionInput(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value.isBlank() ? currentDimensionId() : value.replace(" ", "");
    }

    private void sendServerCommand(String command) {
        if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand(command);
        }
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(1, width - 32));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(1, height - 32));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(16, (height - panelHeight()) / 2);
    }

    private int panelMargin() {
        int target = panelWidth() < 430 ? 20 : 28;
        return Math.max(1, Math.min(target, panelWidth() / 8));
    }

    private int contentLeft() {
        return panelLeft() + panelMargin();
    }

    private int contentRight() {
        return panelLeft() + panelWidth() - panelMargin();
    }

    private int contentWidth() {
        return contentRight() - contentLeft();
    }

    private int settingsStartY() {
        return panelTop() + (panelHeight() < 250 ? 72 : 88);
    }

    private int settingsRowGap() {
        return panelHeight() < 250 ? 40 : 48;
    }

    private enum Tab {
        SYNC("mapsyncer.gui.tab.sync"),
        ADMIN("mapsyncer.gui.tab.admin"),
        SETTINGS("mapsyncer.gui.tab.settings");

        private final String key;

        Tab(String key) {
            this.key = key;
        }

        Component title() {
            return Component.translatable(key);
        }
    }
}
