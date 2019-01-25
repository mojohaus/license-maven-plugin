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

file = new File(basedir, 'target/site/aggregate-third-party-report.html');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert !content.contains('/third-party-report.html#');
assert content.contains('<a href="#commons-logging:commons-logging:1.1.1">commons-logging:commons-logging:1.1.1</a>');
assert content.contains('<td>The Apache Software License, Version 2.0</td>'); // TODO Should be a link
assert content.contains('<a href="#Overview">Back to top</a>');

file = new File(basedir, 'target/site/third-party-report.html');
assert !file.exists();
