package com.uddernetworks.holysheet.grpc;

import com.uddernetworks.grpc.sheetyGui.SheetyGUIServiceGrpc.SheetyGUIServiceImplBase;
import com.uddernetworks.grpc.sheetyGui.SheetyGuiService;
import com.uddernetworks.grpc.sheetyGui.SheetyGuiService.ClipboardRequest;
import com.uddernetworks.grpc.sheetyGui.SheetyGuiService.ClipboardResponse;
import com.uddernetworks.grpc.sheetyGui.SheetyGuiService.SelectorRequest;
import com.uddernetworks.grpc.sheetyGui.SheetyGuiService.SelectorResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Only used in SheetyGUI. This exists simply because Flutter Desktop embedding does not support all features on all
 * platforms, so in the future this class will most likely be deprecated/removed.
 */
public class SheetyGUIServiceImpl extends SheetyGUIServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SheetyGUIServiceImpl.class);

    @Override
    public void openFileSelector(SelectorRequest request, StreamObserver<SelectorResponse> response) {
        CompletableFuture.runAsync(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            var chooser = new JFileChooser(request.getInitialDirectory());
            chooser.setMultiSelectionEnabled(request.getMultiSelect());
            chooser.setDialogTitle(request.getTitle());
            chooser.setFileSelectionMode(request.getModeValue());
            int result;
            if (request.getMode() == SelectorRequest.SelectionMode.Open) {
                result = chooser.showOpenDialog(new JFrame());
            } else {
                result = chooser.showSaveDialog(new JFrame());
            }

            if (result == 0) {
                response.onNext(SelectorResponse.newBuilder()
                        .addAllFiles(Stream.of(chooser.isMultiSelectionEnabled() ? chooser.getSelectedFiles() : new File[]{chooser.getSelectedFile()})
                                .map(File::getAbsolutePath)
                                .collect(Collectors.toUnmodifiableList()))
                        .build());
            } else {
                response.onNext(SelectorResponse.newBuilder().setCancelled(true).build());
            }

            response.onCompleted();
        }).exceptionally(t -> {
            LOGGER.error("An error has occurred while opening file explorer", t);
            response.onError(t);
            return null;
        });
    }

    @Override
    public void getClipboard(ClipboardRequest request, StreamObserver<ClipboardResponse> response) {
        try {
            response.onNext(ClipboardResponse.newBuilder()
                    .setContent((String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor))
                    .build());
            response.onCompleted();
        } catch (UnsupportedFlavorException | IOException e) {
            LOGGER.error("An error has occurred while getting ");
            response.onError(e);
        }
    }
}
