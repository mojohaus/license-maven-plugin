package org.codehaus.mojo.license.extended.spreadsheet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.codehaus.mojo.license.extended.InfoFile;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.*;
import org.odftoolkit.odfdom.dom.OdfContentDom;
import org.odftoolkit.odfdom.dom.OdfSettingsDom;
import org.odftoolkit.odfdom.dom.element.config.ConfigConfigItemElement;
import org.odftoolkit.odfdom.dom.element.config.ConfigConfigItemMapEntryElement;
import org.odftoolkit.odfdom.dom.element.style.StyleParagraphPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTableCellPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.element.text.TextAElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.dom.style.props.OdfTableColumnProperties;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.type.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.codehaus.mojo.license.extended.spreadsheet.SpreadsheetUtil.*;

/**
 * Writes LibreOffices Calc ODS file.
 */
public class CalcFileWriter {
    private static final Logger LOG = LoggerFactory.getLogger(CalcFileWriter.class);

    private static final String HEADER_CELL_STYLE = "headerCellStyle";
    private static final String HYPERLINK_NORMAL_STYLE = "hyperlinkNormalStyle";
    private static final String HYPERLINK_GRAY_STYLE = "hyperlinkGrayStyle";
    private static final String GRAY_CELL_STYLE = "grayCellStyle";
    private static final String NORMAL_CELL_STYLE = "normalCellStyle";
    private static final int DOWNLOAD_COLUMN_WIDTH = 6_000;

    public static void write(List<ProjectLicenseInfo> projectLicenseInfos, final File licensesCalcOutputFile) {
        if (CollectionUtils.isEmpty(projectLicenseInfos)) {
            LOG.debug("Nothing to write to excel, no project data.");
            return;
        }

        try (OdfSpreadsheetDocument spreadsheet = OdfSpreadsheetDocument.newSpreadsheetDocument()) {
            List<OdfTable> tableList = spreadsheet.getTableList();
            final OdfTable table;
            if (!tableList.isEmpty()) {
                table = tableList.get(0);
            } else {
                table = OdfTable.newTable(spreadsheet);
            }
            table.setTableName(TABLE_NAME);

            createHeaderStyle(spreadsheet);

            createHeader(projectLicenseInfos, spreadsheet, table);

            writeData(
                    projectLicenseInfos, spreadsheet, table, convertToOdfColor(SpreadsheetUtil.ALTERNATING_ROWS_COLOR));

            try (OutputStream fileOut = Files.newOutputStream(licensesCalcOutputFile.toPath())) {
                spreadsheet.save(fileOut);
            } catch (IOException e) {
                LOG.error("Error on storing Calc file with license and other information", e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Color convertToOdfColor(final int[] color) {
        return new Color(color[0], color[1], color[2]);
    }

    private static void createHeader(
            List<ProjectLicenseInfo> projectLicenseInfos, OdfSpreadsheetDocument spreadsheet, OdfTable table) {
        boolean hasExtendedInfo = false;
        for (ProjectLicenseInfo projectLicenseInfo : projectLicenseInfos) {
            if (projectLicenseInfo.getExtendedInfo() != null) {
                hasExtendedInfo = true;
                break;
            }
        }

        /*
        All rows must be added before any cell merges, or merges on new lines will be merged like the previous
        rows.
        */

        // Create 1st header row. The Maven/JAR header row
        OdfTableRow mavenJarRow = table.getRowByIndex(0);

        // Create 2nd header row
        OdfTableRow secondHeaderRow = table.appendRow();

        // Create 3rd header row
        OdfTableRow thirdHeaderRow = table.appendRow();

        // Create Maven header cell
        createMergedCellsInRow(
                table, MAVEN_START_COLUMN, MAVEN_END_COLUMN, mavenJarRow, "Maven information", 0, HEADER_CELL_STYLE);

        if (hasExtendedInfo) {
            // Create JAR header cell
            createMergedCellsInRow(
                    table,
                    EXTENDED_INFO_START_COLUMN,
                    EXTENDED_INFO_END_COLUMN,
                    mavenJarRow,
                    "JAR Content",
                    0,
                    HEADER_CELL_STYLE);
        }

        // Create Maven "General" header
        createMergedCellsInRow(
                table, GENERAL_START_COLUMN, GENERAL_END_COLUMN, secondHeaderRow, "General", 1, HEADER_CELL_STYLE);

        // Create Maven "Plugin ID" header
        createMergedCellsInRow(
                table,
                PLUGIN_ID_START_COLUMN,
                PLUGIN_ID_END_COLUMN,
                secondHeaderRow,
                "Plugin ID",
                1,
                HEADER_CELL_STYLE);

        // Gap "General" <-> "Plugin ID".
        setColumnWidth(table, GENERAL_END_COLUMN, GAP_WIDTH);

        // Create Maven "Licenses" header
        createMergedCellsInRow(
                table, LICENSES_START_COLUMN, LICENSES_END_COLUMN, secondHeaderRow, "Licenses", 1, HEADER_CELL_STYLE);

        // Gap "Plugin ID" <-> "Licenses".
        setColumnWidth(table, PLUGIN_ID_END_COLUMN, GAP_WIDTH);

        // Create Maven "Developers" header
        createMergedCellsInRow(
                table,
                DEVELOPERS_START_COLUMN,
                DEVELOPERS_END_COLUMN,
                secondHeaderRow,
                "Developers",
                1,
                HEADER_CELL_STYLE);

        // Gap "Licenses" <-> "Developers".
        setColumnWidth(table, LICENSES_END_COLUMN, GAP_WIDTH);

        // Create Maven "Miscellaneous" header
        createMergedCellsInRow(
                table, MISC_START_COLUMN, MISC_END_COLUMN, secondHeaderRow, "Miscellaneous", 1, HEADER_CELL_STYLE);

        // Gap "Developers" <-> "Miscellaneous".
        setColumnWidth(table, DEVELOPERS_END_COLUMN, GAP_WIDTH);

        if (hasExtendedInfo) {
            createMergedCellsInRow(
                    table,
                    MANIFEST_START_COLUMN,
                    MANIFEST_END_COLUMN,
                    secondHeaderRow,
                    "MANIFEST.MF",
                    1,
                    HEADER_CELL_STYLE);

            // Gap "Miscellaneous" <-> "MANIFEST.MF".
            setColumnWidth(table, DEVELOPERS_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    table,
                    INFO_NOTICES_START_COLUMN,
                    INFO_NOTICES_END_COLUMN,
                    secondHeaderRow,
                    "Notices text files",
                    1,
                    HEADER_CELL_STYLE);

            // Gap "MANIFEST.MF" <-> "Notice text files".
            setColumnWidth(table, MANIFEST_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    table,
                    INFO_LICENSES_START_COLUMN,
                    INFO_LICENSES_END_COLUMN,
                    secondHeaderRow,
                    "License text files",
                    1,
                    HEADER_CELL_STYLE);

            // Gap "Notice text files" <-> "License text files".
            setColumnWidth(table, INFO_NOTICES_END_COLUMN, GAP_WIDTH);

            createMergedCellsInRow(
                    table,
                    INFO_SPDX_START_COLUMN,
                    INFO_SPDX_END_COLUMN,
                    secondHeaderRow,
                    "SPDX license id matched",
                    1,
                    HEADER_CELL_STYLE);

            // Gap "License text files" <-> "SPDX license matches".
            setColumnWidth(table, INFO_LICENSES_END_COLUMN, GAP_WIDTH);
        }
        //        sheet.setColumnGroupCollapsed();

        setColumnWidth(table, getDownloadColumn(hasExtendedInfo) - 1, GAP_WIDTH);
        setColumnWidth(table, getDownloadColumn(hasExtendedInfo), DOWNLOAD_COLUMN_WIDTH);

        // General
        createCellsInRow(thirdHeaderRow, GENERAL_START_COLUMN, HEADER_CELL_STYLE, "Name");
        // Plugin ID
        createCellsInRow(
                thirdHeaderRow, PLUGIN_ID_START_COLUMN, HEADER_CELL_STYLE, "Group ID", "Artifact ID", "Version");
        // Licenses
        createCellsInRow(
                thirdHeaderRow,
                LICENSES_START_COLUMN,
                HEADER_CELL_STYLE,
                "Name",
                "URL",
                "Distribution",
                "Comments",
                "File");
        // Developers
        createCellsInRow(
                thirdHeaderRow,
                DEVELOPERS_START_COLUMN,
                HEADER_CELL_STYLE,
                "Id",
                "Email",
                "Name",
                "Organization",
                "Organization URL",
                "URL",
                "Timezone");
        // Miscellaneous
        createCellsInRow(
                thirdHeaderRow, MISC_START_COLUMN, HEADER_CELL_STYLE, "Inception Year", "Organization", "SCM", "URL");

        int headerLineCount = 3;

        if (hasExtendedInfo) {
            // MANIFEST.MF
            createCellsInRow(
                    thirdHeaderRow,
                    MANIFEST_START_COLUMN,
                    HEADER_CELL_STYLE,
                    "Bundle license",
                    "Bundle vendor",
                    "Implementation vendor");
            // 3 InfoFile groups: Notices, Licenses and SPDX-Licenses.
            createInfoFileCellsInRow(
                    thirdHeaderRow,
                    HEADER_CELL_STYLE,
                    INFO_NOTICES_START_COLUMN,
                    INFO_LICENSES_START_COLUMN,
                    INFO_SPDX_START_COLUMN);

            createFreezePane(spreadsheet, table, getDownloadColumn(true) - 1, headerLineCount);
        } else {
            createFreezePane(spreadsheet, table, getDownloadColumn(false) - 1, headerLineCount);
        }

        createFreezePane(spreadsheet, table, GENERAL_END_COLUMN, headerLineCount);
    }

    private static void setColumnWidth(OdfTable table, int column, int width) {
        table.getColumnByIndex(column)
                .getOdfElement()
                .setProperty(OdfTableColumnProperties.ColumnWidth, (width / 100) + "mm");
    }

    private static void createFreezePane(
            OdfSpreadsheetDocument spreadsheet, OdfTable table, int column, int lineCount) {
        // TODO: Find out why this perfect XML is ignored. Use FreezePane function from ODFToolkit after they add it.

        final OdfSettingsDom settingsDom;
        try {
            settingsDom = spreadsheet.getSettingsDom();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        NodeList childNodes = settingsDom.getFirstChild().getFirstChild().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if ("config:config-item-set".equals(child.getNodeName())
                    && "ooo:view-settings".equals(((Element) child).getAttribute("config:name"))) {

                NodeList subChilds = child.getChildNodes();
                for (int j = 0; j < subChilds.getLength(); j++) {
                    Node subChild = subChilds.item(j);
                    if ("config:config-item-map-indexed".equals(subChild.getNodeName())
                            && "Views".equals(((Element) subChild).getAttribute("config:name"))) {

                        break;
                    }
                }
                break;
            }
        }

        XPath xpath = settingsDom.getXPath();
        NodeList list;
        try {
            list = (NodeList) xpath.evaluate(
                    "/office:document-settings/" + "office:settings/"
                            + "config:config-item-set/"
                            + "config:config-item-map-indexed/"
                            + "config:config-item-map-entry/"
                            + "config:config-item-map-named/"
                            + "config:config-item-map-entry",
                    //                    "/config:config-item-set[@config:name=\"ooo:view-settings\"]" +
                    settingsDom,
                    XPathConstants.NODE);

            /*
            <config:config-item config:name="HorizontalSplitMode" config:type="short">2</config:config-item>
            <config:config-item config:name="VerticalSplitMode" config:type="short">2</config:config-item>
            <config:config-item config:name="HorizontalSplitPosition" config:type="int">1</config:config-item>
            <config:config-item config:name="VerticalSplitPosition" config:type="int">4</config:config-item>
            <config:config-item config:name="ActiveSplitRange" config:type="short">3</config:config-item>

            <config:config-item config:name="PositionLeft" config:type="int">0</config:config-item>
            <config:config-item config:name="PositionRight" config:type="int">1</config:config-item>
            <config:config-item config:name="PositionTop" config:type="int">0</config:config-item>
            <config:config-item config:name="PositionBottom" config:type="int">3</config:config-item>
             */
            if (list instanceof ConfigConfigItemMapEntryElement) {
                ConfigConfigItemMapEntryElement entryElement = (ConfigConfigItemMapEntryElement) list;

                appendConfigItemElement(entryElement, "HorizontalSplitMode", "short", "2");
                appendConfigItemElement(entryElement, "VerticalSplitMode", "short", "2");

                appendConfigItemElement(entryElement, "HorizontalSplitPosition", "int", "1");
                appendConfigItemElement(entryElement, "VerticalSplitPosition", "int", "3");

                appendConfigItemElement(entryElement, "ActiveSplitRange", "short", "3");

                appendConfigItemElement(entryElement, "PositionLeft", "int", "0");
                appendConfigItemElement(entryElement, "PositionRight", "int", "1");
                appendConfigItemElement(entryElement, "PositionTop", "int", "0");
                appendConfigItemElement(entryElement, "PositionBottom", "int", "3");

                appendConfigItemElement(entryElement, "ShowGrid", "boolean", "true");
                appendConfigItemElement(entryElement, "AnchoredTextOverflowLegacy", "boolean", "false");
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendConfigItemElement(
            ConfigConfigItemMapEntryElement entryElement, String configName, String configType, String nodeValue) {
        ConfigConfigItemElement horizontalSplitMode = null;
        if (entryElement.hasChildNodes()) {
            NodeList nodeList = entryElement.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof ConfigConfigItemElement) {
                    ConfigConfigItemElement itemElement = (ConfigConfigItemElement) node;
                    if (configName.equals(itemElement.getConfigNameAttribute())) {
                        horizontalSplitMode = (ConfigConfigItemElement) node;
                        break;
                    }
                }
            }
        }
        if (horizontalSplitMode == null) {
            horizontalSplitMode = entryElement.newConfigConfigItemElement(configName, configType);
        } else {
            if (horizontalSplitMode.hasChildNodes()) {
                // Find text node and set new value if found.
                for (int i = 0; i < horizontalSplitMode.getLength(); i++) {
                    Node child = horizontalSplitMode.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        child.setNodeValue(nodeValue);
                        return;
                    }
                }
            }
        }
        horizontalSplitMode.newTextNode(nodeValue);
    }

    private static void createHeaderStyle(OdfSpreadsheetDocument spreadsheet) {
        OdfOfficeStyles styles = spreadsheet.getOrCreateDocumentStyles();
        OdfStyle headerStyle = styles.newStyle(HEADER_CELL_STYLE, OdfStyleFamily.TableCell);

        headerStyle.setProperty(StyleTextPropertiesElement.FontFamily, "Arial");
        headerStyle.setProperty(StyleTextPropertiesElement.FontWeight, "bold");
        headerStyle.setProperty(StyleTableCellPropertiesElement.BackgroundColor, "#CCFFCC");
        headerStyle.setProperty(StyleParagraphPropertiesElement.TextAlign, "center");
        headerStyle.setProperty(StyleTableCellPropertiesElement.VerticalAlign, "middle");
        headerStyle.setProperty(StyleTableCellPropertiesElement.Border, "1.0pt solid #000000");
    }

    /* Improvement: Clean this method up.
    Reduce parameters, complicated parameters/DTO pattern.
    But keep it still threadsafe.
     */
    private static void writeData(
            List<ProjectLicenseInfo> projectLicenseInfos,
            OdfSpreadsheetDocument wb,
            OdfTable table,
            Color alternatingRowsColor) {
        final int firstRowIndex = 3;
        int currentRowIndex = firstRowIndex;
        final Map<Integer, OdfTableRow> rowMap = new HashMap<>();
        boolean hasExtendedInfo = false;

        final OdfStyle hyperlinkStyleNormal = createHyperlinkStyle(wb, HYPERLINK_NORMAL_STYLE, null);
        final OdfStyle hyperlinkStyleGray = createHyperlinkStyle(wb, HYPERLINK_GRAY_STYLE, alternatingRowsColor);

        boolean grayBackground = false;
        OdfOfficeStyles officeStyles = wb.getOrCreateDocumentStyles();
        OdfStyle styleGray = officeStyles.newStyle(GRAY_CELL_STYLE, OdfStyleFamily.TableCell);
        styleGray.setProperty(StyleTableCellPropertiesElement.BackgroundColor, alternatingRowsColor.toString());
        styleGray.setProperty(OdfTableColumnProperties.UseOptimalColumnWidth, String.valueOf(true));

        /* Set own, empty style, instead of leaving the style out,
        because otherwise it copies the style of the row above. */
        OdfStyle styleNormal = officeStyles.newStyle(NORMAL_CELL_STYLE, OdfStyleFamily.TableCell);
        styleNormal.setProperty(OdfTableColumnProperties.UseOptimalColumnWidth, String.valueOf(true));

        for (ProjectLicenseInfo projectInfo : projectLicenseInfos) {
            final OdfStyle cellStyle, hyperlinkStyle;
            if (grayBackground) {
                cellStyle = styleGray;
                hyperlinkStyle = hyperlinkStyleGray;
            } else {
                cellStyle = styleNormal;
                hyperlinkStyle = hyperlinkStyleNormal;
            }
            grayBackground = !grayBackground;

            int extraRows = 0;
            OdfTableRow currentRow = table.appendRow();
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
            final CellListParameter cellListParameter = new CellListParameter(table, rowMap, cellStyle);
            CurrentRowData currentRowData = new CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
            extraRows = addList(
                    cellListParameter,
                    currentRowData,
                    LICENSES_START_COLUMN,
                    LICENSES_COLUMNS,
                    projectInfo.getLicenses(),
                    (OdfTableRow licenseRow, ProjectLicense license) -> {
                        OdfTableCell[] licenses = createDataCellsInRow(
                                licenseRow,
                                LICENSES_START_COLUMN,
                                cellStyle,
                                license.getName(),
                                license.getUrl(),
                                license.getDistribution(),
                                license.getComments(),
                                license.getFile());
                        addHyperlinkIfExists(table, licenses[1], hyperlinkStyle);
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
                        (OdfTableRow developerRow, Developer developer) -> {
                            OdfTableCell[] licenses = createDataCellsInRow(
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
                            addHyperlinkIfExists(table, licenses[1], hyperlinkStyle, true);
                            addHyperlinkIfExists(table, licenses[4], hyperlinkStyle);
                            addHyperlinkIfExists(table, licenses[5], hyperlinkStyle);
                        });
                // Miscellaneous
                OdfTableCell[] miscCells = createDataCellsInRow(
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
                addHyperlinkIfExists(table, miscCells[2], hyperlinkStyle);
                addHyperlinkIfExists(table, miscCells[3], hyperlinkStyle);

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
            } else {
                createDataCellsInRow(currentRow, GENERAL_START_COLUMN, cellStyle, 1);
                createDataCellsInRow(currentRow, DEVELOPERS_START_COLUMN, cellStyle, DEVELOPERS_COLUMNS);
                createDataCellsInRow(currentRow, MISC_START_COLUMN, cellStyle, MISC_COLUMNS);
            }

            final int downloadColumn = getDownloadColumn(hasExtendedInfo);
            if (CollectionUtils.isNotEmpty(projectInfo.getDownloaderMessages())) {
                currentRowData = new SpreadsheetUtil.CurrentRowData(currentRowIndex, extraRows, hasExtendedInfo);
                extraRows = addList(
                        cellListParameter,
                        currentRowData,
                        downloadColumn,
                        SpreadsheetUtil.DOWNLOAD_COLUMNS,
                        projectInfo.getDownloaderMessages(),
                        (OdfTableRow licenseRow, String message) -> {
                            OdfTableCell[] licenses =
                                    createDataCellsInRow(licenseRow, downloadColumn, cellStyle, message);
                            if (message.matches(SpreadsheetUtil.VALID_LINK)) {
                                addHyperlinkIfExists(table, licenses[0], hyperlinkStyle);
                            }
                        });
            } else {
                // Add empty cell, so it doesn't copy the previous row cell.
                OdfTableCell cell = currentRow.getCellByIndex(downloadColumn);
                cell.setValueType("string");
                cell.getOdfElement().setStyleName(getCellStyleName(cellStyle));
            }
            currentRowIndex += extraRows + 1;
        }

        autosizeColumns(table, hasExtendedInfo, currentRowIndex);
    }

    private static OdfStyle createHyperlinkStyle(OdfSpreadsheetDocument wb, String name, Color backgroundColor) {
        OdfOfficeStyles styles = wb.getOrCreateDocumentStyles();
        OdfStyle hyperlinkStyle = styles.newStyle(name, OdfStyleFamily.TableCell);

        hyperlinkStyle.setProperty(StyleTextPropertiesElement.FontFamily, "Arial");
        hyperlinkStyle.setProperty(StyleTextPropertiesElement.Color, Color.BLUE.toString());
        hyperlinkStyle.setProperty(StyleParagraphPropertiesElement.TextAlign, "center");
        hyperlinkStyle.setProperty(StyleTableCellPropertiesElement.VerticalAlign, "middle");

        if (backgroundColor != null) {
            hyperlinkStyle.setProperty(StyleTableCellPropertiesElement.BackgroundColor, backgroundColor.toString());
        }
        return hyperlinkStyle;
    }

    private static void autosizeColumns(OdfTable table, boolean hasExtendedInfo, int rows) {
        autosizeColumns(
                table,
                rows,
                new ImmutablePair<>(GENERAL_START_COLUMN, GENERAL_END_COLUMN),
                new ImmutablePair<>(PLUGIN_ID_START_COLUMN, PLUGIN_ID_END_COLUMN),
                new ImmutablePair<>(LICENSES_START_COLUMN, LICENSES_END_COLUMN),
                new ImmutablePair<>(DEVELOPERS_START_COLUMN, DEVELOPERS_END_COLUMN - 1),
                new ImmutablePair<>(MISC_START_COLUMN + 1, MISC_END_COLUMN));
        // The column header widths are most likely wider than the actual cells content.
        setColumnWidth(table, DEVELOPERS_END_COLUMN - 1, TIMEZONE_WIDTH);
        setColumnWidth(table, MISC_START_COLUMN, INCEPTION_YEAR_WIDTH);
        if (hasExtendedInfo) {
            autosizeColumns(
                    table,
                    rows,
                    new ImmutablePair<>(MANIFEST_START_COLUMN, MANIFEST_END_COLUMN),
                    new ImmutablePair<>(INFO_NOTICES_START_COLUMN + 2, INFO_NOTICES_END_COLUMN),
                    new ImmutablePair<>(INFO_LICENSES_START_COLUMN + 2, INFO_LICENSES_END_COLUMN),
                    new ImmutablePair<>(INFO_SPDX_START_COLUMN + 2, INFO_SPDX_END_COLUMN));
        }
    }

    @SafeVarargs
    private static void autosizeColumns(OdfTable sheet, int rows, Pair<Integer, Integer>... ranges) {
        for (Pair<Integer, Integer> range : ranges) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                final float sizeFactor = 2.0f;
                float size = 25;
                // Get max width by taking the max string length multiplied by sizeFactor.
                for (int row = 0; row < rows; row++) {
                    OdfTableCell cell = sheet.getCellByPosition(i, row);
                    if ("string".equals(cell.getValueType())) {
                        String stringValue = cell.getStringValue();
                        size = Math.max(stringValue.length() * sizeFactor, size);
                    }
                }
                final OdfTableColumn column = sheet.getColumnByIndex(i);
                // The attribute is ignored by LibreOffice Calc, set it for other applications.
                column.setUseOptimalWidth(true);

                column.setWidth((long) size);
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
                (OdfTableRow infoFileRow, InfoFile infoFile) -> {
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
            BiConsumer<OdfTableRow, T> biConsumer) {
        if (!CollectionUtils.isEmpty(list)) {
            for (int i = 0; i < list.size(); i++) {
                T type = list.get(i);
                Integer index = currentRowData.getCurrentRowIndex() + i;
                OdfTableRow row = cellListParameter.getRows().get(index);
                if (row == null) {
                    row = cellListParameter.getSheet().appendRow();
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
     * @param currentRowData    Passes data about the current indices for rows and columns.
     * @param startColumn       Column where to start setting the style.
     * @param columnsToFill     How many columns to set the style on, starting from 'startColumn'.
     */
    private static void setStyleOnEmptyCells(
            CellListParameter cellListParameter, CurrentRowData currentRowData, int startColumn, int columnsToFill) {
        OdfTableRow row = cellListParameter.getRows().get(currentRowData.getCurrentRowIndex());
        for (int i = 0; i < columnsToFill; i++) {
            OdfTableCell cell = row.getCellByIndex(startColumn + i);
            cell.setValueType("string");
            cell.getOdfElement().setStyleName(getCellStyleName(cellListParameter.getCellStyle()));
        }
    }

    @SafeVarargs
    private static void createAndStyleCells(OdfTableRow row, OdfStyle cellStyle, Pair<Integer, Integer>... ranges) {
        for (Pair<Integer, Integer> range : ranges) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                OdfTableCell cell = row.getCellByIndex(i);
                cell.setValueType("string");
                cell.getOdfElement().setStyleName(getCellStyleName(cellStyle));
            }
        }
    }

    public static void applyHyperlink(OdfTable table, OdfTableCell cell, String hyperlink, boolean isEmail) {

        TextAElement aElement;
        aElement = ((OdfContentDom) (table.getOdfElement().getOwnerDocument())).newOdfElement(TextAElement.class);
        aElement.setXlinkTypeAttribute("simple");
        hyperlink = hyperlink.trim().replace(" dot ", ".");
        if (isEmail) {
            hyperlink = hyperlink.replace(" at ", "@");
            if (hyperlink.contains("@") && hyperlink.matches(".*\\s[a-zA-Z]{2,3}$")) {
                hyperlink = hyperlink.replaceAll(" ", ".");
            }
        }
        aElement.setXlinkHrefAttribute(isEmail ? "mailto:" + hyperlink : hyperlink);
        aElement.setTextContent(hyperlink);
        Node node = cell.getOdfElement().getFirstChild();

        node.appendChild(aElement);
    }

    private static void addHyperlinkIfExists(OdfTable table, OdfTableCell cell, OdfStyle hyperlinkStyle) {
        addHyperlinkIfExists(table, cell, hyperlinkStyle, false);
    }

    private static void addHyperlinkIfExists(
            OdfTable table, OdfTableCell cell, OdfStyle hyperlinkStyle, boolean isEmail) {
        if (!StringUtils.isEmpty(cell.getStringValue())) {
            try {
                cell.getOdfElement().setStyleName(getCellStyleName(hyperlinkStyle));

                String content = cell.getStringValue();
                cell.setStringValue("");

                applyHyperlink(table, cell, content, isEmail);
            } catch (IllegalArgumentException e) {
                LOG.debug(
                        "Can't set Hyperlink for cell value " + cell.getStringValue() + " it gets rejected as URI", e);
            }
        }
    }

    /**
     * Create data cells in row.
     *
     * @param row         Row.
     * @param startColumn Starting column.
     * @param cellStyle   Cell style.
     * @param names       Name of cell values.
     * @return Array of created table cells.
     */
    private static OdfTableCell[] createDataCellsInRow(
            OdfTableRow row, int startColumn, OdfStyle cellStyle, String... names) {
        OdfTableCell[] result = new OdfTableCell[names.length];
        for (int i = 0; i < names.length; i++) {
            OdfTableCell cell = row.getCellByIndex(startColumn + i);
            cell.setValueType("string");
            if (cellStyle != null) {
                cell.getOdfElement().setStyleName(getCellStyleName(cellStyle));
            }
            if (!StringUtils.isEmpty(names[i])) {
                final String value;
                final int maxCellStringLength = Short.MAX_VALUE;
                if (names[i].length() > maxCellStringLength) {
                    value = names[i].substring(0, maxCellStringLength - 3) + "...";
                } else {
                    value = names[i];
                }
                cell.setStringValue(value);
            }
            result[i] = cell;
        }
        return result;
    }

    /**
     * Fills cells with empty strings, so they get created and don't copy the previous rows content and <b>style</b>,
     * like the header's background color and bold border.
     *
     * @param row         Row.
     * @param startColumn Starting column (inclusive).
     * @param cellStyle   Cell style.
     * @param count       Number of columns to set.
     */
    private static void createDataCellsInRow(OdfTableRow row, int startColumn, OdfStyle cellStyle, int count) {
        for (int i = 0; i < count; i++) {
            OdfTableCell cell = row.getCellByIndex(startColumn + i);
            cell.setValueType("string");
            if (cellStyle != null) {
                cell.getOdfElement().setStyleName(getCellStyleName(cellStyle));
            }
            cell.setStringValue("");
        }
    }

    private static String getCellStyleName(OdfStyle cellStyle) {
        return cellStyle.getAttributes().item(1).getNodeValue();
    }

    /**
     * Create cells for InfoFile content.
     *
     * @param row            The row to insert cells into.
     * @param styleName      Name of the style.
     * @param startPositions The start position of the 3 columns for an InfoFile.
     */
    private static void createInfoFileCellsInRow(OdfTableRow row, String styleName, int... startPositions) {
        for (int startPosition : startPositions) {
            createCellsInRow(row, startPosition, styleName, "Content", "Extracted copyright lines", "File");
        }
    }

    private static void createCellsInRow(OdfTableRow row, int startColumn, String styleName, String... names) {
        for (int i = 0; i < names.length; i++) {
            OdfTableCell cell = row.getCellByIndex(startColumn + i);
            cell.setValueType("string");
            cell.getOdfElement().setStyleName(styleName);
            cell.setStringValue(names[i]);
        }
    }

    private static void createMergedCellsInRow(
            OdfTable table,
            int startColumn,
            int endColumn,
            OdfTableRow row,
            String cellValue,
            int rowIndex,
            String styleName) {
        OdfTableCell cell = createCellsInRow(startColumn, endColumn, row);
        final boolean merge = endColumn - 1 > startColumn;

        if (merge) {
            OdfTableCellRange cellRange = table.getCellRangeByPosition(startColumn, rowIndex, endColumn - 1, rowIndex);
            cellRange.merge();
        }

        // Set value and style only after merge
        cell.setStringValue(cellValue);
        cell.getOdfElement().setStyleName(styleName);

        // TODO: Add grouping, with a hierarchy, after ODFToolkit offers it.
    }

    private static OdfTableCell createCellsInRow(int startColumn, int exclusiveEndColumn, OdfTableRow inRow) {
        OdfTableCell firstCell = null;
        for (int i = startColumn; i < exclusiveEndColumn; i++) {
            OdfTableCell cell = inRow.getCellByIndex(i);
            if (i == startColumn) {
                firstCell = cell;
            }
        }
        return firstCell;
    }

    /**
     * Parameters for cells which apply to all cells in each loop iteration.
     */
    private static class CellListParameter {
        private final OdfTable sheet;
        private final Map<Integer, OdfTableRow> rows;
        private final OdfStyle cellStyle;

        private CellListParameter(OdfTable sheet, Map<Integer, OdfTableRow> rows, OdfStyle cellStyle) {
            this.sheet = sheet;
            this.rows = rows;
            this.cellStyle = cellStyle;
        }

        OdfTable getSheet() {
            return sheet;
        }

        Map<Integer, OdfTableRow> getRows() {
            return rows;
        }

        OdfStyle getCellStyle() {
            return cellStyle;
        }
    }
}
