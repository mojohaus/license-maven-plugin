package org.codehaus.mojo.license.extended.spreadsheet;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 Jan-Hendrik Diederich
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

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.codehaus.mojo.license.extended.InfoFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes project license infos into Excel file.
 */
public class ExcelFileWriter {
    private static final Logger LOG = LoggerFactory.getLogger(ExcelFileWriter.class);

    // Columns values for Maven data, including separator gaps for data grouping.
    private static final int GENERAL_START_COLUMN = 0,
            GENERAL_COLUMNS = 1,
            GENERAL_END_COLUMN = GENERAL_START_COLUMN + GENERAL_COLUMNS;
    private static final int PLUGIN_ID_START_COLUMN = GENERAL_END_COLUMN + 1,
            PLUGIN_ID_COLUMNS = 3,
            PLUGIN_ID_END_COLUMN = PLUGIN_ID_START_COLUMN + PLUGIN_ID_COLUMNS;
    // "Start" column are the actual start columns, they are inclusive.
    // "End" columns point just one column behind the last one, it's the exclusive column index.
    private static final int LICENSES_START_COLUMN = PLUGIN_ID_END_COLUMN + 1,
            LICENSES_COLUMNS = 5,
            LICENSES_END_COLUMN = LICENSES_START_COLUMN + LICENSES_COLUMNS;
    private static final int DEVELOPERS_START_COLUMN = LICENSES_END_COLUMN + 1,
            DEVELOPERS_COLUMNS = 7,
            DEVELOPERS_END_COLUMN = DEVELOPERS_START_COLUMN + DEVELOPERS_COLUMNS;
    private static final int MISC_START_COLUMN = DEVELOPERS_END_COLUMN + 1,
            MISC_COLUMNS = 4,
            MISC_END_COLUMN = MISC_START_COLUMN + MISC_COLUMNS;
    private static final int
            MAVEN_DATA_COLUMNS =
                    GENERAL_COLUMNS + PLUGIN_ID_COLUMNS + LICENSES_COLUMNS + DEVELOPERS_COLUMNS + MISC_COLUMNS,
            MAVEN_COLUMN_GROUPING_GAPS = 4;
    private static final int MAVEN_START_COLUMN = 0,
            MAVEN_COLUMNS = MAVEN_DATA_COLUMNS + MAVEN_COLUMN_GROUPING_GAPS,
            MAVEN_END_COLUMN = MAVEN_START_COLUMN + MAVEN_COLUMNS;

    // Columns values for JAR data, including separator gaps for data grouping.
    private static final int INFO_FILES_GAPS = 2, MANIFEST_GAPS = 1;
    private static final int MANIFEST_START_COLUMN = MAVEN_COLUMNS + 1,
            MANIFEST_COLUMNS = 3,
            MANIFEST_END_COLUMN = MANIFEST_START_COLUMN + MANIFEST_COLUMNS;
    private static final int INFO_NOTICES_START_COLUMN = MANIFEST_END_COLUMN + 1,
            INFO_NOTICES_COLUMNS = 3,
            INFO_NOTICES_END_COLUMN = INFO_NOTICES_START_COLUMN + INFO_NOTICES_COLUMNS;
    private static final int INFO_LICENSES_START_COLUMN = INFO_NOTICES_END_COLUMN + 1,
            INFO_LICENSES_COLUMNS = 3,
            INFO_LICENSES_END_COLUMN = INFO_LICENSES_START_COLUMN + INFO_LICENSES_COLUMNS;
    private static final int INFO_SPDX_START_COLUMN = INFO_LICENSES_END_COLUMN + 1,
            INFO_SPDX_COLUMNS = 3,
            INFO_SPDX_END_COLUMN = INFO_SPDX_START_COLUMN + INFO_SPDX_COLUMNS;
    private static final int EXTENDED_INFO_START_COLUMN = MAVEN_END_COLUMN + 1,
            EXTENDED_INFO_COLUMNS =
                    MANIFEST_COLUMNS
                            + INFO_NOTICES_COLUMNS
                            + INFO_LICENSES_COLUMNS
                            + INFO_SPDX_COLUMNS
                            + INFO_FILES_GAPS
                            + MANIFEST_GAPS,
            EXTENDED_INFO_END_COLUMN = EXTENDED_INFO_START_COLUMN + EXTENDED_INFO_COLUMNS;

    // Width of gap columns
    private static final int EXCEL_WIDTH_SCALE = 256;
    private static final int GAP_WIDTH = 3 * EXCEL_WIDTH_SCALE;
    private static final BorderStyle HEADER_CELLS_BORDER_STYLE = BorderStyle.MEDIUM;
    private static final int TIMEZONE_WIDTH = " Timezone ".length() * EXCEL_WIDTH_SCALE,
            INCEPTION_YEAR_WIDTH = " Inception Year ".length() * EXCEL_WIDTH_SCALE;
    /**
     * Color must be dark enough for low contrast monitors.
     * <br>If you get a compile error here, make sure you're using Java 8, not higher.
     */
    private static final Color ALTERNATING_ROWS_COLOR = new Color(220, 220, 220);

    private static final String COPYRIGHT_JOIN_SEPARATOR = "§";

    /**
     * Writes list of projects into excel file.
     *
     * @param projectLicenseInfos     Project license infos to write.
     * @param licensesExcelOutputFile Excel output file in latest format (OOXML).
     */
    public static void write(List<ProjectLicenseInfo> projectLicenseInfos, final File licensesExcelOutputFile) {
        if (CollectionUtils.isEmpty(projectLicenseInfos)) {
            LOG.debug("Nothing to write to excel, no project data.");
            return;
        }

        final XSSFWorkbook wb = new XSSFWorkbook();
        final Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName("License information"));

        final IndexedColorMap colorMap = wb.getStylesSource().getIndexedColors();
        final XSSFColor alternatingRowsColor = new XSSFColor(ALTERNATING_ROWS_COLOR, colorMap);

        createHeader(projectLicenseInfos, wb, sheet);

        writeData(projectLicenseInfos, wb, sheet, alternatingRowsColor);

        try (OutputStream fileOut = new FileOutputStream(licensesExcelOutputFile)) {
            wb.write(fileOut);
        } catch (IOException e) {
            LOG.error("Error on storing Excel file with license and other information", e);
        }
    }

    private static void createHeader(List<ProjectLicenseInfo> projectLicenseInfos, Workbook wb, Sheet sheet) {
        boolean hasExtendedInfo = false;
        for (ProjectLicenseInfo projectLicenseInfo : projectLicenseInfos) {
            if (projectLicenseInfo.getExtendedInfo() != null) {
                hasExtendedInfo = true;
                break;
            }
        }

        // Create header style
        CellStyle headerCellStyle = wb.createCellStyle();
        headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
        setBorderStyle(headerCellStyle, HEADER_CELLS_BORDER_STYLE);

        // Create 1st header row. The Maven/JAR header row
        Row mavenJarRow = sheet.createRow(0);

        // Create Maven header cell
        createMergedCellsInRow(
                sheet, MAVEN_START_COLUMN, MAVEN_END_COLUMN, headerCellStyle, mavenJarRow, "Maven information", 0);

        if (hasExtendedInfo) {
            // Create JAR header cell
            createMergedCellsInRow(
                    sheet,
                    EXTENDED_INFO_START_COLUMN,
                    EXTENDED_INFO_END_COLUMN,
                    headerCellStyle,
                    mavenJarRow,
                    "JAR Content",
                    0);
        }

        // Create 2nd header row
        Row secondHeaderRow = sheet.createRow(1);

        // Create Maven "General" header
        createMergedCellsInRow(
                sheet, GENERAL_START_COLUMN, GENERAL_END_COLUMN, headerCellStyle, secondHeaderRow, "General", 1);

        // Create Maven "Plugin ID" header
        createMergedCellsInRow(
                sheet, PLUGIN_ID_START_COLUMN, PLUGIN_ID_END_COLUMN, headerCellStyle, secondHeaderRow, "Plugin ID", 1);

        // Gap "General" <-> "Plugin ID".
        sheet.setColumnWidth(GENERAL_END_COLUMN, GAP_WIDTH);

        // Create Maven "Licenses" header
        createMergedCellsInRow(
                sheet, LICENSES_START_COLUMN, LICENSES_END_COLUMN, headerCellStyle, secondHeaderRow, "Licenses", 1);

        // Gap "Plugin ID" <-> "Licenses".
        sheet.setColumnWidth(PLUGIN_ID_END_COLUMN, GAP_WIDTH);

        // Create Maven "Developers" header
        createMergedCellsInRow(
                sheet,
                DEVELOPERS_START_COLUMN,
                DEVELOPERS_END_COLUMN,
                headerCellStyle,
                secondHeaderRow,
                "Developers",
                1);

        // Gap "Licenses" <-> "Developers".
        sheet.setColumnWidth(LICENSES_END_COLUMN, GAP_WIDTH);

        // Create Maven "Miscellaneous" header
        createMergedCellsInRow(
                sheet, MISC_START_COLUMN, MISC_END_COLUMN, headerCellStyle, secondHeaderRow, "Miscellaneous", 1);

        // Gap "Developers" <-> "Miscellaneous".
        sheet.setColumnWidth(DEVELOPERS_END_COLUMN, GAP_WIDTH);

        if (hasExtendedInfo) {
            createMergedCellsInRow(
                    sheet,
                    MANIFEST_START_COLUMN,
                    MANIFEST_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "MANIFEST.MF",
                    1);

            // Gap "Miscellaneous" <-> "MANIFEST.MF".
            sheet.setColumnWidth(DEVELOPERS_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    sheet,
                    INFO_NOTICES_START_COLUMN,
                    INFO_NOTICES_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "Notices text files",
                    1);

            // Gap "MANIFEST.MF" <-> "Notice text files".
            sheet.setColumnWidth(MANIFEST_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    sheet,
                    INFO_LICENSES_START_COLUMN,
                    INFO_LICENSES_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "License text files",
                    1);

            // Gap "Notice text files" <-> "License text files".
            sheet.setColumnWidth(INFO_NOTICES_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    sheet,
                    INFO_SPDX_START_COLUMN,
                    INFO_SPDX_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "SPDX license id matched",
                    1);

            // Gap "License text files" <-> "SPDX license matches".
            sheet.setColumnWidth(INFO_LICENSES_END_COLUMN, GAP_WIDTH);
        }
        //        sheet.setColumnGroupCollapsed();

        // Create 3rd header row
        Row thirdHeaderRow = sheet.createRow(2);

        // General
        createCellsInRow(thirdHeaderRow, GENERAL_START_COLUMN, headerCellStyle, "Name");
        // Plugin ID
        createCellsInRow(thirdHeaderRow, PLUGIN_ID_START_COLUMN, headerCellStyle, "Group ID", "Artifact ID", "Version");
        // Licenses
        createCellsInRow(
                thirdHeaderRow,
                LICENSES_START_COLUMN,
                headerCellStyle,
                "Name",
                "URL",
                "Distribution",
                "Comments",
                "File");
        // Developers
        createCellsInRow(
                thirdHeaderRow,
                DEVELOPERS_START_COLUMN,
                headerCellStyle,
                "Id",
                "Email",
                "Name",
                "Organization",
                "Organization URL",
                "URL",
                "Timezone");
        // Miscellaneous
        createCellsInRow(
                thirdHeaderRow, MISC_START_COLUMN, headerCellStyle, "Inception Year", "Organization", "SCM", "URL");

        int headerLineCount = 3;

        if (hasExtendedInfo) {
            // MANIFEST.MF
            createCellsInRow(
                    thirdHeaderRow,
                    MANIFEST_START_COLUMN,
                    headerCellStyle,
                    "Bundle license",
                    "Bundle vendor",
                    "Implementation vendor");
            // 3 InfoFile groups: Notices, Licenses and SPDX-Licenses.
            createInfoFileCellsInRow(
                    thirdHeaderRow,
                    headerCellStyle,
                    INFO_NOTICES_START_COLUMN,
                    INFO_LICENSES_START_COLUMN,
                    INFO_SPDX_START_COLUMN);

            sheet.createFreezePane(EXTENDED_INFO_END_COLUMN, headerLineCount);
        } else {
            sheet.createFreezePane(MAVEN_END_COLUMN, headerLineCount);
        }

        sheet.createFreezePane(GENERAL_END_COLUMN, headerLineCount);
    }

    // TODO: Clean this method up. Too many parameters, too complicated parameters/DTO pattern. But keep it still
    // threadsafe.
    private static void writeData(
            List<ProjectLicenseInfo> projectLicenseInfos,
            XSSFWorkbook wb,
            Sheet sheet,
            XSSFColor alternatingRowsColor) {
        final int firstRowIndex = 3;
        int currentRowIndex = firstRowIndex;
        final Map<Integer, Row> rowMap = new HashMap<>();
        boolean hasExtendedInfo = false;

        final CellStyle hyperlinkStyleNormal = createHyperlinkStyle(wb, null);
        final CellStyle hyperlinkStyleGray = createHyperlinkStyle(wb, alternatingRowsColor);

        boolean grayBackground = false;
        XSSFCellStyle styleGray = wb.createCellStyle();
        styleGray.setFillForegroundColor(alternatingRowsColor);
        styleGray.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (ProjectLicenseInfo projectInfo : projectLicenseInfos) {
            final CellStyle cellStyle, hyperlinkStyle;
            if (grayBackground) {
                cellStyle = styleGray;
                hyperlinkStyle = hyperlinkStyleGray;
            } else {
                cellStyle = null;
                hyperlinkStyle = hyperlinkStyleNormal;
            }
            grayBackground = !grayBackground;

            int extraRows = 0;
            Row currentRow = sheet.createRow(currentRowIndex);
            rowMap.put(currentRowIndex, currentRow);
            // Plugin ID
            createDataCellsInRow(
                    currentRow,
                    PLUGIN_ID_START_COLUMN,
                    cellStyle,
                    projectInfo.getGroupId(),
                    projectInfo.getArtifactId(),
                    projectInfo.getVersion());
            // Licenses
            final CellListParameter cellListParameter = new CellListParameter(sheet, rowMap, cellStyle);
            CurrentRowData currentRowData = new CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
            extraRows = addList(
                    cellListParameter,
                    currentRowData,
                    LICENSES_START_COLUMN,
                    LICENSES_COLUMNS,
                    projectInfo.getLicenses(),
                    (Row licenseRow, ProjectLicense license) -> {
                        Cell[] licenses = createDataCellsInRow(
                                licenseRow,
                                LICENSES_START_COLUMN,
                                cellStyle,
                                license.getName(),
                                license.getUrl(),
                                license.getDistribution(),
                                license.getComments(),
                                license.getFile());
                        addHyperlinkIfExists(wb, licenses[1], hyperlinkStyle, HyperlinkType.URL);
                    });

            final ExtendedInfo extendedInfo = projectInfo.getExtendedInfo();
            if (extendedInfo != null) {
                hasExtendedInfo = true;
                // General
                createDataCellsInRow(currentRow, GENERAL_START_COLUMN, cellStyle, extendedInfo.getName());
                // Developers
                currentRowData = new CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                extraRows = addList(
                        cellListParameter,
                        currentRowData,
                        DEVELOPERS_START_COLUMN,
                        DEVELOPERS_COLUMNS,
                        extendedInfo.getDevelopers(),
                        (Row developerRow, Developer developer) -> {
                            Cell[] licenses = createDataCellsInRow(
                                    developerRow,
                                    DEVELOPERS_START_COLUMN,
                                    cellStyle,
                                    developer.getId(),
                                    developer.getEmail(),
                                    developer.getName(),
                                    developer.getOrganization(),
                                    developer.getOrganizationUrl(),
                                    developer.getUrl(),
                                    developer.getTimezone());
                            addHyperlinkIfExists(wb, licenses[1], hyperlinkStyle, HyperlinkType.EMAIL);
                            addHyperlinkIfExists(wb, licenses[4], hyperlinkStyle, HyperlinkType.URL);
                            addHyperlinkIfExists(wb, licenses[5], hyperlinkStyle, HyperlinkType.URL);
                        });
                // Miscellaneous
                Cell[] miscCells = createDataCellsInRow(
                        currentRow,
                        MISC_START_COLUMN,
                        cellStyle,
                        extendedInfo.getInceptionYear(),
                        Optional.ofNullable(extendedInfo.getOrganization())
                                .map(Organization::getName)
                                .orElse(null),
                        Optional.ofNullable(extendedInfo.getScm())
                                .map(Scm::getUrl)
                                .orElse(null),
                        extendedInfo.getUrl());
                addHyperlinkIfExists(wb, miscCells[2], hyperlinkStyle, HyperlinkType.URL);
                addHyperlinkIfExists(wb, miscCells[3], hyperlinkStyle, HyperlinkType.URL);

                // MANIFEST.MF
                createDataCellsInRow(
                        currentRow,
                        MANIFEST_START_COLUMN,
                        cellStyle,
                        extendedInfo.getBundleLicense(),
                        extendedInfo.getBundleVendor(),
                        extendedInfo.getImplementationVendor());

                // Info files
                if (!CollectionUtils.isEmpty(extendedInfo.getInfoFiles())) {
                    // Sort all info files by type into 3 different lists, each list for each of the 3 types.
                    List<InfoFile> notices = new ArrayList<>();
                    List<InfoFile> licenses = new ArrayList<>();
                    List<InfoFile> spdxs = new ArrayList<>();
                    extendedInfo.getInfoFiles().forEach(infoFile -> {
                        switch (infoFile.getType()) {
                            case LICENSE:
                                licenses.add(infoFile);
                                break;
                            case NOTICE:
                                notices.add(infoFile);
                                break;
                            case SPDX_LICENSE:
                                spdxs.add(infoFile);
                                break;
                            default:
                                break;
                        }
                    });
                    // InfoFile notices text file
                    currentRowData = new CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                    extraRows = addInfoFileList(
                            cellListParameter,
                            currentRowData,
                            INFO_NOTICES_START_COLUMN,
                            INFO_NOTICES_COLUMNS,
                            notices);
                    // InfoFile licenses text file
                    currentRowData = new CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                    extraRows = addInfoFileList(
                            cellListParameter,
                            currentRowData,
                            INFO_LICENSES_START_COLUMN,
                            INFO_LICENSES_COLUMNS,
                            licenses);
                    // InfoFile spdx licenses text file
                    currentRowData = new CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                    extraRows = addInfoFileList(
                            cellListParameter, currentRowData, INFO_SPDX_START_COLUMN, INFO_SPDX_COLUMNS, spdxs);
                } else if (cellListParameter.cellStyle != null) {
                    setStyleOnEmptyCells(
                            cellListParameter, currentRowData, INFO_NOTICES_START_COLUMN, INFO_NOTICES_COLUMNS);
                    setStyleOnEmptyCells(
                            cellListParameter, currentRowData, INFO_LICENSES_START_COLUMN, INFO_LICENSES_COLUMNS);
                    setStyleOnEmptyCells(cellListParameter, currentRowData, INFO_SPDX_START_COLUMN, INFO_SPDX_COLUMNS);
                }
            }
            currentRowIndex += extraRows + 1;
        }

        autosizeColumns(sheet, hasExtendedInfo);
    }

    private static CellStyle createHyperlinkStyle(XSSFWorkbook wb, XSSFColor backgroundColor) {
        Font hyperlinkFont = wb.createFont();
        hyperlinkFont.setUnderline(XSSFFont.U_SINGLE);
        hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
        XSSFCellStyle hyperlinkStyle = wb.createCellStyle();
        if (backgroundColor != null) {
            hyperlinkStyle.setFillForegroundColor(backgroundColor);
            hyperlinkStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        hyperlinkStyle.setFont(hyperlinkFont);
        return hyperlinkStyle;
    }

    private static void autosizeColumns(Sheet sheet, boolean hasExtendedInfo) {
        autosizeColumns(
                sheet,
                new ImmutablePair<>(GENERAL_START_COLUMN, GENERAL_END_COLUMN),
                new ImmutablePair<>(PLUGIN_ID_START_COLUMN, PLUGIN_ID_END_COLUMN),
                new ImmutablePair<>(LICENSES_START_COLUMN, LICENSES_END_COLUMN),
                new ImmutablePair<>(DEVELOPERS_START_COLUMN, DEVELOPERS_END_COLUMN - 1),
                new ImmutablePair<>(MISC_START_COLUMN + 1, MISC_END_COLUMN));
        // The column header widths are most likely wider than the actual cells content.
        sheet.setColumnWidth(DEVELOPERS_END_COLUMN - 1, TIMEZONE_WIDTH);
        sheet.setColumnWidth(MISC_START_COLUMN, INCEPTION_YEAR_WIDTH);
        if (hasExtendedInfo) {
            autosizeColumns(
                    sheet,
                    new ImmutablePair<>(MANIFEST_START_COLUMN, MANIFEST_END_COLUMN),
                    new ImmutablePair<>(INFO_NOTICES_START_COLUMN + 2, INFO_NOTICES_END_COLUMN),
                    new ImmutablePair<>(INFO_LICENSES_START_COLUMN + 2, INFO_LICENSES_END_COLUMN),
                    new ImmutablePair<>(INFO_SPDX_START_COLUMN + 2, INFO_SPDX_END_COLUMN));
        }
    }

    @SafeVarargs
    private static void autosizeColumns(Sheet sheet, Pair<Integer, Integer>... ranges) {
        for (Pair<Integer, Integer> range : ranges) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

    private static int addInfoFileList(
            CellListParameter cellListParameter,
            CurrentRowData currentRowData,
            int startColumn,
            int columnsToFill,
            List<InfoFile> infoFiles) {
        return addList(
                cellListParameter,
                currentRowData,
                startColumn,
                columnsToFill,
                infoFiles,
                (Row infoFileRow, InfoFile infoFile) -> {
                    final String copyrightLines = Optional.ofNullable(infoFile.getExtractedCopyrightLines())
                            .map(strings -> String.join(COPYRIGHT_JOIN_SEPARATOR, strings))
                            .orElse(null);
                    createDataCellsInRow(
                            infoFileRow,
                            startColumn,
                            cellListParameter.getCellStyle(),
                            infoFile.getContent(),
                            copyrightLines,
                            infoFile.getFileName());
                });
    }

    private static <T> int addList(
            CellListParameter cellListParameter,
            CurrentRowData currentRowData,
            int startColumn,
            int columnsToFill,
            List<T> list,
            BiConsumer<Row, T> biConsumer) {
        if (!CollectionUtils.isEmpty(list)) {
            for (int i = 0; i < list.size(); i++) {
                T type = list.get(i);
                Integer index = currentRowData.getCurrentRowIndex() + i;
                Row row = cellListParameter.getRows().get(index);
                if (row == null) {
                    row = cellListParameter.getSheet().createRow(index);
                    cellListParameter.getRows().put(index, row);
                    if (cellListParameter.getCellStyle() != null) {
                        // Style all empty left cells, in the columns left from this
                        createAndStyleCells(
                                row,
                                cellListParameter.getCellStyle(),
                                new ImmutablePair<>(GENERAL_START_COLUMN, GENERAL_END_COLUMN),
                                new ImmutablePair<>(PLUGIN_ID_START_COLUMN, PLUGIN_ID_END_COLUMN),
                                new ImmutablePair<>(LICENSES_START_COLUMN, LICENSES_END_COLUMN));
                        if (currentRowData.isHasExtendedInfo()) {
                            createAndStyleCells(
                                    row,
                                    cellListParameter.getCellStyle(),
                                    new ImmutablePair<>(DEVELOPERS_START_COLUMN, DEVELOPERS_END_COLUMN),
                                    new ImmutablePair<>(MISC_START_COLUMN, MISC_END_COLUMN),
                                    // JAR
                                    new ImmutablePair<>(MANIFEST_START_COLUMN, MANIFEST_END_COLUMN),
                                    new ImmutablePair<>(INFO_LICENSES_START_COLUMN, INFO_LICENSES_END_COLUMN),
                                    new ImmutablePair<>(INFO_NOTICES_START_COLUMN, INFO_NOTICES_END_COLUMN),
                                    new ImmutablePair<>(INFO_SPDX_START_COLUMN, INFO_SPDX_END_COLUMN));
                        }
                    }
                    currentRowData.setExtraRows(currentRowData.getExtraRows() + 1);
                }
                biConsumer.accept(row, type);
            }
        } else if (cellListParameter.cellStyle != null) {
            setStyleOnEmptyCells(cellListParameter, currentRowData, startColumn, columnsToFill);
        }
        return currentRowData.getExtraRows();
    }

    /**
     * If no cells are set, color at least the background,
     * to color concatenated blocks with the same background color.
     *
     * @param cellListParameter Passes data about sheet, row, cell style.
     * @param currentRowData Passes data about the current indices for rows and columns.
     * @param startColumn Column where to start setting the style.
     * @param columnsToFill How many columns to set the style on, starting from 'startColumn'.
     */
    private static void setStyleOnEmptyCells(
            CellListParameter cellListParameter, CurrentRowData currentRowData, int startColumn, int columnsToFill) {
        Row row = cellListParameter.getRows().get(currentRowData.getCurrentRowIndex());
        for (int i = 0; i < columnsToFill; i++) {
            Cell cell = row.createCell(startColumn + i, CellType.STRING);
            cell.setCellStyle(cellListParameter.getCellStyle());
        }
    }

    @SafeVarargs
    private static void createAndStyleCells(Row row, CellStyle cellStyle, Pair<Integer, Integer>... ranges) {
        for (Pair<Integer, Integer> range : ranges) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                Cell cell = row.createCell(i, CellType.STRING);
                cell.setCellStyle(cellStyle);
            }
        }
    }

    private static void addHyperlinkIfExists(
            Workbook workbook, Cell cell, CellStyle hyperlinkStyle, HyperlinkType hyperlinkType) {
        if (!StringUtils.isEmpty(cell.getStringCellValue())) {
            Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(hyperlinkType);
            try {
                hyperlink.setAddress(cell.getStringCellValue());
                cell.setHyperlink(hyperlink);
                cell.setCellStyle(hyperlinkStyle);
            } catch (IllegalArgumentException e) {
                LOG.debug(
                        "Can't set Hyperlink for cell value " + cell.getStringCellValue() + " it gets rejected as URI",
                        e);
            }
        }
    }

    private static Cell[] createDataCellsInRow(Row row, int startColumn, CellStyle cellStyle, String... names) {
        Cell[] result = new Cell[names.length];
        for (int i = 0; i < names.length; i++) {
            Cell cell = row.createCell(startColumn + i, CellType.STRING);
            if (cellStyle != null) {
                cell.setCellStyle(cellStyle);
            }
            if (!StringUtils.isEmpty(names[i])) {
                final String value;
                final int maxCellStringLength = Short.MAX_VALUE;
                if (names[i].length() > maxCellStringLength) {
                    value = names[i].substring(0, maxCellStringLength - 3) + "...";
                } else {
                    value = names[i];
                }
                cell.setCellValue(value);
            }
            result[i] = cell;
        }
        return result;
    }

    /**
     * Create cells for InfoFile content.
     *
     * @param row            The row to insert cells into.
     * @param cellStyle      The cell style of the created cell.
     * @param startPositions The start position of the 3 columns for an InfoFile.
     */
    private static void createInfoFileCellsInRow(Row row, CellStyle cellStyle, int... startPositions) {
        for (int startPosition : startPositions) {
            createCellsInRow(row, startPosition, cellStyle, "Content", "Extracted copyright lines", "File");
        }
    }

    private static void createCellsInRow(Row row, int startColumn, CellStyle cellStyle, String... names) {
        for (int i = 0; i < names.length; i++) {
            Cell cell = row.createCell(startColumn + i, CellType.STRING);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(names[i]);
        }
    }

    private static void createMergedCellsInRow(
            Sheet sheet, int startColumn, int endColumn, CellStyle cellStyle, Row row, String cellValue, int rowIndex) {
        Cell cell = createCellsInRow(startColumn, endColumn, row);
        final boolean merge = endColumn - 1 > startColumn;
        CellRangeAddress mergeAddress = null;
        if (merge) {
            mergeAddress = new CellRangeAddress(rowIndex, rowIndex, startColumn, endColumn - 1);
            sheet.addMergedRegion(mergeAddress);
        }
        // Set value and style only after merge
        cell.setCellValue(cellValue);
        cell.setCellStyle(cellStyle);
        if (merge) {
            setBorderAroundRegion(sheet, mergeAddress, HEADER_CELLS_BORDER_STYLE);
            sheet.groupColumn(startColumn, endColumn - 1);
        }
    }

    private static void setBorderAroundRegion(
            Sheet sheet, CellRangeAddress licensesHeaderAddress, BorderStyle borderStyle) {
        RegionUtil.setBorderLeft(borderStyle, licensesHeaderAddress, sheet);
        RegionUtil.setBorderTop(borderStyle, licensesHeaderAddress, sheet);
        RegionUtil.setBorderRight(borderStyle, licensesHeaderAddress, sheet);
        RegionUtil.setBorderBottom(borderStyle, licensesHeaderAddress, sheet);
    }

    private static Cell createCellsInRow(int startColumn, int exclusiveEndColumn, Row inRow) {
        Cell firstCell = null;
        for (int i = startColumn; i < exclusiveEndColumn; i++) {
            Cell cell = inRow.createCell(i);
            if (i == startColumn) {
                firstCell = cell;
            }
        }
        return firstCell;
    }

    private static void setBorderStyle(CellStyle cellStyle, BorderStyle borderStyle) {
        cellStyle.setBorderLeft(borderStyle);
        cellStyle.setBorderTop(borderStyle);
        cellStyle.setBorderRight(borderStyle);
        cellStyle.setBorderBottom(borderStyle);
    }

    /**
     * Parameters for cells which apply to all cells in each loop iteration.
     */
    private static class CellListParameter {
        private final Sheet sheet;
        private final Map<Integer, Row> rows;
        private final CellStyle cellStyle;

        private CellListParameter(Sheet sheet, Map<Integer, Row> rows, CellStyle cellStyle) {
            this.sheet = sheet;
            this.rows = rows;
            this.cellStyle = cellStyle;
        }

        Sheet getSheet() {
            return sheet;
        }

        Map<Integer, Row> getRows() {
            return rows;
        }

        CellStyle getCellStyle() {
            return cellStyle;
        }
    }

    /**
     * Parameters which may change constantly.
     */
    private static class CurrentRowData {
        private final int currentRowIndex;
        private int extraRows;
        private final boolean hasExtendedInfo;

        CurrentRowData(int currentRowIndex, int extraRows, boolean hasExtendedInfo) {
            this.currentRowIndex = currentRowIndex;
            this.extraRows = extraRows;
            this.hasExtendedInfo = hasExtendedInfo;
        }

        int getCurrentRowIndex() {
            return currentRowIndex;
        }

        int getExtraRows() {
            return extraRows;
        }

        void setExtraRows(int extraRows) {
            this.extraRows = extraRows;
        }

        boolean isHasExtendedInfo() {
            return hasExtendedInfo;
        }
    }
}
