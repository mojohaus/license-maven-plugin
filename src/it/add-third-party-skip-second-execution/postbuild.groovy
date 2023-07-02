/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2023 MojoHaus and Contributors
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

file = new File(basedir, 'build.log');
assert file.exists();
content = file.text;
assert 1 == content.count('[INFO] Writing third-party file to');
assert 1 == content.count('[INFO] Writing bundled third-party file to');
assert 1 == content.count('[INFO] All files are up to date, skip goal execution.');
assert 2 == content.count('[INFO] BUILD SUCCESS');
