import com.github.tomakehurst.wiremock.WireMockServer
import java.nio.file.Files
import java.nio.file.Path

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.exactly

/*
 * Verify that:
 *   1. Both license files were successfully downloaded.
 *   2. Credentials (Authorization: Basic ...) were sent to the URL under serverUrl prefix.
 *   3. Credentials were NOT sent to the URL outside the serverUrl prefix.
 */

final Path basePath = basedir.toPath()
final Path licensesDir = basePath.resolve('target/generated-resources/licenses')
WireMockServer wireMockServer = context.get('wireMockServer')

try {
    // 1. Verify the auth-protected license file was downloaded
    final Path authLicenseFile = licensesDir.resolve('apache license 2.0 - apache-2.0.txt')
    assert Files.exists(authLicenseFile) : "Auth-protected license file was not downloaded: ${authLicenseFile}"
    assert authLicenseFile.toFile().text.contains('Authenticated WireMock') :
            "Auth-protected license file does not contain expected content"

    // 2. Verify the public license file was downloaded
    final Path publicLicenseFile = licensesDir.resolve('mit license - mit.txt')
    assert Files.exists(publicLicenseFile) : "Public license file was not downloaded: ${publicLicenseFile}"
    assert publicLicenseFile.toFile().text.contains('Public WireMock') :
            "Public license file does not contain expected content"

    // 3. Verify that the Authorization header was sent to the protected URL (credentials applied)
    wireMockServer.verify(getRequestedFor(urlEqualTo('/auth-licenses/apache-2.0.txt'))
            .withHeader('Authorization', matching('Basic .*')))

    // 4. Verify that the Authorization header was NOT sent to the public URL (no credential leakage)
    wireMockServer.verify(exactly(0), getRequestedFor(urlEqualTo('/public-licenses/mit.txt'))
            .withHeader('Authorization', matching('.*')))

    // 5. Verify the plugin identifies itself instead of using the Apache HttpClient default user agent
    wireMockServer.verify(getRequestedFor(urlEqualTo('/public-licenses/mit.txt'))
            .withHeader('User-Agent', matching('license-maven-plugin/\\d.+')))
} finally {
    wireMockServer.stop()
}
