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
import java.util.logging.Level
import java.util.logging.Logger

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory

log = Logger.getLogger("test-aggregate-download-licenses-extended-excel")

static boolean searchText(Sheet sheet, String searchText) {
    def log2 = Logger.getLogger("test-aggregate-download-licenses-extended-excel-search")

    for (Iterator<Row> rowIterator = sheet.rowIterator(); rowIterator.hasNext();) {
        Row row = rowIterator.next()
        for (Iterator<Cell> cellIterator = row.cellIterator(); cellIterator.hasNext();) {
            Cell cell = cellIterator.next()
            if (cell.cellType == CellType.STRING || cell.cellType == CellType.BLANK) {
                def cellValue = cell.stringCellValue
                if (cellValue == searchText) {
                    return true
                } else {
                    log2.log(Level.FINEST, "Cell Value: {}", cellValue)
                }
            }
        }
    }
    return false
}

file = new File(basedir, 'target/generated-resources/licenses.xlsx');
assert file.exists()
assert file.length() > 100

searchText = "YourSearchText"
input = new FileInputStream(file)
// So it can be easily opened and inspected manually. In a modern IDE it's just a (double-)click in the log output.
log.log(Level.FINE, "Excel export at: {}", file.absolutePath)
workbook = WorkbookFactory.create(input)
Sheet sheet = workbook.getSheetAt(0)

assert searchText(sheet, "Maven information")
assert searchText(sheet, "Apache Software Foundation")
assert searchText(sheet, "The Apache Software Foundation")

input.close()