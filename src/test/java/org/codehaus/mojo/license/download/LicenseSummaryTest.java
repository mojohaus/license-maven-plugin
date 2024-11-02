package org.codehaus.mojo.license.download;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.codehaus.mojo.license.AbstractDownloadLicensesMojo;
import org.codehaus.mojo.license.Eol;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.codehaus.mojo.license.extended.InfoFile;
import org.codehaus.mojo.license.extended.spreadsheet.CalcFileWriter;
import org.codehaus.mojo.license.extended.spreadsheet.ExcelFileWriter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static org.codehaus.mojo.license.download.LicenseSummaryWriter.LICENSE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @since 1.0
 */
class LicenseSummaryTest {
    private static final Logger LOG = LoggerFactory.getLogger(LicenseSummaryTest.class);

    /**
     * Test reading the license summary xml file into ProjectLicenseInfo objects
     *
     * @throws IOException                  if any
     * @throws ParserConfigurationException if any
     * @throws SAXException                 if any
     */
    @Test
    void testReadLicenseSummary() throws IOException, SAXException, ParserConfigurationException {
        File licenseSummaryFile = new File("src/test/resources/license-summary-test.xml");
        assertTrue(licenseSummaryFile.exists());
        List<ProjectLicenseInfo> list;
        try (InputStream fis = Files.newInputStream(licenseSummaryFile.toPath())) {
            list = LicenseSummaryReader.parseLicenseSummary(fis);
        }
        ProjectLicenseInfo dep = list.get(0);
        assertEquals("org.codehaus.mojo", dep.getGroupId());
        assertEquals("junk", dep.getArtifactId());
        assertEquals("1.1", dep.getVersion());

        List<ProjectLicense> licenses = dep.getLicenses();
        assertEquals(1, licenses.size());
        ProjectLicense lic0 = dep.getLicenses().get(0);
        assertEquals("lgpl", lic0.getName());
        assertEquals("http://www.gnu.org/licenses/lgpl-3.0.txt", lic0.getUrl());
        assertEquals("lgpl-3.0.txt", lic0.getFile());
        assertEquals("lgpl version 3.0", lic0.getComments());
    }

    /**
     * Test writing license information to a license.xml file and then read this file
     * back in to make sure it's ok.
     *
     * @throws IOException                          if any
     * @throws ParserConfigurationException         if any
     * @throws TransformerFactoryConfigurationError if any
     * @throws TransformerException                 if any
     * @throws SAXException                         if any
     */
    @Test
    void testWriteReadLicenseSummary()
            throws IOException, SAXException, ParserConfigurationException, TransformerFactoryConfigurationError,
                    TransformerException {
        List<ProjectLicenseInfo> licSummary = new ArrayList<>();
        ProjectLicenseInfo dep1 = new ProjectLicenseInfo("org.test", "test1", "1.0", buildExtendedInfo(1));
        ProjectLicenseInfo dep2 = new ProjectLicenseInfo("org.test", "test2", "2.0", buildExtendedInfo(2));
        ProjectLicenseInfo dep3 = new ProjectLicenseInfo("com.test", "test3", "3.0", buildExtendedInfo(3));
        ProjectLicenseInfo dep4 = new ProjectLicenseInfo("dk.test", "test4", "4.0", buildExtendedInfo(4));

        ProjectLicense lic = new ProjectLicense();
        lic.setName("lgpl");
        lic.setUrl("http://www.gnu.org/licenses/lgpl-3.0.txt");
        lic.setFile("lgpl-3.0.txt");
        lic.setComments("lgpl version 3.0");
        dep1.addLicense(lic);
        dep2.addLicense(lic);
        dep3.addLicense(lic);

        dep2.addDownloaderMessage("There were server problems");
        // Skip dependency 3, to test correct empty cell filling of ODS export.
        dep4.addDownloaderMessage("http://google.de");

        licSummary.add(dep1);
        licSummary.add(dep2);
        licSummary.add(dep3);
        licSummary.add(dep4);

        File licenseSummaryFile = File.createTempFile("licSummary", "tmp");
        LicenseSummaryWriter.writeLicenseSummary(licSummary, licenseSummaryFile, StandardCharsets.UTF_8, Eol.LF, true);

        assertTrue(licenseSummaryFile.exists());
        FileInputStream fis = new FileInputStream(licenseSummaryFile);
        List<ProjectLicenseInfo> list = LicenseSummaryReader.parseLicenseSummary(fis);
        fis.close();
        ProjectLicenseInfo dep = list.get(0);
        assertEquals("org.test", dep.getGroupId());
        assertEquals("test1", dep.getArtifactId());
        assertEquals("1.0", dep.getVersion());

        List<ProjectLicense> licenses = dep.getLicenses();
        assertEquals(1, licenses.size());
        ProjectLicense lic0 = dep.getLicenses().get(0);
        assertEquals("lgpl", lic0.getName());
        assertEquals("http://www.gnu.org/licenses/lgpl-3.0.txt", lic0.getUrl());
        assertEquals("lgpl-3.0.txt", lic0.getFile());
        assertEquals("lgpl version 3.0", lic0.getComments());

        validateXml(licenseSummaryFile);

        licenseSummaryFile = File.createTempFile("licSummaryNoVersionNoXsd", "tmp");
        LicenseSummaryWriter.writeLicenseSummary(licSummary, licenseSummaryFile, StandardCharsets.UTF_8, Eol.LF, false);

        assertTrue(licenseSummaryFile.exists());
        fis = new FileInputStream(licenseSummaryFile);
        list = LicenseSummaryReader.parseLicenseSummary(fis);
        fis.close();
        dep = list.get(0);
        assertEquals("org.test", dep.getGroupId());
        assertEquals("test1", dep.getArtifactId());
        assertNull(dep.getVersion());

        licenses = dep.getLicenses();
        assertEquals(1, licenses.size());
        lic0 = dep.getLicenses().get(0);
        assertEquals("lgpl", lic0.getName());
        assertEquals("http://www.gnu.org/licenses/lgpl-3.0.txt", lic0.getUrl());
        assertEquals("lgpl-3.0.txt", lic0.getFile());
        assertEquals("lgpl version 3.0", lic0.getComments());

        validateXml(licenseSummaryFile);

        AbstractDownloadLicensesMojo.DataFormatting dataFormatting = new AbstractDownloadLicensesMojo.DataFormatting();

        Path licensesExcelOutputFile = Files.createTempFile("licExcel", ".xlsx");
        ExcelFileWriter.write(licSummary, licensesExcelOutputFile.toFile(), dataFormatting);

        Path licensesCalcOutputFile = Files.createTempFile("licCalc", ".ods");
        CalcFileWriter.write(licSummary, licensesCalcOutputFile.toFile(), dataFormatting);
    }

    /**
     * Validate XML against XSD.
     *
     * @param licenseSummaryFile License summary file.
     * @throws SAXException SAX exception, validation problem.
     * @throws IOException  I/O exception, file problem.
     */
    private static void validateXml(File licenseSummaryFile) throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream inputStream = LicenseSummaryTest.class.getResourceAsStream(LICENSE_PATH)) {
            Source schemaSource = new StreamSource(inputStream);
            Schema schema = schemaFactory.newSchema(schemaSource);
            Validator validator = schema.newValidator();
            Source xmlSource = new StreamSource(licenseSummaryFile);
            validator.validate(xmlSource);
        }
    }

    private static ExtendedInfo buildExtendedInfo(int suffix) {
        ExtendedInfo extendedInfo = new ExtendedInfo();
        Artifact artifact = new DefaultArtifact(
                "org.test", "test" + suffix, "2.0", "compile", "jar", null, new DefaultArtifactHandler());
        extendedInfo.setArtifact(artifact);
        extendedInfo.setBundleLicense("Bundle Test License " + suffix);
        extendedInfo.setBundleVendor("Bundle Test Vendor " + suffix);

        List<Developer> developers = new ArrayList<>();
        Developer developer = createDeveloper(suffix);
        developers.add(developer);
        extendedInfo.setDevelopers(developers);
        extendedInfo.setImplementationVendor("Implementation vendor " + suffix);
        extendedInfo.setInceptionYear((2021 + suffix) + "");

        List<InfoFile> infoFiles = new ArrayList<>();
        for (InfoFile.Type license : InfoFile.Type.values()) {
            infoFiles.add(createInfoFile(license, suffix));
        }
        extendedInfo.setInfoFiles(infoFiles);

        extendedInfo.setName("Test Project " + suffix);

        Organization organization = new Organization();
        organization.setName("Test Organization " + suffix);
        organization.setUrl("www.github.com/" + suffix);
        extendedInfo.setOrganization(organization);

        Scm scm = new Scm();
        scm.setUrl("www.github.com/" + suffix);
        extendedInfo.setScm(scm);

        extendedInfo.setUrl("www.google.de/" + suffix);
        return extendedInfo;
    }

    private static InfoFile createInfoFile(InfoFile.Type noticeType, int suffix) {
        InfoFile infoFile = new InfoFile();
        infoFile.setContent("This is " + noticeType.name() + " test content " + suffix);
        infoFile.setExtractedCopyrightLines(
                new HashSet<>(Collections.singletonList("Test " + noticeType.name() + suffix)));
        infoFile.setFileName(noticeType.name() + " " + suffix + ".txt");
        infoFile.setType(noticeType);
        return infoFile;
    }

    private static Developer createDeveloper(int suffix) {
        Developer developer = new Developer();
        developer.setEmail("developer" + suffix + "@google.com ");
        developer.setId("developer.id " + suffix);
        developer.setName("Top developer " + suffix);
        developer.setOrganization("Developer Organization " + suffix);
        developer.setOrganizationUrl("Test Organization " + suffix);
        developer.setRoles(Collections.singletonList("Lead Developer " + suffix));
        developer.setTimezone("UTC+2");
        developer.setUrl("www.github.com");
        return developer;
    }

    @Test
    void patternOrText() {
        assertEquals("\\Qsimple\\E", LicenseSummaryWriter.patternOrText("simple", true));
        assertEquals("\\Qone two\\E", LicenseSummaryWriter.patternOrText("one two", true));
        assertEquals("\\Qone\\E\\s+\\Qtwo\\E", LicenseSummaryWriter.patternOrText("one  two", true));
        assertEquals("\\Qone\ntwo\\E", LicenseSummaryWriter.patternOrText("one\ntwo", true));
        assertEquals("\\Qone\\E\\s+\\Qtwo\\E", LicenseSummaryWriter.patternOrText("one\n\t\ttwo", true));
    }
}
