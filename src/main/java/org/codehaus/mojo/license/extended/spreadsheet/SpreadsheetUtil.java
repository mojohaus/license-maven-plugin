package org.codehaus.mojo.license.extended.spreadsheet;

/**
 * Utility class to build spreadsheets.
 */
class SpreadsheetUtil {
    static final String TABLE_NAME = "License information";
    static final String VALID_LINK = "\\bhttps?://\\S+";

    // Columns values for Maven data, including separator gaps for data grouping.
    static final int GENERAL_START_COLUMN = 0;
    private static final int GENERAL_COLUMNS = 1;
    static final int GENERAL_END_COLUMN = GENERAL_START_COLUMN + GENERAL_COLUMNS;
    static final int PLUGIN_ID_START_COLUMN = GENERAL_END_COLUMN + 1;
    private static final int PLUGIN_ID_COLUMNS = 3;
    static final int PLUGIN_ID_END_COLUMN = PLUGIN_ID_START_COLUMN + PLUGIN_ID_COLUMNS;
    // "Start" column are the actual start columns, they are inclusive.
    // "End" columns point just one column behind the last one, it's the exclusive column index.
    static final int LICENSES_START_COLUMN = PLUGIN_ID_END_COLUMN + 1;
    static final int LICENSES_COLUMNS = 5;
    static final int LICENSES_END_COLUMN = LICENSES_START_COLUMN + LICENSES_COLUMNS;
    static final int DEVELOPERS_START_COLUMN = LICENSES_END_COLUMN + 1;
    static final int DEVELOPERS_COLUMNS = 7;
    static final int DEVELOPERS_END_COLUMN = DEVELOPERS_START_COLUMN + DEVELOPERS_COLUMNS;
    static final int MISC_START_COLUMN = DEVELOPERS_END_COLUMN + 1;
    static final int MISC_COLUMNS = 4;
    private static final int MAVEN_DATA_COLUMNS =
            GENERAL_COLUMNS + PLUGIN_ID_COLUMNS + LICENSES_COLUMNS + DEVELOPERS_COLUMNS + MISC_COLUMNS;
    static final int MISC_END_COLUMN = MISC_START_COLUMN + MISC_COLUMNS;
    private static final int MAVEN_COLUMN_GROUPING_GAPS = 4;
    private static final int MAVEN_COLUMNS = MAVEN_DATA_COLUMNS + MAVEN_COLUMN_GROUPING_GAPS;
    static final int MANIFEST_START_COLUMN = MAVEN_COLUMNS + 1;
    static final int MAVEN_START_COLUMN = 0;
    static final int MAVEN_END_COLUMN = MAVEN_START_COLUMN + MAVEN_COLUMNS;
    static final int EXTENDED_INFO_START_COLUMN = MAVEN_END_COLUMN + 1;
    // Columns values for JAR data, including separator gaps for data grouping.
    private static final int INFO_FILES_GAPS = 2;
    private static final int MANIFEST_GAPS = 1;
    private static final int MANIFEST_COLUMNS = 3;
    static final int MANIFEST_END_COLUMN = MANIFEST_START_COLUMN + MANIFEST_COLUMNS;
    static final int INFO_NOTICES_START_COLUMN = MANIFEST_END_COLUMN + 1;
    static final int INFO_NOTICES_COLUMNS = 3;
    static final int INFO_NOTICES_END_COLUMN = INFO_NOTICES_START_COLUMN + INFO_NOTICES_COLUMNS;
    static final int INFO_LICENSES_START_COLUMN = INFO_NOTICES_END_COLUMN + 1;
    static final int INFO_LICENSES_COLUMNS = 3;
    static final int INFO_LICENSES_END_COLUMN = INFO_LICENSES_START_COLUMN + INFO_LICENSES_COLUMNS;
    static final int INFO_SPDX_START_COLUMN = INFO_LICENSES_END_COLUMN + 1;
    static final int INFO_SPDX_COLUMNS = 3;
    private static final int EXTENDED_INFO_COLUMNS = MANIFEST_COLUMNS
            + INFO_NOTICES_COLUMNS
            + INFO_LICENSES_COLUMNS
            + INFO_SPDX_COLUMNS
            + INFO_FILES_GAPS
            + MANIFEST_GAPS;
    static final int EXTENDED_INFO_END_COLUMN = EXTENDED_INFO_START_COLUMN + EXTENDED_INFO_COLUMNS;
    static final int INFO_SPDX_END_COLUMN = INFO_SPDX_START_COLUMN + INFO_SPDX_COLUMNS;

    static final int DOWNLOAD_MESSAGE_EXTENDED_COLUMN = INFO_SPDX_END_COLUMN + 1;
    static final int DOWNLOAD_MESSAGE_NOT_EXTENDED_COLUMN = MANIFEST_START_COLUMN;
    static final int DOWNLOAD_MESSAGE_COLUMNS = 1;

    // Width of gap columns
    private static final int EXCEL_WIDTH_SCALE = 256;
    static final int INCEPTION_YEAR_WIDTH = " Inception Year ".length() * EXCEL_WIDTH_SCALE;
    static final int TIMEZONE_WIDTH = " Timezone ".length() * EXCEL_WIDTH_SCALE;
    static final int GAP_WIDTH = 3 * EXCEL_WIDTH_SCALE;
    /**
     * Color must be dark enough for low-contrast monitors.
     * <br>If you get a compiler error here, make sure you're using Java 8, not higher.
     */
    static final int[] ALTERNATING_ROWS_COLOR = new int[] {220, 220, 220};

    static final String COPYRIGHT_JOIN_SEPARATOR = "§";

    static int getDownloadColumn(boolean hasExtendedInfo) {
        return hasExtendedInfo
                ? SpreadsheetUtil.DOWNLOAD_MESSAGE_EXTENDED_COLUMN
                : SpreadsheetUtil.DOWNLOAD_MESSAGE_NOT_EXTENDED_COLUMN;
    }

    /**
     * Parameters which may change constantly.
     */
    static class CurrentRowData {
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
