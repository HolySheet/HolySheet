package com.uddernetworks.drivestore.docs;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.uddernetworks.drivestore.Utility.progressBar;

public class ProgressListener implements MediaHttpUploaderProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressListener.class);

    private final String fileName;

    public ProgressListener(String fileName) {
        this.fileName = fileName;
    }

    public void progressChanged(MediaHttpUploader uploader) throws IOException {
        switch (uploader.getUploadState()) {
            case MEDIA_COMPLETE:
                System.out.print('\r');
                System.out.print(progressBar("Uploading " + fileName + ":", 30, uploader.getProgress()));
                System.out.println();
                LOGGER.info("Upload complete");
                break;
            case MEDIA_IN_PROGRESS:
                System.out.print('\r');
                System.out.print(progressBar("Uploading " + fileName + ":", 30, uploader.getProgress()));
                break;
        }
    }
}