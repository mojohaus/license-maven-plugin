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

return {
    final String id = 'pre-1.18'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path asl2 = outputBase.resolve('licenses/apache license 2.0 - license-2.0.txt')
    assert Files.exists(asl2)
    assert asl2.text.contains('Version 2.0, January 2004')

    final Path bsdAsm = outputBase.resolve('licenses/bsd 3-clause asm - license.txt')
    assert Files.exists(bsdAsm)
    assert bsdAsm.text.contains('ASM: a very small and fast Java bytecode manipulation framework')

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {
    final String id = 'since-1.18'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path asl2 = outputBase.resolve('licenses/apache-license-2.0-license-2.0.txt')
    assert Files.exists(asl2)
    assert asl2.text.contains('Version 2.0, January 2004')

    final Path bsdAsm = outputBase.resolve('licenses/bsd-3-clause-asm-license.txt')
    assert Files.exists(bsdAsm)
    assert bsdAsm.text.contains('ASM: a very small and fast Java bytecode manipulation framework')

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {
    final String id = 'artifact-filters-url'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path bsdAsm = outputBase.resolve('licenses/bsd 3-clause asm - license.txt')
    assert !Files.exists(bsdAsm)

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {
    final String id = 'no-download'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path asl2 = outputBase.resolve('licenses/apache-license-2.0-license-2.0.txt')
    assert Files.exists(asl2)
    assert asl2.text.contains('Fake content')

    final Path bsdAsm = outputBase.resolve('licenses/bsd-3-clause-asm-license.txt')
    assert Files.exists(bsdAsm)
    assert bsdAsm.text.contains('Fake content')

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {
    final String id = 'delete-orphans'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path asl2 = outputBase.resolve('licenses/apache-license-2.0-license-2.0.txt')
    assert Files.exists(asl2)
    assert asl2.text.contains('Fake content')

    final Path bsdAsm = outputBase.resolve('licenses/bsd-3-clause-asm-license.txt')
    assert Files.exists(bsdAsm)
    assert bsdAsm.text.contains('Fake content')

    final Path fooBar = outputBase.resolve('licenses/foo-bar-license.txt')
    assert !Files.exists(fooBar)

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {
    final String id = 'insert-versions'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path expectedLicensesXml = basePath.resolve('licenses-'+ id +'.expected.xml')
    final Path licensesXml = outputBase.resolve('licenses.xml')
    assert expectedLicensesXml.text.equals(licensesXml.text)
    return true
}() && {
    final String id = 'content-sanitizers'
    final Path outputBase = basePath.resolve('target/' + id)

    final Path asl2 = outputBase.resolve('licenses/apache-license-2.0-apache-2.0.txt')
    assert Files.exists(asl2)
    final Path expectedAsl2 = basePath.resolve('src/license/'+ id +'/apache-2.0.expected.txt')
    assert expectedAsl2.text.equals(asl2.text)

    final Path bsdAsm = outputBase.resolve('licenses/bsd-3-clause-asm-bsd3-asm.txt')
    assert Files.exists(bsdAsm)
    final Path expectedBsdAsm = basePath.resolve('src/license/'+ id +'/bsd3-asm.expected.txt')
    assert expectedBsdAsm.text.equals(bsdAsm.text)

    return true
}()
