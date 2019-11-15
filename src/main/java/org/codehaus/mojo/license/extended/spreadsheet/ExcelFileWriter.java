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
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.codehaus.mojo.license.extended.InfoFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Writes project license infos into Excel file.
 */
public class ExcelFileWriter
{
    private static final Logger LOG = LoggerFactory.getLogger( ExcelFileWriter.class );

    // Columns values for Maven data, including separator gaps for data grouping.
    private static final int PLUGIN_ID_COLUMNS = 3;
    // "Start" column are the actual start columns, they are inclusive.
    // "End" columns point just one column behind the last one, it's the exclusive column index.
    private static final int LICENSES_START_COLUMN = PLUGIN_ID_COLUMNS + 1, LICENSES_COLUMNS = 5,
            LICENSES_END_COLUMN = LICENSES_START_COLUMN + LICENSES_COLUMNS;
    private static final int DEVELOPERS_START_COLUMN = LICENSES_END_COLUMN + 1, DEVELOPER_COLUMNS = 7,
            DEVELOPERS_END_COLUMN = DEVELOPERS_START_COLUMN + DEVELOPER_COLUMNS;
    private static final int MISC_START_COLUMN = DEVELOPERS_END_COLUMN + 1, MISC_COLUMNS = 4,
            MISC_END_COLUMN = MISC_START_COLUMN + MISC_COLUMNS;

    private static final int MAVEN_DATA_COLUMNS = PLUGIN_ID_COLUMNS + LICENSES_COLUMNS
            + DEVELOPER_COLUMNS + MISC_COLUMNS,
            MAVEN_COLUMN_GROUPING_GAPS = 3;
    private static final int MAVEN_COLUMNS = MAVEN_DATA_COLUMNS + MAVEN_COLUMN_GROUPING_GAPS;

    // Columns values for JAR data, including separator gaps for data grouping.
    private static final int INFO_FILES_GAPS = 2, MANIFEST_GAPS = 1;
    private static final int MANIFEST_START_COLUMN = MAVEN_COLUMNS + 1, MANIFEST_COLUMNS = 3,
            MANIFEST_END_COLUMN = MANIFEST_START_COLUMN + MANIFEST_COLUMNS;
    private static final int INFO_NOTICES_START_COLUMN = MANIFEST_END_COLUMN + 1, INFO_NOTICES_COLUMNS = 3,
            INFO_NOTICES_END_COLUMN = INFO_NOTICES_START_COLUMN + INFO_NOTICES_COLUMNS;
    private static final int INFO_LICENSES_START_COLUMN = INFO_NOTICES_END_COLUMN + 1, INFO_LICENSES_COLUMNS = 3,
            INFO_LICENSES_END_COLUMN = INFO_LICENSES_START_COLUMN + INFO_LICENSES_COLUMNS;
    private static final int INFO_SPDX_START_COLUMN = INFO_LICENSES_END_COLUMN + 1, INFO_SPDX_COLUMNS = 3,
            INFO_SPDX_END_COLUMN = INFO_SPDX_START_COLUMN + INFO_SPDX_COLUMNS;
    private static final int EXTENDED_INFO_COLUMNS = MANIFEST_COLUMNS
            + INFO_NOTICES_COLUMNS + INFO_LICENSES_COLUMNS + INFO_SPDX_COLUMNS
            + INFO_FILES_GAPS + MANIFEST_GAPS;

    // Width of gap columns
    private static final int GAP_WIDTH = 20;

    /**
     * Writes list of projects into excel file.
     *
     * @param projectLicenseInfos     Project license infos to write.
     * @param licensesExcelOutputFile Excel output file in latest format (OOXML).
     */
    public static void write( List<ProjectLicenseInfo> projectLicenseInfos, final File licensesExcelOutputFile )
    {
        if ( CollectionUtils.isEmpty( projectLicenseInfos ) )
        {
            LOG.debug( "Nothing to write to excel, no project data." );
            return;
        }

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet( WorkbookUtil.createSafeSheetName( "License information" ) );

        createHeader( projectLicenseInfos, wb, sheet );

        writeData( projectLicenseInfos, wb, sheet );

        try ( OutputStream fileOut = new FileOutputStream( licensesExcelOutputFile ) )
        {
            wb.write( fileOut );
        } catch ( IOException e )
        {
            LOG.error( "Error on storing Excel file with license and other information", e );
        }
    }

    private static void writeData( List<ProjectLicenseInfo> projectLicenseInfos, Workbook wb, Sheet sheet )
    {
        final int firstRowIndex = 3;
        int currentRowIndex = firstRowIndex;
        final Map<Integer, Row> rows = new HashMap<>();
        boolean hasExtendedInfo = false;
        for ( ProjectLicenseInfo projectInfo : projectLicenseInfos )
        {
            int extraRows = 0;
            Row currentRow = sheet.createRow( currentRowIndex );
            rows.put( currentRowIndex, currentRow );
            // Plugin ID
            createCellsInRow( currentRow, 0,
                    projectInfo.getGroupId(), projectInfo.getArtifactId(), projectInfo.getVersion() );
            // Licenses
            extraRows = addCollection( sheet, rows, currentRowIndex, extraRows,
                    projectInfo.getLicenses(),
                    ( Row licenseRow, ProjectLicense license ) -> {
                        Cell[] licenses = createCellsInRow( licenseRow, LICENSES_START_COLUMN,
                                license.getName(), license.getUrl(),
                                license.getDistribution(), license.getComments(),
                                license.getFile() );
                        addHyperlinkIfExists( wb, licenses[ 1 ], HyperlinkType.URL );
                    } );

            final ExtendedInfo extendedInfo = projectInfo.getExtendedInfo();
            if ( extendedInfo != null )
            {
                hasExtendedInfo = true;
                // Developers
                extraRows = addCollection( sheet, rows, currentRowIndex, extraRows,
                        extendedInfo.getDevelopers(),
                        ( Row developerRow, Developer developer ) -> {
                            Cell[] licenses = createCellsInRow( developerRow, DEVELOPERS_START_COLUMN,
                                    developer.getId(), developer.getEmail(),
                                    developer.getName(),
                                    developer.getOrganization(), developer.getOrganizationUrl(),
                                    developer.getUrl(), developer.getTimezone() );
                            addHyperlinkIfExists( wb, licenses[ 1 ], HyperlinkType.EMAIL );
                            addHyperlinkIfExists( wb, licenses[ 4 ], HyperlinkType.URL );
                            addHyperlinkIfExists( wb, licenses[ 5 ], HyperlinkType.URL );
                        } );
                // Miscellaneous
                Cell[] miscCells = createCellsInRow( currentRow, MISC_START_COLUMN,
                        extendedInfo.getInceptionYear(),
                        Optional.ofNullable( extendedInfo.getOrganization() )
                                .map( Organization::getName )
                                .orElse( null ),
                        Optional.ofNullable( extendedInfo.getScm() )
                                .map( Scm::getUrl )
                                .orElse( null ),
                        extendedInfo.getUrl() );
                addHyperlinkIfExists( wb, miscCells[ 2 ], HyperlinkType.URL );
                addHyperlinkIfExists( wb, miscCells[ 3 ], HyperlinkType.URL );

                // MANIFEST.MF
                createCellsInRow( currentRow, MANIFEST_START_COLUMN,
                        extendedInfo.getBundleLicense(), extendedInfo.getBundleVendor(),
                        extendedInfo.getImplementationVendor() );

                // Info files
                if ( !CollectionUtils.isEmpty( extendedInfo.getInfoFiles() ) )
                {
                    // Sort all info files by type into 3 different lists, each list for each of the 3 types.
                    List<InfoFile> notices = new ArrayList<>();
                    List<InfoFile> licenses = new ArrayList<>();
                    List<InfoFile> spdxs = new ArrayList<>();
                    extendedInfo.getInfoFiles().forEach( infoFile -> {
                                switch ( infoFile.getType() )
                                {
                                    case LICENSE:
                                        licenses.add( infoFile );
                                        break;
                                    case NOTICE:
                                        notices.add( infoFile );
                                        break;
                                    case SPDX_LICENSE:
                                        spdxs.add( infoFile );
                                        break;
                                    default:
                                        break;
                                }
                            }
                    );
                    // InfoFile notices text file
                    extraRows = addInfoFileList( sheet, rows, currentRowIndex, extraRows,
                            INFO_NOTICES_START_COLUMN, notices );
                    // InfoFile licenses text file
                    extraRows = addInfoFileList( sheet, rows, currentRowIndex, extraRows,
                            INFO_LICENSES_START_COLUMN, licenses );
                    // InfoFile spdx licenses text file
                    extraRows = addInfoFileList( sheet, rows, currentRowIndex, extraRows,
                            INFO_SPDX_START_COLUMN, spdxs );
                }
            }
            currentRowIndex += extraRows + 1;
        }

        autosizeColumns( sheet, hasExtendedInfo );
    }

    private static void createHeader( List<ProjectLicenseInfo> projectLicenseInfos, Workbook wb, Sheet sheet )
    {
        int columns = MAVEN_COLUMNS;
        boolean hasExtendedInfo = false;
        for ( ProjectLicenseInfo projectLicenseInfo : projectLicenseInfos )
        {
            if ( projectLicenseInfo.getExtendedInfo() != null )
            {
                columns += EXTENDED_INFO_COLUMNS;
                hasExtendedInfo = true;
                break;
            }
        }

        // Create header style
        CellStyle headerCellStyle = wb.createCellStyle();
        headerCellStyle.setFillBackgroundColor( IndexedColors.GREY_25_PERCENT.getIndex() );
        Font headerFont = new XSSFFont();
        headerFont.setBold( true );
        headerCellStyle.setFont( headerFont );
        setBorderStyle( headerCellStyle, BorderStyle.MEDIUM );

        // Create 1st header row. The Maven/JAR header row
        Row mavenJarRow = sheet.createRow( 0 );

        // Create Maven header cell
        createMergedCellsInRow( sheet, 0, MAVEN_COLUMNS, headerCellStyle, mavenJarRow, "Maven information", 0 );

        if ( hasExtendedInfo )
        {
            // Create JAR header cell
            Cell jarHeaderCell = mavenJarRow.createCell( 0 );
            jarHeaderCell.setCellValue( "JAR Content" );
            jarHeaderCell.setCellStyle( headerCellStyle );
            createCellsInRow( MAVEN_COLUMNS, columns, mavenJarRow );
            CellRangeAddress jarHeaderAddress = new CellRangeAddress( 0, 0, MAVEN_COLUMNS, columns - 1 );
            sheet.addMergedRegion( jarHeaderAddress );
        }

        // Create 2nd header row
        Row secondHeaderRow = sheet.createRow( 1 );

        // Create Maven "Plugin ID" header
        createMergedCellsInRow( sheet, 0, PLUGIN_ID_COLUMNS, headerCellStyle, secondHeaderRow, "Plugin ID", 1 );

        // Create Maven "Licenses" header
        createMergedCellsInRow( sheet, LICENSES_START_COLUMN, LICENSES_END_COLUMN, headerCellStyle, secondHeaderRow,
                "Licenses", 1 );

        // Gap "Plugin ID" <-> "Licenses".
        sheet.setColumnWidth( PLUGIN_ID_COLUMNS, GAP_WIDTH );

        // Create Maven "Developers" header
        createMergedCellsInRow( sheet, DEVELOPERS_START_COLUMN, DEVELOPERS_END_COLUMN, headerCellStyle,
                secondHeaderRow,
                "Developers", 1 );

        // Gap "Licenses" <-> "Developers".
        sheet.setColumnWidth( LICENSES_END_COLUMN, GAP_WIDTH );

        // Create Maven "Miscellaneous" header
        createMergedCellsInRow( sheet, MISC_START_COLUMN, MISC_END_COLUMN, headerCellStyle,
                secondHeaderRow,
                "Miscellaneous", 1 );

        // Gap "Developers" <-> "Miscellaneous".
        sheet.setColumnWidth( DEVELOPERS_END_COLUMN, GAP_WIDTH );

        if ( hasExtendedInfo )
        {
            createMergedCellsInRow( sheet, MANIFEST_START_COLUMN, MANIFEST_END_COLUMN, headerCellStyle,
                    secondHeaderRow,
                    "MANIFEST.MF", 1 );

            // Gap "Miscellaneous" <-> "MANIFEST.MF".
            sheet.setColumnWidth( DEVELOPERS_END_COLUMN, GAP_WIDTH );

            createMergedCellsInRow( sheet, INFO_NOTICES_START_COLUMN, INFO_NOTICES_END_COLUMN, headerCellStyle,
                    secondHeaderRow,
                    "Notices text files", 1 );

            // Gap "MANIFEST.MF" <-> "Notice text files".
            sheet.setColumnWidth( MANIFEST_END_COLUMN, GAP_WIDTH );

            createMergedCellsInRow( sheet, INFO_LICENSES_START_COLUMN, INFO_LICENSES_END_COLUMN, headerCellStyle,
                    secondHeaderRow,
                    "License text files", 1 );

            // Gap "Notice text files" <-> "License text files".
            sheet.setColumnWidth( INFO_LICENSES_END_COLUMN, GAP_WIDTH );

            createMergedCellsInRow( sheet, INFO_SPDX_START_COLUMN, INFO_SPDX_END_COLUMN, headerCellStyle,
                    secondHeaderRow,
                    "SPDX license id matched", 1 );
        }
//        sheet.setColumnGroupCollapsed();

        // Create 3rd header row
        Row thirdHeaderRow = sheet.createRow( 2 );

        // Plugin ID
        createCellsInRow( thirdHeaderRow, 0, headerCellStyle, "Group ID", "Artifact ID", "Version" );
        // Licenses
        createCellsInRow( thirdHeaderRow, LICENSES_START_COLUMN, headerCellStyle,
                "Name", "URL", "Distribution", "Comments", "File" );
        // Developers
        createCellsInRow( thirdHeaderRow, DEVELOPERS_START_COLUMN, headerCellStyle,
                "Id", "Email", "Name", "Organization", "Organization URL", "URL", "Timezone" );
        // Miscellaneous
        createCellsInRow( thirdHeaderRow, MISC_START_COLUMN, headerCellStyle,
                "Inception Year", "Organization", "SCM", "URL" );

        if ( hasExtendedInfo )
        {
            // MANIFEST.MF
            createCellsInRow( thirdHeaderRow, MANIFEST_START_COLUMN, headerCellStyle,
                    "Bundle license", "Bundle vendor", "Implementation vendor" );
            // 3 InfoFile groups: Notices, Licenses and SPDX-Licenses.
            createInfoFileCellsInRow( thirdHeaderRow, headerCellStyle,
                    INFO_NOTICES_START_COLUMN, INFO_LICENSES_START_COLUMN, INFO_SPDX_START_COLUMN );
        }
    }

    private static void autosizeColumns( Sheet sheet, boolean hasExtendedInfo )
    {
        autosizeColumns( sheet,
                new ImmutablePair<>( 0, PLUGIN_ID_COLUMNS ),
                new ImmutablePair<>( LICENSES_START_COLUMN, LICENSES_END_COLUMN ),
                new ImmutablePair<>( DEVELOPERS_START_COLUMN, DEVELOPERS_END_COLUMN - 1 ),
                new ImmutablePair<>( MISC_START_COLUMN + 1, MISC_END_COLUMN )
        );
        if ( hasExtendedInfo )
        {
            autosizeColumns( sheet,
                    new ImmutablePair<>( MANIFEST_START_COLUMN, MANIFEST_END_COLUMN ),
                    new ImmutablePair<>( INFO_NOTICES_START_COLUMN + 2, INFO_NOTICES_END_COLUMN ),
                    new ImmutablePair<>( INFO_LICENSES_START_COLUMN + 2, INFO_LICENSES_END_COLUMN ),
                    new ImmutablePair<>( INFO_SPDX_START_COLUMN + 2, INFO_SPDX_END_COLUMN )
            );
        }
    }

    private static void autosizeColumns( Sheet sheet, ImmutablePair<Integer, Integer>... ranges )
    {
        for ( ImmutablePair<Integer, Integer> pair : ranges )
        {
            for ( int i = pair.left; i < pair.right; i++ )
            {
                sheet.autoSizeColumn( i );
            }
        }
    }

    private static int addInfoFileList( Sheet sheet, Map<Integer, Row> rows, int currentRowIndex, int extraRows,
                                        int startColumn, List<InfoFile> infoFiles )
    {
        return addCollection( sheet, rows, currentRowIndex, extraRows, infoFiles,
                ( Row infoFileRow, InfoFile infoFile ) -> {
                    final String copyrightLines = Optional
                            .ofNullable( infoFile.getExtractedCopyrightLines() )
                            .map( strings -> String.join( "§", strings ) )
                            .orElse( null );
                    createCellsInRow( infoFileRow, startColumn,
                            infoFile.getContent(), copyrightLines,
                            infoFile.getFileName() );
                } );
    }

    private static <T> int addCollection( Sheet sheet, Map<Integer, Row> rows, int currentRowIndex, int extraRows,
                                          List<T> list, BiConsumer<Row, T> biConsumer )
    {
        if ( !CollectionUtils.isEmpty( list ) )
        {
            for ( int i = 0; i < list.size(); i++ )
            {
                T license = list.get( i );
                Integer index = currentRowIndex + i;
                Row row = rows.get( index );
                if ( row == null )
                {
                    row = sheet.createRow( index );
                    rows.put( index, row );
                    extraRows++;
                }
                biConsumer.accept( row, license );
            }
        }
        return extraRows;
    }

    private static void addHyperlinkIfExists( Workbook workbook, Cell cell, HyperlinkType hyperlinkType )
    {
        if ( !StringUtils.isEmpty( cell.getStringCellValue() ) )
        {
            Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink( hyperlinkType );
            try
            {
                hyperlink.setAddress( cell.getStringCellValue() );
                cell.setHyperlink( hyperlink );
            } catch ( IllegalArgumentException e )
            {
                LOG.debug( "Can't set Hyperlink for cell value " + cell.getStringCellValue()
                        + " it gets rejected as URI", e );
            }
        }
    }

    private static Cell[] createCellsInRow( Row row, int startColumn, String... names )
    {
        Cell[] result = new Cell[ names.length ];
        for ( int i = 0; i < names.length; i++ )
        {
            Cell cell = row.createCell( startColumn + i, CellType.STRING );
            cell.setCellValue( names[ i ] );
            result[ i ] = cell;
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
    private static void createInfoFileCellsInRow( Row row, CellStyle cellStyle, int... startPositions )
    {
        for ( int startPosition : startPositions )
        {
            createCellsInRow( row, startPosition, cellStyle, "Content", "Extracted copyright lines", "File" );
        }
    }

    private static void createCellsInRow( Row row, int startColumn, CellStyle cellStyle, String... names )
    {
        for ( int i = 0; i < names.length; i++ )
        {
            Cell cell = row.createCell( startColumn + i );
            cell.setCellStyle( cellStyle );
            cell.setCellValue( names[ i ] );
        }
    }

    private static void createMergedCellsInRow( Sheet sheet, int startColumn, int endColumn, CellStyle cellStyle,
                                                Row row, String cellValue, int rowIndex )
    {
        Cell licensesCell = row.createCell( startColumn );
        licensesCell.setCellValue( cellValue );
        licensesCell.setCellStyle( cellStyle );
        createCellsInRow( startColumn + 1, endColumn, row );
        CellRangeAddress licensesHeaderAddress = new CellRangeAddress( rowIndex, rowIndex, startColumn, endColumn - 1 );
        sheet.addMergedRegion( licensesHeaderAddress );
        sheet.groupColumn( startColumn, endColumn - 1 );
    }

    private static void createCellsInRow( int startColumn, int exclusiveEndColumn, Row inRow )
    {
        for ( int i = startColumn; i < exclusiveEndColumn; i++ )
        {
            inRow.createCell( i );
        }
    }

    private static void setBorderStyle( CellStyle cellStyle, BorderStyle borderStyle )
    {
        cellStyle.setBorderLeft( borderStyle );
        cellStyle.setBorderTop( borderStyle );
        cellStyle.setBorderRight( borderStyle );
        cellStyle.setBorderBottom( borderStyle );
    }
}