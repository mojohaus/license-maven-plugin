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

def assertExistsFile(file) {
    if (!file.exists() || file.isDirectory()) {
        println(file.getAbsolutePath() + " file is missing or a directory.")
        assert false
    }
    assert true
}

def assertContains(file, content, expected) {
    if (!content.contains(expected)) {
        println(expected + " was not found in file [" + file + "]\n :" + content)
        return false
    }
    return true
}


def lgpl3 = new File(basedir, 'child3/target/generated-resources/licenses/org.codehaus.mojo.license.test.pr-33-child1_lgpl_2.1.txt')
assert lgpl3.exists()
assert lgpl3.text.contains('Version 2.1, February 1999');

def lgpl21 = new File(basedir, 'child3/target/generated-resources/licenses/org.codehaus.mojo.license.test.pr-33-child2_lgpl_3.0.txt')
assert lgpl21.exists()
assert lgpl21.text.contains('Version 3, 29 June 2007');

def licensesXml = new File(basedir, 'child3/target/generated-resources/licenses.xml');
assert licensesXml.exists()
assert licensesXml.text.contains('<file>org.codehaus.mojo.license.test.pr-33-child1_lgpl_2.1.txt</file>')
assert licensesXml.text.contains('<url>https://www.gnu.org/licenses/lgpl-2.1.txt</url>')
assert licensesXml.text.contains('<file>org.codehaus.mojo.license.test.pr-33-child2_lgpl_3.0.txt</file>')
assert licensesXml.text.contains('<url>https://www.gnu.org/licenses/lgpl-3.0.txt</url>')
