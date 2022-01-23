package one.ntonee.mctgbridge;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ActionListener implements Listener {
    private final TelegramApi telegram;

    ActionListener(TelegramApi telegram) {
        this.telegram = telegram;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        telegram.sendMessage("<b>\uD83E\uDD73 " + telegram.escapeText(e.getPlayer().getDisplayName()) +
                " зашёл на сервер" + (!e.getPlayer().hasPlayedBefore() ? " первый раз!</b>" : "</b>"));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        telegram.sendMessage("<b>\uD83D\uDE15 " + telegram.escapeText(e.getPlayer().getDisplayName()) +
                " покинул сервер</b>");
    }

    @EventHandler
    public void onMessage(AsyncPlayerChatEvent e) {
        telegram.sendMessage("\uD83D\uDCAC <b>[" + telegram.escapeText(e.getPlayer().getDisplayName()) +
            "]</b> " + telegram.escapeText(e.getMessage()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        String deathMessage = e.getDeathMessage();
        if (deathMessage == null) {
            deathMessage = e.getEntity().getDisplayName() + " как-то умер";
        }
        telegram.sendMessage("\u2620\uFE0F <b>" + telegram.escapeText(deathMessage) + "</b>");
    }
}
