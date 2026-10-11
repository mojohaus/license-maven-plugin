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

def newLine = System.getProperty("line.separator")

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
//TEST Java files
//

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/java-info.java');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) ' + dateRange + ' License Test');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean.java');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) ' + dateRange + ' License Test');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean2.java');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) 2010 Tony');
assert content.contains('do NOT update!');
assert !content.contains('Fake to be removed!');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean3.java');
assert file.isFile();

content = file.text;
assert content.contains(' * %%Ignore-License');
assert content.contains(' * yet another license');
assert content.contains(' * Copyright (C) 2000 Codelutin Do not update!');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean.java2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) ' + dateRange + ' License Test');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean2.java2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) 2010 Tony');
assert content.contains('do NOT update!');
assert !content.contains('Fake to be removed!');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/MyBean3.java2');
assert file.isFile();

content = file.text;
assert content.contains(' * %%Ignore-License');
assert content.contains(' * yet another license');
assert content.contains(' * Copyright (C) 2000 Codelutin Do not update!');

//
// Test apt files
//

file = new File(basedir, 'src/files/apt/test.apt');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('~~ #%L');
assert content.contains('~~ #L%');
assert content.contains('$Id');
assert !content.contains('~~ ~~');

file = new File(basedir, 'src/files/apt/test2.apt');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('~~ #%L');
assert content.contains('~~ #L%');
assert content.contains('$Id');
assert !content.contains('~~ ~~');

file = new File(basedir, 'src/files/apt/test.apt2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('~~ #%L');
assert content.contains('~~ #L%');
assert content.contains('$Id');
assert !content.contains('~~ ~~');

file = new File(basedir, 'src/files/apt/test2.apt2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('~~ #%L');
assert content.contains('~~ #L%');
assert content.contains('$Id');
assert !content.contains('~~ ~~');

//
// Test rst files
//

file = new File(basedir, 'src/files/rst/test.rst');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('.. * #%L');
assert content.contains('.. * #L%');
assert content.contains('$Id');
assert !content.contains('.. * .. *');

file = new File(basedir, 'src/files/rst/rst2/test.rst');
assert file.isFile();

content = file.text;
assert !content.contains('Copyright (C)');

file = new File(basedir, 'src/files/rst/test2.rst');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('.. * #%L');
assert content.contains('.. * #L%');
assert content.contains('$Id');
assert !content.contains('.. * .. *');

file = new File(basedir, 'src/files/rst/test.rst2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('.. * #%L');
assert content.contains('.. * #L%');
assert content.contains('$Id');
assert !content.contains('.. * .. *');

file = new File(basedir, 'src/files/rst/test2.rst2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('.. * #%L');
assert content.contains('.. * #L%');
assert content.contains('$Id');
assert !content.contains('.. * .. *');

//
// Test xml files
//

file = new File(basedir, 'src/files/xml/test.xml');
assert file.isFile();

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'src/files/xml/test2.xml');
assert file.isFile();

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'src/files/xml/test.xml2');
assert file.isFile();

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'src/files/xml/test2.xml2');
assert file.isFile();

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

//
// Test jsp files
//

file = new File(basedir, 'src/files/jsp/test.jsp');
assert file.isFile();

content = file.text;
assert content.startsWith("<%--");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'src/files/jsp/test2.jsp');
assert file.isFile();

content = file.text;
assert content.startsWith("<%--");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'src/files/jsp/test.jsp2');
assert file.isFile();

content = file.text;
assert content.startsWith("<%--");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'src/files/jsp/test2.jsp2');
assert file.isFile();

content = file.text;
assert content.startsWith("<%--");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

//
// Test properties files
//

file = new File(basedir, 'src/files/properties/test.properties');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'src/files/properties/test2.properties');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert content.contains('# #%L');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'src/files/properties/test.properties2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'src/files/properties/test2.properties2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert content.contains('# #%L');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

//
// Test sh files
//

file = new File(basedir, 'src/files/properties/test.sh');
assert file.isFile();

content = file.text;
assert content.startsWith('#!/bin/sh');
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'src/files/properties/test2.sh');
assert file.isFile();

content = file.text;
assert content.startsWith('#!/bin/sh');
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'src/files/properties/test.sh2');
assert file.isFile();

content = file.text;
assert content.startsWith('#!/bin/sh');
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'src/files/properties/test2.sh2');
assert file.isFile();

content = file.text;
assert content.startsWith('#!/bin/sh');
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

//
// Test ftl files
//

file = new File(basedir, 'src/files/ftl/test.ftl');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('<#--');
assert content.contains(' #%L');
assert content.contains(' #L%');
assert content.contains('$Id');
assert content.contains('-->');

file = new File(basedir, 'src/files/ftl/test2.ftl');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('<#--');
assert content.contains(' #%L');
assert content.contains(' #L%');
assert content.contains('$Id');
assert content.contains('-->');

file = new File(basedir, 'src/files/ftl/test.ftl2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('<#--');
assert content.contains(' #%L');
assert content.contains(' #L%');
assert content.contains('$Id');
assert content.contains('-->');

file = new File(basedir, 'src/files/ftl/test2.ftl2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('<#--');
assert content.contains(' #%L');
assert content.contains(' #L%');
assert content.contains('$Id');
assert content.contains('-->');

//
// Test sql files
//
file = new File(basedir, 'src/files/sql/test.sql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('---');

file = new File(basedir, 'src/files/sql/test2.sql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('---');

file = new File(basedir, 'src/files/sql/test.sql2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('---');

file = new File(basedir, 'src/files/sql/test2.sql2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('---');

//
// Test mysql files
//
file = new File(basedir, 'src/files/mysql/test.mysql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('-- -');

file = new File(basedir, 'src/files/mysql/test2.mysql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('-- -');

file = new File(basedir, 'src/files/mysql/test.mysql2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('-- -');

file = new File(basedir, 'src/files/mysql/test2.mysql2');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('-- -');

// Test on the child1 module

file = new File(basedir, 'child1/src/main/java/org/codehaus/mojo2/license/MyBean.java');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) ' + dateRange + ' License Test');

file = new File(basedir, 'child1/src/main/java/org/codehaus/mojo2/license/MyBean2.java');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) 2010 Tony');
assert content.contains('do NOT update!');
assert !content.contains('Fake to be removed!');

file = new File(basedir, 'child1/src/main/java/org/codehaus/mojo2/license/MyBean3.java');
assert file.isFile();

content = file.text;
assert content.contains(' * %%Ignore-License');
assert content.contains(' * yet another license');
assert content.contains(' * Copyright (C) 2000 Codelutin Do not update!');

//
// Test apt files
//

file = new File(basedir, 'child1/src/files/apt/test.apt');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('~~ #%L');
assert content.contains('~~ #L%');
assert content.contains('$Id');
assert !content.contains('~~ ~~');

file = new File(basedir, 'child1/src/files/apt/test2.apt');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('~~ #%L');
assert content.contains('~~ #L%');
assert content.contains('$Id');
assert !content.contains('~~ ~~');

//
// Test rst files
//

file = new File(basedir, 'child1/src/files/rst/test.rst');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('.. * #%L');
assert content.contains('.. * #L%');
assert content.contains('$Id');
assert !content.contains('.. * .. *');

file = new File(basedir, 'child1/src/files/rst/test2.rst');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('.. * #%L');
assert content.contains('.. * #L%');
assert content.contains('$Id');
assert !content.contains('.. * .. *');

//
// Test xml files
//

file = new File(basedir, 'child1/src/files/xml/test.xml');
assert file.isFile();

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'child1/src/files/xml/test2.xml');
assert file.isFile();

content = file.text;
assert content.startsWith("<?xml version='1.0' encoding='UTF-8'?>");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

//
// Test jsp files
//

file = new File(basedir, 'child1/src/files/jsp/test.jsp');
assert file.isFile();

content = file.text;
assert content.startsWith("<%--");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

file = new File(basedir, 'child1/src/files/jsp/test2.jsp');
assert file.isFile();

content = file.text;
assert content.startsWith("<%--");
assert content.contains('Copyright (C)');
assert content.contains('#%L');
assert content.contains('#L%');
assert content.contains('$Id');

//
// Test properties files
//

file = new File(basedir, 'child1/src/files/properties/test.properties');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'child1/src/files/properties/test2.properties');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert content.contains('# #%L');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

//
// Test sh files
//

file = new File(basedir, 'child1/src/files/properties/test.sh');
assert file.isFile();

content = file.text;
assert content.startsWith('#!/bin/sh');
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

file = new File(basedir, 'child1/src/files/properties/test2.sh');
assert file.isFile();

content = file.text;
assert content.startsWith('#!/bin/sh');
assert content.contains('Copyright (C)');
assert content.contains('# #%L');
assert content.contains('# #L%');
assert !content.contains('# # ' + newLine);
assert content.contains('$Id');

//
// Test ftl files
//

file = new File(basedir, 'child1/src/files/ftl/test.ftl');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('<#--');
assert content.contains(' #%L');
assert content.contains(' #L%');
assert content.contains('$Id');
assert content.contains('-->');

file = new File(basedir, 'child1/src/files/ftl/test2.ftl');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('<#--');
assert content.contains(' #%L');
assert content.contains(' #L%');
assert content.contains('$Id');
assert content.contains('-->');

//
// Test sql files
//
file = new File(basedir, 'child1/src/files/sql/test.sql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('---');

file = new File(basedir, 'child1/src/files/sql/test2.sql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('---');

//
// Test sql files
//
file = new File(basedir, 'child1/src/files/mysql/test.mysql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('-- -');

file = new File(basedir, 'child1/src/files/mysql/test2.mysql');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C)');
assert content.contains('-- #%L');
assert content.contains('-- #L%');
assert content.contains('$Id');
assert content.contains('-- -');
