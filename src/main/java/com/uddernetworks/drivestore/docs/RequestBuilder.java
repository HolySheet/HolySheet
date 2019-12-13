package com.uddernetworks.drivestore.docs;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.TextRun;
import com.google.api.services.docs.v1.model.TextStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RequestBuilder {

//    public static final int REQUESTS_PER_BATCH = 0b11111111111111; // 2^14
    public static final int REQUESTS_PER_BATCH = 0b1111111111111; // 2^13

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

        var index = new AtomicInteger();
        var temp = new AtomicInteger(-1);
        var grouped = requests.stream().map(TextRequest::getRequest).collect(Collectors.groupingBy(x -> {
            if (temp.incrementAndGet() < REQUESTS_PER_BATCH) {
                return index.get();
            }

            temp.set(0);
            return index.incrementAndGet();
        }));

        LOGGER.info("Batch groups: {}", grouped.size());

        double total = grouped.size();

        grouped.forEach((i, group) -> {
            if (i < 23) return;
            try {
                LOGGER.info("{}%", Math.round((i / total) * 100));
                LOGGER.info("Requesting group {}", i);
                docs.documents().batchUpdate(documentId, new BatchUpdateDocumentRequest().setRequests(grouped.get(i))).execute();
                Thread.sleep(1000);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
