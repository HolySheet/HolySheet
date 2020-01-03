package com.uddernetworks.holysheet.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uddernetworks.holysheet.DocStore;
import com.uddernetworks.holysheet.SheetManager;
import com.uddernetworks.holysheet.command.CommandHandler;
import com.uddernetworks.holysheet.socket.payload.BasicPayload;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionRequest;
import com.uddernetworks.holysheet.socket.payload.DownloadRequest;
import com.uddernetworks.holysheet.socket.payload.DownloadStatusResponse;
import com.uddernetworks.holysheet.socket.payload.ErrorPayload;
import com.uddernetworks.holysheet.socket.payload.ListItem;
import com.uddernetworks.holysheet.socket.payload.ListRequest;
import com.uddernetworks.holysheet.socket.payload.ListResponse;
import com.uddernetworks.holysheet.socket.payload.RemoveRequest;
import com.uddernetworks.holysheet.socket.payload.RemoveStatusResponse;
import com.uddernetworks.holysheet.socket.payload.UploadRequest;
import com.uddernetworks.holysheet.socket.payload.UploadStatusResponse;
import com.uddernetworks.holysheet.utility.Utility;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SocketCommunication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketCommunication.class);

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(PayloadType.class, new PayloadTypeAdapter())
            .create();

    private final DocStore docStore;
    private final SheetManager sheetManager;
    private ServerSocket serverSocket;
    private final AtomicReference<Socket> lastClient = new AtomicReference<>(); // The most recently active client

    private List<BiConsumer<Socket, String>> receivers = Collections.synchronizedList(new ArrayList<>());

    public SocketCommunication(DocStore docStore) {
        this.docStore = docStore;
        this.sheetManager = docStore.getSheetManager();
    }

    public void listenIO() {
        LOGGER.info("Listening for console input...");

        var in = new Scanner(System.in);
        for (String line; (line = in.nextLine()) != null; ) {
            handleRequest(data -> System.out.println(GSON.toJson(data)), line);
        }
    }

    public void startSocket(int port) {
        LOGGER.info("Starting payload on port {}...", port);

        try {
            serverSocket = new ServerSocket(port);

            while (true) {
                var socket = serverSocket.accept();
                if (lastClient.get() == null) {
                    lastClient.set(socket);
                }
                CompletableFuture.runAsync(() -> {
                    try {
                        LOGGER.info("Got client");

                        var in = new Scanner(socket.getInputStream());

                        for (String line; (line = in.nextLine()) != null; ) {
                            final var input = line;
                            lastClient.set(socket);
                            receivers.forEach(consumer -> consumer.accept(socket, input));
                            handleRequest(data -> sendData(socket, data), input);
                        }
                    } catch (IOException e) {
                        LOGGER.error("An error occurred while reading/writing to socket", e);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while starting socket", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.error("Error closing payload", e);
            }
        }));
    }

    public void sendPayload(BasicPayload payload) {
        var client = lastClient.get();
        if (client == null) {
            return;
        }

        sendData(client, payload);
    }

    private void handleRequest(Consumer<Object> sendData, String input) {
        CompletableFuture.runAsync(() -> {
            var basicPayload = GSON.fromJson(input, BasicPayload.class);
            try {
                if (basicPayload.getCode() < 1) {
                    LOGGER.error("Unsuccessful request with code {}: {}\nJson: {}", basicPayload.getCode(), basicPayload.getMessage(), input);
                    return;
                }

                var type = basicPayload.getType();
                var state = basicPayload.getState(); // gradle run --args="-s 4567"

                if (!type.isReceivable()) {
                    LOGGER.error("Received unreceivable payload type: {}", type.name());
                    sendData.accept(new ErrorPayload("Received unreceivable payload type: " + type.name(), state, Utility.getStackTrace()));
                    return;
                }

                var sheetIO = sheetManager.getSheetIO();

                switch (type) {
                    case LIST_REQUEST:
                        var listRequest = GSON.fromJson(input, ListRequest.class);

                        LOGGER.info("Got list request. Query: {}", listRequest.getQuery());

                        List<ListItem> uploads;
                        synchronized (this.sheetManager) {
                            uploads = this.sheetManager.listUploads()
                                    .stream()
                                    .map(file -> new ListItem(file.getName(), CommandHandler.getSize(file), CommandHandler.getSheetCount(file), file.getModifiedTime().getValue(), file.getId()))
                                    .collect(Collectors.toUnmodifiableList());
                        }

                        sendData.accept(new ListResponse(1, "Success", state, uploads));
                        break;
                    case UPLOAD_REQUEST:
                        var uploadRequest = GSON.fromJson(input, UploadRequest.class);

                        LOGGER.info("Got upload request for {} or {}", uploadRequest.getFile(), uploadRequest.getId());

                        String name;
                        byte[] data;

                        if (uploadRequest.getFile() != null) {
                            var file = new File(uploadRequest.getFile());

                            if (!file.isFile()) {
                                LOGGER.error("File '{}' does not exist!", file.getAbsolutePath());
                                sendData.accept(new ErrorPayload("File '" + file.getAbsolutePath() + "' does not exist", state, Utility.getStackTrace()));
                                return;
                            }

                            name = FilenameUtils.getName(file.getAbsolutePath());
                            data = new FileInputStream(file).readAllBytes();
                        } else {
                            var dataOptional = sheetIO.downloadFile(uploadRequest.getId());
                            if (dataOptional.isPresent()) {
                                var fileData = dataOptional.get();
                                name = fileData.getFile().getName();
                                data = fileData.getOut().toByteArray();
                            } else {
                                LOGGER.error("Error downloading file '{}' to be cloned", uploadRequest.getId());
                                sendData.accept(new ErrorPayload("Error downloading file '" + uploadRequest.getId() + "' to be cloned", state, Utility.getStackTrace()));
                                return;
                            }
                        }

                        LOGGER.info("Uploading {}...", name);

                        long start = System.currentTimeMillis();

                        sendData.accept(new UploadStatusResponse(1, "Success", state, "PENDING", 0, Collections.emptyList()));

                        var uploaded = sheetIO.uploadData(name, uploadRequest.getSheetSize(), !uploadRequest.getCompression().equals("none"), uploadRequest.getUpload(), data, percentage ->
                                sendData.accept(new UploadStatusResponse(1, "Success", state, "UPLOADING", percentage, Collections.emptyList())));

                        LOGGER.info("Uploaded {} in {}ms", uploaded.getId(), System.currentTimeMillis() - start);

                        sendData.accept(new UploadStatusResponse(1, "Success", state, "COMPLETE", 1, Collections.singletonList(
                                new ListItem(name, CommandHandler.getSize(uploaded), CommandHandler.getSheetCount(uploaded), uploaded.getModifiedTime().getValue(), uploaded.getId()))));
                        break;
                    case DOWNLOAD_REQUEST:
                        var downloadRequest = GSON.fromJson(input, DownloadRequest.class);

                        var id = downloadRequest.getId();

                        start = System.currentTimeMillis();
                        var sheet = sheetManager.getFile(downloadRequest.getId());

                        if (sheet == null) {
                            LOGGER.error("No file could be found with the given ID \"" + id + "\"");
                            sendData.accept(new ErrorPayload("No file could be found with the given ID \"" + id + "\"", state, Utility.getStackTrace()));
                            return;
                        }

                        var destination = new File(downloadRequest.getPath());
                        var parent = destination.getParentFile();

                        if (!parent.exists() || !parent.mkdirs()) {
                            LOGGER.error("Couldn't find or create parent of \"" + parent.getAbsolutePath() + "\"");
                            sendData.accept(new ErrorPayload("Couldn't find or create parent of \"" + parent.getAbsolutePath() + "\"", state, Utility.getStackTrace()));
                            return;
                        }

                        sendData.accept(new DownloadStatusResponse(1, "Success", state, "PENDING", 0));

                        try (var fileStream = new FileOutputStream(destination)) {
                            sheetManager.getSheetIO().downloadData(id, percentage ->
                                    sendData.accept(new DownloadStatusResponse(1, "Success", state, "UPLOADING", percentage)), error ->
                                    sendData.accept(new ErrorPayload(error, state, Utility.getStackTrace())), bytes -> {
                                try {
                                    fileStream.write(bytes.toByteArray());

                                    LOGGER.info("Downloaded in {}ms", System.currentTimeMillis() - start);

                                    sendData.accept(new DownloadStatusResponse(1, "Success", state, "COMPLETE", 1));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                        }
                        break;
                    case REMOVE_REQUEST:
                        var removeRequest = GSON.fromJson(input, RemoveRequest.class);

                        LOGGER.info("Got remove request for {}", removeRequest.getId());

                        sendData.accept(new RemoveStatusResponse(1, "Success", state, "PENDING", 0));

                        sheetIO.deleteData(removeRequest.getId(), false, error ->
                                sendData.accept(new ErrorPayload(error, state, Utility.getStackTrace())), () ->
                                sendData.accept(new RemoveStatusResponse(1, "Success", state, "COMPLETE", 1)));
                        break;
                    case CODE_EXECUTION_REQUEST:
                        var codeExecutionRequest = GSON.fromJson(input, CodeExecutionRequest.class);

                        LOGGER.info("Got code execution request");

                        docStore.getjShellRemote().queueRequest(codeExecutionRequest, sendData::accept);
                        break;
                    default:
                        LOGGER.error("Unsupported type: {}", basicPayload.getType().name());
                        sendData.accept(new ErrorPayload("Unsupported type: " + basicPayload.getType().name(), basicPayload.getState(), ExceptionUtils.getStackTrace(new RuntimeException())));
                        break;
                }
            } catch (Exception e) { // Catching Exception only for error reporting back to GUI/Other client
                LOGGER.error("Exception while parsing client received data", e);
                sendData.accept(new ErrorPayload(e.getMessage(), basicPayload.getState(), ExceptionUtils.getStackTrace(e)));
            }
        }).exceptionally(t -> {
            LOGGER.error("Exception while parsing client received data", t);
            return null;
        });
    }

    public void addReceiver(BiConsumer<Socket, String> receiver) {
        receivers.add(receiver);
    }

    public static void sendData(Socket socket, Object object) {
        sendData(socket, GSON.toJson(object));
    }

    public static void sendData(Socket socket, String data) {
        try {
            var out = socket.getOutputStream();
            out.write(data.getBytes());
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
