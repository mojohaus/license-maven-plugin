import java.nio.file.Files
import java.nio.file.Paths

scriptWithHeader = Paths.get(basedir.getAbsolutePath(), "src", "main", "bash", "license-test.sh")
backupFile = Paths.get(basedir.getAbsolutePath(), "src", "main", "bash", "license-test.sh~")

if(!Files.isExecutable(scriptWithHeader)) {
    println("Executable bit of the file $scriptWithHeader was cleared")
    return false
}

if(!Files.isExecutable(backupFile)) {
    println("Executable bit of the backup file $backupFile was cleared")
    reutrn false
}

return true
