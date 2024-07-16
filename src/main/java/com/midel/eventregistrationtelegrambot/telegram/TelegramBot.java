package com.midel.eventregistrationtelegrambot.telegram;

import com.midel.eventregistrationtelegrambot.telegram.action.Command;
import com.midel.eventregistrationtelegrambot.telegram.annotation.Action;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Getter
@Component
@Slf4j
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botUsername;
    private final String botToken;

    private final TelegramClient telegramClient;
    private final TelegramHandler telegramHandler;


    @Autowired
    public TelegramBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.username}") String botUsername,
            @Lazy TelegramHandler telegramHandler
    ) {

        this.botToken = botToken;
        this.botUsername = botUsername;

        this.telegramHandler = telegramHandler;

        telegramClient = new OkHttpTelegramClient(botToken);
        log.info("Connected to Telegram bot @{}", getBotUsername());
    }


    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        try {
//
//            if (update.hasCallbackQuery()) {
//                handleCallbackQuery(update);
//                return;
//            }
//
//            if (update.hasPollAnswer()) {
//                handlePollAnswer(update);
//                return;
//            }

            if (update.hasMessage()) {
                handleMessage(update);
            }

        } catch (Exception e) {
            log.error("An error occurred while processing a message from a user", e);
        }
    }


    private void handleMessage(Update update) {
        Message message = update.getMessage();

        String text;
        if (message.hasText()) {
            text = message.getText().trim();
        } else if (message.hasContact()) {
            text = message.getContact().getPhoneNumber();
            message.setText(text);
        } else {
            telegramHandler.getResult(Action.COMMAND, Command.CommandIdentifier.START.getName(), null, update);
            return;
        }

        logUserAction(update);

        if (text.startsWith(Command.PREFIX)){
            handleCommand(update, text);
        } else {
            handleTextMessage(update);
        }

    }

    private void handleTextMessage(Update update) {
        try {

            telegramHandler.getResult(Action.REPLY_TO, "BY_USER_STATE", null, update);

        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void handleCommand(Update update, String text) {
        try {

            Command command = new Command(text.replace(Command.MENTIONED + getBotUsername().toLowerCase(), ""));
            telegramHandler.getResult(Action.COMMAND, command.getIdentifier().getName(), command.getArguments(), update);

        } catch (IllegalArgumentException iae) {
            telegramHandler.getResult(Action.COMMAND, "not_found", null, update);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void logUserAction(Update update) {
        Message message;
        String prefix;
        String text;
        User from;

        if (update.hasCallbackQuery()) {
            message = (Message) update.getCallbackQuery().getMessage();
            prefix = "Received callback query ";
            text = update.getCallbackQuery().getData();
            from = update.getCallbackQuery().getFrom();
        } else {
            message = update.getMessage();
            prefix = "Received message ";
            text = update.getMessage().getText();
            from = message.getFrom();
        }

        if (message.getChat().getType().equals("private")) {
            log.info("{}from user @{}(id={}): {}\n{}", prefix, from.getUserName(), from.getId(), text, update);
        } else {
            log.info("{}from group \"{}\"(id={}), user @{}(id={}): {}\n{}", prefix, message.getChat().getTitle(), message.getChat().getId(), from.getUserName(), from.getId(), text, update);
        }
    }
}
