package com.uddernetworks.drivestore;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.ByteArrayStreamingContent;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Body;
import com.google.api.services.docs.v1.model.CreateNamedRangeRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Paragraph;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.SectionBreak;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.docs.v1.model.TextRun;
import com.google.api.services.docs.v1.model.TextStyle;
import com.google.api.services.docs.v1.model.UpdateTextStyleRequest;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.uddernetworks.drivestore.COptional.getCOptional;

public class DocManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocManager.class);

    private final DocStore docStore;
    private final Docs docs;
    private final Drive drive;

    private File docstore;

    public DocManager(DocStore docStore) {
        this.docStore = docStore;
        this.docs = docStore.getDocs();
        this.drive = docStore.getDrive();
    }

    class CustomProgressListener implements MediaHttpUploaderProgressListener {
        public void progressChanged(MediaHttpUploader uploader) throws IOException {
            switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                    System.out.println("Initiation has started!");
                    break;
                case INITIATION_COMPLETE:
                    System.out.println("Initiation is complete!");
                    break;
                case MEDIA_IN_PROGRESS:
                    System.out.println(uploader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    System.out.println("Upload is complete!");
            }
        }
    }

    public void init() {
        try {
            LOGGER.info("Finding docstore folder...");

            docstore = getCOptional(getFiles(1, "name = 'docstore'", Mime.FOLDER)).orElseGet(() -> {
                try {
                    return drive.files().create(new File()
                            .setMimeType(Mime.FOLDER.getMime())
                            .setName("docstore")).execute();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            LOGGER.info("docstore id: {}", docstore.getId());

//            getCOptional(getDocFiles()).ifPresent(file -> {
//                try {
//                    LOGGER.info("Found {} ({})", file.getName(), file.getId());
//
////                    drive.files().get(file.getId()).
////                    try (var os = new ByteArrayOutputStream()) {
////                        drive.files().get(file.getId()).executeMediaAndDownloadTo(os);
////                        LOGGER.info("Data:\n\n");
////                        System.out.println(new String(os.toByteArray()));
////                    }
//
////                    drive.
//
//
////
////                    var document = docs.documents().get(file.getId()).execute();
////
////                    var body = document.getBody();
////                    body.getContent().stream().map(StructuralElement::getParagraph).forEach(paragraph -> {
////                        try {
////                            if (paragraph == null) return;
////                            LOGGER.info(paragraph.toPrettyString());
////                        } catch (IOException e) {
////                            e.printStackTrace();
////                        }
////                    });
//                } catch (IOException e) {
//                    throw new UncheckedIOException(e);
//                }
//            });

            var content = new ByteArrayContent("text/plain", "".getBytes());
            var request = drive.files().create(new File().setMimeType(Mime.DOCUMENT.getMime()).setName("Doc " + System.currentTimeMillis()), content);
            request.getMediaHttpUploader().setProgressListener(new CustomProgressListener());
            var made = request.execute();

            docs.documents().batchUpdate(made.getId(), new BatchUpdateDocumentRequest().setRequests(
                    Arrays.asList(
                            new Request().setInsertText(new InsertTextRequest().setText("1").setLocation(new Location().setIndex(1))),
                            new Request().setUpdateTextStyle(new UpdateTextStyleRequest().setRange(new Range().setStartIndex(1).setEndIndex(2)).setTextStyle(new TextStyle().setBold(true)).setFields("*")),
                            new Request().setInsertText(new InsertTextRequest().setText("2").setLocation(new Location().setIndex(2))),
                            new Request().setUpdateTextStyle(new UpdateTextStyleRequest().setRange(new Range().setStartIndex(2).setEndIndex(3)).setTextStyle(new TextStyle().setItalic(true)).setFields("*")),
                            new Request().setInsertText(new InsertTextRequest().setText("3").setLocation(new Location().setIndex(3))),
                            new Request().setUpdateTextStyle(new UpdateTextStyleRequest().setRange(new Range().setStartIndex(3).setEndIndex(4)).setTextStyle(new TextStyle().setUnderline(true)).setFields("*"))
                    )
            )).execute();
        } catch (IOException e) {
            LOGGER.error("An error occurred while finding or creating docstore directory", e);
        }

        LOGGER.info("Done!");
    }

    private File moveDocument(File file, File to) throws IOException {
        var pp = drive.files().get(file.getId())
                .setFields("parents")
                .execute();

        return drive.files().update(file.getId(), null)
                .setAddParents(to.getId())
                .setRemoveParents(String.join(",", pp.getParents()))
                .setFields("id, parents")
                .execute();
    }

    private List<File> getDocFiles() throws IOException {
        return getFiles(-1, "parents in '" + docstore.getId() + "'", Mime.DOCUMENT);
    }

    /**
     * Gets the files in the google drive with the given limit and matching mime types.
     *
     * @param limit The limit of files to find (-1 for all files, USE SPARINGLY)
     * @param mimes The mime types to match for
     * @return The list of files
     * @throws IOException
     */
    public List<File> getFiles(int limit, Mime... mimes) throws IOException {
        return getFiles(limit, null, mimes);
    }

    /**
     * Gets the files in the google drive with the given limit and matching mime types.
     *
     * @param limit The limit of files to find (-1 for all files, USE SPARINGLY)
     * @param query An additional query to search for
     * @param mimes The mime types to match for
     * @return The list of files
     * @throws IOException
     */
    public List<File> getFiles(int limit, String query, Mime... mimes) throws IOException {
        limit = Math.min(-1, limit);
        var mimeTypes = Arrays.stream(mimes).map(Mime::getMime).collect(Collectors.toUnmodifiableSet());
        var foundFiles = new ArrayList<File>();

        var pageToken = "";
        do {
            var result = getPagesFiles(pageToken, 50, mimes, query);
            pageToken = result.getNextPageToken();
            var files = result.getFiles();
            if (files == null || files.isEmpty()) {
                break;
            }

            for (var file : files) {
                if (!mimeTypes.contains(file.getMimeType())) {
                    continue;
                }

                foundFiles.add(file);

                if (--limit == 0) {
                    return foundFiles;
                }
            }
        } while (pageToken != null);

        return foundFiles;
    }

    private FileList getPagesFiles(String pageToken, int pageSize, Mime[] mimes, String query) throws IOException {
        var builder = drive.files().list()
                .setPageSize(pageSize)
                .setFields("nextPageToken, files(id, name, mimeType, parents)");

        if (pageToken != null && !pageToken.isBlank()) {
            builder.setPageToken(pageToken);
        }

        var q = getQueryFromMime(mimes).orElse("");

        if (query != null && !query.isBlank()) {
            if (q.isBlank()) {
                q = query;
            } else {
                q += " and " + query;
            }
        }

        builder.setQ(q);

        return builder.execute();
    }

    private Optional<String> getQueryFromMime(Mime[] mimes) {
        if (mimes.length == 0) return Optional.empty();
        var base = new StringBuilder();
        for (var mime : mimes) {
            base.append("mimeType = '").append(mime.getMime()).append("' or ");
        }

        var q = base.substring(0, base.length() - 4);
        return Optional.of(q);
    }
}
