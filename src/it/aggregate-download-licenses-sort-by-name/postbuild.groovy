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

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.doc.table.OdfTable
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Path
import java.nio.file.Paths
import java.security.Policy
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

log = Logger.getLogger("test-aggregate-download-licenses-extended-spreadsheet")

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
class DependencyInfos {
    @XmlElement
    List<DependencyInfo> dependencyInfos

    DependencyInfos() {
        this.dependencyInfos = new ArrayList<>()
    }

    DependencyInfos(List<DependencyInfo> dependencyInfos) {
        this.dependencyInfos = dependencyInfos
    }
}

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
class DependencyInfo {
    @XmlAttribute
    String name

    @XmlAttribute
    String groupId

    @XmlAttribute
    String artifactId

    @XmlAttribute
    String version

    @XmlAttribute
    String license

    DependencyInfo() {}

    DependencyInfo(String name, String groupId, String artifactId, String version, String license) {
        this.name = name
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
        this.license = license
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof DependencyInfo)) {
            return false
        }
        DependencyInfo that = (DependencyInfo) o
        return Objects.equals(name, that.name)
            && Objects.equals(groupId, that.groupId)
            && Objects.equals(artifactId, that.artifactId)
            && Objects.equals(version, that.version)
            && Objects.equals(license, that.license)
    }

    @Override
    int hashCode() {
        return Objects.hash(name, groupId, artifactId, version, license)
    }

    @Override
    String toString() {
        return "DependencyInfo{" +
            "name='" + name + '\'' +
            ", groupId='" + groupId + '\'' +
            ", artifactId='" + artifactId + '\'' +
            ", version='" + version + '\'' +
            ", license='" + license + '\'' +
            '}'
    }
}

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
excelFile = new File(basedir, 'target/generated-resources/licenses - orderByDependencyName.xlsx')
assert excelFile.exists()
assert excelFile.length() > 100

try (InputStream input = new FileInputStream(excelFile)) {
    // So it can be easily opened and inspected manually. In a modern IDE it's just a (double-)click in the log output.
    log.log(Level.FINE, "Excel export at: {}", excelFile.absolutePath)
    Workbook workbook = WorkbookFactory.create(input)
    Sheet sheet = workbook.getSheetAt(0)

    assert searchTextInExcel(sheet, "Maven information")
    assert searchTextInExcel(sheet, "Apache License, Version 2.0")
    assert searchTextInExcel(sheet, "EPL 1.0")
}

// -------------- Calc -----------------

calcFile = new File(basedir, 'target/generated-resources/licenses - orderByDependencyName.ods')
assert calcFile.exists()
assert calcFile.length() > 100

try (OdfSpreadsheetDocument spreadsheet = OdfSpreadsheetDocument.loadDocument(calcFile)) {
    // So it can be easily opened and inspected manually. In a modern IDE it's just a (double-)click in the log output.
    log.log(Level.FINE, "Calc export at: {}", calcFile.absolutePath)
    List<OdfTable> tableList = spreadsheet.getTableList()
    OdfTable table = tableList.get(0)
    assert table.getRowCount() >= 3
}

checkResultingLicensesXml()

// -------------- Sort by name -------------------
private void checkResultingLicensesXml()
    throws ParserConfigurationException, SAXException, IOException, JAXBException {
    Path testPath = basedir.toPath()
    Path generatedResourcesPath = Paths.get(testPath.toString(), "target/generated-resources")
    Path licensesPath = Paths.get(generatedResourcesPath.toString(), "licenses.xml")

    File licensesFile = licensesPath.toFile()
    if (!licensesFile.exists()) {
        throw new FileNotFoundException("Licenses file not found: " + licensesFile.getAbsolutePath())
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
    DocumentBuilder builder = factory.newDocumentBuilder()
    Document document = builder.parse(licensesFile)

    NodeList dependenciesRoot = document.getElementsByTagName("dependencies")
    assertEquals(1, dependenciesRoot.getLength())
    NodeList dependencies = dependenciesRoot.item(0).getChildNodes()
    if (dependencies.getLength() == 0) {
        throw new IllegalArgumentException("No dependencies found in: " + licensesFile.getAbsolutePath())
    }
    List<DependencyInfo> dependencyInfos = new ArrayList<>()
    for (int i = 0; i < dependencies.getLength(); i++) {
        if (dependencies.item(i).getNodeName().equals("dependency")) {
            NodeList dependency = dependencies.item(i).getChildNodes()
            String name = null
            String groupId = null
            String artifactId = null
            String version = null
            String license = null
            for (int j = 0; j < dependency.getLength(); j++) {
                if (dependency.item(j).getNodeName().equals("name")) {
                    name = dependency.item(j).getTextContent()
                } else if (dependency.item(j).getNodeName().equals("groupId")) {
                    groupId = dependency.item(j).getTextContent()
                } else if (dependency.item(j).getNodeName().equals("artifactId")) {
                    artifactId = dependency.item(j).getTextContent()
                } else if (dependency.item(j).getNodeName().equals("version")) {
                    version = dependency.item(j).getTextContent()
                } else if (dependency.item(j).getNodeName().equals("licenses")) {
                    Node licensesNode = dependency.item(j)
                    List<String> licenses = new ArrayList<>()
                    for (int k = 0; k < licensesNode.getChildNodes().getLength(); k++) {
                        Node licenseNode = licensesNode.getChildNodes().item(k)
                        if (licenseNode.getNodeName().equals("license")) {
                            for (int l = 0; l < licenseNode.getChildNodes().getLength(); l++) {
                                Node licenseChild =
                                    licenseNode.getChildNodes().item(l)
                                if (licenseChild.getNodeName().equals("name")) {
                                    licenses.add(licenseChild.getTextContent())
                                }
                            }
                        }
                    }
                    if (!licenses.isEmpty()) {
                        licenses.sort(Comparator.naturalOrder())
                        license = licenses.get(0)
                    }
                }
            }
            assertNotNull(groupId)
            assertNotNull(artifactId)
            assertNotNull(version)
            dependencyInfos.add(new DependencyInfo(name, groupId, artifactId, version, license))
            if (name == null) {
                System.out.println("Dependency without name: " + groupId + ":" + artifactId + ":" + version)
            } else {
                System.out.println("Dependency: " + name + " (" + groupId + ":" + artifactId + ":" + version
                    + ") - " + license)
            }
        }
    }

    /*
    Comment this line in, if there have been changes in the data sorting and new files to check against, must be
    created.
    */
    // saveDependencyInfos(dependencyInfos);

    String expected = "sortedByDependencyName.xml"
    Path expectedPath = Paths.get(basedir.toString(), expected)

    JAXBContext jaxbSerializer = createJaxbSerializer()
    DependencyInfos expectedDependencyInfos =
        (DependencyInfos) jaxbSerializer.createUnmarshaller().unmarshal(expectedPath.toFile())

    assertEquals(
        expectedDependencyInfos.dependencyInfos.size(),
        dependencyInfos.size(),
        expectedDependencyInfos.dependencyInfos.stream()
            .map(Object::toString)
            .collect(Collectors.joining("\n"))
            + "\n != \n"
            + dependencyInfos.stream().map(Object::toString).collect(Collectors.joining("\n")))

    for (int i = 0; i < dependencyInfos.size(); i++) {
        DependencyInfo expectedDependencyInfo = expectedDependencyInfos.dependencyInfos.get(i)
        DependencyInfo actualDependencyInfo = dependencyInfos.get(i)

        assertEquals(
            expectedDependencyInfo,
            actualDependencyInfo,
            "Expected: " + expectedDependencyInfo.name + ", Sorted: " + actualDependencyInfo.name)
    }
}

/**
 * Use this method if the sorting has changed or became in other ways incompatible to the standard files.
 * <p>
 * This method saves the created dependency infos to a file as a test-standard if you have checked manually
 * that this data is correct.
 *
 * @param dependencyInfos Created dependency infos.
 * @throws JAXBException JAXB exception at serializing into a file.
 * @throws IOException   File access exception.
 */
@SuppressWarnings("unused")
private static void saveDependencyInfos(List<DependencyInfo> dependencyInfos) throws JAXBException, IOException {
    DependencyInfos dependencyInfosXml = new DependencyInfos(dependencyInfos)
    JAXBContext jaxbContext = createJaxbSerializer()
    File tempFile = File.createTempFile("licensesSort", ".xml")
    jaxbContext.createMarshaller().marshal(dependencyInfosXml, tempFile)
    System.out.println("Sorted XML: " + tempFile.getAbsolutePath())
}

private static JAXBContext createJaxbSerializer() throws JAXBException {
    return JAXBContext.newInstance(Policy.class, DependencyInfos.class)
}