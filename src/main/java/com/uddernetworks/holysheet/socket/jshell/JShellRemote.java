package com.uddernetworks.holysheet.socket.jshell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uddernetworks.holysheet.socket.SocketCommunication;
import com.uddernetworks.holysheet.socket.payload.BasicPayload;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionCallbackResponse;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionRequest;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionResponse;
import com.uddernetworks.holysheet.socket.payload.ErrorPayload;
import com.uddernetworks.holysheet.socket.payload.SerializedVariable;
import com.uddernetworks.holysheet.utility.Utility;
import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JShellRemote {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Logger LOGGER = LoggerFactory.getLogger(JShellRemote.class);

    private static final Pattern CALLBACK_PATTERN = Pattern.compile("(?:\\/\\/ callback )(.*$)", Pattern.MULTILINE);
    private static final Pattern LINEBREAK = Pattern.compile("\\R");

    private static JShellRemote instance;

    private Queue<Map.Entry<CodeExecutionRequest, Consumer<BasicPayload>>> requestQueue = new ConcurrentLinkedQueue<>();

    private JShell shell;

    private Field key;
    private Field diagnostics;
    private Method keyName;

    private final SocketCommunication socketCommunication;
    private GsonExecutionControl executionControl;

    public JShellRemote(SocketCommunication socketCommunication) {
        this.socketCommunication = socketCommunication;
        try {
            key = Snippet.class.getDeclaredField("key");
            key.setAccessible(true);

            diagnostics = Snippet.class.getDeclaredField("diagnostics");
            diagnostics.setAccessible(true);

            keyName = Class.forName("jdk.jshell.Key$PersistentKey").getDeclaredMethod("name");
            keyName.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Error while getting key and name()", e);
        }

        var controlProvider = new GsonExecutionControlProvider();
        executionControl = controlProvider.getExecutionControl();
        shell = JShell.builder().executionEngine(controlProvider, Collections.emptyMap()).build();

        Stream.of(
                "java.lang.*",
                "java.util.*",
                "java.io.*",
                "java.awt.*",
                "java.awt.datatransfer.*",
                "javax.swing.*",
                "java.util.Map",
                "java.util.Collections",
                "java.util.concurrent.*",
                "java.util.stream.*",
                "com.uddernetworks.holysheet.socket.jshell.JShellRemote"
        ).forEach(pkg -> shell.eval("import " + pkg + ";"));
    }

    public void start() {
        instance = this;
        CompletableFuture.runAsync(() -> {
            while (true) {
                if (requestQueue.isEmpty()) {
                    Utility.sleep(100);
                    continue;
                }

                var entry = requestQueue.remove();
                if (entry == null) {
                    Utility.sleep(100);
                    continue;
                }

                try {
                    var request = entry.getKey();
                    var response = runCode(request.getState(), request.getInvokeCode(), request.getReturnVariables());
                    entry.getValue().accept(response);
                } catch (Exception e) {
                    LOGGER.error("An exception occurred", e);
                }
            }
        });
    }

    public void queueRequest(CodeExecutionRequest codeExecutionRequest, Consumer<BasicPayload> responseConsumer) {
        requestQueue.add(new AbstractMap.SimpleEntry<>(codeExecutionRequest, responseConsumer));
    }

    public static void callback(String requestState, String state, Object... dataArr) {
        if (dataArr.length % 2 != 0) {
            LOGGER.error("Data is not a multiple of 2!");
            return;
        }

        System.out.println("dataArr = " + Arrays.toString(dataArr));

        var data = new ArrayList<SerializedVariable>();
        for (int i = 0; i < dataArr.length; i += 2) {
            data.add(new SerializedVariable((String) dataArr[i], dataArr[i + 1]));
        }

        var first = data.get(0);
        System.out.println("first = " + first);
        System.out.println(first.getObject());
        System.out.println(GSON.toJson(first.getObject()));

        instance.sendCallbackResponse(new CodeExecutionCallbackResponse(1, "Success", requestState, state, data.stream().map(SerializedVariable::getName).collect(Collectors.toUnmodifiableList()), data));
    }

    private void sendCallbackResponse(CodeExecutionCallbackResponse response) {
        LOGGER.info("Sending callback response");
        socketCommunication.sendPayload(response);
    }

    public BasicPayload runCode(String state, String code, List<String> returningVariables) {
        LOGGER.info("Preprocessing code...");

        var matcher = CALLBACK_PATTERN.matcher(code);
        code = matcher.replaceAll(result -> {
            var calling = result.group(1).split("\\s+");
            if (calling.length == 0) {
                System.out.println("Calling length 0!");
            } else {
                var callbackState = calling[0];
                var rest = Arrays.copyOfRange(calling, 1, calling.length);
                var variables = Arrays.stream(rest).map(str -> "\"" + str + "\", " + str).collect(Collectors.joining(","));
                if (!variables.isBlank()) {
                    variables = ", " + variables;
                }
                return String.format("JShellRemote.callback(\"%s\", \"%s\"%s);", state, callbackState, variables);
            }

            return result.group();
        });

        var retrieved = new ArrayList<String>();

        for (var sne : shell.eval(code)) {
            try {
                var exception = sne.exception();
                var value = sne.value();
                var snippet = sne.snippet();
                if (exception != null) {
                    return new ErrorPayload("Error occurred", state, Utility.getStackTrace(exception));
                } else if (sne.status() == Snippet.Status.REJECTED) {
                    var message = getDiagnostics(snippet).map(diagnostics ->
                            joinDiagnostics(snippet.source(), diagnostics))
                            .orElse("Rejected for unknown reasons");
                    return new ErrorPayload(message, state, Utility.getStackTrace());
                } else if (value != null) {
                    if (snippet.kind() == Snippet.Kind.VAR) {
                        getKey(snippet).ifPresent(retrieved::add);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new CodeExecutionResponse(1, "Success", state, retrieved, getVariables(retrieved, returningVariables));
    }

    @SafeVarargs
    private List<SerializedVariable> getVariables(List<String>... includeList) {
        var include = Arrays.stream(includeList).flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
        return executionControl.getFields()
                .stream()
                .filter(field -> include.contains(field.getName()))
                .map(field -> new SerializedVariable(field.getName(), get(field)))
                .filter(snippet -> snippet.getObject() != null)
                .collect(Collectors.toUnmodifiableList());
    }

    private String joinDiagnostics(String source, List<Diag> diagnostics) {
        return diagnostics.stream().map(d -> (d.isError() ? "Error:\n" : "Warning:\n") +
                String.join("\n", getDisplayableDiagnostic(source, d)))
                .collect(Collectors.joining("\n"));
    }

    private List<String> getDisplayableDiagnostic(String source, Diag diag) {
        var toDisplay = new ArrayList<String>();

        Arrays.stream(diag.getMessage(null).split("\\r?\\n"))
                .filter(line -> !line.trim().startsWith("location:"))
                .forEach(toDisplay::add);

        int pstart = (int) diag.getStartPosition();
        int pend = (int) diag.getEndPosition();
        var m = LINEBREAK.matcher(source);
        int pstartl = 0;
        int pendl = -2;
        while (m.find(pstartl)) {
            pendl = m.start();
            if (pendl >= pstart) {
                break;
            } else {
                pstartl = m.end();
            }
        }

        if (pendl < pstart) {
            pendl = source.length();
        }

        toDisplay.add(source.substring(pstartl, pendl));

        var builder = new StringBuilder();
        int start = pstart - pstartl;
        builder.append(" ".repeat(Math.max(0, start))).append('^');

        boolean multiline = pend > pendl;
        int end = (multiline ? pendl : pend) - pstartl - 1;
        if (end > start) {
            builder.append("-".repeat(Math.max(0, end - (start + 1))))
                    .append(multiline ? "-..." : "^");
        }

        toDisplay.add(builder.toString());
        return toDisplay;
    }

    private Optional<String> getKey(Snippet snippet) {
        try {
            var key = this.key.get(snippet);
            if (key == null) return Optional.empty();
            return Optional.ofNullable((String) keyName.invoke(key));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Object get(Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Optional<List<Diag>> getDiagnostics(Snippet snippet) {
        try {
            var dia = (List<Diag>) this.diagnostics.get(snippet);
            return Optional.ofNullable(dia);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
