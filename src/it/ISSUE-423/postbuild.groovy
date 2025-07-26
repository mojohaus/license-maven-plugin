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

file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY.txt')
assert file.exists()
content = file.text
// Check that the non-overridden dependency is present.
assert content.contains('(Apache-2.0) Apache Commons Lang (org.apache.commons:commons-lang3:3.18.0 - https://commons.apache.org/proper/commons-lang/)')
// Check that the license name for a dependency can be overridden.
assert content.contains('(Apache-2.0) Commons Logging (commons-logging:commons-logging:1.1.1 - http://commons.apache.org/logging)')
// Check that the unused override does not end up in the report.
assert !content.contains('antlr')

// Check that the log contains a warning for the unused override.
log = new File(basedir, 'build.log')
assert log.exists()
assert log.text.contains('dependency [org.antlr--antlr--3.5.2] does not exist in project.')
