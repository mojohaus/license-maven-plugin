/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
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

file = new File(basedir, 'LICENSE.txt');
assert file.exists();

content = file.text;
assert !content.contains(';; This line will be removed!');
assert !content.contains('GNU LESSER GENERAL PUBLIC LICENSE');
assert content.contains('GNU GENERAL PUBLIC LICENSE');

file = new File(basedir, 'target/generated-sources/license/LICENSE.txt');
assert file.exists();

content = file.text;
assert !content.contains(';; This line will be removed!');
assert !content.contains('GNU LESSER GENERAL PUBLIC LICENSE');
assert content.contains('GNU GENERAL PUBLIC LICENSE');

file = new File(basedir, 'target/generated-sources/license/META-INF/bundleLicense.txt');
assert file.exists();

content = file.text;
assert !content.contains(';; This line will be removed!');
assert !content.contains('GNU LESSER GENERAL PUBLIC LICENSE');
assert content.contains('GNU GENERAL PUBLIC LICENSE');

return true;
