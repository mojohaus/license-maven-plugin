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
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.common.ConsoleNotifier

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor

// The license hosts are reached through this proxy (see settings-proxy.xml). Text a stub returns can only get into
// a license file if the plugin requested the URL instead of using its bundled text.
WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(18080)
        .notifier(new ConsoleNotifier(false)))
wireMockServer.start()

configureFor(wireMockServer.port())

stubFor(get('/licenses/gpl-3.0.txt')
        .withHeader('Host', equalTo('www.gnu.org'))
        .willReturn(aResponse().withStatus(200).withHeader('Content-Type', 'text/plain').withBody('GPL text via WireMock')))
stubFor(get('/licenses/LICENSE-2.0.txt')
        .withHeader('Host', equalTo('www.apache.org'))
        .willReturn(aResponse().withStatus(200).withHeader('Content-Type', 'text/plain').withBody('ASL text via WireMock')))
stubFor(get('/licenses/gpl-3.0.html')
        .withHeader('Host', equalTo('www.gnu.org'))
        .willReturn(aResponse().withStatus(200).withHeader('Content-Type', 'text/html').withBody('<html>GPL page via WireMock</html>')))

context.put("wireMockServer", wireMockServer)
true
