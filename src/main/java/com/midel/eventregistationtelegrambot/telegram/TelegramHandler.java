package com.midel.eventregistationtelegrambot.telegram;

import com.midel.eventregistationtelegrambot.telegram.annotation.Action;
import com.midel.eventregistationtelegrambot.telegram.annotation.AdminAction;
import com.midel.eventregistationtelegrambot.telegram.annotation.Handle;
import com.midel.eventregistationtelegrambot.telegram.annotation.TelegramController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TelegramHandler {

    private final ApplicationContext applicationContext;

    @Value("${bot.admins}")
    private List<Long> adminList;

    @Autowired
    public TelegramHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        initializeHandlers();
    }

    private void initializeHandlers() {
        Set<String> uniquePairs = new HashSet<>();
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(TelegramController.class);
        for (Object controller : controllers.values()) {
            Method[] methods = controller.getClass().getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Handle.class)) {
                    Handle handle = method.getAnnotation(Handle.class);
                    String pair = handle.value().name() + ":" + handle.command().toLowerCase();
                    if (!uniquePairs.add(pair)) {
                        throw new RuntimeException("Duplicate handler found for action: " + handle.value() + " and command: " + handle.command());
                    }
                }
            }
        }
    }

    public void getResult(Action action, String command, List<String> arguments, Update update) {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(TelegramController.class);
        for (Object controller : controllers.values()) {
            Method[] methods = controller.getClass().getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Handle.class)) {
                    Handle handle = method.getAnnotation(Handle.class);
                    if (handle.value().equals(action) && (handle.command().equalsIgnoreCase("BY_USER_STATE") || handle.command().equalsIgnoreCase(command))) {
                        try {
                            if (method.isAnnotationPresent(AdminAction.class)) {
                                Long userId;
                                if (update.hasCallbackQuery()) {
                                    userId = update.getCallbackQuery().getFrom().getId();
                                } else {
                                    userId = update.getMessage().getFrom().getId();
                                }

                                if (!adminList.contains(userId)) {
                                    return;
                                }
                            }

                            method.invoke(controller, arguments, update);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                        return;
                    }
                }
            }
        }
        throw new RuntimeException("No suitable handler found for action: " + action + " and command: " + command);
    }

}
