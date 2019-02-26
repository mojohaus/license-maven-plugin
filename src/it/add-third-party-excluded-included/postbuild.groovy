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

file = new File(basedir, 'target/generated-sources/license/thirdWithoutScope.txt');
assert file.exists();
final String thirdWithoutScope = file.text;
assert !thirdWithoutScope.contains('The project has no dependencies.');
assert thirdWithoutScope.contains('commons-logging:commons-logging:1.1.1');
assert thirdWithoutScope.contains('org.nuiton:nuiton-utils:1.4');
assert thirdWithoutScope.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert thirdWithoutScope.contains('org.nuiton:maven-helper-plugin');
assert !thirdWithoutScope.contains('junit:junit:4.8.2');
assert thirdWithoutScope.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithScope.txt');
assert file.exists();
final String thirdWithScope = file.text;
assert !thirdWithScope.contains('The project has no dependencies.');
assert !thirdWithScope.contains('commons-logging:commons-logging:1.1.1');
assert !thirdWithScope.contains('org.nuiton:nuiton-utils:1.4');
assert !thirdWithScope.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert !thirdWithScope.contains('org.nuiton:maven-helper-plugin');
assert thirdWithScope.contains('junit:junit:4.8.2');
assert !thirdWithScope.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithoutGroup.txt');
assert file.exists();
final String thirdWithoutGroup = file.text;
assert !thirdWithoutGroup.contains('The project has no dependencies.');
assert thirdWithoutGroup.contains('commons-logging:commons-logging:1.1.1');
assert !thirdWithoutGroup.contains('org.nuiton:nuiton-utils:1.4');
assert !thirdWithoutGroup.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert thirdWithoutGroup.contains('junit:junit:4.8.2');
assert thirdWithoutGroup.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithoutArtifact.txt');
assert file.exists();
final String thirdWithoutArtifact = file.text;
assert !thirdWithoutArtifact.contains('The project has no dependencies.');
assert thirdWithoutArtifact.contains('commons-logging:commons-logging:1.1.1');
assert thirdWithoutArtifact.contains('org.nuiton:nuiton-utils:1.4');
assert !thirdWithoutArtifact.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert thirdWithoutArtifact.contains('junit:junit:4.8.2');
assert thirdWithoutArtifact.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithGroupWithoutArtifact.txt');
assert file.exists();
final String thirdWithGroupWithoutArtifact = file.text;
assert !thirdWithGroupWithoutArtifact.contains('The project has no dependencies.');
assert !thirdWithGroupWithoutArtifact.contains('commons-logging:commons-logging:1.1.1');
assert !thirdWithGroupWithoutArtifact.contains('org.nuiton:nuiton-utils:1.4');
assert !thirdWithGroupWithoutArtifact.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert thirdWithGroupWithoutArtifact.contains('org.nuiton:maven-helper-plugin');
assert !thirdWithGroupWithoutArtifact.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithoutGroupWithArtifact.txt');
assert file.exists();
final String thirdWithoutGroupWithArtifact = file.text;
assert thirdWithoutGroupWithArtifact.contains('The project has no dependencies.');
assert !thirdWithoutGroupWithArtifact.contains('commons-logging:commons-logging:1.1.1');
assert !thirdWithoutGroupWithArtifact.contains('org.nuiton:nuiton-utils:1.4');
assert !thirdWithoutGroupWithArtifact.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert !thirdWithoutGroupWithArtifact.contains('org.nuiton:maven-helper-plugin');
assert !thirdWithoutGroupWithArtifact.contains('org.mockito:mockito-all:1.10.19');


file = new File(basedir, 'target/generated-sources/license/thirdWithGroupWithArtifact.txt');
assert file.exists();
final String thirdWithGroupWithArtifact = file.text;
assert !thirdWithGroupWithArtifact.contains('The project has no dependencies.');
assert !thirdWithGroupWithArtifact.contains('commons-logging:commons-logging:1.1.1');
assert thirdWithGroupWithArtifact.contains('org.nuiton:nuiton-utils:1.4');
assert thirdWithGroupWithArtifact.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert thirdWithGroupWithArtifact.contains('org.nuiton:maven-helper-plugin');
assert !thirdWithGroupWithArtifact.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithoutGroupWithoutArtifact.txt');
assert file.exists();
final String thirdWithoutGroupWithoutArtifact = file.text;
assert !thirdWithoutGroupWithoutArtifact.contains('The project has no dependencies.');
assert thirdWithoutGroupWithoutArtifact.contains('commons-logging:commons-logging:1.1.1');
assert !thirdWithoutGroupWithoutArtifact.contains('org.nuiton:nuiton-utils:1.4');
assert !thirdWithoutGroupWithoutArtifact.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert !thirdWithoutGroupWithoutArtifact.contains('org.nuiton:maven-helper-plugin');
assert thirdWithoutGroupWithoutArtifact.contains('junit:junit:4.8.2');
assert thirdWithoutGroupWithoutArtifact.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithoutType.txt');
assert file.exists();
final String thirdWithoutType = file.text;
assert !thirdWithoutType.contains('The project has no dependencies.');
assert thirdWithoutType.contains('commons-logging:commons-logging:1.1.1');
assert thirdWithoutType.contains('org.nuiton:nuiton-utils:1.4');
assert thirdWithoutType.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert thirdWithoutType.contains('org.nuiton:maven-helper-plugin');
assert !thirdWithoutType.contains('org.wildfly:wildfly-ejb-client-bom:9.0.1.Final');
assert thirdWithoutType.contains('junit:junit:4.8.2');
assert thirdWithoutType.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithType.txt');
assert file.exists();
final String thirdWithType = file.text;
assert !thirdWithType.contains('The project has no dependencies.');
assert !thirdWithType.contains('commons-logging:commons-logging:1.1.1');
assert !thirdWithType.contains('org.nuiton:nuiton-utils:1.4');
assert !thirdWithType.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert !thirdWithType.contains('org.nuiton:maven-helper-plugin');
assert thirdWithType.contains('org.wildfly:wildfly-ejb-client-bom:9.0.1.Final');
assert !thirdWithType.contains('junit:junit:4.8.2');
assert !thirdWithType.contains('org.mockito:mockito-all:1.10.19');

file = new File(basedir, 'target/generated-sources/license/thirdWithoutOptional.txt');
assert file.exists();
final String thirdWithoutOptional = file.text;
assert !thirdWithoutOptional.contains('The project has no dependencies.');
assert thirdWithoutOptional.contains('commons-logging:commons-logging:1.1.1');
assert thirdWithoutOptional.contains('org.nuiton:nuiton-utils:1.4');
assert thirdWithoutOptional.contains('org.nuiton.i18n:nuiton-i18n:1.2.2');
assert thirdWithoutOptional.contains('org.nuiton:maven-helper-plugin');
assert thirdWithoutOptional.contains('org.wildfly:wildfly-ejb-client-bom:9.0.1.Final');
assert thirdWithoutOptional.contains('junit:junit:4.8.2');
assert !thirdWithoutOptional.contains('org.mockito:mockito-all:1.10.19');

return true;
