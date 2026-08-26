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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.mojo.license.download.LicenseClassifier;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.type.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The Calc writer only exists on Java 11 and later; on Java 8 the class on the classpath is a stub. */
@EnabledForJreRange(min = JRE.JAVA_11)
class CalcFileWriterTest {

    private static final String FORBIDDEN = "GPL 3.0";
    private static final String OK = "Apache License, Version 2.0";
    private static final String UNCLASSIFIED = "Some House License";

    @TempDir
    File tempDir;

    @Test
    void writesEachLicenseInTheColourOfItsCategory() throws Exception {
        final File file = write("all", formatting(true));

        assertColor(file, FORBIDDEN, SpreadsheetUtil.FORBIDDEN_LICENSE_COLOR);
        assertColor(file, OK, SpreadsheetUtil.OK_LICENSE_COLOR);
        assertColor(file, UNCLASSIFIED, SpreadsheetUtil.UNKNOWN_LICENSE_COLOR);
    }

    @Test
    void leavesUnclassifiedLicensesAloneUnlessAsked() throws Exception {
        final File file = write("classified-only", formatting(false));

        assertColor(file, FORBIDDEN, SpreadsheetUtil.FORBIDDEN_LICENSE_COLOR);
        assertNull(colorOf(file, UNCLASSIFIED), UNCLASSIFIED + " is highlighted although it should not be");
    }

    private static SpreadsheetFormatting formatting(boolean highlightUnknownLicenses) {
        return new SpreadsheetFormatting(
                new LicenseClassifier(Collections.singletonList(FORBIDDEN), null, Collections.singletonList(OK)),
                highlightUnknownLicenses,
                false,
                false);
    }

    private File write(String name, SpreadsheetFormatting formatting) {
        final List<ProjectLicenseInfo> dependencies = Arrays.asList(
                dependency("forbidden", FORBIDDEN), dependency("ok", OK), dependency("unclassified", UNCLASSIFIED));
        final File file = new File(tempDir, "licenses-" + name + ".ods");
        CalcFileWriter.write(dependencies, file, formatting);
        return file;
    }

    private static ProjectLicenseInfo dependency(String artifactId, String licenseName) {
        final ProjectLicenseInfo info = new ProjectLicenseInfo("org.test", artifactId, "1.0", (ExtendedInfo) null);
        info.addLicense(new ProjectLicense(licenseName, null, null, null, null));
        return info;
    }

    private static void assertColor(File file, String licenseName, int[] expected) throws Exception {
        final String color = colorOf(file, licenseName);
        assertNotNull(color, licenseName + " is not highlighted");
        assertEquals(hex(expected), color, licenseName + " has the wrong colour");
    }

    /**
     * The colour the cell holding the license is written in, or {@code null} if it is written in one of the plain
     * styles.
     *
     * <p>No signature in this class mentions an odfdom type on purpose: JUnit reflects over the declared methods
     * before the Java 11 condition disables the tests, and odfdom does not load on Java 8 at all.
     */
    private static String colorOf(File file, String licenseName) throws Exception {
        try (OdfSpreadsheetDocument document = OdfSpreadsheetDocument.loadDocument(file)) {
            final OdfTable table = document.getTableList().get(0);
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    final OdfTableCell cell = table.getCellByPosition(column, row);
                    if (licenseName.equals(cell.getStringValue())) {
                        final String styleName = cell.getOdfElement().getStyleName();
                        if (styleName == null || !styleName.startsWith("licenseCellStyle-")) {
                            return null;
                        }
                        final OdfStyle style =
                                document.getDocumentStyles().getStyle(styleName, OdfStyleFamily.TableCell);
                        return style != null ? style.getProperty(StyleTextPropertiesElement.Color) : null;
                    }
                }
            }
        }
        throw new AssertionError("No cell holds " + licenseName);
    }

    private static String hex(int[] color) {
        return new Color(color[0], color[1], color[2]).toString();
    }
}
