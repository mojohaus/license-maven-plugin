/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 Alessandro Ballarin
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


file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY-missing-file-only.txt');
assert file.exists();
content = file.text;
assert content.contains('(CDDL-1.0 - Common Development and Distribution License 1.0) connector-api (javax.resource:connector-api:1.5 - no url defined)');
assert content.contains('(Apache-2.0 - Apache License 2.0) Jettison (org.codehaus.jettison:jettison:1.1 - no url defined)');
assert content.contains('(The JSON License) JSON (JavaScript Object Notation) (org.json:json:20070829 - http://www.json.org/java/index.html)');

file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY-missing-file-url-only.txt');
assert file.exists();
content = file.text;
assert content.contains('(CDDL-1.0 - Common Development and Distribution License 1.0) connector-api (javax.resource:connector-api:1.5 - no url defined)');
assert content.contains('(Apache-2.0 - Apache License 2.0) Jettison (org.codehaus.jettison:jettison:1.1 - no url defined)');
assert content.contains('(The JSON License) JSON (JavaScript Object Notation) (org.json:json:20070829 - http://www.json.org/java/index.html)');

file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY-missing-file-both.txt');
assert file.exists();
content = file.text;
assert content.contains('(CDDL-1.0 - Common Development and Distribution License 1.0) connector-api (javax.resource:connector-api:1.5 - no url defined)');
assert content.contains('(Apache-2.0 - Apache License 2.0) Jettison (org.codehaus.jettison:jettison:1.1 - no url defined)');
assert content.contains('(The JSON License) JSON (JavaScript Object Notation) (org.json:json:20070829 - http://www.json.org/java/index.html)');
