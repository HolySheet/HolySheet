package com.uddernetworks.holysheet.command;

import com.uddernetworks.holysheet.DocStore;
import com.uddernetworks.holysheet.console.ConsoleTableBuilder;
import com.uddernetworks.holysheet.utility.Utility;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.uddernetworks.holysheet.utility.Utility.humanReadableByteCountSI;

@CommandLine.Command(name = "example", mixinStandardHelpOptions = true, version = "DriveStore 1.0.0", customSynopsis = {
        "([-c] -u=<file>... | -d=<name/id>... |  -r=<name/id>...) [-hlV]"
})
public class CommandHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");
    public static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z0-9-_]+)");

    private final DocStore docStore;

    @Option(names = {"-l", "--list"}, description = "Lists the uploaded files in Google Sheets")
    boolean list;

    @Option(names = {"-s", "--socket"}, description = "Starts communication socket on the given port, used to interface with other apps")
    int socket = -1;

    @Option(names = {"-c", "--compress"}, description = "Compressed before uploading, currently uses Zip format")
    boolean compression;

    @ArgGroup(multiplicity = "0..1")
    RequiresParam param;

    public CommandHandler(DocStore docStore) {
        this.docStore = docStore;
    }

    static class RequiresParam {

//        @ArgGroup(exclusive = false, multiplicity = "1")
//        Upload upload;

        @Option(names = {"-d", "--download"}, arity = "1..*", description = "Download the remote file", paramLabel = "<name>")
        String[] download;

        @Option(names = {"-r", "--remove"}, arity = "1..*", description = "Permanently removes the remote file", paramLabel = "<id>")
        List<String> remove;

        @Option(names = {"-e", "--clone"}, arity = "1..*", description = "Clones the remote file ID to Google Sheets", paramLabel = "<id>")
        List<String> clone;

        @Option(names = {"-u", "--upload"}, arity = "1..*", description = "Upload the local file", paramLabel = "<file>")
        File[] upload;
    }

    @Override
    public void run() {
        docStore.init();

        if (socket > 0) {
            docStore.getjShellRemote().start();
            docStore.getSocketCommunication().start();
            Utility.sleep(Long.MAX_VALUE);
            return;
        }

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

        if (param.clone != null) {
            cloneFiles();
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
                        String.valueOf(getSheetCount(file)),
                        DATE_FORMAT.format(new Date(file.getModifiedTime().getValue())),
                        file.getId()
                )).collect(Collectors.toUnmodifiableList())));
    }

    private void upload() {
        long start = System.currentTimeMillis();
        var upload = param.upload;
        for (var file : upload) {
            uploadFile(file);
        }
        LOGGER.info("Finished the uploading of {} file{} in {}ms", upload.length, upload.length == 1 ? "" : "s", System.currentTimeMillis() - start);
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

            var ups = sheetIO.uploadData(name, compression, new FileInputStream(file).readAllBytes());

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
        param.remove.forEach(docStore.getSheetManager().getSheetIO()::deleteData);
    }

    private void cloneFiles() { // 1BEC7wGs6depFiZQ3t2CK9JFXCIvCtv8A
        var io = docStore.getSheetManager().getSheetIO();
        param.clone.forEach(id -> io.cloneFile(id, compression));
    }

    public static int getSheetCount(com.google.api.services.drive.model.File file) {
        var string = file.getProperties().get("sheets");
        if (!StringUtils.isNumeric(string)) {
            return 0;
        }

        return Integer.parseInt(string);
    }

    public static int getSize(com.google.api.services.drive.model.File file) {
        var string = file.getProperties().get("size");
        if (!StringUtils.isNumeric(string)) {
            return 0;
        }

        return Integer.parseInt(string);
    }
}
