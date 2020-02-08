package com.uddernetworks.holysheet.jshell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionCallbackResponse;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionRequest;
import com.uddernetworks.grpc.HolysheetService.CodeExecutionResponse;
import com.uddernetworks.grpc.HolysheetService.SerializedVariable;
import com.uddernetworks.holysheet.grpc.GRPCClient;
import com.uddernetworks.holysheet.utility.Utility;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated
public class JShellRemote {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Logger LOGGER = LoggerFactory.getLogger(JShellRemote.class);

    private static final Pattern CALLBACK_PATTERN = Pattern.compile("(?:\\/\\/ callback )(.*$)", Pattern.MULTILINE);
    private static final Pattern LINEBREAK = Pattern.compile("\\R");

    private static JShellRemote instance;

    private Queue<Map.Entry<CodeExecutionRequest, StreamObserver<CodeExecutionResponse>>> requestQueue = new ConcurrentLinkedQueue<>();

    private JShell shell;

    private Field key;
    private Field diagnostics;
    private Method keyName;

    private final GRPCClient grpcClient;
    private GsonExecutionControl executionControl;

    public JShellRemote(GRPCClient grpcClient) {
        this.grpcClient = grpcClient;

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
                "com.uddernetworks.holysheet.jshell.JShellRemote"
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
                    runCode(request.getCode(), request.getReturnVariablesList(), entry.getValue());
                } catch (Exception e) {
                    LOGGER.error("An exception occurred", e);
                }
            }
        });
    }

    public void queueRequest(CodeExecutionRequest codeExecutionRequest, StreamObserver<CodeExecutionResponse> response) {
        requestQueue.add(new AbstractMap.SimpleEntry<>(codeExecutionRequest, response));
    }

    public static void callback(String state, Object... dataArr) {
        if (dataArr.length % 2 != 0) {
            LOGGER.error("Data is not a multiple of 2!");
            return;
        }

        var variables = new ArrayList<SerializedVariable>();
        for (int i = 0; i < dataArr.length; i += 2) {
            variables.add(SerializedVariable.newBuilder()
                    .setName((String) dataArr[i])
                    .setObject(GSON.toJson(dataArr[i + 1]))
                    .build());
        }

        instance.sendCallbackResponse(CodeExecutionCallbackResponse.newBuilder()
                .setCallbackState(state)
                .addAllSnippetResult(variables.stream().map(SerializedVariable::getName).collect(Collectors.toUnmodifiableList()))
                .addAllVariables(variables)
                .build());
    }

    private void sendCallbackResponse(CodeExecutionCallbackResponse response) {
        grpcClient.getService().acceptCallback(response);
    }

    public void runCode(String code, List<String> returningVariables, StreamObserver<CodeExecutionResponse> response) {
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
                return String.format("JShellRemote.callback(\"%s\"%s);", callbackState, variables);
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
                    response.onError(new StatusRuntimeException(Status.fromThrowable(exception)));
                    LOGGER.error("An error occurred in snippet", exception);
                    return;
                } else if (sne.status() == Snippet.Status.REJECTED) {
                    LOGGER.info("HERE!!!22222222");
                    var message = getDiagnostics(snippet).map(diagnostics ->
                            joinDiagnostics(snippet.source(), diagnostics))
                            .orElse("Rejected for unknown reasons");
                    response.onError(new StatusRuntimeException(Status.INTERNAL));
                    LOGGER.info("An error occurred resulting in a rejected snippet:\n{}", message);
                    return;
                } else if (value != null) {
                    if (snippet.kind() == Snippet.Kind.VAR) {
                        getKey(snippet).ifPresent(retrieved::add);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        response.onNext(CodeExecutionResponse.newBuilder()
                .addAllSnippetResult(retrieved)
                .addAllVariables(getVariables(retrieved, returningVariables))
                .build());
    }

    @SafeVarargs
    private List<SerializedVariable> getVariables(List<String>... includeList) {
        var include = Arrays.stream(includeList).flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
        return executionControl.getFields()
                .stream()
                .filter(field -> include.contains(field.getName()))
                .map(field -> SerializedVariable.newBuilder()
                        .setName(field.getName())
                        .setObject(GSON.toJson(get(field)))
                        .build())
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
