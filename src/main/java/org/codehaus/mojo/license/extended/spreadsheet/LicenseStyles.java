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

import java.util.EnumMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.mojo.license.download.LicenseClassifier.LicenseMatch;

/**
 * The cell styles a highlighted license is written in. A workbook may hold only a limited number of styles, so one
 * style per category and row background is created up front rather than one per cell.
 *
 * @since 2.8.0
 */
final class LicenseStyles {

    private static final BorderStyle MATCHED_LICENSE_BORDER_STYLE = BorderStyle.MEDIUM;

    private final Map<LicenseMatch, CellStyle> normalRow = new EnumMap<>(LicenseMatch.class);
    private final Map<LicenseMatch, CellStyle> grayRow = new EnumMap<>(LicenseMatch.class);

    private LicenseStyles() {}

    /**
     * Creates the styles a formatting needs, or an empty set of styles when it highlights nothing.
     *
     * @param wb                   the workbook the styles belong to
     * @param alternatingRowsColor the background of every other row
     * @param formatting           how the licenses are marked up
     * @return the styles
     */
    static LicenseStyles of(XSSFWorkbook wb, XSSFColor alternatingRowsColor, SpreadsheetFormatting formatting) {
        final LicenseStyles styles = new LicenseStyles();
        if (!formatting.highlights()) {
            return styles;
        }
        for (LicenseMatch licenseMatch : LicenseMatch.values()) {
            final XSSFColor color = color(wb, SpreadsheetUtil.licenseColor(licenseMatch));
            styles.normalRow.put(licenseMatch, createStyle(wb, null, color, formatting.hasBorder()));
            styles.grayRow.put(licenseMatch, createStyle(wb, alternatingRowsColor, color, formatting.hasBorder()));
        }
        return styles;
    }

    /**
     * Writes a cell in the colour of its license category.
     *
     * @param cell         the cell holding the license name
     * @param licenseMatch the category, or {@code null} to leave the cell as it is
     * @param gray         whether the cell is on a row with the alternating background
     */
    void apply(Cell cell, LicenseMatch licenseMatch, boolean gray) {
        if (licenseMatch == null) {
            return;
        }
        final CellStyle style = (gray ? grayRow : normalRow).get(licenseMatch);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static XSSFColor color(XSSFWorkbook wb, int[] rgb) {
        return new XSSFColor(
                new byte[] {(byte) rgb[0], (byte) rgb[1], (byte) rgb[2]},
                wb.getStylesSource().getIndexedColors());
    }

    private static CellStyle createStyle(
            XSSFWorkbook wb, XSSFColor backgroundColor, XSSFColor fontColor, boolean border) {
        final XSSFFont font = wb.createFont();
        font.setColor(fontColor);
        final XSSFCellStyle style = wb.createCellStyle();
        style.setFont(font);
        if (backgroundColor != null) {
            style.setFillForegroundColor(backgroundColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (border) {
            style.setBorderLeft(MATCHED_LICENSE_BORDER_STYLE);
            style.setBorderTop(MATCHED_LICENSE_BORDER_STYLE);
            style.setBorderRight(MATCHED_LICENSE_BORDER_STYLE);
            style.setBorderBottom(MATCHED_LICENSE_BORDER_STYLE);
            style.setLeftBorderColor(fontColor);
            style.setTopBorderColor(fontColor);
            style.setRightBorderColor(fontColor);
            style.setBottomBorderColor(fontColor);
        }
        return style;
    }
}
