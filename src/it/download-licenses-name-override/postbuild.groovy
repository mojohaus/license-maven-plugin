import java.nio.file.Files
import java.nio.file.Path

final Path basePath = basedir.toPath()
final Path licensesDir = basePath.resolve('target/licenses')

// ── EPL override: junit declares "Eclipse Public License 1.0" with a remote URL;
// the override should have redirected to our local epl-1.0.txt.
// ── Apache override: commons-logging declares "The Apache Software License, Version 2.0"
// with URL http://www.apache.org/licenses/LICENSE-2.0.txt;
// the override should have redirected to our local apache-2.0.txt.
boolean hasEpl = false
boolean hasApache = false

try (def stream = Files.list(licensesDir)) {
    stream.forEach { f ->
        String text = f.toFile().text
        if (text.contains('Eclipse Public License - v 1.0')
                && text.contains('This is a placeholder license text for integration testing purposes.')) {
            hasEpl = true
        }
        if (text.contains('Apache License Version 2.0')
                && text.contains('This is a placeholder license text for integration testing purposes.')) {
            hasApache = true
        }
    }
}

assert hasEpl : "No license file found under ${licensesDir} containing the expected EPL placeholder content"
assert hasApache : "No license file found under ${licensesDir} containing the expected Apache placeholder content"

return true
