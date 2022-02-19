package one.ntonee.mctgbridge;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

enum TelegramEntityType {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    LINK,
    SPOILER;

    static TelegramEntityType fromString(String entity) {
        return switch (entity) {
            case "bold" -> BOLD;
            case "italic" -> ITALIC;
            case "underline" -> UNDERLINE;
            case "strikethrough" -> STRIKETHROUGH;
            case "url", "mention", "text_link" -> LINK;
            case "spoiler" -> SPOILER;
            default -> null;
        };
    }
}

class TelegramEntityEvent {
    public int position;
    public boolean opening;
    public TelegramEntityType type;
    public String data;

    TelegramEntityEvent(int position, boolean opening, TelegramEntityType type, String data) {
        this.position = position;
        this.opening = opening;
        this.type = type;
        this.data = data;
    }

    TelegramEntityEvent(int position, boolean opening, TelegramEntityType type) {
        this.position = position;
        this.opening = opening;
        this.type = type;
        this.data = null;
    }

    static int comparator(TelegramEntityEvent evt1, TelegramEntityEvent evt2) {
        if (evt1.position != evt2.position) {
            return (evt1.position < evt2.position ? -1 : 1);
        }
        if (evt1.opening != evt2.opening) {
            return (evt1.opening ? 1 : -1);
        }
        return 0;
    }
}

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

    private MessageEntity[] getMessageEntities(Message message) {
        if (message.captionEntities() != null) {
            return message.captionEntities();
        }
        return message.entities();
    }

    private ArrayList<TextComponent> serializeMessageMeta(Message msg) {
        ArrayList<TextComponent> result = new ArrayList<>();
        if (msg.forwardFrom() != null) {
            result.add(new TextComponent(lang.formatMessageMetaString("forward", "forwardFrom", getTelegramUserFullName(msg.forwardFrom()))));
        }
        else if (msg.forwardFromChat() != null) {
            result.add(new TextComponent(lang.formatMessageMetaString("forward", "forwardFrom", msg.forwardFromChat().title())));
        }
        else if (msg.forwardSenderName() != null) {
            result.add(new TextComponent(lang.formatMessageMetaString("forward", "forwardFrom", msg.forwardSenderName())));
        }
        else if (msg.replyToMessage() != null) {
            if (msg.replyToMessage().from().id() == botID) {
                int spaceIndex = getMessageText(msg.replyToMessage()).indexOf(' ') + 1;
                HashMap<String, String> substituteValues = new HashMap<>(Map.of(
                        "replySender", getTelegramUserFullName(msg.replyToMessage().from()),
                        "replyText", getMessageText(msg.replyToMessage()),
                        "replyTextAfterSpace", getMessageText(msg.replyToMessage()).substring(spaceIndex)
                ));
                result.add(new TextComponent(lang.formatMessageMetaString("reply-minecraft", substituteValues)));
            }
            else {
                String newlineReplacement = Objects.requireNonNull(
                        lang.getString("minecraft.reply-newline-replacement")
                );
                HashMap<String, LangSubstitutionValue> substituteValues = new HashMap<>(Map.of(
                        "replySender", new LangSubstitutionValue(getTelegramUserFullName(msg.replyToMessage().from())),
                        "replyText", new LangSubstitutionValue(decomposeTelegramText(
                                getMessageText(msg.replyToMessage()),
                                getMessageEntities(msg.replyToMessage()),
                                newlineReplacement
                        ))
                ));

                result.addAll(lang.formatString("minecraft.message-meta.reply", substituteValues));
                result.add(new TextComponent(" "));
            }
        }
        if (msg.viaBot() != null) {
            result.add(new TextComponent(lang.formatMessageMetaString("via-bot", "viaBotUsername", msg.viaBot().username())));
        }
        if (msg.poll() != null) {
            result.add(new TextComponent(lang.formatMessageMetaString("poll", "pollQuestion", msg.poll().question())));
        }
        if (msg.dice() != null) {
            result.add(new TextComponent(lang.formatMessageMetaString("dice", "diceValue", String.valueOf(msg.dice().value()))));
        }
        if (msg.photo() != null) { result.add(new TextComponent(lang.getMessageMetaString("photo"))); }
        if (msg.sticker() != null) { result.add(new TextComponent(lang.getMessageMetaString("sticker"))); }
        if (msg.animation() != null) { result.add(new TextComponent(lang.getMessageMetaString("gif"))); }
        else if (msg.document() != null) { result.add(new TextComponent(lang.getMessageMetaString("file"))); }
        if (msg.audio() != null) { result.add(new TextComponent(lang.getMessageMetaString("audio"))); }
        if (msg.video() != null) { result.add(new TextComponent(lang.getMessageMetaString("video"))); }
        if (msg.videoNote() != null) { result.add(new TextComponent(lang.getMessageMetaString("videomessage"))); }
        if (msg.voice() != null) { result.add(new TextComponent(lang.getMessageMetaString("voicemessage"))); }
        if (msg.contact() != null) { result.add(new TextComponent(lang.getMessageMetaString("contact"))); }
        if (msg.game() != null) { result.add(new TextComponent(lang.getMessageMetaString("game"))); }
        if (msg.venue() != null) { result.add(new TextComponent(lang.getMessageMetaString("venue"))); }
        if (msg.location() != null) { result.add(new TextComponent(lang.getMessageMetaString("geo"))); }
        if (msg.pinnedMessage() != null) { result.add(new TextComponent(lang.getMessageMetaString("pin"))); }
        if (msg.newChatMembers() != null) {
            ArrayList<String> names = new ArrayList<>();
            for (User user : msg.newChatMembers()) {
                names.add(getTelegramUserFullName(user));
            }
            String joint_names = String.join(",", names);

            if (msg.newChatMembers().length == 1) {
                result.add(new TextComponent(lang.formatMessageMetaString("invite-one", "userInvited", joint_names)));
            }
            else {
                result.add(new TextComponent(lang.formatMessageMetaString("invite-many", "usersInvited", joint_names)));
            }
        }
        if (msg.newChatTitle() != null) {
            result.add(new TextComponent(lang.formatMessageMetaString("change-title", "newTitle", msg.newChatTitle())));
        }
        if (msg.newChatPhoto() != null) {
            result.add(new TextComponent(lang.getMessageMetaString("change-photo")));
        }
        if (msg.voiceChatScheduled() != null) {
            result.add(new TextComponent(lang.getMessageMetaString("schedule-voice-chat")));
        }
        if (msg.voiceChatStarted() != null) {
            result.add(new TextComponent(lang.getMessageMetaString("start-voice-chat")));
        }
        if (msg.voiceChatEnded() != null) {
            result.add(new TextComponent(lang.getMessageMetaString("finish-voice-chat")));
        }
        if (msg.voiceChatParticipantsInvited() != null) {
            ArrayList<String> names = new ArrayList<>();
            for (User user : msg.voiceChatParticipantsInvited().users()) {
                names.add(getTelegramUserFullName(user));
            }
            String joint_names = String.join(",", names);

            if (names.size() == 1) {
                result.add(new TextComponent(lang.formatMessageMetaString("invite-one-voice-chat", "userInvited", joint_names)));
            }
            else {
                result.add(new TextComponent(lang.formatMessageMetaString("invite-many-voice-chat", "usersInvited", joint_names)));
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
                return lang.formatTelegramString("announcement-message.server-enabled-zero-online",
                        "announcement", pinnedListAnnouncement);
            }
            return lang.getString("telegram.zero-online");
        }
        if (includeAnnouncement) {
            return lang.formatTelegramString("announcement-message.server-enabled", Map.of(
                    "announcement", pinnedListAnnouncement,
                    "onlineCount", String.valueOf(player_cnt),
                    "onlinePlayers", joint_names
            ));
        }
        else {
            return lang.formatTelegramString("list", Map.of(
                    "onlineCount", String.valueOf(player_cnt),
                    "onlinePlayers", joint_names
            ));
        }
    }

    boolean checkForCommand(String text, String command) {
        return text != null && (text.equalsIgnoreCase("/" + command) || text.equalsIgnoreCase("/" + command + "@" + bot_username));
    }

    ArrayList<TextComponent> decomposeTelegramText(String text, MessageEntity[] entities, String newlineReplacement) {
        if (entities == null) {
            return new ArrayList<>(Collections.singleton(new TextComponent(text)));
        }
        ArrayList<TextComponent> res = new ArrayList<>();
        ArrayList<TelegramEntityEvent> events = new ArrayList<>();

        int[] prefixNewlineCount = new int[text.length() + 1];
        prefixNewlineCount[0] = 0;

        for (int i = 0; i < text.length(); ++i) {
            prefixNewlineCount[i + 1] = prefixNewlineCount[i];
            if (text.charAt(i) == '\n') {
                ++prefixNewlineCount[i + 1];
            }
        }

        text = text.replace("\n", newlineReplacement);

        for (MessageEntity entity : entities) {
            TelegramEntityType type = TelegramEntityType.fromString(entity.type().toString());
            if (type == null) {
                continue;
            }
            String entityData = null;

            int newlineReplacementDelta = newlineReplacement.length() - 1;
            int entity_begin = entity.offset() + prefixNewlineCount[entity.offset() + 1] * newlineReplacementDelta;
            int entity_end = (entity.offset() + entity.length() +
                prefixNewlineCount[entity.offset() + entity.length()] * newlineReplacementDelta);
            
            if (type == TelegramEntityType.LINK) {
                if (entity.type() == MessageEntity.Type.url) {
                    entityData = text.substring(entity_begin, entity_end);
                    if (!entityData.contains("://")) {
                        entityData = "http://" + entityData;
                    }
                }
                else if (entity.type() == MessageEntity.Type.text_link) {
                    entityData = entity.url();
                }
                else if (entity.type() == MessageEntity.Type.mention) {
                    entityData = "https://t.me/" + text.substring(entity_begin + 1, entity_end);
                }
                else {
                    throw new RuntimeException("Unknown link entity type: " + entity.type().toString());
                }
            }
            else if (type == TelegramEntityType.SPOILER) {
                entityData = text.substring(entity_begin, entity_end);
            }
            events.add(new TelegramEntityEvent(entity_begin, true, type, entityData));
            events.add(new TelegramEntityEvent(entity_end, false, type));
        }
        events.sort(TelegramEntityEvent::comparator);
        int eventPtr = 0;
        StringBuilder lastTextComponent = new StringBuilder();
        TextComponent formatting = new TextComponent("used for preserving style");
        boolean underlineEntity = false, linkEntity = false;
        for (int i = 0; i < text.length(); ++i) {
            boolean styleChanged = false;
            TextComponent oldFormatting = new TextComponent(formatting);
            while (eventPtr < events.size() && events.get(eventPtr).position == i) {
                styleChanged = true;
                TelegramEntityEvent curEvent = events.get(eventPtr);
                if (curEvent.type == TelegramEntityType.BOLD) {
                    formatting.setBold(curEvent.opening ? true : null);
                }
                else if (curEvent.type == TelegramEntityType.ITALIC) {
                    formatting.setItalic(curEvent.opening ? true : null);
                }
                else if (curEvent.type == TelegramEntityType.UNDERLINE) {
                    underlineEntity = curEvent.opening;
                    formatting.setUnderlined(underlineEntity || linkEntity ? true : null);
                }
                else if (curEvent.type == TelegramEntityType.STRIKETHROUGH) {
                    formatting.setStrikethrough(curEvent.opening ? true : null);
                }
                else if (curEvent.type == TelegramEntityType.LINK) {
                    linkEntity = curEvent.opening;
                    formatting.setUnderlined(underlineEntity || linkEntity ? true : null);
                    if (curEvent.opening) {
                        formatting.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, curEvent.data));
                    }
                    else {
                        formatting.setClickEvent(null);
                    }
                }
                else if (curEvent.type == TelegramEntityType.SPOILER) {
                    formatting.setObfuscated(curEvent.opening);
                    if (curEvent.opening) {
                        formatting.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(curEvent.data)));
                    }
                    else {
                        formatting.setHoverEvent(null);
                    }
                }
                else {
                    throw new RuntimeException("Unknown curEvent type while decomposing message: " + curEvent.type);
                }
                ++eventPtr;
            }
            if (styleChanged) {
                TextComponent toAdd = new TextComponent(lastTextComponent.toString());
                lastTextComponent = new StringBuilder();
                toAdd.copyFormatting(oldFormatting);
                toAdd.setHoverEvent(oldFormatting.getHoverEvent());
                toAdd.setClickEvent(oldFormatting.getClickEvent());
                res.add(toAdd);
            }
            lastTextComponent.append(text.charAt(i));
        }
        formatting.setText(lastTextComponent.toString());
        res.add(formatting);
        return res;
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
        long startupDate = System.currentTimeMillis() / 1000;
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null) {
                    if (update.message().date() < startupDate) {
                        continue;
                    }
                    if (update.message().chat().id() == chatID) {
                        if (checkForCommand(update.message().text(), "list")) {
                            sendMessage(getListMessage(false));
                            continue;
                        }
                        String messageText = getMessageText(update.message());
                        String newlineReplacement = Objects.requireNonNull(
                                lang.getString("minecraft.message-newline-replacement")
                        );
                        ArrayList<TextComponent> components = decomposeTelegramText(messageText,
                                getMessageEntities(update.message()),
                                newlineReplacement);
                        plugin.getServer().spigot().broadcast(lang.formatString("minecraft.base-message", Map.of(
                                "senderName", new LangSubstitutionValue(getTelegramUserFullName(update.message().from())),
                                "messageMeta", new LangSubstitutionValue(serializeMessageMeta(update.message())),
                                "messageText", new LangSubstitutionValue(components)
                        )).toArray(new TextComponent[0]));
                        plugin.getServer().getConsoleSender().spigot().sendMessage(lang.formatString("minecraft.base-message", Map.of(
                                "senderName", new LangSubstitutionValue(getTelegramUserFullName(update.message().from())),
                                "messageMeta", new LangSubstitutionValue(serializeMessageMeta(update.message())),
                                "messageText", new LangSubstitutionValue(components)
                        )).toArray(new TextComponent[0]));
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
