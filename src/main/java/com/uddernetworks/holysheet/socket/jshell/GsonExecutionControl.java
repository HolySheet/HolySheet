/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.uddernetworks.holysheet.socket.jshell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uddernetworks.holysheet.socket.payload.SerializedVariable;
import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.SPIResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * An implementation of {@link jdk.jshell.spi.ExecutionControl} which executes
 * in the same JVM as the JShell-core.
 *
 * @author Grigory Ptashko
 * @since 9
 */
public class GsonExecutionControl extends DirectExecutionControl {

    private static final boolean SERIALIZE_OUTPUT = false;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Map<Class<?>, List<Field>> classFields = new ConcurrentHashMap<>();

    private final Object STOP_LOCK = new Object();
    private boolean userCodeRunning = false;
    private ThreadGroup execThreadGroup;

    /**
     * Creates an instance, delegating loader operations to the specified
     * delegate.
     *
     * @param loaderDelegate the delegate to handle loading classes
     */
    public GsonExecutionControl(LoaderDelegate loaderDelegate) {
        super(loaderDelegate);
    }

    /**
     * Create an instance using the default class loading.
     */
    public GsonExecutionControl() {
    }

    @Override
    public String invoke(String className, String methodName) throws RunException, InternalException, EngineTerminationException {
        Method doitMethod;
        try {
            Class<?> klass = findClass(className);
            doitMethod = klass.getDeclaredMethod(methodName, new Class<?>[0]);
            doitMethod.setAccessible(true);

            classFields.putIfAbsent(klass, Collections.synchronizedList(new ArrayList<>()));
        } catch (Throwable ex) {
            throw new InternalException(ex.toString());
        }

        try {
            clientCodeEnter();
            String result = invoke(doitMethod);
            System.out.flush();
            return result;
        } catch (RunException | InternalException | EngineTerminationException ex) {
            throw ex;
        } catch (SPIResolutionException ex) {
            return throwConvertedInvocationException(ex);
        } catch (InvocationTargetException ex) {
            return throwConvertedInvocationException(ex.getCause());
        } catch (Throwable ex) {
            return throwConvertedOtherException(ex);
        } finally {
            clientCodeLeave();
        }
    }

    @Override
    protected String invoke(Method doitMethod) throws Exception {
        execThreadGroup = new ThreadGroup("JShell process local execution");

        AtomicReference<InvocationTargetException> iteEx = new AtomicReference<>();
        AtomicReference<IllegalAccessException> iaeEx = new AtomicReference<>();
        AtomicReference<NoSuchMethodException> nmeEx = new AtomicReference<>();
        AtomicReference<Boolean> stopped = new AtomicReference<>(false);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (e instanceof InvocationTargetException) {
                if (e.getCause() instanceof ThreadDeath) {
                    stopped.set(true);
                } else {
                    iteEx.set((InvocationTargetException) e);
                }
            } else if (e instanceof IllegalAccessException) {
                iaeEx.set((IllegalAccessException) e);
            } else if (e instanceof NoSuchMethodException) {
                nmeEx.set((NoSuchMethodException) e);
            } else if (e instanceof ThreadDeath) {
                stopped.set(true);
            }
        });

        final Object[] res = new Object[1];
        Thread snippetThread = new Thread(execThreadGroup, () -> {
            try {
                var declaring = doitMethod.getDeclaringClass();

                res[0] = doitMethod.invoke(null, new Object[0]);

                for (Field declaredField : declaring.getDeclaredFields()) {
                    declaredField.setAccessible(true);
                    classFields.get(declaring).add(declaredField);
                }
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof ThreadDeath) {
                    stopped.set(true);
                } else {
                    iteEx.set(e);
                }
            } catch (IllegalAccessException e) {
                iaeEx.set(e);
            } catch (ThreadDeath e) {
                stopped.set(true);
            }
        });

        snippetThread.start();
        Thread[] threadList = new Thread[execThreadGroup.activeCount()];
        execThreadGroup.enumerate(threadList);
        for (Thread thread : threadList) {
            if (thread != null) {
                thread.join();
            }
        }

        if (stopped.get()) {
            throw new StoppedException();
        }

        if (iteEx.get() != null) {
            throw iteEx.get();
        } else if (nmeEx.get() != null) {
            throw nmeEx.get();
        } else if (iaeEx.get() != null) {
            throw iaeEx.get();
        }


        if (!SERIALIZE_OUTPUT) {
            return "";
        }

        return GSON.toJson(new SerializedVariable("?", res[0]));
    }

    public List<Field> getFields() {
        return classFields.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toUnmodifiableList());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void stop() throws InternalException {
        synchronized (STOP_LOCK) {
            if (!userCodeRunning) {
                return;
            }
            if (execThreadGroup == null) {
                throw new InternalException("Process-local code snippets thread group is null. Aborting stop.");
            }

            execThreadGroup.stop();
        }
    }

    @Override
    protected void clientCodeEnter() {
        synchronized (STOP_LOCK) {
            userCodeRunning = true;
        }
    }

    @Override
    protected void clientCodeLeave() {
        synchronized (STOP_LOCK) {
            userCodeRunning = false;
        }
    }

}
