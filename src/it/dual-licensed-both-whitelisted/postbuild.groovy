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
def log = new File(basedir, 'build.log').text
assert !log.contains('forbidden licenses used')

thirdParty = new File(basedir, 'target/generated-sources/license/THIRD-PARTY.txt')
assert thirdParty.isFile()
assert thirdParty.text.contains('(COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0) (GNU General Public Library) Streaming API for XML (javax.xml.stream:stax-api:1.0-2')

return true
