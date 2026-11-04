import java.nio.file.Files
import java.nio.file.Paths

scriptWithHeader = Paths.get(basedir.getAbsolutePath(), "src", "main", "bash", "license-test.sh")
backupFile = Paths.get(basedir.getAbsolutePath(), "src", "main", "bash", "license-test.sh~")

assert scriptWithHeader.toFile().text.contains("# #%L") : "No header was added to $scriptWithHeader"
assert Files.isExecutable(scriptWithHeader) : "Executable bit of the file $scriptWithHeader was cleared"
assert Files.isExecutable(backupFile) : "Executable bit of the backup file $backupFile was cleared"
