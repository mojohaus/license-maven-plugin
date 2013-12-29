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

file = new File(basedir, 'consume-db/target/site/third-party-report.html');
assert file.exists();
content = file.text;
//assert content.contains('spring-oxm:1.5.8">org.springframework.ws:spring-oxm:1.5.8</a></td><td>compile</td><td></td><td>bundle</td><td>The Apache Software License, Version 2.0');
assert content.contains('spring-oxm:1.5.8">org.springframework.ws:spring-oxm:1.5.8</a></td>');

return true;
