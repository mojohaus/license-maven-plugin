import groovy.xml.XmlParser
import org.xml.sax.SAXException

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Path
import java.nio.file.Paths
import java.security.Policy
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.stream.Collectors

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

/** Utility method to create {@link CustomLogger} from a dynamically parsed file. */
static CustomLogger newCustomLogger(Path logFile) {
    new CustomLogger(logFile)
}

/** For logging into the build.log. */
class CustomLogger {
    private final Logger logger = Logger.getLogger(CustomLogger.class
        .getName())
    private FileHandler fh = null

    CustomLogger(Path logFile) {
        try {
            fh = new FileHandler(logFile.toAbsolutePath().toString(), true)
        } catch (Exception e) {
            e.printStackTrace()
        }

        fh.setFormatter(new SimpleFormatter())
        logger.addHandler(fh)
    }

    void log(Level level, String message, Object... params) {
        logger.log(level, message, params)
    }
}

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
class DependencyInfos {
    @XmlElement
    List<DependencyInfo> dependencyInfos

    DependencyInfos() {
        this.dependencyInfos = new ArrayList<>()
    }

    DependencyInfos(List<DependencyInfo> dependencyInfos) {
        this.dependencyInfos = dependencyInfos
    }
}

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "dependencyInfo")
class DependencyInfo {
    @XmlAttribute
    String name

    @XmlAttribute
    String groupId

    @XmlAttribute
    String artifactId

    @XmlAttribute
    String version

    @XmlElement
    List<String> licenses

    DependencyInfo() {}

    DependencyInfo(String name, String groupId, String artifactId, String version, List<String> licenses) {
        this.name = name
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
        this.licenses = licenses
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof DependencyInfo)) {
            return false
        }
        DependencyInfo that = (DependencyInfo) o
        return Objects.equals(name, that.name)
            && Objects.equals(groupId, that.groupId)
            && Objects.equals(artifactId, that.artifactId)
            && Objects.equals(version, that.version)
            && Objects.equals(licenses, that.licenses)
    }

    @Override
    int hashCode() {
        return Objects.hash(name, groupId, artifactId, version, licenses)
    }

    @Override
    String toString() {
        return "DependencyInfo{" +
            "name='" + name + '\'' +
            ", groupId='" + groupId + '\'' +
            ", artifactId='" + artifactId + '\'' +
            ", version='" + version + '\'' +
            ", licenses='" + licenses + '\'' +
            '}'
    }
}

// -------------- Sort by name -------------------
static void checkResultingLicensesXml(CustomLogger log, File basedir, String expected)
    throws ParserConfigurationException, SAXException, IOException, JAXBException {
    Path testPath = basedir.toPath()
    Path generatedResourcesPath = Paths.get(testPath.toString(), "target/generated-resources")
    Path licensesPath = Paths.get(generatedResourcesPath.toString(), "licenses.xml")

    File licensesFile = licensesPath.toFile()
    if (!licensesFile.exists()) {
        throw new FileNotFoundException("Licenses file not found: " + licensesFile.getAbsolutePath())
    }

    ArrayList<DependencyInfo> dependencyInfos = parseGeneratedLicensesXml(log, licensesFile)

    /*
    Comment this line in, if there have been changes in the data sorting and new files to check against, must be
    created.
    */
    // saveDependencyInfos(log, dependencyInfos)

    Path expectedPath = Paths.get(basedir.toString(), expected)

    JAXBContext jaxbSerializer = createJaxbSerializer()
    DependencyInfos expectedDependencyInfos =
        (DependencyInfos) jaxbSerializer.createUnmarshaller().unmarshal(expectedPath.toFile())

    assertEquals(
        expectedDependencyInfos.dependencyInfos.size(),
        dependencyInfos.size(),
        () -> "Expected:\n" + expectedDependencyInfos.dependencyInfos.stream()
            .map(Object::toString)
            .collect(Collectors.joining("\n"))
            + "\n != Actual:\n"
            + dependencyInfos.stream().map(Object::toString).collect(Collectors.joining("\n")))

    for (int i = 0; i < dependencyInfos.size(); i++) {
        DependencyInfo expectedDependencyInfo = expectedDependencyInfos.dependencyInfos.get(i)
        DependencyInfo actualDependencyInfo = dependencyInfos.get(i)

        assertEquals(
            expectedDependencyInfo,
            actualDependencyInfo,
            () -> "Expected: " + expectedDependencyInfo.name + ", Sorted: " + actualDependencyInfo.name)
    }
    log.log(Level.INFO, "{0} dependencies are sorted as expected.", dependencyInfos.size())
}

private static ArrayList<DependencyInfo> parseGeneratedLicensesXml(CustomLogger log, File licensesFile) {
    final def dependencyInfos = new ArrayList<DependencyInfo>()
    final def xml = new XmlParser().parse(licensesFile)

    def dependencies = xml.dependencies.dependency
    if (dependencies.isEmpty()) {
        throw new IllegalArgumentException("No dependencies found in: " + licensesFile.getAbsolutePath())
    }

    dependencies.each { dependency ->
        def name = dependency.name.text()
        def groupId = dependency.groupId.text()
        def artifactId = dependency.artifactId.text()
        def version = dependency.version.text()
        def licenses = dependency.licenses.license.name*.text().sort()

        // Filter this one out, since this is JDK version dependent.
        if (name == "JavaBeans Activation Framework") {
            return
        }

        assertNotNull(groupId)
        assertNotNull(artifactId)
        assertNotNull(version)
        dependencyInfos.add(new DependencyInfo(name, groupId, artifactId, version, licenses))

        if (name == null) {
            log.log(Level.INFO, "Dependency without name: {0}:{1}:{2}", groupId, artifactId, version)
        } else {
            log.log(Level.INFO, "Dependency: {0} ({1}:{2}:{3}) - {4}", name, groupId, artifactId, version, licenses)
        }
    }

    if (dependencyInfos.empty) {
        throw new IllegalArgumentException("No dependencies found in: " + licensesFile.getAbsolutePath())
    }
    return dependencyInfos
}

/**
 * Use this method if the sorting has changed or became in other ways incompatible to the standard files,
 * to create a new standard to test against.
 * <p>
 * This method saves the created dependency infos to a file as a test-standard if you have checked manually
 * that this data is correct.<br>
 * Look for the line "Sorted XML: " in the log output, to find the file and then copy its content into
 * the corresponding test-standard file (sortedBy...xml).
 *
 * @param dependencyInfos Created dependency infos.
 * @throws javax.xml.bind.JAXBException JAXB exception at serializing into a file.
 * @throws IOException   File access exception.
 */
@SuppressWarnings("unused")
private static void saveDependencyInfos(CustomLogger log, List<DependencyInfo> dependencyInfos) throws JAXBException, IOException {
    DependencyInfos dependencyInfosXml = new DependencyInfos(dependencyInfos)
    JAXBContext jaxbContext = createJaxbSerializer()
    File tempFile = File.createTempFile("licensesSort", ".xml")
    jaxbContext.createMarshaller().marshal(dependencyInfosXml, tempFile)
    /*
     Make this a warning to make it easy to find.
     Remember: This is only in the "target/it/[Test]/build.log" file, not in the normal log output.
     */
    log.log(Level.WARNING, "Sorted XML: {0}", tempFile.getAbsolutePath())
}

private static JAXBContext createJaxbSerializer() throws JAXBException {
    return JAXBContext.newInstance(Policy.class, DependencyInfos.class)
}