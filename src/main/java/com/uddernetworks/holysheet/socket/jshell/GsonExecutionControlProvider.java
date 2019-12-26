package com.uddernetworks.holysheet.socket.jshell;

import jdk.jshell.execution.LocalExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GsonExecutionControlProvider  implements ExecutionControlProvider {

    private GsonExecutionControl executionControl = new GsonExecutionControl();

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonExecutionControlProvider.class);

    /**
     * Create an instance.  An instance can be used to
     * {@linkplain  #generate generate} an {@link ExecutionControl} instance
     * that executes code in the same process.
     */
    public GsonExecutionControlProvider() {
    }

    /**
     * The unique name of this {@code ExecutionControlProvider}.
     *
     * @return "local"
     */
    @Override
    public String name() {
        return "gson";
    }

    /**
     * Create and return the default parameter map for
     * {@code LocalExecutionControlProvider}.
     * {@code LocalExecutionControlProvider} has no parameters.
     *
     * @return an empty parameter map
     */
    @Override
    public Map<String,String> defaultParameters() {
        return ExecutionControlProvider.super.defaultParameters();
    }

    /**
     * Create and return a locally executing {@code ExecutionControl} instance.
     *
     * @param env the execution environment, provided by JShell
     * @param parameters the {@linkplain #defaultParameters()  default} or
     * modified parameter map.
     * @return the execution engine
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) {
        return executionControl;
    }

    public GsonExecutionControl getExecutionControl() {
        return executionControl;
    }

}
