package com.uddernetworks.drivestore.docs;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ProgressListener implements MediaHttpUploaderProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressListener.class);

    private final String name;

    public ProgressListener(String name) {
        this.name = name;
    }

    public void progressChanged(MediaHttpUploader uploader) throws IOException {
        switch (uploader.getUploadState()) {
            case INITIATION_STARTED:
                LOGGER.info("[{}] Initiation started", name);
                break;
            case INITIATION_COMPLETE:
                LOGGER.info("[{}] Initiation is complete", name);
                break;
            case MEDIA_IN_PROGRESS:
                LOGGER.info("[{}] {}%", name, uploader.getProgress());
                break;
            case MEDIA_COMPLETE:
                LOGGER.info("[{}] Upload complete", name);
                break;
        }
    }
}