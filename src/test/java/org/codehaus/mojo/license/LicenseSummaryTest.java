package org.codehaus.mojo.license;

/* 
 * Codehaus License Maven Plugin
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import junit.framework.TestCase;

import org.apache.maven.model.License;
import org.codehaus.mojo.license.LicenseSummaryReader;
import org.codehaus.mojo.license.LicenseSummaryWriter;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.xml.sax.SAXException;

public class LicenseSummaryTest extends TestCase
{
   public void testReadLicenseSummary() throws IOException, SAXException, ParserConfigurationException
   {
      File licenseSummaryFile = new File( "src/test/resources/license-summary-test.xml" );
      this.assertTrue( licenseSummaryFile.exists() );
      FileInputStream fis = new FileInputStream( licenseSummaryFile );
      List<ProjectLicenseInfo> list = LicenseSummaryReader.parseLicenseSummary( fis );
      fis.close();
      ProjectLicenseInfo dep = list.get( 0 );
      this.assertEquals( "org.codehaus.mojo", dep.getGroupId() );
      this.assertEquals( "junk", dep.getArtifactId() );
      this.assertEquals( "1.1", dep.getVersion() );

   }
   
   public void testWriteReadLicenseSummary() throws IOException, SAXException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
   {
      List<ProjectLicenseInfo> licSummary = new ArrayList<ProjectLicenseInfo> ();
      ProjectLicenseInfo dep1 = new ProjectLicenseInfo( "org.test", "test1", "1.0" );
      ProjectLicenseInfo dep2 = new ProjectLicenseInfo( "org.test", "test2", "2.0" );
      
      License lic = new License();
      lic.setName( "lgpl" );
      lic.setUrl( "http://www.gnu.org/licenses/lgpl-3.0.txt" );
      lic.setComments( "lgpl version 3.0" );
      dep1.addLicense( lic );
      dep2.addLicense( lic );
      
      licSummary.add( dep1 );
      licSummary.add( dep2 );
      
      File licenseSummaryFile = File.createTempFile( "licSummary", "tmp" );
      //File licenseSummaryFile = new File( "src/test/resources/license-summary-test-2.xml" );
      LicenseSummaryWriter.writeLicenseSummary( licSummary, licenseSummaryFile );
      
      this.assertTrue( licenseSummaryFile.exists() );
      FileInputStream fis = new FileInputStream( licenseSummaryFile );
      List<ProjectLicenseInfo> list = LicenseSummaryReader.parseLicenseSummary( fis );
      fis.close();
      ProjectLicenseInfo dep = list.get( 0 );
      this.assertEquals( "org.test", dep.getGroupId() );
      this.assertEquals( "test1", dep.getArtifactId() );
      this.assertEquals( "1.0", dep.getVersion() );

   }
}
