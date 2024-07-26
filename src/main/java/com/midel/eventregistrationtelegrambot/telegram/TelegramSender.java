package com.midel.eventregistrationtelegrambot.telegram;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class TelegramSender {

    private final TelegramBot telegramBot;

    public Integer htmlMessage(Long chatId, Integer threadId, String text) {

        if (threadId == null) {
            return htmlMessage(chatId, text);
        }

        return (Integer) send(
            SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("html")
                .messageThreadId(threadId)
                .disableWebPagePreview(true)
            .build()
        );

    }

    public Integer htmlMessage(Long chatId, String text) {

        return (Integer) send(
                SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .parseMode("html")
                        .disableWebPagePreview(true)
                        .build()
        );

    }

    public boolean forwardMessage(Long fromChat, int messageId, Long toChat) {


        Object sendRequest = send(
            CopyMessage.builder()
                .fromChatId(fromChat)
                .chatId(toChat)
                .messageId(messageId)
            .build()
        );

        return sendRequest != null;
    }

    public Integer requestContact(Long chatId, String text) {

        return (Integer) send(
            SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("html")
                .disableWebPagePreview(true)
                .replyMarkup(
                    ReplyKeyboardMarkup.builder()
                        .oneTimeKeyboard(true)
                        .resizeKeyboard(true)
                        .keyboard(
                            List.of(new KeyboardRow(
                                List.of(
                                    KeyboardButton.builder()
                                        .requestContact(true)
                                        .text("Надіслати мій контакт")
                                    .build()
                                )
                            ))
                        )
                    .build()
                )
            .build()
        );

    }

    public void deleteMessage(Long chatId, Integer messageId) {

        send(
            DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
            .build()
        );

    }

    public Object send(BotApiMethod<?> method) {

        try {
            switch (method) {
                case SendMessage sendMessage -> { return telegramBot.getTelegramClient().execute(sendMessage).getMessageId(); }
                case DeleteMessage deleteMessage -> { return telegramBot.getTelegramClient().execute(deleteMessage)?1:0; }
                case SendPoll sendPoll -> { return telegramBot.getTelegramClient().execute(sendPoll); }
                default -> telegramBot.getTelegramClient().execute(method);
            }

            return true;

        } catch (TelegramApiException e) {
            log.warn("Failed to send message - {}. Method: {}", e.getMessage(), method);
        }

        return null;
    }
}
