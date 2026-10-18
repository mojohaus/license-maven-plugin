import java.nio.file.Files
import java.nio.file.Path

final Path basePath = basedir.toPath()
final Path licensesDir = basePath.resolve('target/licenses')

// ── EPL override: junit declares "Eclipse Public License 1.0" with a remote URL;
// the override should have redirected to our local epl-1.0.txt.
final boolean hasEpl = Files.list(licensesDir).anyMatch { f ->
    f.toFile().text.contains('Eclipse Public License - v 1.0')
}
assert hasEpl : "No license file found under ${licensesDir} containing the expected EPL content"

// ── Apache override: commons-logging declares "The Apache Software License, Version 2.0"
// with URL http://www.apache.org/licenses/LICENSE-2.0.txt;
// the override should have redirected to our local apache-2.0.txt.
final boolean hasApache = Files.list(licensesDir).anyMatch { f ->
    f.toFile().text.contains('Apache License Version 2.0')
}
assert hasApache : "No license file found under ${licensesDir} containing the expected Apache content"

return true
