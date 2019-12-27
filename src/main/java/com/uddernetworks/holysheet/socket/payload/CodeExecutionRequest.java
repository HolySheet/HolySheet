package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client --> Server</pre>
 * Send to request the arbitrary execution of code on the Java program. This code is ran in a static class and on the same JVM, having access to specific static &quot;access point&quot; variables, used for smaller things it may not be worth (Or too complex) to make a whole new request for, such as file selection. This supports callbacks as well, which will be demonstrated in <a href='#CodeExecutionCallbackResponse-11'>CodeExecutionCallbackResponse</a>. Code is invoked via JShell.
 * <pre>
 *     {
 *         "code": "Map.of(\"one\", 1)"
 *     }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>invokeCode</td><td>String</td><td>A snippet of Java code, exactly one complete snippet of source code, that is, one expression,statement, variable declaration, method declaration, class declaration,or import. - <a href='https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jshell/jdk/jshell/JShell.html#eval(java.lang.String)'>JShell</a></td></tr></tbody>
 * </table>
 *
 * @see PayloadType#CODE_EXECUTION_REQUEST
 */
public class CodeExecutionRequest extends BasicPayload {

    private String invokeCode;

    public CodeExecutionRequest(int code, String message, String state, String invokeCode) {
        super(code, PayloadType.CODE_EXECUTION_REQUEST, message, state);
        this.invokeCode = invokeCode;
    }

    public String getInvokeCode() {
        return invokeCode;
    }

    @Override
    public String toString() {
        return "ListRequest{" +
                "invokeCode='" + invokeCode + '\'' +
                '}';
    }
}
