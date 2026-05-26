package com.mapsyncer.client;

import com.mapsyncer.network.PacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapSyncerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapSyncerClient.class);

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        SyncHudOverlay.register();
        MapSyncerKeybinds.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                MapSyncerCommand.register(dispatcher));

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.SyncResponsePayload.TYPE,
                (payload, context) -> MapPacketReceiver.handleSyncResponse(payload, context));

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.SyncProgressPayload.TYPE,
                (payload, context) -> MapPacketReceiver.handleSyncProgress(payload, context));

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.ServerInstalledPayload.TYPE,
                (payload, context) -> MapPacketReceiver.handleServerInstalled(payload, context));

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.AdminStatusPayload.TYPE,
                (payload, context) -> AdminStatusClientState.handle(payload, context));

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.OpenGuiPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new MapSyncerScreen())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientJoinHandler.onClientJoin(client));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AdminStatusClientState.reset();
            ClientJoinHandler.onClientDisconnect(client);
        });

        LOGGER.info("MapSyncer client initialized for Fabric");
    }
}
