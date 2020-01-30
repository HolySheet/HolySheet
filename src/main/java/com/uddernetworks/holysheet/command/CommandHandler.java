package com.uddernetworks.holysheet.command;

import com.google.api.services.drive.model.User;
import com.uddernetworks.holysheet.HolySheet;
import com.uddernetworks.holysheet.SheetManager;
import com.uddernetworks.holysheet.console.ConsoleTableBuilder;
import com.uddernetworks.holysheet.io.SheetIO;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.uddernetworks.grpc.HolysheetService.UploadRequest.Compression.*;
import static com.uddernetworks.grpc.HolysheetService.UploadRequest.Upload.MULTIPART;
import static com.uddernetworks.holysheet.utility.Utility.humanReadableByteCountSI;

@CommandLine.Command(name = "example", mixinStandardHelpOptions = true, version = "DriveStore 1.0.0", customSynopsis = {
        "([-cm] -u=<file>... | [-cm] -e=<id> | -d=<name/id>... |  -r=<name/id>...) [-agphlV]"
})
public class CommandHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");
    public static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z0-9-_]+)");

    private final HolySheet holySheet;
    private SheetManager sheetManager;
    private SheetIO sheetIO;

    @Option(names = {"-l", "--list"}, description = "Lists the uploaded files in Google Sheets")
    boolean list;

    @Option(names = {"-a", "--credentials"}, description = "The (absolute or relative) location of your personal credentials.json file")
    String credentials = "credentials.json";

    @Option(names = {"-g", "--grpc"}, description = "Starts the gRPC server on the given port, used to interface with other apps")
    int grpc = -1;

    @Option(names = {"-p", "--parent"}, description = "Kills the process (When running with socket) when the given PID is killed")
    int parent = -1;

    @Option(names = {"-c", "--compress"}, description = "Compressed before uploading, currently uses Zip format")
    boolean compression;

    @Option(names = {"-m", "--sheetSize"}, defaultValue = "10000000", description = "The maximum size in bytes a single sheet can be. Defaults to 10MB")
    int sheetSize;

    @ArgGroup(multiplicity = "0..1")
    RequiresParam param;

    public CommandHandler(HolySheet holySheet) {
        this.holySheet = holySheet;
    }

    static class RequiresParam {

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
        suicideForParent(parent);

        holySheet.init(credentials);
        var authManager = holySheet.getAuthManager();
        sheetManager = new SheetManager(authManager.getDrive(), authManager.getSheets());
        sheetIO = sheetManager.getSheetIO();

        if (grpc > 0) {
            holySheet.getjShellRemote().start();
            holySheet.getGrpcClient().start(grpc);
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
        var table = new ConsoleTableBuilder()
                .addColumn("Name", 20)
                .addColumn("Size", 8)
                .addColumn("Sheets", 6)
                .addColumn("Owner", 20)
                .addColumn("Date", 10)
                .addColumn("Id", 33)
                .setHorizontalSpacing(3);

        var uploads = sheetManager.listUploads();

        System.out.println("\n");
        System.out.println(table.generateTable(uploads
                .stream()
                .map(file -> List.of(
                        file.getName(),
                        humanReadableByteCountSI(Long.parseLong(file.getProperties().get("size"))),
                        String.valueOf(getSheetCount(file)),
                        file.getOwners().stream().map(User::getDisplayName).collect(Collectors.joining(",")),
                        DATE_FORMAT.format(new Date(file.getModifiedTime().getValue())),
                        file.getId()
                )).collect(Collectors.toList()), List.of(
                "Total",
                humanReadableByteCountSI(uploads.stream().mapToLong(file -> Long.parseLong(file.getProperties().get("size"))).sum()),
                String.valueOf(uploads.stream().mapToInt(CommandHandler::getSheetCount).sum()),
                "",
                "",
                ""
        )));
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

        try {
            long start = System.currentTimeMillis();
            var name = FilenameUtils.getName(file.getAbsolutePath());

            var ups = sheetIO.uploadData(name, file.length(), sheetSize, compression ? ZIP : NONE, MULTIPART, new FileInputStream(file));

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
            if (ID_PATTERN.matcher(idName).matches()) {
                idName = sheetManager.getIdOfName(idName).orElse(idName);
            }

            long start = System.currentTimeMillis();
            var sheet = sheetManager.getFile(idName);

            if (sheet == null) {
                LOGGER.info("Couldn't find file with id/name of {}", idName);
                return;
            }

            try (var fileStream = new FileOutputStream(sheet.getName())) {
                var byteOptional = sheetManager.getSheetIO().downloadData(idName);
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
        param.remove.forEach(sheetIO::deleteData);
    }

    private void cloneFiles() {
        param.clone.forEach(id -> sheetIO.cloneFile(id, sheetSize, compression ? ZIP : NONE));
    }

    private void suicideForParent(int parent) {
        if (parent == -1) {
            return;
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            var processOptional = ProcessHandle.of(parent);
            if (processOptional.isEmpty()) {
                LOGGER.info("No PID found of {}. Terminating...", parent);
                System.exit(0);
            }
        }, 1, 3, TimeUnit.SECONDS);
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
