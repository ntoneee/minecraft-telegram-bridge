package one.ntonee.mctgbridge;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class TelegramApi {
    private final TelegramBot bot;
    private final long chat_id;

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

        if (msg.viaBot() != null) { result += "[через @" + msg.viaBot().username() + "] "; }
        if (msg.poll() != null) { result += "[Опрос] "; }
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

    TelegramApi(FileConfiguration config) throws RuntimeException {
        bot = new TelegramBot(config.getString("telegram-token"));
        chat_id = config.getLong("telegram-chat-id");
        BaseResponse resp = bot.execute(new SendChatAction(chat_id, ChatAction.typing));
        if (!resp.isOk()) {
            throw new RuntimeException("Something went wrong while initializing Telegram API!\n" +
                    "Maybe you didn't correctly fill config.yml?\n" +
                    "Error description: " + resp.errorCode() + " " + resp.description());
        }
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null) {
                    String res_text = "[" + ChatColor.AQUA + getTelegramUserFullName(update.message().from()) + ChatColor.RESET +  "] ";
                    res_text += serializeMessageMeta(update.message());
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
