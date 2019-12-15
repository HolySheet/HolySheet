package com.uddernetworks.drivestore;

import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class DocStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocStore.class);

    private AuthManager authManager;
    private DocManager docManager;
    private Drive drive;
    private Sheets sheets;

    public static void main(String[] args) {
        new DocStore().start(args);
    }

    private void start(String[] args) {
        var commandHandler = new CommandHandler();
        commandHandler.parseCommand(args, this, () -> {
            try {
                LOGGER.info("Initializing everything...");

                authManager = new AuthManager();
                authManager.initialize();
                drive = authManager.getDrive();
                sheets = authManager.getSheets();

                docManager = new DocManager(this);
                docManager.init();
            } catch (GeneralSecurityException | IOException e) {
                LOGGER.error("Error initializing", e);
            }
        });
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public DocManager getDocManager() {
        return docManager;
    }

    public Drive getDrive() {
        return drive;
    }

    public Sheets getSheets() {
        return sheets;
    }
}
