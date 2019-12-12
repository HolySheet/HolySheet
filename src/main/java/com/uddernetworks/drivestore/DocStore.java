package com.uddernetworks.drivestore;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class DocStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocStore.class);

    private AuthManager authManager;
    private DocManager docManager;
    private Docs docs;
    private Drive drive;

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        new DocStore().main();
    }

    private void main() throws GeneralSecurityException, IOException {
        authManager = new AuthManager();
        authManager.initialize();
        docs = authManager.getDocs();
        drive = authManager.getDrive();

        docManager = new DocManager(this);
        docManager.init();
    }

    public Docs getDocs() {
        return docs;
    }

    public Drive getDrive() {
        return drive;
    }
}
