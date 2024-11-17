package org.codehaus.mojo.license.download;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.mojo.license.AbstractDownloadLicensesMojo;
import org.codehaus.mojo.license.AggregateDownloadLicensesMojo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class DownloadLicensesIT extends AbstractMojoTestCase {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadLicensesIT.class);

    public static final String LICENSE_AGGREGATE_DOWNLOAD_LICENSES = "license:" + AggregateDownloadLicensesMojo.GOAL;
    private final Parameter parameter;

    @XmlRootElement
    private static class DependencyInfos {
        @XmlElement
        List<DependencyInfo> dependencyInfos;

        public DependencyInfos() {
            this.dependencyInfos = new ArrayList<>();
        }

        public DependencyInfos(List<DependencyInfo> dependencyInfos) {
            this.dependencyInfos = dependencyInfos;
        }
    }

    @XmlRootElement
    private static class DependencyInfo {
        @XmlAttribute
        String name;

        @XmlAttribute
        String groupId;

        @XmlAttribute
        String artifactId;

        @XmlAttribute
        String version;

        @XmlAttribute
        String license;

        public DependencyInfo() {
        }

        public DependencyInfo(String name, String groupId, String artifactId, String version, String license) {
            this.name = name;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.license = license;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DependencyInfo)) return false;
            DependencyInfo that = (DependencyInfo) o;
            return Objects.equals(name, that.name)
                && Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version)
                && Objects.equals(license, that.license);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, groupId, artifactId, version, license);
        }

        @Override
        public String toString() {
            return "DependencyInfo{" + "name='"
                + name + '\'' + ", groupId='"
                + groupId + '\'' + ", artifactId='"
                + artifactId + '\'' + ", version='"
                + version + '\'' + ", license='"
                + license + '\'' + '}';
        }
    }

    public static class Parameter {
        final AbstractDownloadLicensesMojo.DataFormatting dataFormatting;
        final String pom;
        private final String expected;

        public Parameter(AbstractDownloadLicensesMojo.DataFormatting dataFormatting, String pom, String expected) {
            this.dataFormatting = dataFormatting;
            this.pom = pom;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "Order: " + dataFormatting.orderBy.toString();
        }
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Parameter> data() {
        AbstractDownloadLicensesMojo.DataFormatting dataFormatting1 = new AbstractDownloadLicensesMojo.DataFormatting();
        dataFormatting1.orderBy = AbstractDownloadLicensesMojo.DataFormatting.OrderBy.dependencyName;

        AbstractDownloadLicensesMojo.DataFormatting dataFormatting2 = new AbstractDownloadLicensesMojo.DataFormatting();
        dataFormatting2.orderBy = AbstractDownloadLicensesMojo.DataFormatting.OrderBy.dependencyPluginId;

        AbstractDownloadLicensesMojo.DataFormatting dataFormatting3 = new AbstractDownloadLicensesMojo.DataFormatting();
        dataFormatting3.orderBy = AbstractDownloadLicensesMojo.DataFormatting.OrderBy.licenseMatch;

        AbstractDownloadLicensesMojo.DataFormatting dataFormatting4 = new AbstractDownloadLicensesMojo.DataFormatting();
        dataFormatting4.orderBy = AbstractDownloadLicensesMojo.DataFormatting.OrderBy.licenseName;

        return Arrays.asList(
            new Parameter(dataFormatting1, "pom - orderBy.dependencyName.xml", "sortedByDependencyName.xml"),
            new Parameter(
                dataFormatting2, "pom - orderBy.dependencyPluginId.xml", "sortedByDependencyPluginId.xml"),
            new Parameter(dataFormatting3, "pom - orderBy.licenseMatch.xml", "sortedByLicenseMatch.xml"),
            new Parameter(dataFormatting4, "pom - orderBy.licenseName.xml", "sortedByLicenseName.xml"));
    }

    public DownloadLicensesIT(Parameter parameter) {
        super();
        this.parameter = parameter;
    }

    /**
     * Tests if all licenses are correctly formatted.
     *
     * @throws Exception Exception happened, e.g., at setup.
     */
    @Test
    public void testDataFormatting() throws Exception {
        super.setUp();

        File pom = getTestFile("src/test/resources/unit/AbstractDownloadLicensesMojoIT/" + parameter.pom);
        assertNotNull(pom);
        assertTrue(pom.exists());

        // "MavenSettingsBuilder" may be deprecated, but there is no alternative interface?!
        MavenSettingsBuilder mavenSettingsBuilder =
            (MavenSettingsBuilder) getContainer().lookup(MavenSettingsBuilder.ROLE);
        Settings settings = mavenSettingsBuilder.buildSettings();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        request.setLocalRepositoryPath(settings.getLocalRepository());
        request.setGoals(Collections.singletonList(LICENSE_AGGREGATE_DOWNLOAD_LICENSES));

        MavenExecutionRequestPopulator populator = getContainer().lookup(MavenExecutionRequestPopulator.class);
        populator.populateDefaults(request);

        Properties userProperties = new Properties();
        userProperties.setProperty("java.version", "11.0");
        request.setUserProperties(userProperties);

        DefaultMaven maven = (DefaultMaven) getContainer().lookup(Maven.class);

        MavenExecutionResult executed = maven.execute(request);
        if (executed.getExceptions() != null) {
            for (Throwable exception : executed.getExceptions()) {
                System.out.println("Error in executing \"testDownloadLicenses\"\n" + exception + "\n"
                    + Arrays.toString(exception.getStackTrace()));
            }
            assertEquals(1, executed.getExceptions().size());
            assertTrue(executed.getExceptions()
                .get(0).getMessage().contains("1 unique forbidden licenses found"));
        }

        checkResultingLicensesXml();
    }

    private void checkResultingLicensesXml()
        throws ParserConfigurationException, SAXException, IOException, JAXBException {
        Path testPath = Paths.get(getBasedir(), "src/test/resources/unit/AbstractDownloadLicensesMojoIT");
        Path generatedResourcesPath = Paths.get(testPath.toString(), "target/generated-resources");
        Path licensesPath = Paths.get(generatedResourcesPath.toString(), "licenses.xml");

        File licensesFile = licensesPath.toFile();
        if (!licensesFile.exists()) {
            throw new FileNotFoundException("Licenses file not found: " + licensesFile.getAbsolutePath());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(licensesFile);

        NodeList dependenciesRoot = document.getElementsByTagName("dependencies");
        assertEquals(1, dependenciesRoot.getLength());
        NodeList dependencies = dependenciesRoot.item(0).getChildNodes();
        List<DependencyInfo> dependencyInfos = new ArrayList<>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            if (dependencies.item(i).getNodeName().equals("dependency")) {
                NodeList dependency = dependencies.item(i).getChildNodes();
                String name = null;
                String groupId = null;
                String artifactId = null;
                String version = null;
                String license = null;
                for (int j = 0; j < dependency.getLength(); j++) {
                    if (dependency.item(j).getNodeName().equals("name")) {
                        name = dependency.item(j).getTextContent();
                    } else if (dependency.item(j).getNodeName().equals("groupId")) {
                        groupId = dependency.item(j).getTextContent();
                    } else if (dependency.item(j).getNodeName().equals("artifactId")) {
                        artifactId = dependency.item(j).getTextContent();
                    } else if (dependency.item(j).getNodeName().equals("version")) {
                        version = dependency.item(j).getTextContent();
                    } else if (dependency.item(j).getNodeName().equals("licenses")) {
                        Node licensesNode = dependency.item(j);
                        List<String> licenses = new ArrayList<>();
                        for (int k = 0; k < licensesNode.getChildNodes().getLength(); k++) {
                            Node licenseNode = licensesNode.getChildNodes().item(k);
                            if (licenseNode.getNodeName().equals("license")) {
                                for (int l = 0; l < licenseNode.getChildNodes().getLength(); l++) {
                                    Node licenseChild =
                                        licenseNode.getChildNodes().item(l);
                                    if (licenseChild.getNodeName().equals("name")) {
                                        licenses.add(licenseChild.getTextContent());
                                    }
                                }
                            }
                        }
                        if (!licenses.isEmpty()) {
                            licenses.sort(Comparator.naturalOrder());
                            license = licenses.get(0);
                        }
                    }
                }
                assertNotNull(groupId);
                assertNotNull(artifactId);
                assertNotNull(version);
                dependencyInfos.add(new DependencyInfo(name, groupId, artifactId, version, license));
                if (name == null) {
                    System.out.println("Dependency without name: " + groupId + ":" + artifactId + ":" + version);
                }
                System.out.println(
                    "Dependency: " + name + " (" + groupId + ":" + artifactId + ":" + version + ") - " + license);
            }
        }

        /*
        Comment this line in, if there have been changes in the data sorting and new files to check against, must be
        created.
        */
        // saveDependencyInfos(dependencyInfos);

        Path testResourcesPath = Paths.get(testPath.toString(), "src/test/resources");
        Path expectedPath = Paths.get(testResourcesPath.toString(), parameter.expected);

        JAXBContext jaxbSerializer = createJaxbSerializer();
        DependencyInfos expectedDependencyInfos =
            (DependencyInfos) jaxbSerializer.createUnmarshaller().unmarshal(expectedPath.toFile());

        assertEquals(
            expectedDependencyInfos.dependencyInfos.stream().map(Object::toString)
                .collect(Collectors.joining("\n"))
                + "\n != \n"
                + dependencyInfos.stream().map(Object::toString)
                .collect(Collectors.joining("\n")),
            expectedDependencyInfos.dependencyInfos.size(), dependencyInfos.size());

        for (int i = 0; i < dependencyInfos.size(); i++) {
            DependencyInfo expectedDependencyInfo = expectedDependencyInfos.dependencyInfos.get(i);
            DependencyInfo actualDependencyInfo = dependencyInfos.get(i);

            assertEquals(
                "Expected: " + expectedDependencyInfo.name + ", Sorted: " + actualDependencyInfo.name,
                expectedDependencyInfo,
                actualDependencyInfo);
        }
    }

    /**
     * Use this method if the sorting has changed or became in other ways incompatible to the standard files.
     * <p>
     * This method saves the created dependency infos to a file as a test-standard if you have checked manually
     * that this data is correct.
     *
     * @param dependencyInfos Created dependency infos.
     * @throws JAXBException JAXB exception at serializing into a file.
     * @throws IOException   File access exception.
     */
    @SuppressWarnings("unused")
    private static void saveDependencyInfos(List<DependencyInfo> dependencyInfos) throws JAXBException, IOException {
        DependencyInfos dependencyInfosXml = new DependencyInfos(dependencyInfos);
        JAXBContext jaxbContext = createJaxbSerializer();
        File tempFile = File.createTempFile("licensesSort", ".xml");
        jaxbContext.createMarshaller().marshal(dependencyInfosXml, tempFile);
        System.out.println("Sorted XML: " + tempFile.getAbsolutePath());
    }

    private static JAXBContext createJaxbSerializer() throws JAXBException {
        return JAXBContext.newInstance(Policy.class, DependencyInfos.class);
    }
}
