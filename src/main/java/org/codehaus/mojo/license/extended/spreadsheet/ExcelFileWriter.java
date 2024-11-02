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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.mojo.license.AbstractDownloadLicensesMojo;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.codehaus.mojo.license.extended.InfoFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codehaus.mojo.license.extended.spreadsheet.SpreadsheetUtil.GAP_WIDTH;
import static org.codehaus.mojo.license.extended.spreadsheet.SpreadsheetUtil.getDownloadColumn;

/**
 * Writes project license infos into Excel file.
 */
public class ExcelFileWriter {
    private static final BorderStyle HEADER_CELLS_BORDER_STYLE = BorderStyle.MEDIUM;
    private static final Logger LOG = LoggerFactory.getLogger(ExcelFileWriter.class);

    private ExcelFileWriter() {}

    /**
     * In which color, according to a license, the cell is styled.
     */
    private enum LicenseColorStyle {
        /**
         * Unknown license highlight.
         */
        UNKNOWN,
        /**
         * Forbidden license highlight.
         */
        FORBIDDEN,
        /**
         * Problematic license highlight.
         */
        PROBLEMATIC,
        /**
         * OK license highlight.
         */
        OK,
        /**
         * No highlighting at all.
         */
        NONE
    }

    /**
     * Writes a list of projects into Excel file.
     *
     * @param projectLicenseInfos     Project license infos to write.
     * @param licensesExcelOutputFile Excel output file in latest format (OOXML).
     * @param dataFormatting
     */
    public static void write(List<ProjectLicenseInfo> projectLicenseInfos, final File licensesExcelOutputFile,
                             AbstractDownloadLicensesMojo.DataFormatting dataFormatting) {
        if (CollectionUtils.isEmpty(projectLicenseInfos)) {
            LOG.debug("Nothing to write to excel, no project data.");
            return;
        }
        LOG.debug("Write Microsoft Excel file {}", licensesExcelOutputFile);

        final XSSFWorkbook wb = new XSSFWorkbook();
        final Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(SpreadsheetUtil.TABLE_NAME));

        final IndexedColorMap colorMap = wb.getStylesSource().getIndexedColors();
        final XSSFColor alternatingRowsColor = new XSSFColor(
                new byte[] {
                    (byte) SpreadsheetUtil.ALTERNATING_ROWS_COLOR[0],
                    (byte) SpreadsheetUtil.ALTERNATING_ROWS_COLOR[1],
                    (byte) SpreadsheetUtil.ALTERNATING_ROWS_COLOR[2]
                },
                colorMap);

        createHeader(projectLicenseInfos, wb, sheet);

        writeData(projectLicenseInfos, wb, sheet, alternatingRowsColor, dataFormatting);

        try (OutputStream fileOut = Files.newOutputStream(licensesExcelOutputFile.toPath())) {
            wb.write(fileOut);
            LOG.debug("Written Microsoft Excel file {}", licensesExcelOutputFile);
        } catch (IOException e) {
            LOG.error("Error on storing Microsoft Excel file with license and other information", e);
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
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
                sheet,
                SpreadsheetUtil.MAVEN_START_COLUMN,
                SpreadsheetUtil.MAVEN_END_COLUMN,
                headerCellStyle,
                mavenJarRow,
                "Maven information",
                0);

        if (hasExtendedInfo) {
            // Create JAR header cell
            createMergedCellsInRow(
                    sheet,
                    SpreadsheetUtil.EXTENDED_INFO_START_COLUMN,
                    SpreadsheetUtil.EXTENDED_INFO_END_COLUMN,
                    headerCellStyle,
                    mavenJarRow,
                    "JAR Content",
                    0);
        }

        // Create 2nd header row
        Row secondHeaderRow = sheet.createRow(1);

        // Create Maven "General" header
        createMergedCellsInRow(
                sheet,
                SpreadsheetUtil.GENERAL_START_COLUMN,
                SpreadsheetUtil.GENERAL_END_COLUMN,
                headerCellStyle,
                secondHeaderRow,
                "General",
                1);

        // Create Maven "Plugin ID" header
        createMergedCellsInRow(
                sheet,
                SpreadsheetUtil.PLUGIN_ID_START_COLUMN,
                SpreadsheetUtil.PLUGIN_ID_END_COLUMN,
                headerCellStyle,
                secondHeaderRow,
                "Plugin ID",
                1);

        // Gap "General" <-> "Plugin ID".
        sheet.setColumnWidth(SpreadsheetUtil.GENERAL_END_COLUMN, GAP_WIDTH);

        // Create Maven "Licenses" header
        createMergedCellsInRow(
                sheet,
                SpreadsheetUtil.LICENSES_START_COLUMN,
                SpreadsheetUtil.LICENSES_END_COLUMN,
                headerCellStyle,
                secondHeaderRow,
                "Licenses",
                1);

        // Gap "Plugin ID" <-> "Licenses".
        sheet.setColumnWidth(SpreadsheetUtil.PLUGIN_ID_END_COLUMN, GAP_WIDTH);

        // Create Maven "Developers" header
        createMergedCellsInRow(
                sheet,
                SpreadsheetUtil.DEVELOPERS_START_COLUMN,
                SpreadsheetUtil.DEVELOPERS_END_COLUMN,
                headerCellStyle,
                secondHeaderRow,
                "Developers",
                1);

        // Gap "Licenses" <-> "Developers".
        sheet.setColumnWidth(SpreadsheetUtil.LICENSES_END_COLUMN, GAP_WIDTH);

        // Create Maven "Miscellaneous" header
        createMergedCellsInRow(
                sheet,
                SpreadsheetUtil.MISC_START_COLUMN,
                SpreadsheetUtil.MISC_END_COLUMN,
                headerCellStyle,
                secondHeaderRow,
                "Miscellaneous",
                1);

        // Gap "Developers" <-> "Miscellaneous".
        sheet.setColumnWidth(SpreadsheetUtil.DEVELOPERS_END_COLUMN, GAP_WIDTH);

        if (hasExtendedInfo) {
            createMergedCellsInRow(
                    sheet,
                    SpreadsheetUtil.MANIFEST_START_COLUMN,
                    SpreadsheetUtil.MANIFEST_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "MANIFEST.MF",
                    1);

            // Gap "Miscellaneous" <-> "MANIFEST.MF".
            sheet.setColumnWidth(SpreadsheetUtil.DEVELOPERS_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    sheet,
                    SpreadsheetUtil.INFO_NOTICES_START_COLUMN,
                    SpreadsheetUtil.INFO_NOTICES_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "Notices text files",
                    1);

            // Gap "MANIFEST.MF" <-> "Notice text files".
            sheet.setColumnWidth(SpreadsheetUtil.MANIFEST_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    sheet,
                    SpreadsheetUtil.INFO_LICENSES_START_COLUMN,
                    SpreadsheetUtil.INFO_LICENSES_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "License text files",
                    1);

            // Gap "Notice text files" <-> "License text files".
            sheet.setColumnWidth(SpreadsheetUtil.INFO_NOTICES_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    sheet,
                    SpreadsheetUtil.INFO_SPDX_START_COLUMN,
                    SpreadsheetUtil.INFO_SPDX_END_COLUMN,
                    headerCellStyle,
                    secondHeaderRow,
                    "SPDX license id matched",
                    1);

            // Gap "License text files" <-> "SPDX license matches".
            sheet.setColumnWidth(SpreadsheetUtil.INFO_LICENSES_END_COLUMN, GAP_WIDTH);
        }
        //        sheet.setColumnGroupCollapsed();

        sheet.setColumnWidth(getDownloadColumn(hasExtendedInfo) - 1, GAP_WIDTH);

        // Create 3rd header row
        Row thirdHeaderRow = sheet.createRow(2);

        // General
        createCellsInRow(thirdHeaderRow, SpreadsheetUtil.GENERAL_START_COLUMN, headerCellStyle, "Name");
        // Plugin ID
        createCellsInRow(
                thirdHeaderRow,
                SpreadsheetUtil.PLUGIN_ID_START_COLUMN,
                headerCellStyle,
                "Group ID",
                "Artifact ID",
                "Version");
        // Licenses
        createCellsInRow(
                thirdHeaderRow,
                SpreadsheetUtil.LICENSES_START_COLUMN,
                headerCellStyle,
                "Name",
                "URL",
                "Distribution",
                "Comments",
                "File");
        // Developers
        createCellsInRow(
                thirdHeaderRow,
                SpreadsheetUtil.DEVELOPERS_START_COLUMN,
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
                thirdHeaderRow,
                SpreadsheetUtil.MISC_START_COLUMN,
                headerCellStyle,
                "Inception Year",
                "Organization",
                "SCM",
                "URL");

        int headerLineCount = 3;

        if (hasExtendedInfo) {
            // MANIFEST.MF
            createCellsInRow(
                    thirdHeaderRow,
                    SpreadsheetUtil.MANIFEST_START_COLUMN,
                    headerCellStyle,
                    "Bundle license",
                    "Bundle vendor",
                    "Implementation vendor");
            // 3 InfoFile groups: Notices, Licenses and SPDX-Licenses.
            createInfoFileCellsInRow(
                    thirdHeaderRow,
                    headerCellStyle,
                    SpreadsheetUtil.INFO_NOTICES_START_COLUMN,
                    SpreadsheetUtil.INFO_LICENSES_START_COLUMN,
                    SpreadsheetUtil.INFO_SPDX_START_COLUMN);

            sheet.createFreezePane(getDownloadColumn(true) - 1, headerLineCount);
        } else {
            sheet.createFreezePane(getDownloadColumn(false) - 1, headerLineCount);
        }

        sheet.createFreezePane(SpreadsheetUtil.GENERAL_END_COLUMN, headerLineCount);
    }

    private static class CellStyles {
        private final CellStyle hyperlinkStyleNormal;
        private final CellStyle hyperlinkStyleGray;
        private final XSSFCellStyle grayStyle;
        private final CellStyle unknownLicenseStyleNormal;
        private final CellStyle unknownLicenseStyleGray;
        private final CellStyle forbiddenLicenseStyleNormal;
        private final CellStyle forbiddenLicenseStyleGray;
        private final CellStyle problematicLicenseStyleNormal;
        private final CellStyle problematicLicenseStyleGray;
        private final CellStyle okLicenseStyleNormal;
        private final CellStyle okLicenseStyleGray;

        CellStyles(XSSFWorkbook wb, XSSFColor alternatingRowsColor) {
            hyperlinkStyleNormal = createHyperlinkStyle(wb, null);
            hyperlinkStyleGray = createHyperlinkStyle(wb, alternatingRowsColor);

            grayStyle = wb.createCellStyle();
            grayStyle.setFillForegroundColor(alternatingRowsColor);
            grayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            unknownLicenseStyleNormal = createColoredFontStyle(wb, null, IndexedColors.TEAL);
            unknownLicenseStyleGray = createColoredFontStyle(wb, alternatingRowsColor, IndexedColors.TEAL);

            forbiddenLicenseStyleNormal = createColoredFontStyle(wb, null, IndexedColors.RED);
            forbiddenLicenseStyleGray = createColoredFontStyle(wb, alternatingRowsColor, IndexedColors.RED);

            problematicLicenseStyleNormal = createColoredFontStyle(wb, null, IndexedColors.ORANGE);
            problematicLicenseStyleGray = createColoredFontStyle(wb, alternatingRowsColor, IndexedColors.ORANGE);

            okLicenseStyleNormal = createColoredFontStyle(wb, null, IndexedColors.GREEN);
            okLicenseStyleGray = createColoredFontStyle(wb, alternatingRowsColor, IndexedColors.GREEN);
        }

        private static CellStyle createHyperlinkStyle(XSSFWorkbook wb, XSSFColor backgroundColor) {
            Font hyperlinkFont = wb.createFont();
            hyperlinkFont.setUnderline(Font.U_SINGLE);
            hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
            XSSFCellStyle hyperlinkStyle = wb.createCellStyle();
            if (backgroundColor != null) {
                hyperlinkStyle.setFillForegroundColor(backgroundColor);
                hyperlinkStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            hyperlinkStyle.setFont(hyperlinkFont);
            return hyperlinkStyle;
        }

        private static CellStyle createColoredFontStyle(XSSFWorkbook wb, XSSFColor backgroundColor,
                                                            IndexedColors indexedColor) {
            Font highlightUnknownFont = wb.createFont();
            highlightUnknownFont.setColor(indexedColor.getIndex());
            XSSFCellStyle colorStyle = wb.createCellStyle();
            if (backgroundColor != null) {
                colorStyle.setFillForegroundColor(backgroundColor);
                colorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            colorStyle.setFont(highlightUnknownFont);
            colorStyle.setLeftBorderColor(indexedColor.getIndex());
            colorStyle.setTopBorderColor(indexedColor.getIndex());
            colorStyle.setRightBorderColor(indexedColor.getIndex());
            colorStyle.setBottomBorderColor(indexedColor.getIndex());
            return colorStyle;
        }

        public CellStyle getGrayStyle(boolean grayBackground) {
            return grayBackground
                ? grayStyle
                : null;
        }

        public CellStyle getHyperlinkStyle(boolean grayBackground) {
            return grayBackground
                ? hyperlinkStyleGray
                : hyperlinkStyleNormal;
        }

        public CellStyle getLicenseStyle(LicenseColorStyle licenseColorStyle, boolean grayBackground) {
            switch (licenseColorStyle) {
                case UNKNOWN:
                    return grayBackground ? unknownLicenseStyleGray : unknownLicenseStyleNormal;
                case FORBIDDEN:
                    return grayBackground ? forbiddenLicenseStyleGray : forbiddenLicenseStyleNormal;
                case PROBLEMATIC:
                    return grayBackground ? problematicLicenseStyleGray : problematicLicenseStyleNormal;
                case OK:
                    return grayBackground ? okLicenseStyleGray : okLicenseStyleNormal;
                case NONE:
                    return null;
                default:
                    throw new IllegalStateException("Unexpected LicenseColorStyle: " + licenseColorStyle);
            }
        }
    }

    /* Possible improvement:
    Clean this method up.
    Reduce parameters, complicated parameters/DTO pattern.
    But keep it still threadsafe. */
    @SuppressWarnings("checkstyle:MethodLength")
    private static void writeData(
        List<ProjectLicenseInfo> projectLicenseInfos,
        XSSFWorkbook wb,
        Sheet sheet,
        XSSFColor alternatingRowsColor,
        AbstractDownloadLicensesMojo.DataFormatting dataFormatting) {
        final int firstRowIndex = 3;
        int currentRowIndex = firstRowIndex;
        final Map<Integer, Row> rowMap = new HashMap<>();
        boolean hasExtendedInfo = false;

        boolean grayBackground = false;

        final CellStyles cellStyles = new CellStyles(wb, alternatingRowsColor);

        for (ProjectLicenseInfo projectInfo : projectLicenseInfos) {
            final CellStyle cellStyle, hyperlinkStyle;
            LOG.debug("Writing {}:{} into Microsoft Excel file", projectInfo.getGroupId(), projectInfo.getArtifactId());
            if (grayBackground) {
                cellStyle = cellStyles.grayStyle;
                hyperlinkStyle = cellStyles.hyperlinkStyleGray;
            } else {
                cellStyle = null;
                hyperlinkStyle = cellStyles.hyperlinkStyleNormal;
            }
            grayBackground = !grayBackground;

            int extraRows = 0;
            Row currentRow = sheet.createRow(currentRowIndex);
            rowMap.put(currentRowIndex, currentRow);
            // Plugin ID
            createDataCellsInRow(
                    currentRow,
                    SpreadsheetUtil.PLUGIN_ID_START_COLUMN,
                    cellStyles,
                    grayBackground,
                    projectInfo.getGroupId(),
                    projectInfo.getArtifactId(),
                    projectInfo.getVersion());
            // Licenses
            final CellListParameter cellListParameter = new CellListParameter(sheet, rowMap, cellStyles, grayBackground);
            SpreadsheetUtil.CurrentRowData currentRowData =
                    new SpreadsheetUtil.CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
            boolean finalGrayBackground = grayBackground;
            extraRows = addList(
                cellListParameter,
                currentRowData,
                SpreadsheetUtil.LICENSES_START_COLUMN,
                SpreadsheetUtil.LICENSES_COLUMNS,
                projectInfo.getLicenses(),
                (Row licenseRow, ProjectLicense license)
                    -> addLicenses(wb, licenseRow, license, cellStyles, finalGrayBackground, dataFormatting));

            final ExtendedInfo extendedInfo = projectInfo.getExtendedInfo();
            if (extendedInfo != null) {
                hasExtendedInfo = true;
                // General
                createDataCellsInRow(
                        currentRow, SpreadsheetUtil.GENERAL_START_COLUMN, cellStyles, grayBackground, extendedInfo.getName());
                // Developers
                if (!dataFormatting.skipDevelopers) {
                    currentRowData = new SpreadsheetUtil.CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                    extraRows = addList(
                        cellListParameter,
                        currentRowData,
                        SpreadsheetUtil.DEVELOPERS_START_COLUMN,
                        SpreadsheetUtil.DEVELOPERS_COLUMNS,
                        extendedInfo.getDevelopers(),
                        (Row developerRow, Developer developer) -> {
                            Cell[] licenses = createDataCellsInRow(
                                developerRow,
                                SpreadsheetUtil.DEVELOPERS_START_COLUMN,
                                cellStyles,
                                finalGrayBackground,
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
                }
                // Miscellaneous
                Cell[] miscCells = createDataCellsInRow(
                        currentRow,
                        SpreadsheetUtil.MISC_START_COLUMN,
                        cellStyles,
                        grayBackground,
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
                        SpreadsheetUtil.MANIFEST_START_COLUMN,
                        cellStyles,
                        grayBackground,
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
                    currentRowData = new SpreadsheetUtil.CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                    extraRows = addInfoFileList(
                            cellListParameter,
                            currentRowData,
                            SpreadsheetUtil.INFO_NOTICES_START_COLUMN,
                            SpreadsheetUtil.INFO_NOTICES_COLUMNS,
                            notices);
                    // InfoFile licenses text file
                    currentRowData = new SpreadsheetUtil.CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                    extraRows = addInfoFileList(
                            cellListParameter,
                            currentRowData,
                            SpreadsheetUtil.INFO_LICENSES_START_COLUMN,
                            SpreadsheetUtil.INFO_LICENSES_COLUMNS,
                            licenses);
                    // InfoFile spdx licenses text file
                    currentRowData = new SpreadsheetUtil.CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                    extraRows = addInfoFileList(
                            cellListParameter,
                            currentRowData,
                            SpreadsheetUtil.INFO_SPDX_START_COLUMN,
                            SpreadsheetUtil.INFO_SPDX_COLUMNS,
                            spdxs);
                } else if (cellListParameter.getCellStyles() != null) {
                    setStyleOnEmptyCells(
                            cellListParameter,
                            currentRowData,
                            SpreadsheetUtil.INFO_NOTICES_START_COLUMN,
                            SpreadsheetUtil.INFO_NOTICES_COLUMNS);
                    setStyleOnEmptyCells(
                            cellListParameter,
                            currentRowData,
                            SpreadsheetUtil.INFO_LICENSES_START_COLUMN,
                            SpreadsheetUtil.INFO_LICENSES_COLUMNS);
                    setStyleOnEmptyCells(
                            cellListParameter,
                            currentRowData,
                            SpreadsheetUtil.INFO_SPDX_START_COLUMN,
                            SpreadsheetUtil.INFO_SPDX_COLUMNS);
                }
            }
            if (CollectionUtils.isNotEmpty(projectInfo.getDownloaderMessages())) {
                currentRowData = new SpreadsheetUtil.CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);

                int startColumn = hasExtendedInfo
                        ? SpreadsheetUtil.DOWNLOAD_MESSAGE_EXTENDED_COLUMN
                        : SpreadsheetUtil.DOWNLOAD_MESSAGE_NOT_EXTENDED_COLUMN;
                extraRows = addList(
                        cellListParameter,
                        currentRowData,
                        startColumn,
                        SpreadsheetUtil.DOWNLOAD_MESSAGE_COLUMNS,
                        projectInfo.getDownloaderMessages(),
                        (Row licenseRow, String message) -> {
                            Cell[] licenses = createDataCellsInRow(licenseRow, startColumn, cellStyles, finalGrayBackground,
                                message);
                            if (message.matches(SpreadsheetUtil.VALID_LINK)) {
                                addHyperlinkIfExists(wb, licenses[0], hyperlinkStyle, HyperlinkType.URL);
                            }
                        });
            }
            currentRowIndex += extraRows + 1;
        }

        autosizeColumns(sheet, hasExtendedInfo);
    }

    private static void addLicenses(XSSFWorkbook wb, Row licenseRow, ProjectLicense license, CellStyles cellStyles,
                                    boolean grayBackground, AbstractDownloadLicensesMojo.DataFormatting dataFormatting) {
        Cell[] licenses = createDataCellsInRow(
            licenseRow,
                SpreadsheetUtil.LICENSES_START_COLUMN,
            cellStyles,
            grayBackground,
                license.getName(),
                license.getUrl(),
                license.getDistribution(),
                license.getComments(),
                license.getFile());
        final LicenseColorStyle licenseColorStyle = getLicenseColorStyle(license, dataFormatting);
        if (licenseColorStyle != LicenseColorStyle.NONE) {
            licenses[0].setCellStyle(cellStyles.getLicenseStyle(licenseColorStyle, grayBackground));
        }
        addHyperlinkIfExists(wb, licenses[1], cellStyles.getHyperlinkStyle(grayBackground), HyperlinkType.URL);
    }

    private static LicenseColorStyle getLicenseColorStyle(ProjectLicense license, AbstractDownloadLicensesMojo.DataFormatting dataFormatting) {
        final LicenseColorStyle licenseColorStyle;
        if (dataFormatting.forbiddenLicenses.contains(license.getName())) {
            licenseColorStyle = LicenseColorStyle.FORBIDDEN;
        } else if (dataFormatting.problematicLicenses.contains(license.getName())) {
            licenseColorStyle = LicenseColorStyle.PROBLEMATIC;
        } else if (dataFormatting.okLicenses.contains(license.getName())) {
            licenseColorStyle = LicenseColorStyle.OK;
        } else if (dataFormatting.highlightUnknownLicenses) {
            licenseColorStyle = LicenseColorStyle.UNKNOWN;
        } else {
            licenseColorStyle = LicenseColorStyle.NONE;
        }
        return licenseColorStyle;
    }

    private static void autosizeColumns(Sheet sheet, boolean hasExtendedInfo) {
        autosizeColumns(
                sheet,
                new ImmutablePair<>(SpreadsheetUtil.GENERAL_START_COLUMN, SpreadsheetUtil.GENERAL_END_COLUMN),
                new ImmutablePair<>(SpreadsheetUtil.PLUGIN_ID_START_COLUMN, SpreadsheetUtil.PLUGIN_ID_END_COLUMN),
                new ImmutablePair<>(SpreadsheetUtil.LICENSES_START_COLUMN, SpreadsheetUtil.LICENSES_END_COLUMN),
                new ImmutablePair<>(SpreadsheetUtil.DEVELOPERS_START_COLUMN, SpreadsheetUtil.DEVELOPERS_END_COLUMN - 1),
                new ImmutablePair<>(SpreadsheetUtil.MISC_START_COLUMN + 1, SpreadsheetUtil.MISC_END_COLUMN));
        // The column header widths are most likely wider than the actual cells content.
        sheet.setColumnWidth(SpreadsheetUtil.DEVELOPERS_END_COLUMN - 1, SpreadsheetUtil.TIMEZONE_WIDTH);
        sheet.setColumnWidth(SpreadsheetUtil.MISC_START_COLUMN, SpreadsheetUtil.INCEPTION_YEAR_WIDTH);
        if (hasExtendedInfo) {
            autosizeColumns(
                    sheet,
                    new ImmutablePair<>(SpreadsheetUtil.MANIFEST_START_COLUMN, SpreadsheetUtil.MANIFEST_END_COLUMN),
                    new ImmutablePair<>(
                            SpreadsheetUtil.INFO_NOTICES_START_COLUMN + 2, SpreadsheetUtil.INFO_NOTICES_END_COLUMN),
                    new ImmutablePair<>(
                            SpreadsheetUtil.INFO_LICENSES_START_COLUMN + 2, SpreadsheetUtil.INFO_LICENSES_END_COLUMN),
                    new ImmutablePair<>(
                            SpreadsheetUtil.INFO_SPDX_START_COLUMN + 2, SpreadsheetUtil.INFO_SPDX_END_COLUMN));
        }
        autosizeColumns(
                sheet,
                new ImmutablePair<>(
                        getDownloadColumn(hasExtendedInfo),
                        getDownloadColumn(hasExtendedInfo) + SpreadsheetUtil.DOWNLOAD_MESSAGE_COLUMNS));
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
            SpreadsheetUtil.CurrentRowData currentRowData,
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
                            .map(strings -> String.join(SpreadsheetUtil.COPYRIGHT_JOIN_SEPARATOR, strings))
                            .orElse(null);
                    createDataCellsInRow(
                            infoFileRow,
                            startColumn,
                            cellListParameter.getCellStyles(),
                            cellListParameter.getGrayBackground(),
                            infoFile.getContent(),
                            copyrightLines,
                            infoFile.getFileName());
                });
    }

    private static <T> int addList(
            CellListParameter cellListParameter,
            SpreadsheetUtil.CurrentRowData currentRowData,
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
                    if (cellListParameter.getCellStyles() != null) {
                        // Style all empty left cells, in the columns left from this
                        createAndStyleCells(
                                row,
                                cellListParameter.getCellStyles(),
                                cellListParameter.getGrayBackground(),
                                new ImmutablePair<>(
                                        SpreadsheetUtil.GENERAL_START_COLUMN, SpreadsheetUtil.GENERAL_END_COLUMN),
                                new ImmutablePair<>(
                                        SpreadsheetUtil.PLUGIN_ID_START_COLUMN, SpreadsheetUtil.PLUGIN_ID_END_COLUMN),
                                new ImmutablePair<>(
                                        SpreadsheetUtil.LICENSES_START_COLUMN, SpreadsheetUtil.LICENSES_END_COLUMN));
                        if (currentRowData.isHasExtendedInfo()) {
                            createAndStyleCells(
                                    row,
                                    cellListParameter.getCellStyles(),
                                    cellListParameter.getGrayBackground(),
                                    new ImmutablePair<>(
                                            SpreadsheetUtil.DEVELOPERS_START_COLUMN,
                                            SpreadsheetUtil.DEVELOPERS_END_COLUMN),
                                    new ImmutablePair<>(
                                            SpreadsheetUtil.MISC_START_COLUMN, SpreadsheetUtil.MISC_END_COLUMN),
                                    // JAR
                                    new ImmutablePair<>(
                                            SpreadsheetUtil.MANIFEST_START_COLUMN, SpreadsheetUtil.MANIFEST_END_COLUMN),
                                    new ImmutablePair<>(
                                            SpreadsheetUtil.INFO_LICENSES_START_COLUMN,
                                            SpreadsheetUtil.INFO_LICENSES_END_COLUMN),
                                    new ImmutablePair<>(
                                            SpreadsheetUtil.INFO_NOTICES_START_COLUMN,
                                            SpreadsheetUtil.INFO_NOTICES_END_COLUMN),
                                    new ImmutablePair<>(
                                            SpreadsheetUtil.INFO_SPDX_START_COLUMN,
                                            SpreadsheetUtil.INFO_SPDX_END_COLUMN));
                        }
                    }
                    currentRowData.setExtraRows(currentRowData.getExtraRows() + 1);
                }
                biConsumer.accept(row, type);
            }
        } else if (cellListParameter.getCellStyles() != null) {
            setStyleOnEmptyCells(cellListParameter, currentRowData, startColumn, columnsToFill);
        }
        return currentRowData.getExtraRows();
    }

    /**
     * If no cells are set, color at least the background,
     * to color concatenated blocks with the same background color.
     *
     * @param cellListParameter Passes data about sheet, row, cell style.
     * @param currentRowData    Passes data about the current indices for rows and columns.
     * @param startColumn       Column where to start setting the style.
     * @param columnsToFill     How many columns to set the style on, starting from 'startColumn'.
     */
    private static void setStyleOnEmptyCells(
            CellListParameter cellListParameter,
            SpreadsheetUtil.CurrentRowData currentRowData,
            int startColumn,
            int columnsToFill) {
        Row row = cellListParameter.getRows().get(currentRowData.getCurrentRowIndex());
        for (int i = 0; i < columnsToFill; i++) {
            Cell cell = row.createCell(startColumn + i, CellType.STRING);
            cell.setCellStyle(cellListParameter.getCellStyles().getGrayStyle(cellListParameter.getGrayBackground()));
        }
    }

    @SafeVarargs
    private static void createAndStyleCells(Row row, CellStyles cellStyles, boolean grayBackground,
                                            Pair<Integer, Integer>... ranges) {
        for (Pair<Integer, Integer> range : ranges) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                Cell cell = row.createCell(i, CellType.STRING);
                cell.setCellStyle(cellStyles.getGrayStyle(grayBackground));
            }
        }
    }

    private static void addHyperlinkIfExists(
            Workbook workbook, Cell cell, CellStyle hyperlinkStyle, HyperlinkType hyperlinkType) {
        final String link = cell.getStringCellValue();
        if (!StringUtils.isEmpty(link)) {
            Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(hyperlinkType);
            final String modifiedLink = prefixedHyperlink(hyperlinkType, link);
            try {
                hyperlink.setAddress(modifiedLink);
                cell.setHyperlink(hyperlink);
                cell.setCellStyle(hyperlinkStyle);
            } catch (IllegalArgumentException e) {
                LOG.debug(
                        "Can't set Hyperlink for cell value " + link + " (" + modifiedLink
                                + ") it gets rejected as URI",
                        e);
            }
        }
    }

    /**
     * Adds "https://" prefix to link if it's missing.
     *
     * @param hyperlinkType Type of hyperlink.
     * @param link          Hyperlink address.
     * @return Prefixed hyperlink.
     */
    private static String prefixedHyperlink(HyperlinkType hyperlinkType, String link) {
        final String modifiedLink;
        link = link.trim().replace(" dot ", ".");
        if (hyperlinkType == HyperlinkType.EMAIL) {
            // Replace all "bla com" with "bla.com".
            link = link.replace(" at ", "@");
            if (link.contains("@") && link.matches(".*\\s[a-zA-Z]{2,3}$")) {
                modifiedLink = link.replace(" ", ".");
            } else {
                modifiedLink = link;
            }
        } else if (!link.startsWith("http://") && !link.startsWith("https://")) {
            modifiedLink = "https://" + link;
        } else {
            modifiedLink = link;
        }
        return modifiedLink;
    }

    private static Cell[] createDataCellsInRow(Row row, int startColumn, CellStyles cellStyles, boolean grayBackground,
                                               String... names) {
        Cell[] result = new Cell[names.length];
        for (int i = 0; i < names.length; i++) {
            Cell cell = row.createCell(startColumn + i, CellType.STRING);
            if (cellStyles.getGrayStyle(grayBackground) != null) {
                cell.setCellStyle(cellStyles.getGrayStyle(grayBackground));
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
        if (cell == null) {
            return;
        }
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
        private final CellStyles cellStyles;
        private final boolean grayBackground;

        private CellListParameter(Sheet sheet, Map<Integer, Row> rows, CellStyles cellStyles, boolean grayBackground) {
            this.sheet = sheet;
            this.rows = rows;
            this.cellStyles = cellStyles;
            this.grayBackground = grayBackground;
        }

        Sheet getSheet() {
            return sheet;
        }

        Map<Integer, Row> getRows() {
            return rows;
        }

        CellStyles getCellStyles() {
            return cellStyles;
        }

        boolean getGrayBackground() {
            return grayBackground;
        }
    }
}
