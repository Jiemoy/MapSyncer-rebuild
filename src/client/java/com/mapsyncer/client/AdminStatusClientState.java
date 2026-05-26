package com.mapsyncer.client;

import com.mapsyncer.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

final class AdminStatusClientState {
    private static volatile PacketHandler.AdminStatusPayload lastStatus;
    private static volatile String lastError = "";

    private AdminStatusClientState() {
    }

    static PacketHandler.AdminStatusPayload getLastStatus() {
        return lastStatus;
    }

    static String getLastError() {
        return lastError;
    }

    static boolean requestNow() {
        try {
            if (!ClientPlayNetworking.canSend(PacketHandler.AdminStatusRequestPayload.TYPE)) {
                lastError = "unsupported";
                return false;
            }
            ClientPlayNetworking.send(new PacketHandler.AdminStatusRequestPayload());
            lastError = "";
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            lastError = "unavailable";
            return false;
        }
    }

    static void handle(PacketHandler.AdminStatusPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            lastStatus = payload;
            lastError = "";
        });
    }

    static void reset() {
        lastStatus = null;
        lastError = "";
    }
}
