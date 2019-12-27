package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

import java.util.List;

/**
 * <pre>Client <-- Server</pre>
 * A response with any variables (automatically or manually) created, along with a dump of all current or previous variables. In the future, there will be an option in the request to only dump certain ones, however this is not available in the early stages of the protocol. The below JSON sample shows the result for the above code executed in <a href='#CodeExecutionRequest-9'>CodeExecutionRequest</a>.
 * <pre>
 *      {
 *        "snippetResult": [
 *          "$1"
 *        ],
 *        "variables": [
 *          {
 *            "name": "$1",
 *            "type": "java.util.ImmutableCollections.Map1",
 *            "object": {
 *              "one": 1
 *            }
 *          }
 *        ]
 *      }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>snippetResult</td><td>String[]</td><td>A list of implicit or explicitly created variables in the snippet.</td></tr><tr><td>variables</td><td>Variable[]</td><td>A list of all variables created or used in the current or past snippets. The Variable object is outlined in the following 3 properties.</td></tr><tr><td>name</td><td>String</td><td>The name of the variable</td></tr><tr><td>type</td><td>String</td><td>The canonical Java class name of the object</td></tr><tr><td>object</td><td>Object</td><td>The Gson-serialized Java object. This is not always small or easy to manage, so it is important to limit variables and general use of this request.</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#CODE_EXECUTION_RESPONSE
 */
public class CodeExecutionResponse extends BasicPayload {

    private List<String> snippetResult;
    private List<SerializedVariable> variables;

    public CodeExecutionResponse(int code, String message, String state, List<String> snippetResult, List<SerializedVariable> variables) {
        super(code, PayloadType.CODE_EXECUTION_RESPONSE, message, state);
        this.snippetResult = snippetResult;
        this.variables = variables;
    }

    public List<String> getSnippetResult() {
        return snippetResult;
    }

    public List<SerializedVariable> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "CodeExecutionResponse{" +
                "snippetResult=" + snippetResult +
                ", variables=" + variables +
                '}';
    }
}
