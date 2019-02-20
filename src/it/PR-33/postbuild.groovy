/*
 * #%L
 * License Maven Plugin
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


def asl2 = new File(basedir, 'child3/target/generated-resources/licenses/org.codehaus.mojo.license.test.pr-33-child2_apache_license_2.0.txt')
assert asl2.exists()
assert asl2.text.contains('Version 2.0, January 2004');

def asl1 = new File(basedir, 'child3/target/generated-resources/licenses/org.codehaus.mojo.license.test.pr-33-child1_apache_license_1.0.txt')
assert asl1.exists()
assert asl1.text.contains('Copyright (c) 1995-1999 The Apache Group.');

def licensesXml = new File(basedir, 'child3/target/generated-resources/licenses.xml');
assert licensesXml.exists()
assert licensesXml.text.contains('<file>org.codehaus.mojo.license.test.pr-33-child2_apache_license_2.0.txt</file>')
assert licensesXml.text.contains('<url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>')
assert licensesXml.text.contains('<file>org.codehaus.mojo.license.test.pr-33-child1_apache_license_1.0.txt</file>')
assert licensesXml.text.contains('<url>https://www.apache.org/licenses/LICENSE-1.0</url>')
