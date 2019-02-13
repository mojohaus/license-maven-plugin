/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 - Falco Nikolas
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

// verify that missingFileUrl taken into account
assert content.contains('Unknown host this.url.does.not.exists') || content.contains('No such host is known (this.url.does.not.exists)');