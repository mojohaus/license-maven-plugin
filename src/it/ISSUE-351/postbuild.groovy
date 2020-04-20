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

file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY.txt');
assert file.exists();
content = file.text;
assert content.contains('(Eclipse Public License 2.0) jersey-container-servlet (org.glassfish.jersey.containers:jersey-container-servlet:2.30.1 - https://projects.eclipse.org/projects/ee4j.jersey/project/jersey-container-servlet');
assert !content.contains('(Apache License, 2.0) (BSD 2-Clause) (EDL 1.0) (EPL 2.0) (GPL2 w/ CPE) (MIT license) (Modified BSD) (Public Domain) (W3C license) (jQuery license) jersey-container-servlet (org.glassfish.jersey.containers:jersey-container-servlet:2.30.1 - https://projects.eclipse.org/projects/ee4j.jersey/project/jersey-container-servlet');


return true;