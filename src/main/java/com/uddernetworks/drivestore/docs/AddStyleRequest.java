package com.uddernetworks.drivestore.docs;

import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.TextStyle;
import com.google.api.services.docs.v1.model.UpdateTextStyleRequest;

public class AddStyleRequest implements TextRequest {

    private final AddTextRequest textRequest;
    private final TextStyle style;

    public AddStyleRequest(AddTextRequest textRequest, TextStyle style) {
        this.textRequest = textRequest;
        this.style = style;
    }

    @Override
    public Request getRequest() {
        return new Request().setUpdateTextStyle(new UpdateTextStyleRequest()
                .setRange(textRequest.getRange())
                .setTextStyle(style)
                // https://developers.google.com/docs/api/reference/rest/v1/documents#textstyle
                .setFields("bold,italic,underline,backgroundColor,foregroundColor,fontSize"));
    }
}
