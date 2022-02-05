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
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TelegramApi {
    private final TelegramBot bot;
    private final Main plugin;
    private final long chatID, adminChatID;
    private final long botID;
    private final String bot_username;
    private final int listMessageID;
    private String previousPinnedListContent, pinnedListAnnouncement;
    private final StringBuilder botMessageBuffer;
    private final LangConfiguration lang;

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
        if (msg.forwardFrom() != null) {
            result = lang.formatMessageMetaString("forward", "forwardFrom", getTelegramUserFullName(msg.forwardFrom()));
        }
        else if (msg.forwardFromChat() != null) {
            result = lang.formatMessageMetaString("forward", "forwardFrom", msg.forwardFromChat().title());
        }
        else if (msg.forwardSenderName() != null) {
            result = lang.formatMessageMetaString("forward", "forwardFrom", msg.forwardSenderName());
        }
        else if (msg.replyToMessage() != null) {
            HashMap<String, String> substituteValues = new HashMap<>(Map.of(
                    "replySender", getTelegramUserFullName(msg.replyToMessage().from()),
                    "replyText", getMessageText(msg.replyToMessage()).replace("\n",
                            Objects.requireNonNull(lang.getString("minecraft.reply-newline-replacement")))
            ));
            if (msg.replyToMessage().from().id() == botID) {
                int spaceIndex = substituteValues.get("replyText").indexOf(' ');
                substituteValues.put("replyTextAfterSpace",
                        substituteValues.get("replyText").substring((spaceIndex == -1 ? 0 : spaceIndex)));
                result = lang.formatMessageMetaString("reply-minecraft", substituteValues);
            }
            else {
                result = lang.formatMessageMetaString("reply", substituteValues);
            }
        }
        if (msg.viaBot() != null) {
            result += lang.formatMessageMetaString("via-bot", "viaBotUsername", msg.viaBot().username());
        }
        if (msg.poll() != null) {
            result += lang.formatMessageMetaString("poll", "pollQuestion", msg.poll().question());
        }
        if (msg.dice() != null) {
            result += lang.formatMessageMetaString("dice", "diceValue", String.valueOf(msg.dice().value()));
        }
        if (msg.photo() != null) { result += lang.getMessageMetaString("photo"); }
        if (msg.sticker() != null) { result += lang.getMessageMetaString("sticker"); }
        if (msg.animation() != null) { result += lang.getMessageMetaString("gif"); }
        else if (msg.document() != null) { result += lang.getMessageMetaString("file"); }
        if (msg.audio() != null) { result += lang.getMessageMetaString("audio"); }
        if (msg.video() != null) { result += lang.getMessageMetaString("video"); }
        if (msg.videoNote() != null) { result += lang.getMessageMetaString("videomessage"); }
        if (msg.voice() != null) { result += lang.getMessageMetaString("voicemessage"); }
        if (msg.contact() != null) { result += lang.getMessageMetaString("contact"); }
        if (msg.game() != null) { result += lang.getMessageMetaString("game"); }
        if (msg.venue() != null) { result += lang.getMessageMetaString("venue"); }
        if (msg.location() != null) { result += lang.getMessageMetaString("geo"); }
        if (msg.pinnedMessage() != null) { result += lang.getMessageMetaString("pin"); }
        if (msg.newChatMembers() != null) {
            ArrayList<String> names = new ArrayList<>();
            for (User user : msg.newChatMembers()) {
                names.add(getTelegramUserFullName(user));
            }
            String joint_names = String.join(",", names);

            if (msg.newChatMembers().length == 1) {
                result += lang.formatMessageMetaString("invite-one", "userInvited", joint_names);
            }
            else {
                result += lang.formatMessageMetaString("invite-many", "usersInvited", joint_names);
            }
        }
        if (msg.newChatTitle() != null) {
            result += lang.formatMessageMetaString("change-title", "newTitle", msg.newChatTitle());
        }
        if (msg.newChatPhoto() != null) {
            result += lang.getMessageMetaString("change-photo");
        }
        if (msg.voiceChatScheduled() != null) {
            result += lang.getMessageMetaString("schedule-voice-chat");
        }
        if (msg.voiceChatStarted() != null) {
            result += lang.getMessageMetaString("start-voice-chat");
        }
        if (msg.voiceChatEnded() != null) {
            result += lang.getMessageMetaString("finish-voice-chat");
        }
        if (msg.voiceChatParticipantsInvited() != null) {
            ArrayList<String> names = new ArrayList<>();
            for (User user : msg.voiceChatParticipantsInvited().users()) {
                names.add(getTelegramUserFullName(user));
            }
            String joint_names = String.join(",", names);

            if (names.size() == 1) {
                result += lang.formatMessageMetaString("invite-one-voice-chat", "userInvited", joint_names);
            }
            else {
                result += lang.formatMessageMetaString("invite-many-voice-chat", "usersInvited", joint_names);
            }
        }
        return result;
    }

    String getListMessage(boolean includeAnnouncement) {
        StringBuilder names = new StringBuilder();
        int player_cnt = 0;
        for (Player player: Bukkit.getOnlinePlayers()) {
            if (player_cnt != 0) {
                names.append(", ");
            }
            names.append(player.getDisplayName());
            ++player_cnt;
        }
        String joint_names = names.toString();
        if (player_cnt == 0) {
            if (includeAnnouncement) {
                return lang.formatString("telegram.announcement-message.server-enabled-zero-online",
                        "announcement", pinnedListAnnouncement);
            }
            return lang.getString("telegram.zero-online");
        }
        if (includeAnnouncement) {
            return lang.formatString("telegram.announcement-message.server-enabled", Map.of(
                    "announcement", pinnedListAnnouncement,
                    "onlineCount", String.valueOf(player_cnt),
                    "onlinePlayers", joint_names
            ));
        }
        else {
            return lang.formatString("telegram.list", Map.of(
                    "onlineCount", String.valueOf(player_cnt),
                    "onlinePlayers", joint_names
            ));
        }
    }

    boolean checkForCommand(String text, String command) {
        return text != null && (text.equalsIgnoreCase("/" + command) || text.equalsIgnoreCase("/" + command + "@" + bot_username));
    }

    TelegramApi(FileConfiguration config, Main plugin) throws RuntimeException {
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
        lang = plugin.getLangConfig();
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
                        if (checkForCommand(update.message().text(), "list")) {
                            sendMessage(getListMessage(false));
                            continue;
                        }
                        Bukkit.broadcastMessage(lang.formatString("minecraft.base-message", Map.of(
                                "senderName", getTelegramUserFullName(update.message().from()),
                                "messageMeta", serializeMessageMeta(update.message()),
                                "messageText",
                                getMessageText(update.message()).replace("\n",
                                        lang.getString("minecraft.message-newline-replacement"))
                        )));
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> safeCallMethod(request));
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

    int syncSendMessageForce(String text) throws RuntimeException {
        return safeCallMethod(new SendMessage(chatID, text).parseMode(ParseMode.HTML).disableWebPagePreview(true)).message().messageId();
    }

    void sendMessageForce(String text) {
        // This method used to set lastBotMessageID variable, that is why asyncSafeCallMethod is not used
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            syncSendMessageForce(text);
        });
    }

    String escapeText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
