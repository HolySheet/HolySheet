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

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        new DocStore().start(args);
    }

    private void start(String[] args) throws GeneralSecurityException, IOException {
        var commandHandler = new CommandHandler();
        commandHandler.parseCommand(args, this, () -> {
            try {
                LOGGER.info("Initializing everything...");

                authManager = new AuthManager();
                authManager.initialize();
//                docs = authManager.getDocs();
                drive = authManager.getDrive();
                sheets = authManager.getSheets();

                docManager = new DocManager(this);
                docManager.init();

//                new SheetManager(sheets).init();
            } catch (GeneralSecurityException | IOException e) {
                LOGGER.error("Error initializing", e);
            }
        });



//        LOGGER.info("Listing...");
//        var uploads = docManager.listUploads();
//        uploads.forEach(file -> {
//            System.out.println(file.getName() + " > " + file.getId());
//        });

//        docManager.uploadData("This is a test of some text lmao maybe this will work, maybe it won't, who really knows. This should be capable of storing any binary values, at a very large capacity due to being able to store an entire long (64 bytes) in a single character.".getBytes()).ifPresent(id -> {
//            LOGGER.info("Created ID: {}", id);
//        });

        // Has data: 1RfXAXrkIsqvMc9nOCjB23HcEP9VIsy5BZr5S4EZddgo

//        docManager.retrieveData("1-Wpyi94msOtOdKDQT4ahQyPPgqkNiYKefxjVTJVrgwc").ifPresentOrElse(bytes -> {
//            LOGGER.info("Found data:\n\t{}", new String(bytes));
//        }, () -> {
//            LOGGER.error("No data found!");
//        });

        /*

        This is a test of some text lmao maybe this will work, maybe it won't, who really knows.
        This should be capable of storing any binary values, at a very large capacity due to being
        able to store an entire long (64 bytes) in a single character.

         */
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
