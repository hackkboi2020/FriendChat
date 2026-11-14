package dev.friendchat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FriendChatClient implements ClientModInitializer {
    public static KeyMapping OPEN_KEY;

    @Override
    public void onInitializeClient() {
        ChatConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("friendchat", "main"));
        OPEN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.friendchat.open", InputConstants.Type.KEYSYM, InputConstants.KEY_Y, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_KEY.consumeClick()) {
                if (mc.screen == null) mc.setScreenAndShow(new FriendChatScreen(null));
            }
        });

        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen || screen instanceof JoinMultiplayerScreen) {
                Screens.getButtons(screen).add(
                        Button.builder(Component.literal("Chat"), b -> mc.setScreenAndShow(new FriendChatScreen(screen)))
                                .bounds(4, 4, 50, 20).build());
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            if (ChatConfig.get().autoConnect) ChatClient.INSTANCE.connect();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ChatClient.INSTANCE.shutdown());
    }
}
