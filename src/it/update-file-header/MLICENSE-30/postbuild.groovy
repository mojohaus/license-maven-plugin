/*
 * #%L
 * License Maven Plugin
 *
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit, Tony chemit
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

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/test/MyBean.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2112 License Test');
assert assertContains(file, content, 'MyBean.java - License Test :: MLICENSE-30 - License Test - 2112');
assert assertContains(file, content, 'org.codehaus.mojo.license.test-MLICENSE-30-1.1');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '%%');
assert assertContains(file, content, '#L%');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/test/MyBean2.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony Do not update!');
assert assertNotContains(file, content, 'License Test :: Will be updated!');
assert assertNotContains(file, content, 'Copyright (C) 2112 License Test');
assert assertContains(file, content, 'MyBean2.java - License Test :: MLICENSE-30 - License Test - 2112');
assert assertContains(file, content, 'org.codehaus.mojo.license.test-MLICENSE-30-1.1');
assert assertContains(file, content, 'Copyright (C) 2010 Tony Do not update!');
assert assertNotContains(file, content, 'Fake to be removed!');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '%%');
assert assertContains(file, content, '#L%');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/test/MyBean3.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, '%%Ignore-License');
assert assertContains(file, content, 'Copyright (C) 2000 Codelutin Do not update!');
assert assertNotContains(file, content, '#%L');
assert assertNotContains(file, content, '#L%');

return true;
