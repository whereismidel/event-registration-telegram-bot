package com.midel.eventregistrationtelegrambot.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.midel.eventregistrationtelegrambot.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SheetAPI {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static Sheets sheetService;

    @Value("${google.credentials}")
    private String credentials;

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = new ByteArrayInputStream(credentials.getBytes());
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public void updateUserTable(List<User> userList, String sheetId, String range) {
        try {
            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            if (sheetService == null) {
                sheetService =
                        new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
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
                        user.getRegisteredAt().toString()
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
