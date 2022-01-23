package one.ntonee.mctgbridge;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TelegramApi {
    private final TelegramBot bot;
    private final long chat_id;
    private final long bot_id;
    private final String bot_username;

    private String getTelegramUserFullName(User user) {
        if (user.lastName() != null) {
            return user.firstName() + " " + user.lastName();
        }
        return user.firstName();
    }

    private String serializeMessageMeta(Message msg) {
        String result = "";
        if (msg.forwardFrom() != null) { result = "[Переслано от " + getTelegramUserFullName(msg.forwardFrom()) + "] "; }
        else if (msg.forwardFromChat() != null) { result = "[Переслано от " + msg.forwardFromChat().title() + "] "; }
        else if (msg.forwardSenderName() != null) { result = "[Переслано от " + msg.forwardSenderName() + "] "; }
        else if (msg.replyToMessage() != null) {
            result = "[В ответ на ";
            //getTelegramUserFullName(msg.replyToMessage().from()) + " \"" + msg.replyToMessage().text() + "\"] "
            if (msg.replyToMessage().from().id() == bot_id) {
                result += msg.replyToMessage().text().substring(msg.replyToMessage().text().indexOf(' ') + 1);
            }
            else {
                result += "TG [" + getTelegramUserFullName(msg.replyToMessage().from()) + "] " + msg.replyToMessage().text();
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
        return escapeText(result);
    }

    TelegramApi(FileConfiguration config) throws RuntimeException {
        String token = config.getString("telegram-token");
        bot = new TelegramBot(token);
        long my_id = 0;
        for (int i = 0; i < token.length(); ++i) {
            if (token.charAt(i) == ':') break;
            my_id = my_id * 10 + token.charAt(i) - '0';
        }
        bot_id = my_id;
        chat_id = config.getLong("telegram-chat-id");
        BaseResponse resp = bot.execute(new SendChatAction(chat_id, ChatAction.typing));
        if (!resp.isOk()) {
            throw new RuntimeException("Something went wrong while initializing Telegram API!\n" +
                    "Maybe you didn't correctly fill config.yml?\n" +
                    "Error description: " + resp.errorCode() + " " + resp.description());
        }
        bot_username = bot.execute(new GetMe()).user().username();
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null && update.message().chat().id() == chat_id) {
                    if (Objects.equals(update.message().text(), "/list") || Objects.equals(update.message().text(), "/list@" + bot_username)) {
                        StringBuilder res_text = new StringBuilder();
                        int player_cnt = 0;
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            res_text.append(player_cnt != 0 ? ", " : "").append(player.getDisplayName());
                            ++player_cnt;
                        }
                        String suffix = (player_cnt % 100 >= 10 && player_cnt % 100 <= 20 || player_cnt % 10 >= 5 || player_cnt % 10 == 0 ? "ов" : player_cnt % 10 == 1 ? "" : "а");
                        String message = "На сервере " + player_cnt + " игрок" + suffix + ": " + res_text.toString();
                        sendMessage(message);
                        continue;
                    }
                    String res_text = "[" + ChatColor.AQUA + getTelegramUserFullName(update.message().from()) + ChatColor.RESET +  "] ";
                    res_text += ChatColor.ITALIC + serializeMessageMeta(update.message()) + ChatColor.RESET;
                    if (update.message().caption() != null) {
                        res_text += update.message().caption();
                    }
                    else if (update.message().text() != null) {
                        res_text += update.message().text();
                    }
                    Bukkit.broadcastMessage(res_text);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    void sendMessage(String text) throws RuntimeException {
        BaseResponse resp = bot.execute(new SendMessage(chat_id, text).parseMode(ParseMode.HTML));
        if (!resp.isOk()) {
            throw new RuntimeException("Error sending message: " + resp.description());
        }
    }

    String escapeText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
