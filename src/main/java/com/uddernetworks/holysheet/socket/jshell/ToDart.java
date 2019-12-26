package com.uddernetworks.holysheet.socket.jshell;

import java.util.List;

public class ToDart {

    private String callbackState;
    private List<String> snippetResult;
    private List<SerializedVariable> variables;

    public ToDart(String callbackState, List<String> snippetResult, List<SerializedVariable> variables) {
        this.callbackState = callbackState;
        this.snippetResult = snippetResult;
        this.variables = variables;
    }

    public String getCallbackState() {
        return callbackState;
    }

    public List<String> getSnippetResult() {
        return snippetResult;
    }

    public List<SerializedVariable> getVariables() {
        return variables;
    }
}
