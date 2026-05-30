package kr.shkworld.shktown.listener;

import org.bukkit.event.Listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import kr.shkworld.shktown.chat.ChatManager;

public class ChatListener implements Listener {
    private final ChatManager chatManager;

    public ChatListener(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public void onPlayerchat(AsyncChatEvent event) {
        event.setCancelled(true);
        chatManager.processChat(event);
    }
}
