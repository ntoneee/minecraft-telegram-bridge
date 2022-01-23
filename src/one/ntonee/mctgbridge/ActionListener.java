package one.ntonee.mctgbridge;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

class AdvancementMetadata {
    String title;
    String description;
    String type;

    String getFriendlyAction(String username) {
        String friendlyType;
        String emoji;
        if (Objects.equals(type, "challenge")) {
            friendlyType = "завершил испытание";
            emoji = "\uD83C\uDFC5";
        }
        else if (Objects.equals(type, "goal")) {
            friendlyType = "достиг цели";
            emoji = "\uD83C\uDFAF";
        }
        else if (Objects.equals(type, "task")) {
            friendlyType = "выполнил задачу";
            emoji = "\uD83D\uDCDD";
        }
        else {
            System.err.println("type: " + type);
            friendlyType = "получил достижение";
            emoji = "\uD83D\uDE3C";
        }
        return String.join(" ", new String[]{emoji, username, friendlyType, title});
    }
}

public class ActionListener implements Listener {
    private final TelegramApi telegram;
    private HashMap<String, AdvancementMetadata> advancementIDToData;
    private final FileConfiguration config;

    ActionListener(TelegramApi telegram, FileConfiguration config) {
        this.config = config;
        this.telegram = telegram;
        Gson gson = new Gson();
        InputStream idJSONStream = getClass().getResourceAsStream("/localization/advancements_id-en.json");
        InputStreamReader reader = new InputStreamReader(idJSONStream);
        BufferedReader buf = new BufferedReader(reader);
        advancementIDToData = gson.fromJson(buf.lines().collect(Collectors.joining()), new TypeToken<HashMap<String, AdvancementMetadata>>() {
        }.getType());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!config.getBoolean("bridge-to-telegram.join-leave")) {
            return;
        }
        telegram.sendMessage("<b>\uD83E\uDD73 " + telegram.escapeText(e.getPlayer().getDisplayName()) +
                " зашёл на сервер" + (!e.getPlayer().hasPlayedBefore() ? " первый раз!</b>" : "</b>"));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if (!config.getBoolean("bridge-to-telegram.join-leave")) {
            return;
        }
        telegram.sendMessage("<b>\uD83D\uDE15 " + telegram.escapeText(e.getPlayer().getDisplayName()) +
                " покинул сервер</b>");
    }

    @EventHandler
    public void onMessage(AsyncPlayerChatEvent e) {
        if (!config.getBoolean("bridge-to-telegram.messages")) {
            return;
        }
        telegram.sendMessage("\uD83D\uDCAC <b>[" + telegram.escapeText(e.getPlayer().getDisplayName()) +
            "]</b> " + telegram.escapeText(e.getMessage()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!config.getBoolean("bridge-to-telegram.death")) {
            return;
        }
        String deathMessage = e.getDeathMessage();
        if (deathMessage == null) {
            deathMessage = e.getEntity().getDisplayName() + " как-то умер";
        }
        telegram.sendMessage("\u2620\uFE0F <b>" + telegram.escapeText(deathMessage) + "</b>");
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        String advancement_id = String.valueOf(e.getAdvancement().getKey());
        AdvancementMetadata advancement = advancementIDToData.get(advancement_id);
        if (advancement == null) {
//            Bukkit.getLogger().info("Advancement " + advancement_id + " not found in JSON, ignoring");
            return;
        }
        if (!config.getBoolean("bridge-to-telegram.advancements." + advancement.type)) {
            return;
        }
        String message = "<b>" + telegram.escapeText(advancement.getFriendlyAction(e.getPlayer().getDisplayName())) + "</b>\n\n";
        message += "<i>" + telegram.escapeText(advancement.description) + "</i>";
        telegram.sendMessage(message);
    }
}
