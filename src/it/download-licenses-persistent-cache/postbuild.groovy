import com.github.tomakehurst.wiremock.WireMockServer

import static com.github.tomakehurst.wiremock.client.WireMock.exactly
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

WireMockServer wireMockServer = context.get('wireMockServer')
try {
    // The second run came after "clean": its license files were written from the cache
    def licensesDir = new File(basedir, 'target/generated-resources/licenses')
    def apache = new File(licensesDir, 'apache license 2.0 - apache-2.0.txt')
    def mit = new File(licensesDir, 'mit license - mit.txt')
    assert apache.isFile() : "missing ${apache}"
    assert mit.isFile() : "missing ${mit}"
    assert apache.text == 'Apache License 2.0 text from the server'
    assert mit.text == 'MIT License text from the server'

    // Each URL reached the server once, although the plugin ran twice
    wireMockServer.verify(exactly(1), getRequestedFor(urlEqualTo('/licenses/apache-2.0.txt')))
    wireMockServer.verify(exactly(1), getRequestedFor(urlEqualTo('/licenses/mit.txt')))

    // The entries are in the cache directory
    def entries = new File(basedir, 'license-cache').listFiles({ f -> f.name.endsWith('.entry') } as FileFilter)
    assert entries != null && entries.length == 2 : "cache entries: ${entries}"

    def log = new File(basedir, 'build.log').text
    assert log.contains('2 license text(s) from the cache') : 'second run did not report cache use'
    assert log.contains('0 license text(s) from the cache') && log.contains('2 downloaded') :
            'first run did not report downloads'
} finally {
    wireMockServer.stop()
}
true
