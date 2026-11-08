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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractLicensesXmlMojoTest {

    private static class TestMojo extends AbstractLicensesXmlMojo {
        TestMojo(File licensesOutputFile) {
            super(null);
            this.licensesOutputFile = licensesOutputFile;
            this.licensesOutputFileEol = Eol.AUTODETECT;
        }

        @Override
        public void execute() {}
    }

    @Test
    void autodetectFallsBackToLfWhenNoCandidateFileExists(@TempDir Path dir) {
        TestMojo mojo = new TestMojo(dir.resolve("licenses.xml").toFile());

        mojo.getEncoding();

        assertEquals(Eol.LF, mojo.licensesOutputFileEol);
        assertEquals("\n", mojo.licensesOutputFileEol.getEolString());
    }

    @Test
    void autodetectStillReadsAnExistingFile(@TempDir Path dir) throws Exception {
        Path existing = dir.resolve("licenses.xml");
        Files.write(existing, "<licenseSummary>\r\n</licenseSummary>".getBytes(StandardCharsets.UTF_8));
        TestMojo mojo = new TestMojo(existing.toFile());

        mojo.getEncoding();

        assertEquals(Eol.CRLF, mojo.licensesOutputFileEol);
    }
}
