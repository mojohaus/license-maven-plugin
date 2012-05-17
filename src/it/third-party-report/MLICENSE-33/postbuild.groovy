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
def assertExistsDirectory(file)
{
  if ( !file.exists() || !file.isDirectory() )
  {
    println(file.getAbsolutePath() + " file is missing or is not a directory.")
    return false
  }
  return true
}

def assertExistsFile(file)
{
  if ( !file.exists() || file.isDirectory() )
  {
    println(file.getAbsolutePath() + " file is missing or a directory.")
    return false
  }
  return true
}

def assertNotExistsFile(file)
{
  if ( file.exists() )
  {
    println(file.getAbsolutePath() + " file should not exists.")
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

File target = new File(basedir, "target");
File defaultThirdPartyFile = new File(target, "generated-sources/license/THIRD-PARTY.txt");
File reportThirdPartyFile = new File(target, "site/third-party-report.html");

assert assertExistsDirectory(target);
assert assertExistsFile(defaultThirdPartyFile);
assert assertExistsFile(reportThirdPartyFile);

//String content = reportThirdPartyFile.text
//
//String[] expectedContents = ["commons-logging:commons-logging:1.1.1"]
//
//expectedContents.each {
//  assert assertContent(otherThirdPartyFile, content, it)
//}

return true;