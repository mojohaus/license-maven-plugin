import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor

/*
 * Serve two license texts on port 18083. The postbuild script counts the requests.
 */
new File(basedir, 'license-cache').deleteDir()

WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(18083)
        .notifier(new ConsoleNotifier(false)))
wireMockServer.start()
configureFor(wireMockServer.port())

stubFor(get('/licenses/apache-2.0.txt')
        .willReturn(aResponse()
                .withStatus(200)
                .withHeader('Content-Type', 'text/plain; charset=UTF-8')
                .withHeader('ETag', '"apache-v1"')
                .withBody('Apache License 2.0 text from the server')))

stubFor(get('/licenses/mit.txt')
        .willReturn(aResponse()
                .withStatus(200)
                .withHeader('Content-Type', 'text/plain; charset=UTF-8')
                .withBody('MIT License text from the server')))

context.put('wireMockServer', wireMockServer)
true
