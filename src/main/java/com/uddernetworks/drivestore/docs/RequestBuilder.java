package com.uddernetworks.drivestore.docs;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.TextRun;
import com.google.api.services.docs.v1.model.TextStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RequestBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBuilder.class);

    private final Docs docs;
    private int textIndex = 1;
    private boolean executed;
    private List<TextRequest> requests = new ArrayList<>();

    public RequestBuilder(Docs docs) {
        this.docs = docs;
    }

    public RequestBuilder addStyledText(TextRun textRun) {
        return addStyledText(textRun.getContent(), textRun.getTextStyle());
    }

    public RequestBuilder addStyledText(String text, TextStyle style) {
        var insertText = new AddTextRequest(text, textIndex);
        textIndex = insertText.getEndIndex();
        System.out.println("textIndex = " + textIndex);
        requests.add(insertText);
        requests.add(new AddStyleRequest(insertText, style));
        return this;
    }

    public RequestBuilder addRequest(TextRequest request) {
        requests.add(request);
        return this;
    }

    public List<Request> getRequests() {
        return requests.stream().map(TextRequest::getRequest).collect(Collectors.toUnmodifiableList());
    }

    public void execute(String documentId) throws IOException {
        if (executed) {
            LOGGER.error("RequestBuilder already executed!");
            return;
        }

        executed = true;
        var request = docs.documents().batchUpdate(documentId, new BatchUpdateDocumentRequest().setRequests(getRequests()));
//        System.out.println("Request " + request);
//        System.out.println("media " + request.getMediaHttpUploader());
//        request.getMediaHttpUploader().setProgressListener(new ProgressListener("Style"));
        request.execute();
    }
}
