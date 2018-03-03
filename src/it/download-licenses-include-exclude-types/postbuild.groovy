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
def assertExistsFile(file)
{
    if ( !file.exists() || file.isDirectory() )
    {
        println(file.getAbsolutePath() + " file is missing or a directory.")
        assert false
    }
    assert true
}

def assertNotExistsFile(file)
{
  if ( file.exists() )
  {
    println(file.getAbsolutePath() + " file should not exists.")
    assert false
  }
  assert true
}

file = new File(basedir, 'target/generated-resources/licenses1/eclipse public license 1.0 - epl-v10.html');
assertExistsFile(file);

file = new File(basedir, 'target/generated-resources/licenses2/eclipse public license 1.0 - epl-v10.html');
assertNotExistsFile(file);

file = new File(basedir, 'target/generated-resources/licenses1/lgpl - lgpl-2.1.txt');
assertNotExistsFile(file);
file = new File(basedir, 'target/generated-resources/licenses2/lgpl - lgpl-2.1.txt');
assertExistsFile(file);

file = new File(basedir, 'target/generated-resources/licenses1.xml');
assertExistsFile(file);
file = new File(basedir, 'target/generated-resources/licenses2.xml');
assertExistsFile(file);

return true;
