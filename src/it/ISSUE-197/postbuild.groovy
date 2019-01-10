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

Path basePath = basedir.toPath()
Path licensesXmlPath = basePath.resolve('target/generated-resources/licenses.xml')
Path aslPath20 = basePath.resolve('target/generated-resources/licenses/apache license 2.0 - license-2.0.txt')
Path aslPath11 = basePath.resolve('target/generated-resources/licenses/asl-1.1.txt')

assert Files.exists(licensesXmlPath)
assert Files.exists(aslPath20)
assert Files.exists(aslPath11)

assert aslPath20.text.contains(' Version 2.0, January 2004')
assert aslPath11.text.contains('The Apache Software License, Version 1.1')
