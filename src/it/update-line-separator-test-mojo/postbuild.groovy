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
    assert false
  }
  assert true
}

def assertExistsFile(file)
{
  if ( !file.exists() || file.isDirectory() )
  {
    println(file.getAbsolutePath() + " file is missing or a directory.")
    assert false
  }
  assert true
}

def assertContains(file, content, expected)
{
  if ( !content.contains(expected) )
  {
    println(expected + " was not found in file [" + file + "]\n :" + content)
    return false
  }
  return true
}

def assertNotContains(file, content, expected)
{
  if ( content.contains(expected) )
  {
    println(expected + " should not be found in file [" + file + "]\n :" + content)
    return false
  }
  return true
}

//def newLine = System.getProperty("line.separator")

Calendar cal = Calendar.getInstance();
cal.setTime(new Date());
int currentYear = cal.get(Calendar.YEAR);
dateRange = "2012";
if ( currentYear > 2012 )
{
  dateRange += " - " + currentYear;
}
println("Current year: " + currentYear)
println("Date range to use: " + dateRange)

//
// TEST CRLF
//

file = new File(basedir, 'src/main/java/crlf/org/codehaus/mojo/license/Lf.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony\r\n');

file = new File(basedir, 'src/main/java/crlf/org/codehaus/mojo/license/CrLf.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony\r\n');

//
// TEST LF
//

file = new File(basedir, 'src/main/java/lf/org/codehaus/mojo/license/Lf.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony\n');

file = new File(basedir, 'src/main/java/lf/org/codehaus/mojo/license/CrLf.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony\n');

//
// TEST CRLF
//

file = new File(basedir, 'src/main/java/str/org/codehaus/mojo/license/Lf.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 TonySTR');

file = new File(basedir, 'src/main/java/crlf/org/codehaus/mojo/license/CrLf.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 TonySTR');


return true;
