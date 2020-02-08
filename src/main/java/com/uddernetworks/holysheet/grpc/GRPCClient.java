package com.uddernetworks.holysheet.grpc;

import com.uddernetworks.holysheet.HolySheet;
import com.uddernetworks.holysheet.SheetManager;
import io.grpc.ServerBuilder;
import io.grpc.ServerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GRPCClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GRPCClient.class);

    private final HolySheetServiceImpl service;

    public GRPCClient(HolySheet holySheet) {
        this.service = new HolySheetServiceImpl(holySheet);
    }

    public static void main(String[] args) {
        var client = new GRPCClient(null);
        client.start(8888);
    }

    public void start(int port) {
        try {
            var server = ServerBuilder.forPort(port)
                    .addService(service)
                    .build();

            // Start the server
            server.start();

            // App threads are running in the background.
            System.out.println("App started");

            // Don't exit the main thread. Wait until server is terminated.
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("An error occurred in the gRPC server", e);
        }
    }

    public HolySheetServiceImpl getService() {
        return service;
    }
}
