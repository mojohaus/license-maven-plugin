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

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/test/MyBean.java');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) 2112 License Test');
assert content.contains('MyBean.java - License Test :: MLICENSE-30 - License Test - 2112');
assert content.contains('org.codehaus.mojo.license.test-MLICENSE-30-1.1');
assert content.contains('#%L');
assert content.contains('%%');
assert content.contains('#L%');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/test/MyBean2.java');
assert file.isFile();

content = file.text;
assert content.contains('Copyright (C) 2010 Tony Do not update!');
assert !content.contains('License Test :: Will be updated!');
assert !content.contains('Copyright (C) 2112 License Test');
assert content.contains('MyBean2.java - License Test :: MLICENSE-30 - License Test - 2112');
assert content.contains('org.codehaus.mojo.license.test-MLICENSE-30-1.1');
assert content.contains('Copyright (C) 2010 Tony Do not update!');
assert !content.contains('Fake to be removed!');
assert content.contains('#%L');
assert content.contains('%%');
assert content.contains('#L%');

file = new File(basedir, 'src/main/java/org/codehaus/mojo/license/test/MyBean3.java');
assert file.isFile();

content = file.text;
assert content.contains('%%Ignore-License');
assert content.contains('Copyright (C) 2000 Codelutin Do not update!');
assert !content.contains('#%L');
assert !content.contains('#L%');
