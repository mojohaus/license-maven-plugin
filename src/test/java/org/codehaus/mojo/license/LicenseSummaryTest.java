package org.codehaus.mojo.license;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file 
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations 
 * under the License.
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
import org.codehaus.mojo.license.model.DependencyProject;
import org.xml.sax.SAXException;

public class LicenseSummaryTest extends TestCase
{
   public void testReadLicenseSummary() throws IOException, SAXException, ParserConfigurationException
   {
      File licenseSummaryFile = new File( "src/test/resources/license-summary-test.xml" );
      this.assertTrue( licenseSummaryFile.exists() );
      FileInputStream fis = new FileInputStream( licenseSummaryFile );
      List<DependencyProject> list = LicenseSummaryReader.parseLicenseSummary( fis );
      fis.close();
      DependencyProject dep = list.get( 0 );
      this.assertEquals( "org.codehaus.mojo", dep.getGroupId() );
      this.assertEquals( "junk", dep.getArtifactId() );
      this.assertEquals( "1.1", dep.getVersion() );

   }
   
   public void testWriteReadLicenseSummary() throws IOException, SAXException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
   {
      List<DependencyProject> licSummary = new ArrayList<DependencyProject> ();
      DependencyProject dep1 = new DependencyProject( "org.test", "test1", "1.0" );
      DependencyProject dep2 = new DependencyProject( "org.test", "test2", "2.0" );
      
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
      List<DependencyProject> list = LicenseSummaryReader.parseLicenseSummary( fis );
      fis.close();
      DependencyProject dep = list.get( 0 );
      this.assertEquals( "org.test", dep.getGroupId() );
      this.assertEquals( "test1", dep.getArtifactId() );
      this.assertEquals( "1.0", dep.getVersion() );

   }
}
