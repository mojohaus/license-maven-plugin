/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
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

file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert content.contains('(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1 - http://commons.apache.org/logging)');
assert !content.contains('(Apache License, Version 2.0) JHLabs Image Processing Filters (com.jhlabs:filters:2.0.235 - http://www.jhlabs.com/ip/index.html)');
assert content.contains('(The Apache Software License, Version 2.0) JHLabs Image Processing Filters (com.jhlabs:filters:2.0.235 - http://www.jhlabs.com/ip/index.html)');

file = new File(basedir, 'target/generated-sources/license/META-INF/test-add-third-party-merge-license-THIRD-PARTY.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert content.contains('(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1 - http://commons.apache.org/logging)');
assert !content.contains('(Apache License, Version 2.0) JHLabs Image Processing Filters (com.jhlabs:filters:2.0.235 - http://www.jhlabs.com/ip/index.html)');
assert content.contains('(The Apache Software License, Version 2.0) JHLabs Image Processing Filters (com.jhlabs:filters:2.0.235 - http://www.jhlabs.com/ip/index.html)');

return true;
