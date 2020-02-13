package com.uddernetworks.holysheet;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.uddernetworks.holysheet.utility.Utility.credentialsReader;

/**
 * Adds support for arbitrary requests for users with given credentials upon a gRPC request. This is based off of no
 * initial credentials, and assumes the user already has the <code>https://www.googleapis.com/auth/drive</code> and
 * <code>https://www.googleapis.com/auth/spreadsheets</code> scopes.
 */
public class RemoteAuthManager implements AuthManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAuthManager.class);

    private static final String APPLICATION_NAME = "HolySheet";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CLIENT_SECRET = System.getenv("CLIENT_SECRET");

    private static NetHttpTransport HTTP_TRANSPORT;
    private static String clientId;
    private static String clientSecret;
    private Drive drive;
    private Sheets sheets;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error creating HTTP_TRANSPORT. This is fatal, aborting!", e);
            System.exit(0);
        }

        try {
            var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, credentialsReader(CLIENT_SECRET));
            clientId = clientSecrets.getDetails().getClientId();
            clientSecret = clientSecrets.getDetails().getClientSecret();
        } catch (IOException e) {
            LOGGER.error("Error reading client secrets! This is fatal, aborting!", e);
            System.exit(0);
        }
    }

    public void useToken(String accessToken) {
        try {
            var credentials = createCredentialWithRefreshToken(clientId, clientSecret, accessToken);

            drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                    .setApplicationName(APPLICATION_NAME)
                    .setHttpRequestInitializer(new BackOffInitializer(credentials))
                    .build();

            sheets = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Error verifying given token", e);
        }
    }

    public Credential createCredentialWithRefreshToken(String clientId, String clientSecret, String accessToken) {
        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod()).setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                .setClientAuthentication(new BasicAuthentication(clientId, clientSecret))
                .build()
                .setAccessToken(accessToken);
    }

    @Override
    public Sheets getSheets() {
        return sheets;
    }

    @Override
    public Drive getDrive() {
        return drive;
    }
}
