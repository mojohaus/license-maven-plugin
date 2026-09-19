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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.codehaus.mojo.license.download.LicenseClassifier;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableColumnGroupElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.pkg.OdfElement;
import org.odftoolkit.odfdom.type.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import static org.codehaus.mojo.license.extended.spreadsheet.SpreadsheetUtil.GENERAL_START_COLUMN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Calc writer only exists on Java 11 and later; on Java 8 the class on the classpath is a stub.
 */
@EnabledForJreRange(min = JRE.JAVA_11)
class CalcFileWriterTest {
    /**
     * Run the following to see the debug output of the CalcFileWriter:<pre>
     * mvn test -Dtest=CalcFileWriterTest -Dorg.slf4j.simpleLogger.log.org.codehaus.mojo.license.extended.spreadsheet=DEBUG</pre>
     */
    private static final Logger LOG = LoggerFactory.getLogger(CalcFileWriterTest.class);

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

    static Stream<GroupingExpectation> groupingExpectations() {
        return java.util.stream.Stream.of(
                // Without extended info there should be 1 main group with 4 subgroups.
                new GroupingExpectation(
                        dependency("forbidden", FORBIDDEN),
                        List.of(
                                List.of(new RowEntry(
                                        GENERAL_START_COLUMN, SpreadsheetUtil.MAVEN_END_COLUMN, "Maven information")),
                                List.of(
                                        new RowEntry(
                                                GENERAL_START_COLUMN, SpreadsheetUtil.GENERAL_END_COLUMN, "General"),
                                        new RowEntry(
                                                SpreadsheetUtil.PLUGIN_ID_START_COLUMN,
                                                SpreadsheetUtil.PLUGIN_ID_END_COLUMN,
                                                "Plugin ID"),
                                        new RowEntry(
                                                SpreadsheetUtil.LICENSES_START_COLUMN,
                                                SpreadsheetUtil.LICENSES_END_COLUMN,
                                                "Licenses"),
                                        new RowEntry(
                                                SpreadsheetUtil.DEVELOPERS_START_COLUMN,
                                                SpreadsheetUtil.DEVELOPERS_END_COLUMN,
                                                "Developers"),
                                        new RowEntry(
                                                SpreadsheetUtil.MISC_START_COLUMN,
                                                SpreadsheetUtil.MISC_END_COLUMN,
                                                "Miscellaneous"))),
                        1,
                        4),
                // With extended info there should be 2 main groups, both with 4 subgroups.
                new GroupingExpectation(
                        dependencyWithExtendedInfo("grouped"),
                        List.of(
                                List.of(
                                        new RowEntry(0, SpreadsheetUtil.MAVEN_END_COLUMN, "Maven information"),
                                        new RowEntry(
                                                SpreadsheetUtil.EXTENDED_INFO_START_COLUMN,
                                                SpreadsheetUtil.EXTENDED_INFO_END_COLUMN,
                                                "JAR Content")),
                                List.of(
                                        new RowEntry(
                                                GENERAL_START_COLUMN, SpreadsheetUtil.GENERAL_END_COLUMN, "General"),
                                        new RowEntry(
                                                SpreadsheetUtil.PLUGIN_ID_START_COLUMN,
                                                SpreadsheetUtil.PLUGIN_ID_END_COLUMN,
                                                "Plugin ID"),
                                        new RowEntry(
                                                SpreadsheetUtil.LICENSES_START_COLUMN,
                                                SpreadsheetUtil.LICENSES_END_COLUMN,
                                                "Licenses"),
                                        new RowEntry(
                                                SpreadsheetUtil.DEVELOPERS_START_COLUMN,
                                                SpreadsheetUtil.DEVELOPERS_END_COLUMN,
                                                "Developers"),
                                        new RowEntry(
                                                SpreadsheetUtil.MISC_START_COLUMN,
                                                SpreadsheetUtil.MISC_END_COLUMN,
                                                "Miscellaneous"),
                                        new RowEntry(
                                                SpreadsheetUtil.MANIFEST_START_COLUMN,
                                                SpreadsheetUtil.MANIFEST_END_COLUMN,
                                                "MANIFEST.MF"),
                                        new RowEntry(
                                                SpreadsheetUtil.INFO_NOTICES_START_COLUMN,
                                                SpreadsheetUtil.INFO_NOTICES_END_COLUMN,
                                                "Notices text files"),
                                        new RowEntry(
                                                SpreadsheetUtil.INFO_LICENSES_START_COLUMN,
                                                SpreadsheetUtil.INFO_LICENSES_END_COLUMN,
                                                "License text files"),
                                        new RowEntry(
                                                SpreadsheetUtil.INFO_SPDX_START_COLUMN,
                                                SpreadsheetUtil.INFO_SPDX_END_COLUMN,
                                                "SPDX license id matched"))),
                        2,
                        4,
                        4));
    }

    @ParameterizedTest
    @MethodSource("groupingExpectations")
    void writesNestedColumnGroupsForMergedHeaders(GroupingExpectation groupingExpectation) throws Exception {
        final File file = write(
                "grouped-headers",
                formatting(false),
                Collections.singletonList(groupingExpectation.projectLicenseInfo));

        try (OdfSpreadsheetDocument document = OdfSpreadsheetDocument.loadDocument(file)) {
            OdfTable table = document.getTableList(false).get(0);
            final TableTableElement tableElement = table.getOdfElement();
            final List<TableTableColumnGroupElement> topLevelGroups = directColumnGroups(tableElement);

            assertEquals(groupingExpectation.topSize, topLevelGroups.size());
            for (int i = 0; i < groupingExpectation.topSize; i++) {
                assertEquals(
                        (int) groupingExpectation.subSizes.get(i),
                        directColumnGroups(topLevelGroups.get(i)).size());
            }

            // Check that the merged headers are written in the right cells.
            for (int row = 0; row < groupingExpectation.rowEntries.size(); row++) {
                final List<RowEntry> rowEntries = groupingExpectation.rowEntries.get(row);
                for (RowEntry rowEntry : rowEntries) {
                    // Assert beginning of merged cell.
                    assertTrue(
                            rowEntry.columnStart < table.getColumnCount(),
                            String.format(
                                    "Row entry at row %d has start column %d >= column count %d",
                                    row, rowEntry.columnStart, table.getColumnCount()));
                    // Assert content of merged cell.
                    final OdfTableCell cell = table.getCellByPosition(rowEntry.columnStart, row);
                    assertEquals(
                            rowEntry.cellContent,
                            cell.getStringValue(),
                            String.format("Wrong content in cell at row %d, column %d.", row, rowEntry.columnStart));
                    // Assert the cell is merged across the expected number of columns.
                    assertEquals(
                            rowEntry.columnEnd - rowEntry.columnStart,
                            columnSpanOf(cell),
                            String.format(
                                    "Cell at row %d, column %d is not merged across the expected number of columns.",
                                    row, rowEntry.columnStart));
                }
            }
        }
    }

    /**
     * The number of columns a cell is merged across, or <code>1</code> if it is not merged.
     */
    private static int columnSpanOf(OdfTableCell cell) {
        final String value = cell.getOdfElement()
                .getAttributeNS("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "number-columns-spanned");
        LOG.debug("Column span attribute: '{}'", value);
        return (value == null || value.isEmpty()) ? 1 : Integer.parseInt(value);
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
        return write(name, formatting, dependencies);
    }

    private File write(String name, SpreadsheetFormatting formatting, List<ProjectLicenseInfo> dependencies) {
        final File file = new File(tempDir, "licenses-" + name + ".ods");
        // Set breakpoint after log-output and open Calc for manual debugging.
        LOG.debug("Writing licenses to {}", file.getAbsolutePath());
        assertDoesNotThrow(
                () -> CalcFileWriter.write(dependencies, file, formatting),
                String.format("JRE version %d.", JRE.currentJre().version()));
        return file;
    }

    private static ProjectLicenseInfo dependency(String artifactId, String licenseName) {
        final ProjectLicenseInfo info = new ProjectLicenseInfo("org.test", artifactId, "1.0", (ExtendedInfo) null);
        info.addLicense(new ProjectLicense(licenseName, null, null, null, null));
        return info;
    }

    private static ProjectLicenseInfo dependencyWithExtendedInfo(String artifactId) {
        final ExtendedInfo extendedInfo = new ExtendedInfo();
        extendedInfo.setName("Dependency with extended info");
        extendedInfo.setDevelopers(Collections.emptyList());
        final ProjectLicenseInfo info = new ProjectLicenseInfo("org.test", artifactId, "1.0", extendedInfo);
        info.addLicense(new ProjectLicense(OK, null, null, null, null));
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
            final OdfTable table = document.getTableList(false).get(0);
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

    private static List<TableTableColumnGroupElement> directColumnGroups(OdfElement parent) {
        final List<TableTableColumnGroupElement> groups = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof TableTableColumnGroupElement) {
                groups.add((TableTableColumnGroupElement) child);
            }
        }
        return groups;
    }

    /**
     * Merged cell entry in a row.
     */
    private static class RowEntry {
        final int columnStart;
        final int columnEnd;
        final String cellContent;

        RowEntry(int columnStart, int columnEnd, String cellContent) {
            this.columnStart = columnStart;
            this.columnEnd = columnEnd;
            this.cellContent = cellContent;
        }
    }

    /** Expected parameters for a test of the nested column group writer. */
    private static class GroupingExpectation {
        final ProjectLicenseInfo projectLicenseInfo;
        final List<List<RowEntry>> rowEntries;
        final int topSize;
        final List<Integer> subSizes;

        GroupingExpectation(
                ProjectLicenseInfo forbidden, List<List<RowEntry>> rowEntries, int topSize, Integer... subSizes) {
            this.projectLicenseInfo = forbidden;
            this.rowEntries = rowEntries;
            this.topSize = topSize;
            this.subSizes = List.of(subSizes);
        }
    }
}
