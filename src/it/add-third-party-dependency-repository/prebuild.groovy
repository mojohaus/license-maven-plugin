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

// a dependency POM's repository URL cannot use ${user.dir} or ${basedir}: write the absolute URL of repo-b
def pom = new File(basedir, 'repo-a/org/codehaus/mojo/license/test/it752/direct/1.0/direct-1.0.pom')
pom.text = pom.text.replace('@repo-b.url@', new File(basedir, 'repo-b').toURI().toString())
assert !pom.text.contains('@repo-b.url@')
true
