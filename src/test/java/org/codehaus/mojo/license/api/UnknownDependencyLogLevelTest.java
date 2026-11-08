package org.codehaus.mojo.license.api;

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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class UnknownDependencyLogLevelTest {

    private static final String MESSAGE = "dependency [{}] does not exist in project, remove it from the missing file.";

    @Test
    void remoteMissingFileEntryIsLoggedAtDebug() {
        final Logger log = mock(Logger.class);

        DefaultThirdPartyTool.logUnknownDependency(log, true, MESSAGE, "org.test:a:1");

        verify(log).debug(MESSAGE, "org.test:a:1");
        verifyNoMoreInteractions(log);
    }

    @Test
    void localMissingFileEntryIsLoggedAtWarn() {
        final Logger log = mock(Logger.class);

        DefaultThirdPartyTool.logUnknownDependency(log, false, MESSAGE, "org.test:a:1");

        verify(log).warn(MESSAGE, "org.test:a:1");
        verifyNoMoreInteractions(log);
    }
}
