package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 Codehaus
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class VerboseParameterTest {

    private static class TestMojo extends AbstractLicenseMojo {
        TestMojo() {
            super(mock(MavenProjectHelper.class));
        }

        @Override
        public boolean isSkip() {
            return true;
        }

        @Override
        protected void init() {}

        @Override
        protected void doAction() {}
    }

    /** Records the warnings and reports debug level when asked to, as with {@code mvn -X}. */
    private static class RecordingLog extends SystemStreamLog {
        private final boolean debug;

        private final List<String> warnings = new ArrayList<>();

        RecordingLog(boolean debug) {
            this.debug = debug;
        }

        @Override
        public boolean isDebugEnabled() {
            return debug;
        }

        @Override
        public void warn(CharSequence content) {
            warnings.add(content.toString());
        }
    }

    private static void execute(AbstractMojo mojo, Log log) throws Exception {
        mojo.setLog(log);
        mojo.execute();
    }

    @Test
    void debugModeDoesNotTurnVerboseOn() throws Exception {
        TestMojo mojo = new TestMojo();

        execute(mojo, new RecordingLog(true));

        assertFalse(mojo.isVerbose());
    }

    @Test
    void settingVerboseWarnsOnce() throws Exception {
        TestMojo mojo = new TestMojo();
        mojo.verbose = Boolean.TRUE;
        RecordingLog log = new RecordingLog(false);

        execute(mojo, log);

        assertEquals(1, log.warnings.size());
        assertTrue(log.warnings.get(0).contains("'verbose'"), log.warnings.get(0));
    }

    @Test
    void verboseNotSetDoesNotWarn() throws Exception {
        RecordingLog log = new RecordingLog(false);

        execute(new TestMojo(), log);

        assertTrue(log.warnings.isEmpty());
    }
}
