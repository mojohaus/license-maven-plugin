package org.codehaus.mojo.license.extended.spreadsheet;

import java.io.File;
import java.util.List;

import org.codehaus.mojo.license.download.ProjectLicenseInfo;

/**
 * Writes LibreOffice Calc ODS file.
 */
public class CalcFileWriter {

    private CalcFileWriter() {}

    public static void write(List<ProjectLicenseInfo> projectLicenseInfos, final File licensesCalcOutputFile) {
        throw new UnsupportedOperationException("Write LibreOffice Calc file (ODS) requires JDK 11+");
    }
}
