package com.uddernetworks.holysheet.grpc;

import com.uddernetworks.holysheet.HolySheet;
import com.uddernetworks.holysheet.SheetManager;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GRPCClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GRPCClient.class);

    private final HolySheetServiceImpl service;
    private Server server;

    public GRPCClient(HolySheet holySheet) {
        this.service = new HolySheetServiceImpl(holySheet);
    }

    public static void main(String[] args) {
        var client = new GRPCClient(null);
        client.start(8888);
    }

    public void start(int port) {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(service)
                    .build();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                LOGGER.info("Shutting down the gRPC server due to JVM shutdown");
                try {
                    stop();
                } catch (InterruptedException e) {
                    LOGGER.error("An error has occurred while stopping");
                }
                LOGGER.info("Server shut down");
            }));

            // Start the server
            server.start();

            // App threads are running in the background.
            LOGGER.info("gRPC Server started");

            // Don't exit the main thread. Wait until server is terminated.
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("An error occurred in the gRPC server", e);
        }
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public HolySheetServiceImpl getService() {
        return service;
    }
}
