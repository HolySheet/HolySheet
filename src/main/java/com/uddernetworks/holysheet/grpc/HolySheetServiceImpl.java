package com.uddernetworks.holysheet.grpc;

import com.uddernetworks.grpc.HolySheetServiceGrpc;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionCallbackResponse;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionRequest;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionResponse;
import com.uddernetworks.grpc.HolysheetService.DownloadRequest;
import com.uddernetworks.grpc.HolysheetService.DownloadResponse;
import com.uddernetworks.grpc.HolysheetService.DownloadResponse.DownloadStatus;
import com.uddernetworks.grpc.HolysheetService.ListItem;
import com.uddernetworks.grpc.HolysheetService.ListRequest;
import com.uddernetworks.grpc.HolysheetService.ListResponse;
import com.uddernetworks.grpc.HolysheetService.ListenCallbacksRequest;
import com.uddernetworks.grpc.HolysheetService.RemoveRequest;
import com.uddernetworks.grpc.HolysheetService.RemoveResponse;
import com.uddernetworks.grpc.HolysheetService.RemoveResponse.RemoveStatus;
import com.uddernetworks.grpc.HolysheetService.UploadRequest;
import com.uddernetworks.grpc.HolysheetService.UploadResponse;
import com.uddernetworks.grpc.HolysheetService.UploadResponse.UploadStatus;
import com.uddernetworks.holysheet.HolySheet;
import com.uddernetworks.holysheet.SheetManager;
import com.uddernetworks.holysheet.command.CommandHandler;
import com.uddernetworks.holysheet.io.SheetIO;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

public class HolySheetServiceImpl extends HolySheetServiceGrpc.HolySheetServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HolySheetServiceImpl.class);

    private final HolySheet holySheet;
    private final SheetManager sheetManager;
    private final SheetIO sheetIO;
    private StreamObserver<CodeExecutionCallbackResponse> response;

    public HolySheetServiceImpl(HolySheet holySheet, SheetManager sheetManager) {
        this.holySheet = holySheet;
        this.sheetManager = sheetManager;
        this.sheetIO = sheetManager.getSheetIO();
    }

    @Override
    public void listFiles(ListRequest request, StreamObserver<ListResponse> response) {
        synchronized (this.sheetManager) {
            var files = this.sheetManager.listUploads()
                    .stream()
                    .map(file -> {
                        var owner = file.getOwners().get(0);
                        return ListItem.newBuilder()
                                .setName(file.getName())
                                .setId(file.getId())
                                .setSize(CommandHandler.getSize(file))
                                .setSheets(CommandHandler.getSheetCount(file))
                                .setDate(file.getModifiedTime().getValue())
                                .setSelfOwned(owner.getMe())
                                .setOwner(owner.getDisplayName())
                                .setDriveLink(file.getWebViewLink())
                                .build();
                    })
                    .collect(Collectors.toUnmodifiableList());

            response.onNext(ListResponse.newBuilder()
                    .addAllItems(files)
                    .build());

            response.onCompleted();
        }
    }

    @Override
    public void uploadFile(UploadRequest request, StreamObserver<UploadResponse> response) {
        String name;
        InputStream data;

        try {
            if (!request.getFile().isBlank()) {
                var file = new File(request.getFile());

                if (!file.isFile()) {
                    response.onError(new FileNotFoundException("File '" + file.getAbsolutePath() + "' does not exist"));
                    return;
                }

                name = FilenameUtils.getName(file.getAbsolutePath());
                data = new FileInputStream(file);
            } else {
                var dataOptional = sheetIO.downloadFile(request.getId());
                if (dataOptional.isPresent()) {
                    var fileData = dataOptional.get();
                    name = fileData.getFile().getName();
                    data = fileData.getIn();
                } else {
                    response.onError(new FileNotFoundException("Error downloading file '" + request.getId() + "' to be cloned"));
                    return;
                }
            }

            LOGGER.info("Uploading {}...", name);

            long start = System.currentTimeMillis();

            response.onNext(UploadResponse.newBuilder()
                    .setStatus(UploadStatus.PENDING)
                    .setPercentage(0)
                    .build());

            var uploaded = sheetIO.uploadData(name, request.getSheetSize(), request.getCompression(), request.getUpload(), data, percentage ->
                    response.onNext(UploadResponse.newBuilder()
                            .setStatus(UploadStatus.UPLOADING)
                            .setPercentage(percentage)
                            .build()));

            LOGGER.info("Uploaded {} in {}ms", uploaded.getId(), System.currentTimeMillis() - start);

            var owner = uploaded.getOwners().get(0);

            response.onNext(UploadResponse.newBuilder()
                    .setStatus(UploadStatus.COMPLETE)
                    .setPercentage(1)
                    .setItems(ListItem.newBuilder()
                            .setName(uploaded.getName())
                            .setId(uploaded.getId())
                            .setSize(CommandHandler.getSize(uploaded))
                            .setSheets(CommandHandler.getSheetCount(uploaded))
                            .setDate(uploaded.getModifiedTime().getValue())
                            .setSelfOwned(owner.getMe())
                            .setOwner(owner.getDisplayName())
                            .setDriveLink(uploaded.getWebViewLink())
                            .build())
                    .build());

            response.onCompleted();
        } catch (IOException e) {
            LOGGER.error("An error has occurred while uploading a file", e);
            response.onError(e);
        }
    }

    @Override
    public void downloadFile(DownloadRequest request, StreamObserver<DownloadResponse> response) {
        try {
            var id = request.getId();

            long start = System.currentTimeMillis();
            var sheet = sheetManager.getFile(id);

            if (sheet == null) {
                response.onError(new FileNotFoundException("No file could be found with the given ID \"" + id + "\""));
                return;
            }

            var destination = new File(request.getPath());
            var parent = destination.getParentFile();

            if (!parent.exists() && !parent.mkdirs()) {
                response.onError(new FileNotFoundException("Couldn't find or create parent of \"" + destination.getAbsolutePath() + "\""));
                return;
            }

            response.onNext(DownloadResponse.newBuilder()
                    .setStatus(DownloadStatus.PENDING)
                    .setPercentage(0)
                    .build());

            try (var fileStream = new FileOutputStream(destination)) {
                sheetManager.getSheetIO().downloadData(id, percentage ->
                        response.onNext(DownloadResponse.newBuilder()
                                .setStatus(DownloadStatus.DOWNLOADING)
                                .setPercentage(percentage)
                                .build()), error -> response.onError(new RuntimeException(error)), bytes -> {
                    try {
                        fileStream.write(bytes.toByteArray());

                        LOGGER.info("Downloaded in {}ms", System.currentTimeMillis() - start);

                        response.onNext(DownloadResponse.newBuilder()
                                .setStatus(DownloadStatus.COMPLETE)
                                .setPercentage(1)
                                .build());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

            response.onCompleted();
        } catch (IOException e) {
            LOGGER.error("An error has occurred while uploading a file", e);
            response.onError(e);
        }
    }

    @Override
    public void removeFile(RemoveRequest request, StreamObserver<RemoveResponse> response) {
        response.onNext(RemoveResponse.newBuilder()
                .setStatus(RemoveStatus.PENDING)
                .setPercentage(0)
                .build());

        sheetIO.deleteData(request.getId(), false, error -> response.onError(new RuntimeException(error)), () -> {
            response.onNext(RemoveResponse.newBuilder()
                    .setStatus(RemoveStatus.COMPLETE)
                    .setPercentage(1)
                    .build());
            response.onCompleted();
        });
    }

    @Override
    public void executeCode(CodeExecutionRequest request, StreamObserver<CodeExecutionResponse> response) {
        holySheet.getjShellRemote().queueRequest(request, response);
    }

    @Override
    public void listenCallbacks(ListenCallbacksRequest request, StreamObserver<CodeExecutionCallbackResponse> response) {
        this.response = response;
    }

    public void acceptCallback(CodeExecutionCallbackResponse callbackResponse) {
        if (response != null) {
            response.onNext(callbackResponse);
        }
    }
}
