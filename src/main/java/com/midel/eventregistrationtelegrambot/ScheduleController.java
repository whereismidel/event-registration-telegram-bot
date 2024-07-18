package com.midel.eventregistrationtelegrambot;

import com.midel.eventregistrationtelegrambot.entity.User;
import com.midel.eventregistrationtelegrambot.google.SheetAPI;
import com.midel.eventregistrationtelegrambot.repository.UserRepository;
import com.midel.eventregistrationtelegrambot.telegram.TelegramSender;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableAsync
@RequiredArgsConstructor
public class ScheduleController {

    private final SheetAPI sheetAPI;
    private final TelegramSender telegramSender;
    private final UserRepository userRepository;

    @Scheduled(cron = "0/10 0/1 * ? * *", zone = "Europe/Kiev")
    public void updateDataSheet() {
        List<User> userList = userRepository.findAll();

        sheetAPI.updateUserTable(userList, "1WjugTJe9ss4-OqHKmG-WifLKHrL69aXYwNQ8leqboW0", "Користувачі!A1");
    }

    @Scheduled(cron = "40 21 2 ? * *", zone = "Europe/Kiev")
    public void test() {
        telegramSender.htmlMessage(787943933L, "Я працюю блін");
    }

}
