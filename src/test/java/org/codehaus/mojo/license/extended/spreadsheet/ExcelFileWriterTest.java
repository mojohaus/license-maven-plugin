package org.codehaus.mojo.license.extended.spreadsheet;

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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.mojo.license.download.LicenseClassifier;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelFileWriterTest {

    private static final String FORBIDDEN = "GPL 3.0";
    private static final String PROBLEMATIC = "EPL 2.0";
    private static final String OK = "Apache License, Version 2.0";
    private static final String UNCLASSIFIED = "Some House License";

    @TempDir
    File tempDir;

    @Test
    void writesEachLicenseInTheColourOfItsCategory() throws IOException {
        final File file = write("all", formatting(true));

        assertColor(file, FORBIDDEN, SpreadsheetUtil.FORBIDDEN_LICENSE_COLOR);
        assertColor(file, PROBLEMATIC, SpreadsheetUtil.PROBLEMATIC_LICENSE_COLOR);
        assertColor(file, OK, SpreadsheetUtil.OK_LICENSE_COLOR);
        assertColor(file, UNCLASSIFIED, SpreadsheetUtil.UNKNOWN_LICENSE_COLOR);
    }

    @Test
    void leavesUnclassifiedLicensesAloneUnlessAsked() throws IOException {
        final File file = write("classified-only", formatting(false));

        assertColor(file, FORBIDDEN, SpreadsheetUtil.FORBIDDEN_LICENSE_COLOR);
        assertNoColor(file, UNCLASSIFIED);
    }

    @Test
    void writesNoColourAtAllWithoutAnyConfiguredLicense() throws IOException {
        final File file = write("none", SpreadsheetFormatting.NONE);

        assertNoColor(file, FORBIDDEN);
        assertNoColor(file, UNCLASSIFIED);
    }

    private static SpreadsheetFormatting formatting(boolean highlightUnknownLicenses) {
        return new SpreadsheetFormatting(
                new LicenseClassifier(
                        Collections.singletonList(FORBIDDEN),
                        Collections.singletonList(PROBLEMATIC),
                        Collections.singletonList(OK)),
                highlightUnknownLicenses,
                false);
    }

    private File write(String name, SpreadsheetFormatting formatting) {
        final List<ProjectLicenseInfo> dependencies = Arrays.asList(
                dependency("forbidden", FORBIDDEN),
                dependency("problematic", PROBLEMATIC),
                dependency("ok", OK),
                dependency("unclassified", UNCLASSIFIED));
        final File file = new File(tempDir, "licenses-" + name + ".xlsx");
        ExcelFileWriter.write(dependencies, file, formatting);
        return file;
    }

    private static ProjectLicenseInfo dependency(String artifactId, String licenseName) {
        final ProjectLicenseInfo info = new ProjectLicenseInfo("org.test", artifactId, "1.0", (ExtendedInfo) null);
        info.addLicense(new ProjectLicense(licenseName, null, null, null, null));
        return info;
    }

    private static void assertColor(File file, String licenseName, int[] expected) throws IOException {
        final XSSFColor color = fontColorOf(file, licenseName);
        assertNotNull(color, licenseName + " is not highlighted");
        assertArrayEquals(bytes(expected), color.getRGB(), licenseName + " has the wrong colour");
    }

    /**
     * A cell that was never styled still reports a font colour, so this checks that it is not one of the four the
     * highlighting uses rather than that there is none.
     */
    private static void assertNoColor(File file, String licenseName) throws IOException {
        final byte[] rgb = rgbOf(fontColorOf(file, licenseName));
        for (int[] highlightColor : new int[][] {
            SpreadsheetUtil.FORBIDDEN_LICENSE_COLOR,
            SpreadsheetUtil.PROBLEMATIC_LICENSE_COLOR,
            SpreadsheetUtil.OK_LICENSE_COLOR,
            SpreadsheetUtil.UNKNOWN_LICENSE_COLOR
        }) {
            assertFalse(
                    Arrays.equals(bytes(highlightColor), rgb),
                    licenseName + " is highlighted although it should not be");
        }
    }

    private static byte[] rgbOf(XSSFColor color) {
        return color != null ? color.getRGB() : null;
    }

    private static byte[] bytes(int[] color) {
        return new byte[] {(byte) color[0], (byte) color[1], (byte) color[2]};
    }

    private static XSSFColor fontColorOf(File file, String licenseName) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getAbsolutePath())) {
            final Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING && licenseName.equals(cell.getStringCellValue())) {
                        return ((XSSFCellStyle) cell.getCellStyle()).getFont().getXSSFColor();
                    }
                }
            }
        }
        throw new AssertionError("No cell holds " + licenseName);
    }
}
