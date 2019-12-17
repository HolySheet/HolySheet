package com.uddernetworks.drivestore.command;

import com.uddernetworks.drivestore.DocStore;
import com.uddernetworks.drivestore.console.ConsoleTableBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.uddernetworks.drivestore.utility.Utility.humanReadableByteCountSI;

@CommandLine.Command(name = "example", mixinStandardHelpOptions = true, version = "1.0.0-BETA", customSynopsis = {
        "(-u=<file>... | -d=<name/id>... |  -r=<name/id>...) [-hlV]"
})
public class CommandHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");
    private static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z0-9-_]+)");

    private final DocStore docStore;

    @ArgGroup(multiplicity = "1")
    RequiresParam param;

    @Option(names = {"-l", "--list"}, description = "Lists the uploaded files in Google Sheets")
    boolean list;

    public CommandHandler(DocStore docStore) {
        this.docStore = docStore;
    }

    static class RequiresParam {

        @Option(names = {"-u", "--upload"}, arity = "1..*", description = "Upload the local file", paramLabel = "<file>")
        File[] upload;

        @Option(names = {"-d", "--download"}, arity = "1..*", description = "Download the remote file", paramLabel = "<name>")
        String[] download;

        @Option(names = {"-r", "--remove"}, arity = "1..*", description = "Remove the remote file", paramLabel = "<name>")
        List<String> remove;
    }

    @Override
    public void run() {
        docStore.init();

        if (list) {
            list();
            return;
        }

        if (param.upload != null) {
            upload();
            return;
        }

        if (param.download != null) {
            download();
            return;
        }

        if (param.remove != null) {
            remove();
            return;
        }
    }

    private void list() {
        var docManager = docStore.getSheetManager();
        var table = new ConsoleTableBuilder()
                .addColumn("Name", 20)
                .addColumn("Size", 8)
                .addColumn("Sheets", 6)
                .addColumn("Date", 10)
                .addColumn("Id", 33)
                .setHorizontalSpacing(3);
        System.out.println("\n");
        System.out.println(table.generateTable(docManager.listUploads()
                .stream()
                .map(file -> List.of(
                        file.getName(),
                        humanReadableByteCountSI(Long.parseLong(file.getProperties().get("size"))),
                        getSheetCount(file),
                        DATE_FORMAT.format(new Date(file.getModifiedTime().getValue())),
                        file.getId()
                )).collect(Collectors.toUnmodifiableList())));
    }

    private void upload() {
        long start = System.currentTimeMillis();
        for (var file : param.upload) {
            uploadFile(file);
        }
        LOGGER.info("Finished the uploading of {} file{} in {}ms", param.upload, param.upload.length == 1 ? "" : "s", System.currentTimeMillis() - start);
    }

    private void uploadFile(File file) {
        if (!file.isFile()) {
            LOGGER.error("File '{}' does not exist!", file.getAbsolutePath());
            return;
        }

        LOGGER.info("Uploading {}...", file.getName());

        var sheetIO = docStore.getSheetManager().getSheetIO();

        try {
            long start = System.currentTimeMillis();
            var name = FilenameUtils.getName(file.getAbsolutePath());

            var ups = sheetIO.uploadData(name, new FileInputStream(file).readAllBytes());

            LOGGER.info("Uploaded {} in {}ms", ups.getId(), System.currentTimeMillis() - start);
        } catch (IOException e) {
            LOGGER.error("Error reading and uploading file", e);
        }
    }

    private void download() {
        for (var idName : param.download) {
            downloadIdName(idName);
        }
    }

    private void downloadIdName(String idName) {
        try {
            var docManager = docStore.getSheetManager();

            if (ID_PATTERN.matcher(idName).matches()) {
                idName = docManager.getIdOfName(idName).orElse(idName);
            }

            long start = System.currentTimeMillis();
            var sheet = docManager.getFile(idName);

            if (sheet == null) {
                LOGGER.info("Couldn't find file with id/name of {}", idName);
                return;
            }

            try (var fileStream = new FileOutputStream(sheet.getName())) {
                var byteOptional = docManager.getSheetIO().downloadData(idName);
                if (byteOptional.isEmpty()) {
                    LOGGER.error("An error occurred while downloading {}", idName);
                    return;
                }

                fileStream.write(byteOptional.get().toByteArray());
            }

            LOGGER.info("Downloaded in {}ms", System.currentTimeMillis() - start);
        } catch (IOException e) {
            LOGGER.error("An error occurred while downloading file " + idName, e);
        }
    }

    private void remove() {
        LOGGER.error("Removing files not implemented yet");
    }

    private String getSheetCount(com.google.api.services.drive.model.File file) {
        var string = file.getProperties().get("sheets");
        if (!StringUtils.isNumeric(string)) {
            return "0";
        }

        return String.valueOf(Integer.parseInt(string));
    }
}
