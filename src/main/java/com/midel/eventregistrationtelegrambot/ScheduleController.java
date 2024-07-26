package com.midel.eventregistrationtelegrambot;

import com.midel.eventregistrationtelegrambot.entity.User;
import com.midel.eventregistrationtelegrambot.entity.enums.State;
import com.midel.eventregistrationtelegrambot.entity.enums.Status;
import com.midel.eventregistrationtelegrambot.google.SheetAPI;
import com.midel.eventregistrationtelegrambot.repository.UserRepository;
import com.midel.eventregistrationtelegrambot.telegram.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final SheetAPI sheetAPI;
    private final TelegramSender telegramSender;
    private final UserRepository userRepository;

    @Value("${google.sheet-id}")
    private String sheetId;

    @Scheduled(cron = "0 0/2 * ? * *", zone = "Europe/Kiev")
    public void updateDataSheet() {
        List<User> userList = userRepository.findAll();

        try {

            sheetAPI.updateUserTable(
                    userList.stream()
                        .sorted(Comparator.comparing(User::getRegisteredAt))
                        .collect(Collectors.toList()),
                    sheetId,
                    "Користувачі!A1"
            );
        } catch (Exception e) {
            sheetAPI.updateUserTable(userList, sheetId, "Користувачі!A1");
        }
    }


    @Scheduled(cron = "0 0 10/12 ? * *", zone = "Europe/Kiev")
//    @Scheduled(cron = "0/30 0/1 * ? * *", zone = "Europe/Kiev")
    public void sendScheduleStats() {
        sendStats(List.of(
//            List.of(787943933L),
            List.of(-1001316709142L, 1923L)
        ));
    }

    public void sendStats(List<List<Long>> chatsAndThreadId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusHours(12);
            LocalDateTime end = LocalDateTime.now();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM")
                    .withZone(ZoneId.of("Europe/Kiev"));

            List<User> totalUserList = userRepository.findAll();
            List<User> periodUserList = totalUserList.stream().filter(user -> user.getRegisteredAt().isAfter(start) && user.getRegisteredAt().isBefore(end)).toList();


            long finalStageCount = totalUserList.stream().filter(user -> user.getState().equals(State.REGISTERED)).count();
            long activeCount = totalUserList.stream().filter(user -> user.getStatus().equals(Status.ACTIVE)).count();

            for (List<Long> chatIdAndThreadId : chatsAndThreadId) {
                Long chatId = chatIdAndThreadId.getFirst();
                Integer threadId = chatIdAndThreadId.size() >= 2 ? Math.toIntExact(chatIdAndThreadId.get(1)) : null;

                telegramSender.htmlMessage(
                        chatId,
                        threadId,
                        String.format(
                                """
                                <b>Статистика за 12 годин <u>(%s - %s)</u></b>
                                
                                <b>Реєстрацій за вказаний період — %d</b>
                                <i>🔸завершили реєстрацію — %d</i>
                                
                                <b>Всього зареєструвалось — %d</b>
                                <i>🔹активних — %d</i>
                                <i>🔹заблокували бота — %d</i>
                                <i>🔸завершили реєстрацію — %d</i>
                                <i>🔸не пройшли всі етапи — %d</i>
                                """,
                                start.format(formatter), end.format(formatter),
                                periodUserList.size(),
                                periodUserList.stream().filter(user -> user.getState().equals(State.REGISTERED)).count(),
                                totalUserList.size(),
                                activeCount,
                                totalUserList.size() - activeCount,
                                finalStageCount,
                                totalUserList.size() - finalStageCount
                        )
                );
            }
        } catch (Exception e) {
            log.error("Failed to send stats.", e);
        }
    }
}
