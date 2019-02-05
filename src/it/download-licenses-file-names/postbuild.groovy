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

assert !Files.exists(basePath.resolve('target/generated-resources/licenses/apache license 2.0 - license-2.0.txt'))
assert !Files.exists(basePath.resolve('target/generated-resources/licenses/bsd license - license.html'))

final Path asl2 = basePath.resolve('target/generated-resources/licenses/asl2.txt')
assert Files.exists(asl2)
assert asl2.text.contains('Version 2.0, January 2004')

final Path bsdAntlr = basePath.resolve('target/generated-resources/licenses/bsd-antlr.html')
assert Files.exists(bsdAntlr)
assert bsdAntlr.text.contains('Copyright (c) 2012 Terence Parr and Sam Harwell')

final Path cddl = basePath.resolve('target/generated-resources/licenses/cddl-gplv2-ce.txt')
assert Files.exists(cddl)
assert new String(Files.readAllBytes(cddl), 'ISO-8859-1').contains('COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1')

final Path expectedLicensesXml = basePath.resolve('licenses.expected.xml')
final Path licensesXml = basePath.resolve('target/generated-resources/licenses.xml')
assert expectedLicensesXml.text.equals(licensesXml.text)

