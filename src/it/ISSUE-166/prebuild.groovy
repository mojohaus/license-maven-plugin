import java.nio.file.Files
import java.nio.file.Paths

// Ensure the file is executable to begin with.
scriptWithoutHeader = Paths.get(basedir.getAbsolutePath(), "src", "main", "bash", "license-test.sh")

if(!Files.isExecutable(scriptWithoutHeader))
    scriptWithoutHeader.toFile().setExecutable(true, true)

if(!Files.isExecutable(scriptWithoutHeader)) {
    println("File $scriptWithoutHeader is not executable")
    return false
}
return true