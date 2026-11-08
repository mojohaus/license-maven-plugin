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
// the included and excluded licenses are accepted both as one '|'-separated string and as a list
def log = new File(basedir, 'build.log').text
assert !log.contains('forbidden licenses used')

file = new File(basedir, 'target/generated-sources/license/with-single-string-included-licenses-THIRD-PARTY.txt')
assert file.isFile()
assert file.text.contains('(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1')
assert file.text.contains('(Lesser General Public License (LGPL) v 3.0) Nuiton Processor :: Api')
file = new File(basedir, 'target/generated-sources/license/with-single-string-excluded-licenses-THIRD-PARTY.txt')
assert file.isFile()
assert file.text.contains('(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1')
assert file.text.contains('(Lesser General Public License (LGPL) v 3.0) Nuiton Processor :: Api')
file = new File(basedir, 'target/generated-sources/license/with-string-list-included-licenses-THIRD-PARTY.txt')
assert file.isFile()
assert file.text.contains('(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1')
assert file.text.contains('(Lesser General Public License (LGPL) v 3.0) Nuiton Processor :: Api')
file = new File(basedir, 'target/generated-sources/license/with-string-list-excluded-licenses-THIRD-PARTY.txt')
assert file.isFile()
assert file.text.contains('(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1')
assert file.text.contains('(Lesser General Public License (LGPL) v 3.0) Nuiton Processor :: Api')

return true
