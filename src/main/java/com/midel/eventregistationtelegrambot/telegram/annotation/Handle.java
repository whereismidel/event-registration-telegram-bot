package com.midel.eventregistationtelegrambot.telegram.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Handle {
    Action value();
    String command();
}

