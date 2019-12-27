package com.uddernetworks.holysheet.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uddernetworks.holysheet.DocStore;
import com.uddernetworks.holysheet.SheetManager;
import com.uddernetworks.holysheet.command.CommandHandler;
import com.uddernetworks.holysheet.socket.payload.BasicPayload;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionRequest;
import com.uddernetworks.holysheet.socket.payload.ErrorPayload;
import com.uddernetworks.holysheet.socket.payload.ListRequest;
import com.uddernetworks.holysheet.socket.payload.ListResponse;
import com.uddernetworks.holysheet.socket.payload.ListResponse.ListItem;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.stream.Collectors;

public class SocketCommunication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketCommunication.class);

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(PayloadType.class, new PayloadTypeAdapter())
            .create();

    private static final int PORT = 4567;

    private final DocStore docStore;
    private final SheetManager sheetManager;
    private ServerSocket serverSocket;
    private final AtomicReference<Socket> lastClient = new AtomicReference<Socket>(); // The most recently active client

    private List<BiConsumer<Socket, String>> receivers = Collections.synchronizedList(new ArrayList<>());

    public SocketCommunication(DocStore docStore) {
        this.docStore = docStore;
        this.sheetManager = docStore.getSheetManager();
    }

    public void start() {
        LOGGER.info("Starting payload on port {}...", PORT);

        try {
            serverSocket = new ServerSocket(PORT);

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
                            handleRequest(socket, input);
                        }
                    } catch (IOException e) {
                        LOGGER.error("An error occurred while reading/writing to socket", e);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private void handleRequest(Socket socket, String input) {
        CompletableFuture.runAsync(() -> {
            var basicPayload = GSON.fromJson(input, BasicPayload.class);
            try {
                if (basicPayload.getCode() < 1) {
                    LOGGER.error("Unsuccessful request with code {}: {}\nJson: {}", basicPayload.getCode(), basicPayload.getMessage(), input);
                    return;
                }

                var type = basicPayload.getType();
                var state = basicPayload.getState();

                if (!type.isReceivable()) {
                    LOGGER.error("Received unreceivable payload type: {}", type.name());
                    sendData(socket, GSON.toJson(new ErrorPayload("Received unreceivable payload type: " + type.name(), state, ExceptionUtils.getStackTrace(new RuntimeException()))));
                    return;
                }

                switch (type) {
                    case LIST_REQUEST:
                        var listRequest = GSON.fromJson(input, ListRequest.class);

                        LOGGER.info("Got list request. Query: {}", listRequest.getQuery());

                        List<ListItem> uploads;
                        synchronized (sheetManager) {
                            uploads = sheetManager.listUploads()
                                    .stream()
                                    .map(file -> new ListItem(file.getName(), CommandHandler.getSize(file), CommandHandler.getSheetCount(file), file.getModifiedTime().getValue(), file.getId()))
                                    .collect(Collectors.toUnmodifiableList());
                        }

                        sendData(socket, new ListResponse(1, "Success", state, uploads));
                        break;
                    case CODE_EXECUTION_REQUEST:
                        var codeExecutionRequest = GSON.fromJson(input, CodeExecutionRequest.class);

                        LOGGER.info("Got code execution request");

                        docStore.getjShellRemote().queueRequest(codeExecutionRequest, response ->
                                sendData(socket, response));
                        break;
                    default:
                        LOGGER.error("Unsupported type: {}", basicPayload.getType().name());
                        sendData(socket, GSON.toJson(new ErrorPayload("Unsupported type: " + basicPayload.getType().name(), basicPayload.getState(), ExceptionUtils.getStackTrace(new RuntimeException()))));
                        break;
                }
            } catch (Exception e) { // Catching Exception only for error reporting back to GUI/Other client
                LOGGER.error("Exception while parsing client received data", e);
                sendData(socket, GSON.toJson(new ErrorPayload(e.getMessage(), basicPayload.getState(), ExceptionUtils.getStackTrace(e))));
            }
        }).exceptionally(t -> {
            LOGGER.error("Exception while parsing client received data", t);
            return null;
        });
    }

    public void addReceiver(BiConsumer<Socket, String> receiver) {
        receivers.add(receiver);
    }

    public static void sendDataAsync(Socket socket, String data) {
        CompletableFuture.runAsync(() -> sendData(socket, data));
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
