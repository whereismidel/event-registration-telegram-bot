package com.midel.eventregistrationtelegrambot.telegram.action;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@ToString
public class Command {

    @Getter
    public enum CommandIdentifier {
        START("start",
                "Розпочати реєстрацію.",
                """
                :)
                """),
        NOTIFY("notify",
                "Розіслати сповіщення по всім зареєстрованим користувачам.",
                """
                ;)
                """),
        BLOCK("block",
                "Заблокувати користувача",
                """
                ;)
                """),
        STATS("stats",
                "Надати статистику по реєстраціям",
                """
                ;)
                """);

        private final String name;
        private final String description;
        private final String sample;

        CommandIdentifier(String name, String description, String sample) {
            this.name = name;
            this.description = description;
            this.sample = sample;
        }
    }

    public static final String PREFIX = "/";
    public static final String MENTIONED = "@";
    public static final Pattern PATTERN = Pattern.compile("\"([^\"]+)\"|\\S+");

    private final CommandIdentifier identifier;
    private final List<String> arguments;

    public Command(String userInput) {
        Matcher matcher = Command.PATTERN.matcher(userInput);

        this.arguments = new ArrayList<>();

        while (matcher.find()) {
            arguments.add(matcher.group(1) != null ? matcher.group(1) : matcher.group());
        }
        String safeIdentifier = arguments.getFirst().toLowerCase()
                .replace("/", "")
                .replace("?", "")
                .replace("&", "")
                .replace("=", "");
        this.identifier = CommandIdentifier.valueOf(safeIdentifier.toUpperCase());

        arguments.removeFirst();
    }
}
