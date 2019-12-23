package com.uddernetworks.holysheet.socket;

import com.uddernetworks.holysheet.DocStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class SocketCommunication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketPayload.class);

    private static final int PORT = 4567;

    private final DocStore docStore;
    private ServerSocket serverSocket;

    public SocketCommunication(DocStore docStore) {
        this.docStore = docStore;
    }

    public void start() {
        LOGGER.info("Starting payload on port {}...", PORT);

        try {
            serverSocket = new ServerSocket(PORT);

            while (true) {
                var socket = serverSocket.accept();
                CompletableFuture.runAsync(() -> {
                    try {
                        LOGGER.info("Got client");

                        var in = new Scanner(socket.getInputStream());

                        for (String line; (line = in.nextLine()) != null; ) {
                            System.out.println("Received: " + line);
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
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

}
