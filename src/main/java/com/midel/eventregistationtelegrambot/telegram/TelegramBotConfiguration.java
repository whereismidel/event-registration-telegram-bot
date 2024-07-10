package com.midel.eventregistationtelegrambot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
@Slf4j
public class TelegramBotConfiguration {

    @Bean
    public BotSession telegramBotsApi(TelegramBot telegramBot) {

        BotSession botSession;
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botSession = botsApplication.registerBot(telegramBot.getToken(), telegramBot);

            log.info("Connected to Telegram bot @{}", telegramBot.getUsername());
            return botSession;
        } catch (Exception e) {
            log.error("Failed to connect to Telegram bot @{}", telegramBot.getUsername(), e);
            return null;
        }

    }

}
