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

def assertExistsFile(file)
{
  if ( !file.exists() || file.isDirectory() )
  {
    println(file.getAbsolutePath() + " file is missing or a directory.")
    return false
  }
  return true
}

def assertContent(file, content, wantedText)
{
  if ( !content.contains(wantedText) )
  {
    println(file.getAbsolutePath() + " should contains content " + wantedText)
    return false
  }
  return true
}

def assertNotContent(file, content, wantedText)
{
  if ( content.contains(wantedText) )
  {
    println(file.getAbsolutePath() + " should not contains content " + wantedText)
    return false
  }
  return true
}

def checkThirdPartyFile(filePath) {
  file = new File(basedir, filePath);
  assertExistsFile(file);

  content = file.text;
  assert assertNotContent(file, content, 'the project has no dependencies.');
  assert assertNotContent(file, content, '(Apache License, Version 2.0)');
  assert assertNotContent(file, content, '(Apache Public License 2.0)');
  assert assertContent(file, content, '(The Apache Software License, Version 2.0)');
  assert assertContent(file, content, '(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1 - http://commons.apache.org/logging)');
  assert assertContent(file, content, '(The Apache Software License, Version 2.0) JHLabs Image Processing Filters (com.jhlabs:filters:2.0.235 - http://www.jhlabs.com/ip/index.html)');
  assert assertContent(file, content, '(The Apache Software License, Version 2.0) Plexus Cipher: encryption/decryption Component (org.sonatype.plexus:plexus-cipher:1.4 - http://spice.sonatype.org/plexus-cipher)');
}

checkThirdPartyFile('target/generated-sources/license/THIRD-PARTY-singleList.txt');
checkThirdPartyFile('target/generated-sources/license/THIRD-PARTY-singleListSplit.txt');
checkThirdPartyFile('target/generated-sources/license/THIRD-PARTY-twoList.txt');
checkThirdPartyFile('target/generated-sources/license/THIRD-PARTY-twoListSplit.txt');

return true;
