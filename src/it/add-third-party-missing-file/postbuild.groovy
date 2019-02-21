/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019, Falco Nikolas
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

file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY-by-classpath.txt');
assert file.exists();
content = file.text;
assert content.contains('The JSON License by classpath url');
assert content.contains('CDDL + GPLv2 with classpath exception by classpath url');

file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY-by-file.txt');
assert file.exists();
content = file.text;
assert content.contains('The JSON License by file url');
assert content.contains('CDDL + GPLv2 with classpath exception by file url');