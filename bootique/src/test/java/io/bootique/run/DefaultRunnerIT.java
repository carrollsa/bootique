/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.run;

import io.bootique.BQCoreModule;
import io.bootique.Bootique;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.command.Commands;
import io.bootique.help.HelpCommand;
import io.bootique.log.BootLogger;
import io.bootique.log.DefaultBootLogger;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.unit.BQInternalInMemoryPrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultRunnerIT {

    private BQInternalInMemoryPrintStream out;
    private BootLogger logger;

    @BeforeEach
    public void before() {
        this.out = new BQInternalInMemoryPrintStream(System.out);
        this.logger = new DefaultBootLogger(true, out, System.err);
    }

    @Test
    public void testRun_Explicit() {

        Bootique.app("-x")
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        assertTrue(out.toString().contains("x_was_run"));
    }

    @Test
    public void testRun_Implicit_Default() {

        BQInternalInMemoryPrintStream out = new BQInternalInMemoryPrintStream(System.out);
        BootLogger logger = new DefaultBootLogger(false, out, System.err);

        Bootique.app()
                .module(b -> BQCoreModule.extend(b).setDefaultCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        assertTrue(out.toString().contains("x_was_run"));
    }

    @Test
    public void testRun_Implicit_Help() {

        Bootique.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertTrue(help.contains("-h, --help"));
        assertTrue(help.contains("-x"));
        assertFalse(help.contains("x_was_run"));
    }

    @Test
    public void testRun_Implicit_NoModuleCommands_NoHelp() {

        Bootique.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .moduleProvider(Commands.builder(YCommand.class).noModuleCommands().build())
                .bootLogger(logger)
                .createRuntime()
                .run();

        assertFalse(out.toString().contains("-h, --help"));
    }

    @Test
    public void testRun_Implicit_NoModuleCommands_HelpAllowed() {

        Bootique.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .moduleProvider(Commands.builder(YCommand.class, HelpCommand.class).noModuleCommands().build())
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertTrue(help.contains("-h, --help"));
        assertFalse(help.contains("-x"));
        assertTrue(help.contains("-y"));

        assertFalse(help.contains("x_was_run"));
        assertFalse(help.contains("y_was_run"));
    }


    @Test
    public void testRun_Implicit_HelpRedefined() {

        Bootique.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .moduleProvider(Commands.builder(XHelpCommand.class).noModuleCommands().build())
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertFalse(help.contains("-h, --help"));
        assertTrue(help.contains("xhelp_was_run"));
    }

    @Test
    public void testRun_Implicit_Default_NoModuleCommands() {

        Bootique.app()
                .module(b -> BQCoreModule.extend(b).setDefaultCommand(XCommand.class))
                .moduleProvider(Commands.builder(X1Command.class).noModuleCommands().build())
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertFalse(help.contains("x_was_run"));
        assertTrue(help.contains("x1_was_run"));
    }

    public static class XHelpCommand extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public XHelpCommand(BootLogger logger) {
            // use meta from X
            super(CommandMetadata.builder(HelpCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("xhelp_was_run");
            return CommandOutcome.succeeded();
        }
    }

    public static class XCommand extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public XCommand(BootLogger logger) {
            super(CommandMetadata.builder(XCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("x_was_run");
            return CommandOutcome.succeeded();
        }
    }

    public static class X1Command extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public X1Command(BootLogger logger) {
            // use meta from X
            super(CommandMetadata.builder(XCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("x1_was_run");
            return CommandOutcome.succeeded();
        }
    }

    public static class YCommand extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public YCommand(BootLogger logger) {
            super(CommandMetadata.builder(YCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("y_was_run");
            return CommandOutcome.succeeded();
        }
    }
}
