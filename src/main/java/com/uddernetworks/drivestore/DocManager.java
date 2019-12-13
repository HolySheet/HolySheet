package com.uddernetworks.drivestore;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.uddernetworks.drivestore.docs.DownloadProgressListener;
import com.uddernetworks.drivestore.docs.ProgressListener;
import com.uddernetworks.drivestore.docs.RequestBuilder;
import com.uddernetworks.drivestore.encoding.ByteUtil;
import com.uddernetworks.drivestore.encoding.DataChunk;
import com.uddernetworks.drivestore.encoding.DocCoder;
import com.uddernetworks.drivestore.encoding.DocOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.uddernetworks.drivestore.COptional.getCOptional;
import static java.lang.Long.toBinaryString;

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
        } catch (IOException e) {
            LOGGER.error("An error occurred while finding or creating docstore directory", e);
        }

        LOGGER.info("Done!");
    }

    /**
     * Uploads the given binary data to google docs. This should be converted into a file upload (or InputStream) later
     * on for dealing with large uploads.
     *
     * @param bytes The bytes to upload
     * @return The ID of the upload
     */
    public Optional<String> uploadData(byte[] bytes) {
        try (var chunkOS = new DocOutputStream()) {
            chunkOS.write(bytes);
            chunkOS.close();
            var chunks = chunkOS.getChunks();

            LOGGER.info("Processed {} chunks", chunks.size());
            LOGGER.info("Uploading document...");

            return Optional.of(createDocument("Doc " + System.currentTimeMillis(), requestBuilder -> DocCoder.encodeChunks(requestBuilder, chunks)).getId());
        } catch (IOException e) {
            LOGGER.error("An error occurred while encoding/uploading", e);
            return Optional.empty();
        }
    }

    public Optional<byte[]> retrieveData(String documentId) {
        try {
            var request = docs.documents().get(documentId);
//            request.getMediaHttpDownloader().setProgressListener(new DownloadProgressListener("Retrieve"));
            var fetched = request.execute();
            var content = fetched.getBody().getContent();

            var out = new ByteArrayOutputStream();
            content.forEach(elem -> {
                if (elem == null || elem.getParagraph() == null) return;
                var pelem = elem.getParagraph().getElements();
                pelem.stream()
                        .map(ParagraphElement::getTextRun)
                        .filter(Objects::nonNull)
                        .forEach(run -> {
                            // TODO: Direct TextRun > long?
                            DocCoder.decodeChunk(run)
                                    .map(DataChunk::deconstructChunk)
                                    .map(ByteUtil::longToBytes)
                                    .ifPresent(out::writeBytes);
                        });
            });

            return Optional.of(out.toByteArray());
        } catch (IOException e) {
            LOGGER.error("An error occurred while decoding/retrieving", e);
            return Optional.empty();
        }
    }

    public List<File> listUploads() {
        try {
            return getDocFiles();
        } catch (IOException e) {
            LOGGER.error("An error occurred while listing uploads", e);
            return Collections.emptyList();
        }
    }

    public File createDocument(String title, Consumer<RequestBuilder> requestBuilderConsumer) throws IOException {
        var content = new ByteArrayContent("text/plain", "".getBytes());
        var request = drive.files().create(new File()
                .setMimeType(Mime.DOCUMENT.getMime())
                .setName(title)
                .setParents(Collections.singletonList(docstore.getId())), content);
        request.getMediaHttpUploader().setProgressListener(new ProgressListener("Upload"));
        var created = request.execute();

        var requestBuilder = new RequestBuilder(docs);
        requestBuilderConsumer.accept(requestBuilder);
        requestBuilder.execute(created.getId());
        return created;
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
