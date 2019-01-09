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

antlr = '(Unknown license) antlr (antlr:antlr:2.7.1 - no url defined)'
commonsLang = '(Apache License, Version 2.0) Apache Commons Lang (org.apache.commons:commons-lang3:3.8.1 - http://commons.apache.org/proper/commons-lang/)'
commonsLogging = '(The Apache Software License, Version 2.0) Commons Logging (commons-logging:commons-logging:1.1.1 - http://commons.apache.org/logging)'

file = new File(basedir, 'target/generated-sources/license/third.txt');
assert file.exists();
content = file.text;
assert content.contains(antlr);
assert content.indexOf(antlr) < content.indexOf(commonsLogging);
assert content.indexOf(commonsLang) < content.indexOf(commonsLogging);

file = new File(basedir, 'target/generated-sources/license/test/third.txt');
assert file.exists();
content = file.text;
assert content.contains(antlr);
assert content.indexOf(antlr) < content.indexOf(commonsLogging);
assert content.indexOf(commonsLang) < content.indexOf(commonsLogging);
