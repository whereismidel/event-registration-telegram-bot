package com.midel.eventregistationtelegrambot.telegram.action;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Callback {

    @Getter
    public enum CallbackIdentifier {
        REMOVE_POLL, FINISH_POLL;

        public String getName() {
            return this.name().toLowerCase();
        }
    }

    public static final String PATTERN = "[#,]";

    private final CallbackIdentifier identifier;
    private final List<String> arguments;

    public Callback(CallbackIdentifier identifier, List<String> arguments) {
        this.identifier = identifier;
        this.arguments = arguments;
    }

    public Callback(String callbackData) {
        this.arguments = new ArrayList<>(Arrays.stream(callbackData.split(PATTERN)).toList());
        this.identifier = CallbackIdentifier.valueOf(arguments.getFirst().toUpperCase());
        arguments.removeFirst();
    }

    public String toCallbackData() {
        return identifier.name() + "#" + String.join(",", arguments);
    }
}
