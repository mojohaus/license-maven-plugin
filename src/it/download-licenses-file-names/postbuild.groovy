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

import java.nio.file.Path
import java.nio.file.Files
import groovy.transform.Field

@Field Path basePath = basedir.toPath()

return {
    final String id = 'basic'
    final Path outputBase = basePath.resolve('target/' + id)

    assert !Files.exists(outputBase.resolve('licenses/apache license 2.0 - license-2.0.txt'))
    assert !Files.exists(outputBase.resolve('licenses/bsd license - license.html'))

    final Path asl2 = outputBase.resolve('licenses/asl2.txt')
    assert Files.exists(asl2)
    assert asl2.text.contains('Version 2.0, January 2004')

    final Path bsdAntlr = outputBase.resolve('licenses/bsd-antlr.html')
    assert Files.exists(bsdAntlr)
    assert bsdAntlr.text.contains('Copyright (c) 2012 Terence Parr and Sam Harwell')

    final Path cddl = outputBase.resolve('licenses/cddl-gplv2-ce.txt')
    assert Files.exists(cddl)
    assert new String(Files.readAllBytes(cddl), 'ISO-8859-1').contains('COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1')

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {
    final String id = 'spdx'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path asl2 = outputBase.resolve('licenses/Apache-2.0.txt')
    assert Files.exists(asl2)
    assert asl2.text.contains('Version 2.0, January 2004')

    final Path bsdAntlr = outputBase.resolve('licenses/bsd-antlr.html')
    assert Files.exists(bsdAntlr)
    assert bsdAntlr.text.contains('Copyright (c) 2012 Terence Parr and Sam Harwell')

    final Path cddl = outputBase.resolve('licenses/cddl-gplv2-ce.txt')
    assert Files.exists(cddl)
    assert new String(Files.readAllBytes(cddl), 'ISO-8859-1').contains('COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1')

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {

    final String id = 'cleanLicDir'

    final Path outputBase = basePath.resolve('target/' + id)

    final Path asl2 = outputBase.resolve('licenses/Apache-2.0.txt')
    assert Files.exists(asl2)
    assert asl2.text.contains('Version 2.0, January 2004')

    final Path bsdAntlr = outputBase.resolve('licenses/bsd-license-from-config.html')
    assert Files.exists(bsdAntlr)
    assert bsdAntlr.text.contains('Copyright (c) 2012 Terence Parr and Sam Harwell')

    final Path cddl = outputBase.resolve('licenses/cddl-gplv2-ce.txt')
    assert Files.exists(cddl)
    assert new String(Files.readAllBytes(cddl), 'ISO-8859-1').contains('COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1')

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)

    final Path foo = basePath.resolve('target/cleanLicDir/licenses/foo.txt')
    assert !Files.exists(foo)

    return true
}()


