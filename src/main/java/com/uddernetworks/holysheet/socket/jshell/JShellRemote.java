package com.uddernetworks.holysheet.socket.jshell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JShellRemote {

    // TODO: shell.sourceCodeAnalysis().analyzeCompletion

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Logger LOGGER = LoggerFactory.getLogger(JShellRemote.class);

    private static final Pattern callbackPattern = Pattern.compile("(?:\\/\\/ callback )(.*$)", Pattern.MULTILINE);

    private static JShellRemote instance;

    private JShell shell;

    // These are ONLY to be used by JShell, they should not be accessed anywhere in the project
    // they allow to interactively call the managers from the /evaluate command
    public static Object obj;

    private Field key;
    private Method keyName;

    private GsonExecutionControl executionControl;

    public JShellRemote() {
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

    public static JShellRemote getInstance() {
        if (instance == null) {
            instance = new JShellRemote();
        }

        return instance;
    }

    public void start() {
        runCode("CompletableFuture.runAsync(() -> {\n" +
                "            try {\n" +
                "                Thread.sleep(5000);\n" +
                "            } catch (InterruptedException ignored) {}\n" +
                "            System.out.println(\"After 5 seconds!\");\n" +
                "            long theTime = System.currentTimeMillis();\n" +
                "            long theTimeTen = System.currentTimeMillis() * 10;\n" +
                "            // callback 030ccb35-8e0b-4c13-a0a1-9a6347ad8849 theTime theTimeTen\n" +
                "        });");

//        runCode("Map.of(\"one\", 1)");

        try {
            Thread.sleep(100000);
        } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) {
        getInstance().start();
    }

    public static void callback(String state, Object... dataArr) {
        if (dataArr.length % 2 != 0) {
            LOGGER.error("Data is not a multiple of 2!");
            return;
        }

        var data = new ArrayList<SerializedVariable>();
        for (int i = 0; i < dataArr.length; i += 2) {
            data.add(new SerializedVariable((String) dataArr[i], dataArr[i + 1]));
        }

        LOGGER.warn("THIS WILL BE REPLACED WITH A SOCKET CALL BACK TO DART");

        getInstance().sendToDart(new ToDart(state, data.stream().map(SerializedVariable::getName).collect(Collectors.toUnmodifiableList()), data));
    }

    public void runCode(String code) {
        System.out.println(code);
        System.exit(0);
        LOGGER.info("Preprocessing code...");

        var matcher = callbackPattern.matcher(code);
        code = matcher.replaceAll(result -> {
            var calling = result.group(1).split("\\s+");
            if (calling.length == 0) {
                System.out.println("Calling length 0!");
            } else {
                var state = calling[0];
                var rest = Arrays.copyOfRange(calling, 1, calling.length);
                return String.format("JShellRemote.callback(\"%s\", %s);", state, Arrays.stream(rest).map(str -> "\"" + str + "\", " + str).collect(Collectors.joining(",")));
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

        sendToDart(new ToDart("1-1-1-1", retrieved, getVariables()));
    }

    private void sendToDart(ToDart toDart) {
        var toDartJson = GSON.toJson(toDart);
        System.out.println("toDartJson = \n" + toDartJson);
    }

    private List<SerializedVariable> getVariables() {
        return executionControl.getFields()
                .stream()
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
