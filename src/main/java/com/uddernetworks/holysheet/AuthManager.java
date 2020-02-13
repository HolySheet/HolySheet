package com.uddernetworks.holysheet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.GeneralSecurityException;

/**
 * Manages authentication, either locally or generically with a given token. Is in charge of creating {@link Sheets}
 * and {@link Drive}.
 */
public interface AuthManager {
    Sheets getSheets();

    Drive getDrive();

    class BackOffInitializer implements HttpRequestInitializer {

        private final Credential credential;

        public BackOffInitializer(Credential credential) {
            this.credential = credential;
        }

        @Override
        public void initialize(HttpRequest httpRequest) throws IOException {
            httpRequest.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(backOff()));
            credential.initialize(httpRequest);
            httpRequest.setConnectTimeout(300 * 60000);
            httpRequest.setReadTimeout(300 * 60000);
        }

        private final ExponentialBackOff.Builder BACK_OFF = new ExponentialBackOff.Builder().setInitialIntervalMillis(500);

        private BackOff backOff() {
            return BACK_OFF.build();
        }
    }
}
