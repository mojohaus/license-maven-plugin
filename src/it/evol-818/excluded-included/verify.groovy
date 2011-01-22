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

file = new File(basedir, 'target/generated-sources/license/thirdWithoutGroup.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert content.contains('commons-logging:commons-logging:1.1.1');
assert !content.contains('org.nuiton:nuiton-utils:1.4');
assert !content.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');


file = new File(basedir, 'target/generated-sources/license/thirdWithoutArtifact.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert content.contains('commons-logging:commons-logging:1.1.1');
assert content.contains('org.nuiton:nuiton-utils:1.4');
assert !content.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');


file = new File(basedir, 'target/generated-sources/license/thirdWithGroupWithoutArtifact.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert !content.contains('commons-logging:commons-logging:1.1.1');
assert !content.contains('org.nuiton:nuiton-utils:1.4');
assert !content.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert content.contains('org.nuiton:maven-helper-plugin');


file = new File(basedir, 'target/generated-sources/license/thirdWithoutGroupWithArtifact.txt');
assert file.exists();
content = file.text;
assert content.contains('the project has no dependencies.');
assert !content.contains('commons-logging:commons-logging:1.1.1');
assert !content.contains('org.nuiton:nuiton-utils:1.4');
assert !content.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert !content.contains('org.nuiton:maven-helper-plugin');


file = new File(basedir, 'target/generated-sources/license/thirdWithGroupWithArtifact.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert !content.contains('commons-logging:commons-logging:1.1.1');
assert content.contains('org.nuiton:nuiton-utils:1.4');
assert content.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert content.contains('org.nuiton:maven-helper-plugin');


file = new File(basedir, 'target/generated-sources/license/thirdWithoutGroupWithoutArtifact.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert content.contains('commons-logging:commons-logging:1.1.1');
assert !content.contains('org.nuiton:nuiton-utils:1.4');
assert !content.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert !content.contains('org.nuiton:maven-helper-plugin');

return true;
