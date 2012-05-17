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

//
//TEST Java files
//

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2012 License Test');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean2.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony');
assert assertContains(file, content, 'do NOT update!');
assert assertNotContains(file, content, 'Fake to be removed!');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean3.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, ' * %%Ignore-License');
assert assertContains(file, content, ' * yet another license');
assert assertContains(file, content, ' * Copyright (C) 2000 Codelutin Do not update!');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean.java2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2012 License Test');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean2.java2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony');
assert assertContains(file, content, 'do NOT update!');
assert assertNotContains(file, content, 'Fake to be removed!');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean3.java2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, ' * %%Ignore-License');
assert assertContains(file, content, ' * yet another license');
assert assertContains(file, content, ' * Copyright (C) 2000 Codelutin Do not update!');

//
// Test apt files
//

file = new File(basedir, 'src/files/apt/test.apt');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '~~ #%L');
assert assertContains(file, content, '~~ #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '~~ ~~');

file = new File(basedir, 'src/files/apt/test2.apt');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '~~ #%L');
assert assertContains(file, content, '~~ #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '~~ ~~');

file = new File(basedir, 'src/files/apt/test.apt2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '~~ #%L');
assert assertContains(file, content, '~~ #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '~~ ~~');

file = new File(basedir, 'src/files/apt/test2.apt2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '~~ #%L');
assert assertContains(file, content, '~~ #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '~~ ~~');

//
// Test rst files
//

file = new File(basedir, 'src/files/rst/test.rst');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '.. * #%L');
assert assertContains(file, content, '.. * #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '.. * .. *');

file = new File(basedir, 'src/files/rst/test2.rst');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '.. * #%L');
assert assertContains(file, content, '.. * #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '.. * .. *');

file = new File(basedir, 'src/files/rst/test.rst2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '.. * #%L');
assert assertContains(file, content, '.. * #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '.. * .. *');

file = new File(basedir, 'src/files/rst/test2.rst2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '.. * #%L');
assert assertContains(file, content, '.. * #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '.. * .. *');

//
// Test xml files
//

file = new File(basedir, 'src/files/xml/test.xml');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/xml/test2.xml');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/xml/test.xml2');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/xml/test2.xml2');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

//
// Test jsp files
//

file = new File(basedir, 'src/files/jsp/test.jsp');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<%--");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/jsp/test2.jsp');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<%--");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/jsp/test.jsp2');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<%--");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/jsp/test2.jsp2');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<%--");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

//
// Test properties files
//

file = new File(basedir, 'src/files/properties/test.properties');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/properties/test2.properties');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertContains(file, content, '# #%L');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');


file = new File(basedir, 'src/files/properties/test.properties2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');


file = new File(basedir, 'src/files/properties/test2.properties2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertContains(file, content, '# #%L');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

//
// Test sh files
//

file = new File(basedir, 'src/files/properties/test.sh');
assertExistsFile(file);

content = file.text;
assert content.startsWith('#!/bin/sh');
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/properties/test2.sh');
assertExistsFile(file);

content = file.text;
assert content.startsWith('#!/bin/sh');
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/properties/test.sh2');
assertExistsFile(file);

content = file.text;
assert content.startsWith('#!/bin/sh');
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'src/files/properties/test2.sh2');
assertExistsFile(file);

content = file.text;
assert content.startsWith('#!/bin/sh');
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

//
// Test ftl files
//

file = new File(basedir, 'src/files/ftl/test.ftl');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '<#--');
assert assertContains(file, content, ' #%L');
assert assertContains(file, content, ' #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '-->');

file = new File(basedir, 'src/files/ftl/test2.ftl');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '<#--');
assert assertContains(file, content, ' #%L');
assert assertContains(file, content, ' #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '-->');

file = new File(basedir, 'src/files/ftl/test.ftl2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '<#--');
assert assertContains(file, content, ' #%L');
assert assertContains(file, content, ' #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '-->');

file = new File(basedir, 'src/files/ftl/test2.ftl2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '<#--');
assert assertContains(file, content, ' #%L');
assert assertContains(file, content, ' #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '-->');

//
// Test sql files
//
file = new File(basedir, 'src/files/sql/test.sql');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '-- #%L');
assert assertContains(file, content, '-- #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '---');

file = new File(basedir, 'src/files/sql/test2.sql');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '-- #%L');
assert assertContains(file, content, '-- #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '---');

file = new File(basedir, 'src/files/sql/test.sql2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '-- #%L');
assert assertContains(file, content, '-- #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '---');

file = new File(basedir, 'src/files/sql/test2.sql2');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '-- #%L');
assert assertContains(file, content, '-- #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '---');

// Test on the child1 module

file = new File(basedir, 'child1/src/main/java/org/codehaus/mojo2/license/MyBean.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2012 License Test');

file = new File(basedir, 'child1/src/main/java/org/codehaus/mojo2/license/MyBean2.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C) 2010 Tony');
assert assertContains(file, content, 'do NOT update!');
assert assertNotContains(file, content, 'Fake to be removed!');

file = new File(basedir, 'child1/src/main/java/org/codehaus/mojo2/license/MyBean3.java');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, ' * %%Ignore-License');
assert assertContains(file, content, ' * yet another license');
assert assertContains(file, content, ' * Copyright (C) 2000 Codelutin Do not update!');

//
// Test apt files
//

file = new File(basedir, 'child1/src/files/apt/test.apt');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '~~ #%L');
assert assertContains(file, content, '~~ #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '~~ ~~');

file = new File(basedir, 'child1/src/files/apt/test2.apt');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '~~ #%L');
assert assertContains(file, content, '~~ #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '~~ ~~');

//
// Test rst files
//

file = new File(basedir, 'child1/src/files/rst/test.rst');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '.. * #%L');
assert assertContains(file, content, '.. * #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '.. * .. *');

file = new File(basedir, 'child1/src/files/rst/test2.rst');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '.. * #%L');
assert assertContains(file, content, '.. * #L%');
assert assertContains(file, content, '$Id');
assert assertNotContains(file, content, '.. * .. *');

//
// Test xml files
//

file = new File(basedir, 'child1/src/files/xml/test.xml');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'child1/src/files/xml/test2.xml');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

//
// Test jsp files
//

file = new File(basedir, 'child1/src/files/jsp/test.jsp');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<%--");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'child1/src/files/jsp/test2.jsp');
assertExistsFile(file);

content = file.text;
assert content.startsWith("<%--");
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '#%L');
assert assertContains(file, content, '#L%');
assert assertContains(file, content, '$Id');

//
// Test properties files
//

file = new File(basedir, 'child1/src/files/properties/test.properties');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'child1/src/files/properties/test2.properties');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertContains(file, content, '# #%L');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

//
// Test sh files
//

file = new File(basedir, 'child1/src/files/properties/test.sh');
assertExistsFile(file);

content = file.text;
assert content.startsWith('#!/bin/sh');
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

file = new File(basedir, 'child1/src/files/properties/test2.sh');
assertExistsFile(file);

content = file.text;
assert content.startsWith('#!/bin/sh');
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '# #%L');
assert assertContains(file, content, '# #L%');
assert assertNotContains(file, content, '# # \n');
assert assertContains(file, content, '$Id');

//
// Test ftl files
//

file = new File(basedir, 'child1/src/files/ftl/test.ftl');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '<#--');
assert assertContains(file, content, ' #%L');
assert assertContains(file, content, ' #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '-->');

file = new File(basedir, 'child1/src/files/ftl/test2.ftl');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '<#--');
assert assertContains(file, content, ' #%L');
assert assertContains(file, content, ' #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '-->');

//
// Test sql files
//
file = new File(basedir, 'child1/src/files/sql/test.sql');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '-- #%L');
assert assertContains(file, content, '-- #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '---');

file = new File(basedir, 'child1/src/files/sql/test2.sql');
assertExistsFile(file);

content = file.text;
assert assertContains(file, content, 'Copyright (C)');
assert assertContains(file, content, '-- #%L');
assert assertContains(file, content, '-- #L%');
assert assertContains(file, content, '$Id');
assert assertContains(file, content, '---');

return true;
