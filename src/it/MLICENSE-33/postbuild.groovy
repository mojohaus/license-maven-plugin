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

File target = new File(basedir, "target");
File defaultThirdPartyFile = new File(target, "generated-sources/license/THIRD-PARTY.txt");
File reportThirdPartyFile = new File(target, "site/third-party-report.html");

assert target.isDirectory();
assert defaultThirdPartyFile.isFile();
assert reportThirdPartyFile.isFile();

String content = reportThirdPartyFile.text

String[] expectedContents = ["commons-logging:commons-logging:1.1.1",
        "commons-primitives:commons-primitives:1.0",
        "woodstox:wstx-asl:3.2.7"]

for (expected in expectedContents) {
  assert content.contains(expected)
}
