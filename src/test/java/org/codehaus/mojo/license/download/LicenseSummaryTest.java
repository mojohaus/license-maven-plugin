package org.codehaus.mojo.license.download;

import org.codehaus.mojo.license.Eol;
import org.codehaus.mojo.license.download.LicenseSummaryReader;
import org.codehaus.mojo.license.download.LicenseSummaryWriter;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.0
 */
public class LicenseSummaryTest
{

    /**
     * Test reading the license summary xml file into ProjectLicenseInfo objects
     *
     * @throws IOException                  if any
     * @throws ParserConfigurationException if any
     * @throws SAXException                 if any
     */
    @Test
    public void testReadLicenseSummary()
        throws IOException, SAXException, ParserConfigurationException
    {
        File licenseSummaryFile = new File( "src/test/resources/license-summary-test.xml" );
        Assert.assertTrue( licenseSummaryFile.exists() );
        FileInputStream fis = new FileInputStream( licenseSummaryFile );
        List<ProjectLicenseInfo> list = LicenseSummaryReader.parseLicenseSummary( fis );
        fis.close();
        ProjectLicenseInfo dep = list.get( 0 );
        Assert.assertEquals( "org.codehaus.mojo", dep.getGroupId() );
        Assert.assertEquals( "junk", dep.getArtifactId() );
        Assert.assertEquals( "1.1", dep.getVersion() );

        List<ProjectLicense> licenses = dep.getLicenses();
        Assert.assertEquals( 1, licenses.size() );
        ProjectLicense lic0 = dep.getLicenses().get(0);
        Assert.assertEquals( "lgpl", lic0.getName() );
        Assert.assertEquals( "http://www.gnu.org/licenses/lgpl-3.0.txt", lic0.getUrl() );
        Assert.assertEquals( "lgpl-3.0.txt", lic0.getFile() );
        Assert.assertEquals( "lgpl version 3.0", lic0.getComments() );

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
    public void testWriteReadLicenseSummary()
        throws IOException, SAXException, ParserConfigurationException, TransformerFactoryConfigurationError,
        TransformerException
    {
        List<ProjectLicenseInfo> licSummary = new ArrayList<>();
        ProjectLicenseInfo dep1 = new ProjectLicenseInfo( "org.test", "test1", "1.0" );
        ProjectLicenseInfo dep2 = new ProjectLicenseInfo( "org.test", "test2", "2.0" );

        ProjectLicense lic = new ProjectLicense();
        lic.setName( "lgpl" );
        lic.setUrl( "http://www.gnu.org/licenses/lgpl-3.0.txt" );
        lic.setFile( "lgpl-3.0.txt" );
        lic.setComments( "lgpl version 3.0" );
        dep1.addLicense( lic );
        dep2.addLicense( lic );

        licSummary.add( dep1 );
        licSummary.add( dep2 );

        {
            File licenseSummaryFile = File.createTempFile( "licSummary", "tmp" );
            LicenseSummaryWriter.writeLicenseSummary( licSummary, licenseSummaryFile, StandardCharsets.UTF_8, Eol.LF,
                                                      true );

            Assert.assertTrue( licenseSummaryFile.exists() );
            FileInputStream fis = new FileInputStream( licenseSummaryFile );
            List<ProjectLicenseInfo> list = LicenseSummaryReader.parseLicenseSummary( fis );
            fis.close();
            ProjectLicenseInfo dep = list.get( 0 );
            Assert.assertEquals( "org.test", dep.getGroupId() );
            Assert.assertEquals( "test1", dep.getArtifactId() );
            Assert.assertEquals( "1.0", dep.getVersion() );

            List<ProjectLicense> licenses = dep.getLicenses();
            Assert.assertEquals( 1, licenses.size() );
            ProjectLicense lic0 = dep.getLicenses().get(0);
            Assert.assertEquals( "lgpl", lic0.getName() );
            Assert.assertEquals( "http://www.gnu.org/licenses/lgpl-3.0.txt", lic0.getUrl() );
            Assert.assertEquals( "lgpl-3.0.txt", lic0.getFile() );
            Assert.assertEquals( "lgpl version 3.0", lic0.getComments() );
        }

        {
            File licenseSummaryFile = File.createTempFile( "licSummaryNoVersion", "tmp" );
            LicenseSummaryWriter.writeLicenseSummary( licSummary, licenseSummaryFile, StandardCharsets.UTF_8, Eol.LF,
                                                      false );

            Assert.assertTrue( licenseSummaryFile.exists() );
            FileInputStream fis = new FileInputStream( licenseSummaryFile );
            List<ProjectLicenseInfo> list = LicenseSummaryReader.parseLicenseSummary( fis );
            fis.close();
            ProjectLicenseInfo dep = list.get( 0 );
            Assert.assertEquals( "org.test", dep.getGroupId() );
            Assert.assertEquals( "test1", dep.getArtifactId() );
            Assert.assertNull( dep.getVersion() );

            List<ProjectLicense> licenses = dep.getLicenses();
            Assert.assertEquals( 1, licenses.size() );
            ProjectLicense lic0 = dep.getLicenses().get(0);
            Assert.assertEquals( "lgpl", lic0.getName() );
            Assert.assertEquals( "http://www.gnu.org/licenses/lgpl-3.0.txt", lic0.getUrl() );
            Assert.assertEquals( "lgpl-3.0.txt", lic0.getFile() );
            Assert.assertEquals( "lgpl version 3.0", lic0.getComments() );
        }

    }

    @Test
    public void patternOrText()
    {
        Assert.assertEquals( "\\Qsimple\\E", LicenseSummaryWriter.patternOrText( "simple", true ) );
        Assert.assertEquals( "\\Qone two\\E", LicenseSummaryWriter.patternOrText( "one two", true ) );
        Assert.assertEquals( "\\Qone\\E\\s+\\Qtwo\\E", LicenseSummaryWriter.patternOrText( "one  two", true ) );
        Assert.assertEquals( "\\Qone\ntwo\\E", LicenseSummaryWriter.patternOrText( "one\ntwo", true ) );
        Assert.assertEquals( "\\Qone\\E\\s+\\Qtwo\\E", LicenseSummaryWriter.patternOrText( "one\n\t\ttwo", true ) );
    }
}
