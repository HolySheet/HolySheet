package com.uddernetworks.holysheet;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.uddernetworks.holysheet.io.SheetIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.uddernetworks.holysheet.utility.Utility.getCollectionFirst;

public class SheetManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SheetManager.class);

    private static final int MB = 0x100000;
    private static final double MAX_SHEET_SIZE = 25 * MB;

    private final DocStore docStore;
    private final Drive drive;
    private final Sheets sheets;
    private SheetIO sheetIO;

    private File docstore;

    public SheetManager(DocStore docStore) {
        this.docStore = docStore;
        this.drive = docStore.getDrive();
        this.sheets = docStore.getSheets();
    }

    public void init() {
        try {
            LOGGER.info("Finding docstore folder...");

            docstore = getCollectionFirst(getFiles(1, "name = 'docstore'", Mime.FOLDER)).orElseGet(() -> {
                try {
                    return createFolder("docstore");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            LOGGER.info("docstore id: {}", docstore.getId());

            sheetIO = new SheetIO(this);
        } catch (IOException e) {
            LOGGER.error("An error occurred while finding or creating docstore directory", e);
        }

        LOGGER.info("Done!");
    }

    public File getFile(String id) throws IOException {
        return drive.files().get(id).execute();
    }

    public Optional<String> getIdOfName(String name) {
        try {
            return getCollectionFirst(getFiles(-1, "parents in '" + docstore.getId() + "' and name contains '" + name.replace("'", "") + "'", Mime.DOCUMENT)).map(File::getId);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public List<File> listUploads() {
        try {
            return getAllSheets();
        } catch (IOException e) {
            LOGGER.error("An error occurred while listing uploads", e);
            return Collections.emptyList();
        }
    }

    public List<File> getAllSheets() throws IOException {
        return getAllFolder(docstore.getId());
    }

    public List<File> getAllFolder(String id) throws IOException {
        return getFiles(-1, "parents in '" + id + "' and properties has { key='directParent' and value='true' }", Mime.FOLDER);
    }

    public List<File> getAllSheets(String id) throws IOException {
        return getFiles(-1, "parents in '" + id + "'", Mime.SHEET);
    }

    public File createFolder(String name) throws IOException {
        return createFolder(name, null);
    }

    public File createFolder(String name, File parent) throws IOException {
        return createFolder(name, parent, null);
    }

    public File createFolder(String name, File parent, Map<String, String> properties) throws IOException {
        return drive.files().create(new File()
                .setMimeType(Mime.FOLDER.getMime())
                .setParents(parent == null ? null : Collections.singletonList(parent.getId()))
                .setProperties(properties)
                .setName(name)).execute();
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
                .setFields("nextPageToken, files(id, name, mimeType, parents, size, modifiedTime, properties)");

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

    public Drive getDrive() {
        return drive;
    }

    public Sheets getSheets() {
        return sheets;
    }

    public File getDocstore() {
        return docstore;
    }

    public SheetIO getSheetIO() {
        return sheetIO;
    }
}
