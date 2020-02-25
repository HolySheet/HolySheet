package com.uddernetworks.holysheet.grpc;

import com.uddernetworks.grpc.HolySheetServiceGrpc.HolySheetServiceImplBase;
import com.uddernetworks.grpc.HolysheetService;
import com.uddernetworks.grpc.HolysheetService.ChunkResponse;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionCallbackResponse;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionRequest;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionResponse;
import com.uddernetworks.grpc.HolysheetService.CreateFolderRequest;
import com.uddernetworks.grpc.HolysheetService.DownloadRequest;
import com.uddernetworks.grpc.HolysheetService.DownloadResponse;
import com.uddernetworks.grpc.HolysheetService.DownloadResponse.DownloadStatus;
import com.uddernetworks.grpc.HolysheetService.FileChunk;
import com.uddernetworks.grpc.HolysheetService.FolderResponse;
import com.uddernetworks.grpc.HolysheetService.ListItem;
import com.uddernetworks.grpc.HolysheetService.ListRequest;
import com.uddernetworks.grpc.HolysheetService.ListResponse;
import com.uddernetworks.grpc.HolysheetService.ListenCallbacksRequest;
import com.uddernetworks.grpc.HolysheetService.MoveFileRequest;
import com.uddernetworks.grpc.HolysheetService.MoveFileResponse;
import com.uddernetworks.grpc.HolysheetService.RemoveRequest;
import com.uddernetworks.grpc.HolysheetService.RemoveResponse;
import com.uddernetworks.grpc.HolysheetService.RestoreRequest;
import com.uddernetworks.grpc.HolysheetService.RestoreResponse;
import com.uddernetworks.grpc.HolysheetService.StarRequest;
import com.uddernetworks.grpc.HolysheetService.StarResponse;
import com.uddernetworks.grpc.HolysheetService.UploadRequest;
import com.uddernetworks.grpc.HolysheetService.UploadResponse;
import com.uddernetworks.grpc.HolysheetService.UploadResponse.UploadStatus;
import com.uddernetworks.holysheet.HolySheet;
import com.uddernetworks.holysheet.RemoteAuthManager;
import com.uddernetworks.holysheet.SheetManager;
import com.uddernetworks.holysheet.command.CommandHandler;
import com.uddernetworks.holysheet.encoding.EncodingOutputStream;
import com.uddernetworks.holysheet.io.SheetIO;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HolySheetServiceImpl extends HolySheetServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HolySheetServiceImpl.class);
    private static final Map<String, Processor> processing = new ConcurrentHashMap<>();

    private final HolySheet holySheet;
    private SheetManager sheetManager;
    private SheetIO sheetIO;
    private StreamObserver<CodeExecutionCallbackResponse> response;

    public HolySheetServiceImpl(HolySheet holySheet) {
        this.holySheet = holySheet;
    }

    private void useToken(String token) {
        var authManager = new RemoteAuthManager();
        authManager.useToken(token);
        sheetManager = new SheetManager(authManager.getDrive(), authManager.getSheets());
        sheetIO = sheetManager.getSheetIO();
    }

    @Override
    public void listFiles(ListRequest request, StreamObserver<ListResponse> response) {
        useToken(request.getToken());

        var files = sheetManager.listUploads(request.getPath(), request.getStarred(), request.getTrashed())
                .stream()
                .map(this::getListItem)
                .collect(Collectors.toUnmodifiableList());

        response.onNext(ListResponse.newBuilder()
                .addAllItems(files)
                .addAllFolders(sheetIO.getFolders())
                .build());

        response.onCompleted();
    }

    static class Processor {
        private final String processingId;
        private final EncodingOutputStream encodingOut;
        private final Consumer<com.google.api.services.drive.model.File> onComplete;

        public Processor(String processingId, long maxLength, Consumer<com.google.api.services.drive.model.File> onComplete) {
            this.processingId = processingId;
            this.encodingOut = new EncodingOutputStream(maxLength);
            this.onComplete = onComplete;
        }

        public String getProcessingId() {
            return processingId;
        }

        public EncodingOutputStream getEncodingOut() {
            return encodingOut;
        }

        public void complete(com.google.api.services.drive.model.File file) {
            onComplete.accept(file);
        }
    }

    @Override
    public void uploadFile(UploadRequest request, StreamObserver<UploadResponse> response) {
        useToken(request.getToken());

        var path = sheetIO.cleanPath(request.getPath());
        var name = request.getName();
        name = name.substring(0, Math.min(name.length(), 32));

        LOGGER.info("Uploading {}...", name);

        try {
            if (request.getId() != null && !request.getId().isBlank()) {
                var dataOptional = sheetIO.downloadFile(request.getId());
                if (dataOptional.isPresent()) {
                    var fileData = dataOptional.get();
                    var data = fileData.getIn();
                    var fileSize = fileData.getSize();

                    long start = System.currentTimeMillis();

                    var uploaded = sheetIO.uploadDataFile(name, path, fileSize, request.getSheetSize(), request.getCompression(), request.getUpload(), data);

                    LOGGER.info("Uploaded {} in {}ms", uploaded.getId(), System.currentTimeMillis() - start);

                    sheetIO.createFolder(path);

                    response.onNext(UploadResponse.newBuilder()
                            .setItem(getListItem(uploaded))
                            .build());

                    response.onCompleted();

                    return;
                }
            }

            var processor = new Processor(request.getProcessingId(), request.getSheetSize(), file -> {
                try {
                    sheetIO.createFolder(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                response.onNext(UploadResponse.newBuilder()
                        .setUploadStatus(UploadStatus.COMPLETE)
                        .setItem(getListItem(file))
                        .build());
                response.onCompleted();
            });

            processing.put(request.getProcessingId(), processor);

            sheetIO.uploadDataStream(name, path, request.getFileSize(), request.getSheetSize(), request.getCompression(), request.getUpload(), processor.getEncodingOut())
                    .thenAccept(processor::complete);

            response.onNext(UploadResponse.newBuilder()
                    .setUploadStatus(UploadStatus.READY)
                    .build());

        } catch (IOException e) {
            LOGGER.error("An error has occurred while uploading a file", e);
            response.onError(e);
        }
    }

    @Override
    public StreamObserver<FileChunk> sendFile(StreamObserver<ChunkResponse> response) {
        AtomicReference<Processor> processor = new AtomicReference<>();
        return new StreamObserver<>() {
            @Override
            public void onNext(FileChunk chunk) {
                if (!processing.containsKey(chunk.getProcessingId())) {
                    LOGGER.error("Unknown processing ID: {}", chunk.getProcessingId());
                    return;
                }

                processor.set(processing.get(chunk.getProcessingId()));

                try {
                    processor.get().getEncodingOut().write(chunk.getContent().toByteArray());

                    if (chunk.getStatus() == FileChunk.ChunkStatus.Complete) {
                        processor.get().getEncodingOut().close();
                        response.onCompleted();
                    } else {
                        response.onNext(ChunkResponse.newBuilder()
                                .setCurrentBuffer(processor.get().getEncodingOut().getBufferLength())
                                .build());
                    }
                } catch (IOException e) {
                    LOGGER.error("An error occurred while writing data", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("An error has occurred while sending file", t);
                response.onError(t);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Complete with {}", processor.get().getProcessingId());
                response.onCompleted();
            }
        };
    }

    @Override
    public void downloadFile(DownloadRequest request, StreamObserver<DownloadResponse> response) {
        useToken(request.getToken());

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

            sheetManager.getSheetIO().downloadData(destination, id, percentage ->
                    response.onNext(DownloadResponse.newBuilder()
                            .setStatus(DownloadStatus.DOWNLOADING)
                            .setPercentage(percentage)
                            .build()))
                    .thenAccept(file -> {
                        LOGGER.info("Downloaded in {}ms", System.currentTimeMillis() - start);

                        response.onNext(DownloadResponse.newBuilder()
                                .setStatus(DownloadStatus.COMPLETE)
                                .setPercentage(1)
                                .setItem(getListItem(file))
                                .build());

                        response.onCompleted();
                    }).exceptionally(t -> {
                LOGGER.error("An error has occurred!", t);
                response.onError(new RuntimeException(t));
                return null;
            });

        } catch (IOException e) {
            LOGGER.error("An error has occurred while uploading a file", e);
            response.onError(e);
        }
    }

    @Override
    public void removeFile(RemoveRequest request, StreamObserver<RemoveResponse> response) {
        useToken(request.getToken());

        try {
            sheetIO.deleteData(request.getId(), false, request.getPermanent());

            response.onNext(RemoveResponse.newBuilder().build());
            response.onCompleted();
        } catch (Exception e) {
            response.onError(e);
            LOGGER.error("An error occurred while deleting file ID \"" + request.getId() + "\"", e);
        }
    }

    @Override
    public void restoreFile(RestoreRequest request, StreamObserver<RestoreResponse> response) {
        try {
            sheetIO.restoreData(request.getId());

            response.onNext(RestoreResponse.newBuilder().build());
            response.onCompleted();
        } catch (Exception e) {
            response.onError(e);
            LOGGER.error("An error occurred while restoring file ID \"" + request.getId() + "\"", e);
        }
    }

    @Override
    public void executeCode(CodeExecutionRequest request, StreamObserver<CodeExecutionResponse> response) {
        // was some jshell rubbish.
    }

    @Override
    public void listenCallbacks(ListenCallbacksRequest request, StreamObserver<CodeExecutionCallbackResponse> response) {
        this.response = response;
    }

    @Override
    public void starRequest(StarRequest request, StreamObserver<StarResponse> response) {
        useToken(request.getToken());

        try {
            sheetIO.setStarred(request.getId(), request.getStarred());
            response.onNext(StarResponse.newBuilder().build());
            response.onCompleted();
        } catch (IOException e) {
            LOGGER.error("An error has occurred while setting star status of a file", e);
            response.onError(e);
        }
    }

    @Override
    public void moveFile(MoveFileRequest request, StreamObserver<MoveFileResponse> response) {
        useToken(request.getToken());

        try {
            sheetIO.setPath(request.getId(), request.getPath());
            response.onNext(MoveFileResponse.newBuilder().build());
            response.onCompleted();
        } catch (IOException e) {
            LOGGER.error("An error has occurred while setting the path of a file to \"" + request.getPath() + "\"", e);
            response.onError(e);
        }
    }

    @Override
    public void createFolder(CreateFolderRequest request, StreamObserver<FolderResponse> response) {
        useToken(request.getToken());

        try {
            sheetIO.createFolder(request.getPath());
            response.onNext(FolderResponse.newBuilder().build());
            response.onCompleted();
        } catch (IOException e) {
            LOGGER.error("An error has occurred while creating folder \"" + request.getPath() + "\"", e);
            response.onError(e);
        }
    }

    public void acceptCallback(CodeExecutionCallbackResponse callbackResponse) {
        if (response != null) {
            response.onNext(callbackResponse);
        }
    }

    ListItem getListItem(com.google.api.services.drive.model.File file) {
        var owner = file.getOwners().get(0);
        return ListItem.newBuilder()
                .setName(file.getName())
                .setId(file.getId())
                .setPath(CommandHandler.getPath(file))
                .setSize(CommandHandler.getSize(file))
                .setSheets(CommandHandler.getSheetCount(file))
                .setDate(file.getModifiedTime().getValue())
                .setSelfOwned(owner.getMe())
                .setOwner(owner.getDisplayName())
                .setDriveLink(file.getWebViewLink())
                .setStarred(CommandHandler.isStarred(file))
                .setTrashed(file.getTrashed())
                .build();
    }
}
