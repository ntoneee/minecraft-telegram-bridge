package one.ntonee.mctgbridge;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

public class TelegramApi {
    private final TelegramBot bot;
    private final JavaPlugin plugin;
    private final long chatID, adminChatID;
    private final long botID;
    private final String bot_username;
    private final int listMessageID;
    private int lastBotMessageID = -1;
    private String previousPinnedListContent, pinnedListAnnouncement;
    private String rawPreviousBotMessageContent;
    private final StringBuilder botMessageBuffer;

    public String getPinnedListAnnouncement() {
        return pinnedListAnnouncement;
    }

    private String getTelegramUserFullName(User user) {
        if (user.lastName() != null) {
            return user.firstName() + " " + user.lastName();
        }
        return user.firstName();
    }

    private String getMessageText(Message message) {
        if (message.caption() != null) {
            return message.caption();
        }
        else if (message.text() != null) {
            return message.text();
        }
        return "";
    }

    private String serializeMessageMeta(Message msg) {
        String result = "";
        if (msg.forwardFrom() != null) { result = "[Переслано от " + getTelegramUserFullName(msg.forwardFrom()) + "] "; }
        else if (msg.forwardFromChat() != null) { result = "[Переслано от " + msg.forwardFromChat().title() + "] "; }
        else if (msg.forwardSenderName() != null) { result = "[Переслано от " + msg.forwardSenderName() + "] "; }
        else if (msg.replyToMessage() != null) {
            result = "[В ответ на ";
            if (msg.replyToMessage().from().id() == botID) {
                result += msg.replyToMessage().text().substring(msg.replyToMessage().text().indexOf(' ') + 1).replace("\n", " / ");
            }
            else {
                result += "[" + getTelegramUserFullName(msg.replyToMessage().from()) + "] " + getMessageText(msg.replyToMessage()).replace("\n", " / ");
            }
            result += "] ";
        }
        if (msg.viaBot() != null) { result += "[через @" + msg.viaBot().username() + "] "; }
        if (msg.poll() != null) { result += "[Опрос: " + msg.poll().question() + "] "; }
        if (msg.dice() != null) { result += "[Кубиковое: " + msg.dice().value() + " очк.] "; }
        if (msg.photo() != null) { result += "[Фотография] "; }
        if (msg.sticker() != null) { result += "[Стикер] "; }
        if (msg.animation() != null) { result += "[GIF] "; }
        else if (msg.document() != null) { result += "[Файл] "; }
        if (msg.audio() != null) { result += "[Аудио] "; }
        if (msg.video() != null) { result += "[Видео] "; }
        if (msg.videoNote() != null) { result += "[Видеосообщение] "; }
        if (msg.voice() != null) { result += "[Голосовое сообщение] "; }
        if (msg.contact() != null) { result += "[Контакт] "; }
        if (msg.game() != null) { result += "[Игра] "; }
        if (msg.venue() != null) { result += "[Venue] "; }
        if (msg.location() != null) { result += "[Геолокация] "; }
        if (msg.pinnedMessage() != null) { result += "[закрепляет сообщение] "; }
        if (msg.newChatMembers() != null) {
            ArrayList<String> names = new ArrayList<>();
            for (User user : msg.newChatMembers()) {
                names.add(getTelegramUserFullName(user));
            }
            result += "[добавляет пользовател" + (msg.newChatMembers().length == 1 ? "я " : "ей ") + String.join(",", names) + "] ";
        }
        if (msg.newChatTitle() != null) {
            result += "[меняет название чата на " + msg.newChatTitle() + "] ";
        }
        if (msg.newChatPhoto() != null) {
            result += "[меняет фото чата] ";
        }
        if (msg.voiceChatScheduled() != null) {
            result += "[запланировал голосовой чат] ";
        }
        if (msg.voiceChatStarted() != null) {
            result += "[начинает голосовой чат] ";
        }
        if (msg.voiceChatEnded() != null) {
            result += "[завершает голосовой чат] ";
        }
        if (msg.voiceChatParticipantsInvited() != null) {
            ArrayList<String> names = new ArrayList<>();
            for (User user : msg.voiceChatParticipantsInvited().users()) {
                names.add(getTelegramUserFullName(user));
            }
            result += "[приглашает в ГЧ пользовател" +
                    (msg.voiceChatParticipantsInvited().users().size() == 1 ? "я " : "ей ") +
                    String.join(",", names) + "] ";
        }
        // надеюсь за сим все
        return result;
    }

    String getListMessage(boolean includeAnnouncement) {
        StringBuilder res_text = new StringBuilder();
        int player_cnt = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            res_text.append(player_cnt != 0 ? ", " : "").append(player.getDisplayName());
            ++player_cnt;
        }
        String prefix = "\uD83D\uDCDD ";
        if (includeAnnouncement) {
            prefix += pinnedListAnnouncement;
        }
        if (player_cnt == 0) {
            return prefix + "<b>На сервере 0 игроков</b>";
        }
        String suffix = (player_cnt % 100 >= 10 && player_cnt % 100 <= 20 || player_cnt % 10 >= 5 || player_cnt % 10 == 0 ? "ов" : player_cnt % 10 == 1 ? "" : "а");
        return prefix + "<b>На сервере " + player_cnt + " игрок" + suffix + ": " + escapeText(res_text.toString()) + "</b>";
    }

    boolean checkForCommand(String text, String command) {
        return text != null && (text.equalsIgnoreCase("/" + command) || text.equalsIgnoreCase("/" + command + "@" + bot_username));
    }

    TelegramApi(FileConfiguration config, JavaPlugin plugin) throws RuntimeException {
        String token = Objects.requireNonNull(config.getString("telegram-token"));
        bot = new TelegramBot(token);
        long my_id = 0;
        for (int i = 0; i < token.length(); ++i) {
            if (token.charAt(i) == ':') break;
            my_id = my_id * 10 + token.charAt(i) - '0';
        }
        botID = my_id;
        chatID = config.getLong("telegram-chat-id");
        adminChatID = config.getLong("telegram-admin-chat-id");
        pinnedListAnnouncement = config.getString("pinned-announcement");
        listMessageID = config.getInt("telegram-list-message-id");
        botMessageBuffer = new StringBuilder();
        this.plugin = plugin;
        BaseResponse resp = bot.execute(new SendChatAction(chatID, ChatAction.typing));
        if (!resp.isOk()) {
            throw new RuntimeException("Something went wrong while initializing Telegram API!\n" +
                    "Maybe you didn't correctly fill config.yml?\n" +
                    "Error description: " + resp.errorCode() + " " + resp.description());
        }
        bot_username = safeCallMethod(new GetMe()).user().username();
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null) {
                    if (update.message().chat().id() == chatID) {
                        lastBotMessageID = -1;
                        if (checkForCommand(update.message().text(), "list")) {
                            sendMessage(getListMessage(false));
                            continue;
                        }
                        String res_text = "[" + ChatColor.AQUA + getTelegramUserFullName(update.message().from()) + ChatColor.RESET + "] ";
                        res_text += ChatColor.ITALIC + serializeMessageMeta(update.message()) + ChatColor.RESET;
                        res_text += getMessageText(update.message());
                        res_text = res_text.replace("\n", "\n> ");
                        Bukkit.broadcastMessage(res_text);
                    }
                    else if (update.message().chat().id() == adminChatID && update.message().text() != null) {
                        String[] split = update.message().text().split("\\s+", 2);
                        if (checkForCommand(split[0], "setAnnouncement") && split.length != 1) {
                            pinnedListAnnouncement = split[1] + " ";
                            config.set("pinned-announcement", pinnedListAnnouncement);
                            plugin.saveConfig();
                            actualizeListMessage();
                            asyncSafeCallMethod(new SendMessage(adminChatID, "\u2705"));  // white_check_mark
                        }
                        else if (checkForCommand(split[0], "ping")) {
                            asyncSafeCallMethod(new SendMessage(adminChatID, "Pong"));
                        }
                        else if (checkForCommand(split[0], "unsetAnnouncement")) {
                            pinnedListAnnouncement = "";
                            config.set("pinned-announcement", pinnedListAnnouncement);
                            plugin.saveConfig();
                            actualizeListMessage();
                            asyncSafeCallMethod(new SendMessage(adminChatID, "\u2705"));  // white_check_mark
                        }
                    }
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    <T extends BaseRequest<T, R>, R extends BaseResponse> void asyncSafeCallMethod(BaseRequest<T, R> request) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                safeCallMethod(request);
            }
        });
    }

    <T extends BaseRequest<T, R>, R extends BaseResponse> R safeCallMethod(BaseRequest<T, R> request) throws RuntimeException {
        R resp = bot.execute(request);
        if (!resp.isOk()) {
            throw new RuntimeException("Telegram API Error: " + resp.errorCode() + " " + resp.description());
        }
        return resp;
    }

    void actualizeListMessage() throws RuntimeException {
        if (listMessageID != 0) {
            String nowList = getListMessage(true);
            if (Objects.equals(nowList, previousPinnedListContent)) return;
            asyncSafeCallMethod(new EditMessageText(chatID, listMessageID, nowList).parseMode(ParseMode.HTML));
            previousPinnedListContent = nowList;
        }
    }

    void setListMessage(String text) throws RuntimeException {
        if (listMessageID != 0 && !Objects.equals(text, previousPinnedListContent)) {
            EditMessageText request = new EditMessageText(chatID, listMessageID, text).parseMode(ParseMode.HTML);
            if (plugin.isEnabled()) {
                asyncSafeCallMethod(request);
            }
            else {
                safeCallMethod(request);
            }
            previousPinnedListContent = text;
        }
    }

    void sendMessage(String text) throws RuntimeException {
        sendMessageForce(text);
//        botMessageBuffer.append("\n").append(text);
    }

    void flushMessageBuffer() {
        if (botMessageBuffer.isEmpty()) {
            return;
        }
        String toAppend = botMessageBuffer.toString();
        botMessageBuffer.setLength(0);
        sendMessageForce(toAppend);
    }

    void sendMessageForce(String text) throws RuntimeException {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                SendResponse resp = safeCallMethod(new SendMessage(chatID, text).parseMode(ParseMode.HTML).disableWebPagePreview(true));
            }
        });
    }

    String escapeText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
