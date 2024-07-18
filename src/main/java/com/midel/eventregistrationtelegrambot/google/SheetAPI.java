package com.midel.eventregistrationtelegrambot.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.midel.eventregistrationtelegrambot.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SheetAPI {
    private static final String APPLICATION_NAME = "FIRST_SCHOLARSHIP";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);

    private static Sheets sheetService;

    @Value("${google.credentials}")
    private String credentials;

    public void updateUserTable(List<User> userList, String sheetId, String range) {
        try {
            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            InputStream in = new ByteArrayInputStream(credentials.getBytes());

            GoogleCredentials googleCredential = GoogleCredentials.fromStream(in)
                    .createScoped(SCOPES);


            if (sheetService == null) {
                sheetService =
                        new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(googleCredential))
                                .setApplicationName(APPLICATION_NAME)
                                .build();
            }

            // Create data to be written
            List<List<Object>> values = new ArrayList<>();
            values.add(
                List.of("Учасник", "Номер телефону", "Телеграм", "Ідентифікатор", "Етап", "Статус", "Дата реєстрації")
            );

            for(User user : userList) {
                values.add(
                    List.of(
                        (user.getFirstName() != null?user.getFirstName():"") + " " + (user.getLastName() != null?user.getLastName():""),
                        user.getPhoneNumber() != null?user.getPhoneNumber():"-",
                        user.getUsername() != null?"@" + user.getUsername():"-",
                        user.getId().toString(),
                        user.getState().name(),
                        user.getStatus().name(),
                        user.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                                .withZone(ZoneId.of("Europe/Kiev")))
                    )
                );
            }

            // Prepare the value range to be written
            ValueRange body = new ValueRange()
                    .setValues(values);

            // Write data to the spreadsheet
            sheetService.spreadsheets().values()
                    .update(sheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();

            log.info("Data written to the spreadsheet.");
        } catch (Exception e) {
            log.error("Failed to update user table.", e);
        }
    }
}
