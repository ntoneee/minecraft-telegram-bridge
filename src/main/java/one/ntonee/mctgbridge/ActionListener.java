package one.ntonee.mctgbridge;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.pengrad.telegrambot.request.DeleteMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

class AdvancementMetadata {
    String title;
    String description;
    String type;
}

public class ActionListener implements Listener {
    private final TelegramApi telegram;
    private HashMap<String, AdvancementMetadata> advancementIDToData;
    private final FileConfiguration config;
    private final Main plugin;
    private final LangConfiguration lang;

    private HashMap<String, Long> lastLeaveTime;
    private HashMap<String, Integer> lastLeaveMessageID;

    ActionListener(TelegramApi telegram, FileConfiguration config, Main plugin) {
        this.config = config;
        this.telegram = telegram;
        this.plugin = plugin;
        this.lang = plugin.getLangConfig();

        lastLeaveTime = new HashMap<>();
        lastLeaveMessageID = new HashMap<>();

        Gson gson = new Gson();
        String advancements = plugin.readResourceFile("/localization/advancements_id-en.json");
        advancementIDToData = gson.fromJson(advancements, new TypeToken<HashMap<String, AdvancementMetadata>>() {
        }.getType());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        telegram.actualizeListMessage();
        if (config.getBoolean("bridge-to-telegram.join-leave")) {
            if (lastLeaveTime.getOrDefault(e.getPlayer().getName(), 0L)
                    + config.getInt("delete-rejoin-before") * 1000L >= System.currentTimeMillis()) {
                telegram.safeCallMethod(new DeleteMessage(
                        config.getLong("telegram-chat-id"), lastLeaveMessageID.get(e.getPlayer().getName())
                ));
                return;
            }
            String langPathJoinEvent = "player-event.join";
            if (!e.getPlayer().hasPlayedBefore()) {
                langPathJoinEvent += "-first-time";
            }
            telegram.sendMessage(lang.formatTelegramString(langPathJoinEvent,
                    "userDisplayName", telegram.escapeText(e.getPlayer().getDisplayName())));

        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if (config.getBoolean("bridge-to-telegram.join-leave")) {
            int msgID = telegram.sendMessage(lang.formatTelegramString("player-event.leave",
                    "userDisplayName", telegram.escapeText(e.getPlayer().getDisplayName())));
            lastLeaveTime.put(e.getPlayer().getName(), System.currentTimeMillis());
            lastLeaveMessageID.put(e.getPlayer().getName(), msgID);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, telegram::actualizeListMessage, 1);  // after 1 tick update list
    }

    @EventHandler
    public void onMessage(AsyncPlayerChatEvent e) {
        if (!config.getBoolean("bridge-to-telegram.messages")) {
            return;
        }
        telegram.sendMessage(lang.formatTelegramString("player-event.message", Map.of(
                "userDisplayName", telegram.escapeText(e.getPlayer().getDisplayName()),
                "message", telegram.escapeText(e.getMessage())
        )));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!config.getBoolean("bridge-to-telegram.death")) {
            return;
        }
        String deathMessage = e.getDeathMessage();
        if (deathMessage == null) {  // Fallback, since getDeathMessage is nullable. No known way to reproduce null.
            deathMessage = e.getEntity().getDisplayName() + " died";
        }
        telegram.sendMessage(lang.formatTelegramString("player-event.death", "deathMessage", deathMessage));
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        String advancement_id = String.valueOf(e.getAdvancement().getKey());
        AdvancementMetadata advancement = advancementIDToData.get(advancement_id);
        if (advancement == null) {
            return;
        }
        if (!config.getBoolean("bridge-to-telegram.advancements." + advancement.type)) {
            return;
        }
        String message = lang.formatTelegramString("player-event.advancement." + advancement.type, Map.of(
                "userDisplayName", telegram.escapeText(e.getPlayer().getDisplayName()),
                "advancementTitle", telegram.escapeText(advancement.title),
                "advancementDescription", telegram.escapeText(advancement.description)
        ));
        telegram.sendMessage(message);
    }
}
