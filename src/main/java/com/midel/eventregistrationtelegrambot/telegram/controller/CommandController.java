package com.midel.eventregistrationtelegrambot.telegram.controller;

import com.midel.eventregistrationtelegrambot.entity.User;
import com.midel.eventregistrationtelegrambot.entity.enums.State;
import com.midel.eventregistrationtelegrambot.entity.enums.Status;
import com.midel.eventregistrationtelegrambot.repository.UserRepository;
import com.midel.eventregistrationtelegrambot.telegram.TelegramSender;
import com.midel.eventregistrationtelegrambot.telegram.annotation.Action;
import com.midel.eventregistrationtelegrambot.telegram.annotation.AdminAction;
import com.midel.eventregistrationtelegrambot.telegram.annotation.Handle;
import com.midel.eventregistrationtelegrambot.telegram.annotation.TelegramController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@TelegramController
@RequiredArgsConstructor
@Slf4j
public class CommandController {

    private final TelegramSender telegramSender;
    private final UserRepository userRepository;

    @Handle(value = Action.COMMAND, command = "start")
    public void startCommand(List<String> arguments, Update update) {

        User user = userRepository.getUserById(update.getMessage().getFrom().getId()).orElse(null);

        if (user == null) {
//            telegramSender.htmlMessage(update.getMessage().getChatId(), """
//                    <b>Привіт! Я допоможу тобі зареєструватися у розіграші твоєї першої стипендії за вступ до НАУ.</b>
//
//                    Випускники НАУ створили фонд, щоб <b><u>розіграти 20 перших стипендій у розмірі 3000 грн</u></b> серед тих, хто стане студентом цього року.
//
//                    <u>Як стати одним/однією з 20 переможців:</u>
//                    1. <b>Пройти реєстрацію</b> у цьому боті
//                    2. <b>Вступити до НАУ</b> на бакалавра на базі ПСЗО
//                    3. <b>Не блокувати бота</b> до 1 вересня, адже саме через нього ми зв'яжемося з переможцями
//
//                    📄<b><a href="https://vstup2024.nau.edu.ua/rules">Правила розіграшу</a></b>
//                    """);

            telegramSender.htmlMessage(update.getMessage().getChatId(), """
                    Для реєстрації тебе як учасника/ці нам потрібно твоє <i>прізвище, ім'я та номер телефону</i>. Надаючи цю інформацію ти автоматично даєш згоду на її обробку в рамках проведення вступної кампанії НАУ.
                    
                    <u>Не хвилюйся, ці дані захищені та потрібні лише для визначення переможця.</u>
                    """);

            user = User.builder()
                    .id(update.getMessage().getFrom().getId())
                    .state(State.NAME)
                    .status(Status.ACTIVE)
                    .username(update.getMessage().getFrom().getUserName())
                    .build();

            user = userRepository.save(user);
        }

        handleState(update, user);
    }

    @Handle(value = Action.COMMAND, command = "notify")
    @AdminAction
    public void notifyCommand(List<String> arguments, Update update) {

        if (update.getMessage().getReplyToMessage() == null) {
            telegramSender.htmlMessage(update.getMessage().getChatId(),
                    """
                        Використання:
                        Відміть повідомлення для розсилки і напиши /notify
                        """);
            return;
        }

        List<User> userList = userRepository.findAll();
        long delay = 200;

        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            for (int i = 0; i < userList.size(); i++) {
                int finalI = i;
                scheduler.schedule(() ->
                                telegramSender.forwardMessage(update.getMessage().getChatId(), update.getMessage().getReplyToMessage().getMessageId(), userList.get(finalI).getId()),
                        delay * i,
                        TimeUnit.MILLISECONDS
                );
            }

            scheduler.schedule(() -> {
                telegramSender.htmlMessage(update.getMessage().getChatId(), "Розсилка завершена.");
                scheduler.shutdown();
            }, delay * userList.size(), TimeUnit.MILLISECONDS);
        }
    }


    @Handle(value = Action.COMMAND, command = "block")
    @AdminAction
    public void blockCommand(List<String> arguments, Update update) {



        if (arguments == null || arguments.size() != 1) {

            telegramSender.htmlMessage(update.getMessage().getChatId(),
                    """
                        Використання:
                        /block userId
                        """);
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(arguments.getFirst());
        } catch (Exception e) {
            telegramSender.htmlMessage(update.getMessage().getChatId(),
                    """
                        Використання:
                        /block userId
                        """);
            return;
        }
        User user = userRepository.getUserById(userId).orElse(null);

        if (user == null) {
            telegramSender.htmlMessage(update.getMessage().getChatId(),
                    """
                        User not found.
                        """);
            return;
        }

//        userRepository.delete(user);

    }

    @Handle(value = Action.COMMAND, command = "not_found")
    @AdminAction
    public void notFoundCommand(List<String> arguments, Update update) {
        telegramSender.htmlMessage(update.getMessage().getChatId(), "Command doesn't exist.");
    }

    @Handle(value = Action.REPLY_TO, command = "BY_USER_STATE")
    public void stateReply(List<String> arguments, Update update) {

        User user = userRepository.getUserById(update.getMessage().getFrom().getId()).orElse(null);
        if (user == null) {
            startCommand(arguments, update);
            return;
        }

        String userInput;
        if (update.getMessage().hasContact()) {
            userInput = update.getMessage().getContact().getPhoneNumber();
        } else {
            userInput = update.getMessage().getText();
        }

        user.setState(
                switch (user.getState()) {
                    case NAME -> {
                        user.setFirstName(Character.toUpperCase(userInput.charAt(0)) + userInput.substring(1).toLowerCase());

                        yield State.SURNAME;
                    }
                    case SURNAME -> {
                        user.setLastName(Character.toUpperCase(userInput.charAt(0)) + userInput.substring(1).toLowerCase());
                        yield State.PHONE;
                    }
                    case PHONE -> {

                        if (userInput.length() < 10 || !userInput.matches("[\\d\\s+]+")) {
                            telegramSender.htmlMessage(update.getMessage().getChatId(),"""
                            Введений номер не є дійсним. Будь ласка, введи правильний номер телефону.
                            """);
                            yield State.PHONE;
                        }

                        user.setPhoneNumber(
                                "+38" + userInput
                                    .replace(" ", "")
                                    .replaceFirst("^\\+?38", "")
                        );

                        telegramSender.htmlMessage(
                                update.getMessage().getChatId(),
                        String.format("""
                        <b>Дякую! Тепер ти приймаєш участь у Акції Моя перша стипендія</b>

                        Твої дані:
                        Ім'я та прізвище - %s
                        Номер телефону - %s

                        <i>Не блокуй бота, адже саме тут 1 вересня ми анонсуємо переможця: <b>можливо це будеш саме ти!</b></i>
                        """, user.getFirstName() + " " + user.getLastName(), user.getPhoneNumber()));
                        yield State.REGISTERED;
                    }

                    case REGISTERED -> State.REGISTERED;
                }
        );

        userRepository.save(user);
        handleState(update, user);
    }

    private void handleState(Update update, User user) {
        String message;
        switch (user.getState()) {
            case NAME -> {
                message = """
                        Напиши своє <u><b>ім'я</b></u>. Приклад: Іван
                        """;
                telegramSender.htmlMessage(update.getMessage().getChatId(), message);
            }
            case SURNAME -> {
                message = """
                        Напиши своє <u><b>прізвище</b></u>. Приклад: Петрощук
                        """;
                telegramSender.htmlMessage(update.getMessage().getChatId(), message);
            }
            case PHONE -> {
                message = """
                        Напиши свій <u><b>номер телефону</b></u>. Можеш скористатись кнопкою нижче
                        Або написати вручну. Приклад: 0681345903

                        <u>Він нам буде потрібний, щоб зв'язатись з тобою у разі перемоги! Тому вказуй уважно</u>
                        """;
                telegramSender.requestContact(update.getMessage().getChatId(), message);
            }
            case REGISTERED  -> {
                message = """
                        <b>А поки чекаєш результатів..</b>
                        
                        <b>Підпишись</b> на канал вступника, щоб точно нічого не пропустити про вступ.
                        
                        Також <b>долучайся до чату</b> абітурієнтів, щоб в режимі онлайн отримувати відповіді про навчання та двіж в НАУ від наших студентів.
                        
                        <b><a href="https://t.me/pknau">Вступник НАУ</a> | <a href="https://t.me/nau_abit_chat">Чат абітурієнтів</a></b>
                        """;
                telegramSender.htmlMessageWithBottomPhoto(update.getMessage().getChatId(), message, "https://telegra.ph/file/13e6fded376bb93add4c2.png");
            }
        }
    }

}

