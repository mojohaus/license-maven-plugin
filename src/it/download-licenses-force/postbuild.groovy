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

import java.nio.file.Path;
import java.nio.file.Files;

final Path basePath = basedir.toPath()

final Path asl2 = basePath.resolve('target/generated-resources/licenses/apache license 2.0 - license-2.0.txt')
assert Files.exists(asl2)
assert !asl2.text.contains('This content is fake.')
assert asl2.text.contains('Version 2.0, January 2004')

final Path lgpl21 = basePath.resolve('target/generated-resources/licenses/gnu lesser general public license v2.1 or later - lgpl-2.1.html')
assert Files.exists(lgpl21)
assert lgpl21.text.contains('Version 2.1, February 1999')

final Path expectedLicensesXml = basePath.resolve('licenses.expected.xml')
final Path licensesXml = basePath.resolve('target/generated-resources/licenses.xml')
assert expectedLicensesXml.text.equals(licensesXml.text)

final Path expectedLicensesErrorsXml = basePath.resolve('licenses-errors.expected.xml')
final Path licensesErrorsXml = basePath.resolve('target/generated-resources/licenses-errors.xml')
assert expectedLicensesErrorsXml.text.equals(licensesErrorsXml.text)

final Path log = basePath.resolve('build.log')
assert Files.exists(log)
assert log.text.contains('There were 2 download errors - check ')
