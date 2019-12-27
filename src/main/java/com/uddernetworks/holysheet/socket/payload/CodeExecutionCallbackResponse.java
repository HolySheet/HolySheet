package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

import java.util.List;

/**
 * <pre>Client <-- Server</pre>
 * A response sent to the client an arbitrary amount of times, an arbitrary amount of time after a given <a href='#CodeExecutionRequest-9'>CodeExecutionRequest</a>. These callbacks are defined in CodeExecutionRequests, and allow for very dynamic variable fetching/code execution. The following snippet is a standard JSON response, and after the property table will be the Java code sent in the CodeExecutionRequest to generate this output. With this code snippet, this callback response is sent 5 seconds after the initial code request.
 * <pre>
 * {
 *   "callbackState": "030ccb35-8e0b-4c13-a0a1-9a6347ad8849",
 *   "snippetResult": [
 *     "theTime",
 *     "theTimeTen"
 *   ],
 *   "variables": [
 *     {
 *       "name": "theTime",
 *       "type": "java.lang.Long",
 *       "object": 1577394471130
 *     },
 *     {
 *       "name": "theTimeHalved",
 *       "type": "java.lang.Long",
 *       "object": 788697235565
 *     }
 *   ]
 * }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>callbackState</td><td>String</td><td>An untrimmed UUID (Set in the code) to identify the callback. This is separate from the standard <code>state</code> property.</td></tr><tr><td>snippetResult</td><td>String[]</td><td>A list of variable names passed to the callback, sent by the client</td></tr><tr><td>variables</td><td>Variable[]</td><td>A list of all variables listed in the snippetResult. The Variable object is outlined in the following 3 properties.</td></tr><tr><td>name</td><td>String</td><td>The name of the variable</td></tr><tr><td>type</td><td>String</td><td>The canonical Java class name of the object</td></tr><tr><td>object</td><td>Object</td><td>The Gson-serialized Java object. This is not always small or easy to manage, so it is important to limit variables and general use of this request.</td></tr></tbody>
 * </table>
 *
 * The code used in order to create callbacks like these are in the following format:
 *
 * <pre>
 * // callback UUID variable1 variable2 variable3...
 * </pre>
 *
 * This comment will be converted into the proper code that will be executed on the same line as the comment. The following code (Sent by the <a href='#CodeExecutionRequest-9'>CodeExecutionRequest</a>) was used to produce the above request. Due to the <code>Thread.sleep</code>, the callback is sent after 5 seconds with the local variables <code>theTime</code> and <code>theTimeHalved</code> sent over as well.
 *
 * <pre>
 * CompletableFuture.runAsync(() -> {
 *     try {
 *         Thread.sleep(5000);
 *     } catch (InterruptedException ignored) {}
 *     long theTime = System.currentTimeMillis();
 *     long theTimeHalved = System.currentTimeMillis() / 2;
 *     // callback 030ccb35-8e0b-4c13-a0a1-9a6347ad8849 theTime theTimeHalved
 * });
 * </pre>
 *
 * @see PayloadType#CODE_EXECUTION_CALLBACK_RESPONSE
 */
public class CodeExecutionCallbackResponse extends BasicPayload {

    private String callbackState;
    private List<String> snippetResult;
    private List<SerializedVariable> variables;

    public CodeExecutionCallbackResponse(int code, String message, String state, String callbackState, List<String> snippetResult, List<SerializedVariable> variables) {
        super(code, PayloadType.CODE_EXECUTION_CALLBACK_RESPONSE, message, state);
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

    @Override
    public String toString() {
        return "CodeExecutionCallbackResponse{" +
                "callbackState='" + callbackState + '\'' +
                ", snippetResult=" + snippetResult +
                ", variables=" + variables +
                '}';
    }
}
