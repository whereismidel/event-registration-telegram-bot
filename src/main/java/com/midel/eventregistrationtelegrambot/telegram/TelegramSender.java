package com.midel.eventregistrationtelegrambot.telegram;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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

    public Integer htmlForceReplyMessage(Long chatId, String text) {

        return (Integer) send(
            SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("html")
                .disableWebPagePreview(true)
                .replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build())
            .build()
        );

    }

    public Integer htmlMessageWithBottomPhoto(Long chatId, String text, String link) {

        return (Integer) send(
            SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .linkPreviewOptions(LinkPreviewOptions.builder().urlField(link).build())
                .parseMode("html")
            .build()
        );

    }

    public void forwardMessage(Long fromChat, int messageId, Long toChat) {

        send(
            CopyMessage.builder()
                .fromChatId(fromChat)
                .chatId(toChat)
                .messageId(messageId)
            .build()
        );

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

    public void sendDocument(Long chatId, InputFile inputFile) {

        try {
            telegramBot.getTelegramClient().execute(
                SendDocument.builder()
                    .chatId(chatId)
                    .document(inputFile)
                .build()
            );
        } catch (TelegramApiException e) {
            log.warn("Failed to send document: {}", e.getMessage());
        }

    }

    public Integer inlineKeyboard(Long chatId, String title, InlineKeyboardMarkup inlineKeyboardMarkup) {

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(title)
                .parseMode("html")
                .disableWebPagePreview(true)
                .build();

        if (inlineKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        }

        return (Integer) send(sendMessage);

    }

    public void deleteMessage(Long chatId, Integer messageId) {

        send(
            DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
            .build()
        );

    }

    private Object send(BotApiMethod<?> method) {

        try {
            switch (method) {
                case SendMessage sendMessage -> { return telegramBot.getTelegramClient().execute(sendMessage).getMessageId(); }
                case DeleteMessage deleteMessage -> { return telegramBot.getTelegramClient().execute(deleteMessage)?1:0; }
                case SendPoll sendPoll -> { return telegramBot.getTelegramClient().execute(sendPoll); }
                default -> telegramBot.getTelegramClient().execute(method);
            }

        } catch (TelegramApiException e) {
            log.warn("Failed to send message - {}. Method: {}", e.getMessage(), method);
        }

        return null;
    }
}
