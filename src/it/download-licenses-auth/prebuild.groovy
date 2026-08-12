import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.common.ConsoleNotifier

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor

/*
 * Start a local WireMock server on port 18081 to serve license texts for both:
 *   /auth-licenses/apache-2.0.txt  - located under the serverUrl prefix (credentials expected)
 *   /public-licenses/mit.txt       - outside the serverUrl prefix (credentials must NOT be sent)
 */
WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(18081)
        .notifier(new ConsoleNotifier(false)))
wireMockServer.start()

configureFor(wireMockServer.port())

stubFor(get('/auth-licenses/apache-2.0.txt')
        .willReturn(aResponse()
                .withStatus(200)
                .withBody('Apache License 2.0 text served by Authenticated WireMock')))

stubFor(get('/public-licenses/mit.txt')
        .willReturn(aResponse()
                .withStatus(200)
                .withBody('MIT License text served by Public WireMock')))

context.put('wireMockServer', wireMockServer)
true
