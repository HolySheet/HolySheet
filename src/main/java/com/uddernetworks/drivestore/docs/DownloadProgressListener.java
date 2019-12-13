package com.uddernetworks.drivestore.docs;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DownloadProgressListener implements MediaHttpDownloaderProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadProgressListener.class);

    private final String name;

    public DownloadProgressListener(String name) {
        this.name = name;
    }

    @Override
    public void progressChanged(MediaHttpDownloader uploader) {
        switch (uploader.getDownloadState()) {
            case NOT_STARTED:
                break;
            case MEDIA_IN_PROGRESS:
                LOGGER.info("[{}] {}%", name, uploader.getProgress());
                break;
            case MEDIA_COMPLETE:
                LOGGER.info("[{}] Download complete", name);
                break;
        }
    }
}