/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
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

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.doc.table.OdfTable

import java.util.logging.Level
import java.util.logging.Logger

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

log = Logger.getLogger("test-aggregate-download-licenses-extended-spreadsheet")

static boolean searchTextInExcel(Sheet sheet, String searchText) {
    def log2 = Logger.getLogger("test-aggregate-download-licenses-extended-spreadsheet-search")

    for (Iterator<Row> rowIterator = sheet.rowIterator(); rowIterator.hasNext();) {
        Row row = rowIterator.next()
        for (Iterator<Cell> cellIterator = row.cellIterator(); cellIterator.hasNext();) {
            Cell cell = cellIterator.next()
            if (cell.cellType == CellType.STRING || cell.cellType == CellType.BLANK) {
                def cellValue = cell.stringCellValue
                if (cellValue == searchText) {
                    return true
                } else {
                    log2.log(Level.FINEST, "Cell Value: {0}", cellValue)
                }
            }
        }
    }
    return false
}

// -------------- Excel ----------------------
excelFile = new File(basedir, 'target/generated-resources/licenses.xlsx')
assert excelFile.exists()
assert excelFile.length() > 100

try (InputStream input = new FileInputStream(excelFile)) {
    // So it can be easily opened and inspected manually. In a modern IDE it's just a (double-)click in the log output.
    log.log(Level.FINE, "Excel export at: {}", excelFile.absolutePath)
    Workbook workbook = WorkbookFactory.create(input)
    Sheet sheet = workbook.getSheetAt(0)

    assert searchTextInExcel(sheet, "Maven information")
    assert searchTextInExcel(sheet, "The Apache Software License, Version 2.0")
    assert searchTextInExcel(sheet, "The Apache Software Foundation")
}

// -------------- Calc -----------------

calcFile = new File(basedir, 'target/generated-resources/licenses.ods')
assert calcFile.exists()
assert calcFile.length() > 100

try (OdfSpreadsheetDocument spreadsheet = OdfSpreadsheetDocument.loadDocument(calcFile)) {
    // So it can be easily opened and inspected manually. In a modern IDE it's just a (double-)click in the log output.
    log.log(Level.FINE, "Calc export at: {}", calcFile.absolutePath)
    List<OdfTable> tableList = spreadsheet.getTableList()
    OdfTable table = tableList.get(0)
    assert table.getRowCount() >= 3
}

// ----------- Check for XSD file ----------------
licensesXsd = new File(basedir, 'target/generated-resources/licenses.xsd')
assert licensesXsd.exists()