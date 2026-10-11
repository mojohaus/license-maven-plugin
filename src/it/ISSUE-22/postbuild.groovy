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

def newLine = System.getProperty("line.separator")

file = new File(basedir, 'src/main/resources/HelloWorld.html');
assert file.isFile();

content = file.text;
assert content.contains('<!DOCTYPE html>' + newLine +
        '<!--');

file = new File(basedir, 'src/main/resources/HelloWorld2.html');
assert file.isFile();

content = file.text;
assert content.contains('<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">' + newLine +
        '<!--');

file = new File(basedir, 'src/main/resources/HelloWorld3.html');
assert file.isFile();

content = file.text;
assert content.contains('<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">' + newLine +
        '<!--');

file = new File(basedir, 'src/main/resources/HelloWorld4.xhtml');
assert file.isFile();

content = file.text;
assert content.contains('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">' + newLine +
        '<!--');
