package dev.friendchat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class FriendChatClient implements ClientModInitializer {
    public static KeyMapping OPEN_KEY;
    public static KeyMapping TOGGLE_KEY;

    public static void toggleRedirect() {
        ChatConfig c = ChatConfig.get();
        c.redirectGameChat = !c.redirectGameChat;
        ChatConfig.save();
        ChatClient.INSTANCE.toast("Friend Chat", c.redirectGameChat ? "Game chat now goes to Friend Chat" : "Game chat goes to the server again");
    }

    public static volatile Screen CURRENT;

    private static void addWidget(Screen s, Button b) {
        try {
            java.lang.reflect.Method m = Screen.class.getDeclaredMethod("addRenderableWidget", GuiEventListener.class);
            m.setAccessible(true);
            m.invoke(s, b);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onInitializeClient() {
        ChatConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("friendchat", "main"));
        OPEN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.friendchat.open", InputConstants.Type.KEYBOARD, InputConstants.KEY_Y, category));
        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.friendchat.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_F9, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (TOGGLE_KEY.consumeClick()) toggleRedirect();
            while (OPEN_KEY.consumeClick()) {
                if (CURRENT == null) mc.setScreenAndShow(new FriendChatScreen(null));
            }
        });

        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            CURRENT = screen;
            ScreenEvents.remove(screen).register(s -> {
                if (CURRENT == s) CURRENT = null;
            });
            if (screen instanceof TitleScreen || screen instanceof PauseScreen || screen instanceof JoinMultiplayerScreen) {
                addWidget(screen,
                        Button.builder(Component.literal("Chat"), b -> mc.setScreenAndShow(new FriendChatScreen(screen)))
                                .bounds(4, 4, 50, 20).build());
            }
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!ChatConfig.get().redirectGameChat) return true;
            if (!ChatClient.INSTANCE.send(message)) ChatClient.INSTANCE.toast("Friend Chat", "Not connected, message not sent");
            return false;
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            if (ChatConfig.get().autoConnect) ChatClient.INSTANCE.connect();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ChatClient.INSTANCE.shutdown());
    }
}
