package com.uddernetworks.holysheet;

import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.uddernetworks.holysheet.command.CommandHandler;
import com.uddernetworks.holysheet.grpc.GRPCClient;
import com.uddernetworks.holysheet.jshell.JShellRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class HolySheet {
    private static final Logger LOGGER = LoggerFactory.getLogger(HolySheet.class);

    private LocalAuthManager authManager;
    private GRPCClient grpcClient;
    private JShellRemote jShellRemote;
//    private Drive drive;
//    private Sheets sheets;

    public static void main(String[] args) {
        new HolySheet().start(args);
    }

    private void start(String[] args) {
        if (args.length == 0) {
            args = new String[]{"-h"};
        }

        System.exit(new CommandLine(new CommandHandler(this)).execute(args));
    }

    public void init(String credentialPath) {
        try {
            LOGGER.info("Initializing everything...");

            authManager = new LocalAuthManager(credentialPath);
            authManager.initialize();

            jShellRemote = new JShellRemote(grpcClient);
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error initializing", e);
        }
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public GRPCClient getGrpcClient() {
        return grpcClient;
    }

    public JShellRemote getjShellRemote() {
        return jShellRemote;
    }
}
