/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 MojoHaus
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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

import java.nio.file.Files
import java.nio.file.Path

final Path licenses = basedir.toPath().resolve('target/generated-resources/licenses')

// default setting: URLs that return plain text are written from the bundled texts without a request
final Path gpl = licenses.resolve('gnu general public license v3.0 text - gpl-3.0.txt')
assert Files.exists(gpl)
assert gpl.toFile().text.contains('Version 3, 29 June 2007')
assert !gpl.toFile().text.contains('WireMock')

final Path asl = licenses.resolve('apache license 2.0 text - license-2.0.txt')
assert Files.exists(asl)
assert asl.toFile().text.contains('Version 2.0, January 2004')
assert !asl.toFile().text.contains('WireMock')

// a URL of an HTML page is downloaded as before
final Path page = licenses.resolve('gnu general public license v3.0 page - gpl-3.0.html')
assert Files.exists(page)
assert page.toFile().text.contains('GPL page via WireMock')

WireMockServer wireMockServer = context.get("wireMockServer")
def requested = wireMockServer.findAll(WireMock.anyRequestedFor(WireMock.urlMatching('/licenses/.*')))*.url
assert requested == ['/licenses/gpl-3.0.html'] : "Unexpected requests: ${requested}"

final String log = new File(basedir, 'build.log').text
assert log.contains('Wrote 2 license(s) from texts bundled with the plugin')

wireMockServer.stop()
