import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Path
import java.nio.file.Paths
import java.security.Policy
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

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
static void checkResultingLicensesXml(Logger log, File basedir, String expected)
    throws ParserConfigurationException, SAXException, IOException, JAXBException {
    Path testPath = basedir.toPath()
    Path generatedResourcesPath = Paths.get(testPath.toString(), "target/generated-resources")
    Path licensesPath = Paths.get(generatedResourcesPath.toString(), "licenses.xml")

    File licensesFile = licensesPath.toFile()
    if (!licensesFile.exists()) {
        throw new FileNotFoundException("Licenses file not found: " + licensesFile.getAbsolutePath())
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
    DocumentBuilder builder = factory.newDocumentBuilder()
    Document document = builder.parse(licensesFile)

    NodeList dependenciesRoot = document.getElementsByTagName("dependencies")
    assertEquals(1, dependenciesRoot.getLength())
    NodeList dependencies = dependenciesRoot.item(0).getChildNodes()
    if (dependencies.getLength() == 0) {
        throw new IllegalArgumentException("No dependencies found in: " + licensesFile.getAbsolutePath())
    }
    List<DependencyInfo> dependencyInfos = new ArrayList<>()
    dependenciesLoop:
    for (int i = 0; i < dependencies.getLength(); i++) {
        if (dependencies.item(i).getNodeName().equals("dependency")) {
            NodeList dependency = dependencies.item(i).getChildNodes()
            String name = null
            String groupId = null
            String artifactId = null
            String version = null
            List<String> licenses = new ArrayList<>()
            for (int j = 0; j < dependency.getLength(); j++) {
                if (dependency.item(j).getNodeName().equals("name")) {
                    name = dependency.item(j).getTextContent()
                    // Filter this one out, since this is JDK version dependent.
                    if ("JavaBeans Activation Framework".equals(name)) {
                        continue dependenciesLoop
                    }
                } else if (dependency.item(j).getNodeName().equals("groupId")) {
                    groupId = dependency.item(j).getTextContent()
                } else if (dependency.item(j).getNodeName().equals("artifactId")) {
                    artifactId = dependency.item(j).getTextContent()
                } else if (dependency.item(j).getNodeName().equals("version")) {
                    version = dependency.item(j).getTextContent()
                } else if (dependency.item(j).getNodeName().equals("licenses")) {
                    Node licensesNode = dependency.item(j)
                    for (int k = 0; k < licensesNode.getChildNodes().getLength(); k++) {
                        Node licenseNode = licensesNode.getChildNodes().item(k)
                        if (licenseNode.getNodeName().equals("license")) {
                            for (int l = 0; l < licenseNode.getChildNodes().getLength(); l++) {
                                Node licenseChild =
                                    licenseNode.getChildNodes().item(l)
                                if (licenseChild.getNodeName().equals("name")) {
                                    licenses.add(licenseChild.getTextContent())
                                }
                            }
                        }
                    }
                    if (!licenses.isEmpty()) {
                        licenses.sort(Comparator.naturalOrder())
                    }
                }
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
    }

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
private static void saveDependencyInfos(Logger log, List<DependencyInfo> dependencyInfos) throws JAXBException, IOException {
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