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

file = new File( basedir, 'LICENSE.txt' );
assert file.isFile()

String[] notExpectedContents = ["MYLicense!",
        ";; This line will be removed!1"];

String[] expectedContents = ["The License!",
        "ArtifactId: test-update-project-license-MLICENSE-92",
        "Property versionId: 1.0.a",
        "ProjectName: License Test :: update-project-license-MLICENSE-92",
        "OrganisationName: License Test",
        "CopyrightYear: 2013",
        "CopyrightOwners: copyrightOwners",
        "ExtraParam: myValue"];

content = file.text;

for (expected in notExpectedContents) {
  assert !content.contains(expected)
}

for (expected in expectedContents) {
  assert content.contains(expected)
}
