import java.nio.file.Files
import java.nio.file.Path

final Path basePath = basedir.toPath()
final String baseUri = basePath.toUri().toString()

// Replace the %project.baseUri% placeholder in the override file with the actual file:// URI
// so that the LicenseDownloader can resolve the local epl-1.0.txt file.
final Path overridesFile = basePath.resolve('src/license/license-name-url-overrides.xml')
String content = new String(Files.readAllBytes(overridesFile), 'utf-8')
content = content.replace('%project.baseUri%', baseUri)
Files.write(overridesFile, content.getBytes('utf-8'))

return true
