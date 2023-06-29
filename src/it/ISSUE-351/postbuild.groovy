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

file = new File(basedir, 'child2-overriding/target/generated-sources/license/THIRD-PARTY.txt')
assert file.exists()
content = file.text
assert content.contains('(Eclipse Public License 2.0) child1-multi-license (org.codehaus.mojo.license.test:child1-multi-license:1.0.0 - no url defined)')
assert !content.contains('(TestLicense-01) (TestLicense-02) child1-multi-license (org.codehaus.mojo.license.test:child1-multi-license:1.0.0 - no url defined)')
return true
