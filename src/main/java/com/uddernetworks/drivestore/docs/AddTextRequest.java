package com.uddernetworks.drivestore.docs;

import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;

public class AddTextRequest implements TextRequest {

    private final String text;
    private final int prevIndex;
    private final int endIndex;
    private final Range range;

    public AddTextRequest(String text, int prevIndex) {
        this.text = text;
        this.prevIndex = prevIndex;
        this.endIndex = prevIndex + text.length();
        this.range = new Range().setStartIndex(prevIndex).setEndIndex(endIndex);
    }

    public int getEndIndex() {
        return endIndex;
    }

    public Range getRange() {
        return range;
    }

    @Override
    public Request getRequest() {
        return new Request().setInsertText(new InsertTextRequest().setText(text).setLocation(new Location().setIndex(prevIndex)));
    }
}
