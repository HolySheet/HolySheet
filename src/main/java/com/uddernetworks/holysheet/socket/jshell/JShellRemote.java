package com.uddernetworks.holysheet.socket.jshell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uddernetworks.holysheet.socket.SocketCommunication;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionCallbackResponse;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionRequest;
import com.uddernetworks.holysheet.socket.payload.CodeExecutionResponse;
import com.uddernetworks.holysheet.socket.payload.SerializedVariable;
import com.uddernetworks.holysheet.utility.Utility;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JShellRemote {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Logger LOGGER = LoggerFactory.getLogger(JShellRemote.class);

    private static final Pattern callbackPattern = Pattern.compile("(?:\\/\\/ callback )(.*$)", Pattern.MULTILINE);

    private static JShellRemote instance;

    private Queue<Map.Entry<CodeExecutionRequest, Consumer<CodeExecutionResponse>>> requestQueue = new ConcurrentLinkedQueue<>();

    private JShell shell;

    private Field key;
    private Method keyName;

    private final SocketCommunication socketCommunication;
    private GsonExecutionControl executionControl;

    public JShellRemote(SocketCommunication socketCommunication) {
        this.socketCommunication = socketCommunication;
        try {
            key = Snippet.class.getDeclaredField("key");
            key.setAccessible(true);

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
                "java.util.Map",
                "java.util.Collections",
                "java.util.concurrent.CompletableFuture",
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

                var request = entry.getKey();
                var response = runCode(request.getState(), request.getInvokeCode(), request.getReturnVariables());
                entry.getValue().accept(response);
            }
        });
    }

    public void queueRequest(CodeExecutionRequest codeExecutionRequest, Consumer<CodeExecutionResponse> responseConsumer) {
        requestQueue.add(new AbstractMap.SimpleEntry<>(codeExecutionRequest, responseConsumer));
    }

    public static void callback(String requestState, String state, Object... dataArr) {
        if (dataArr.length % 2 != 0) {
            LOGGER.error("Data is not a multiple of 2!");
            return;
        }

        var data = new ArrayList<SerializedVariable>();
        for (int i = 0; i < dataArr.length; i += 2) {
            data.add(new SerializedVariable((String) dataArr[i], dataArr[i + 1]));
        }

        instance.sendCallbackResponse(new CodeExecutionCallbackResponse(1, "Success", requestState, state, data.stream().map(SerializedVariable::getName).collect(Collectors.toUnmodifiableList()), data));
    }

    private void sendCallbackResponse(CodeExecutionCallbackResponse response) {
        LOGGER.info("Sending callback response");
        socketCommunication.sendPayload(response);
    }

    public CodeExecutionResponse runCode(String state, String code, List<String> returningVariables) {
        LOGGER.info("Preprocessing code...");

        var matcher = callbackPattern.matcher(code);
        code = matcher.replaceAll(result -> {
            var calling = result.group(1).split("\\s+");
            if (calling.length == 0) {
                System.out.println("Calling length 0!");
            } else {
                var callbackState = calling[0];
                var rest = Arrays.copyOfRange(calling, 1, calling.length);
                return String.format("JShellRemote.callback(\"%s\", \"%s\", %s);", state, callbackState, Arrays.stream(rest).map(str -> "\"" + str + "\", " + str).collect(Collectors.joining(",")));
            }

            return result.group();
        });

        // Variable names of returned data
        var retrieved = new ArrayList<String>();

        shell.eval(code).forEach(sne -> {
            try {
                var exception = sne.exception();
                var value = sne.value();
                if (exception != null) {
                    LOGGER.error("An error occurred", exception);
                } else if (sne.status() == Snippet.Status.REJECTED) {
                    LOGGER.error("Rejected");
                } else if (value != null) {
                    var snippet = sne.snippet();
                    if (snippet.kind() == Snippet.Kind.VAR) {
                        getKey(snippet).ifPresent(retrieved::add);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

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

}
