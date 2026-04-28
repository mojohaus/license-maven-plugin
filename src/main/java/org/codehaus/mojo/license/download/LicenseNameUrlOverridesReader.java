package org.codehaus.mojo.license.download;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.license.LicenseNameUrlOverride;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads a {@code licenseNameUrlOverridesFile} XML file into a list of {@link LicenseNameUrlOverride} instances.
 *
 * <p>The expected file format is:</p>
 * <pre>
 * {@code
 * <licenseNameUrlOverrides>
 *   <licenseNameUrlOverride>
 *     <licenseNamePattern>(?i)apache.*2\.0</licenseNamePattern>
 *     <url>https://nexus.internal/licenses/apache-2.0.txt</url>
 *   </licenseNameUrlOverride>
 * </licenseNameUrlOverrides>
 * }
 * </pre>
 */
public class LicenseNameUrlOverridesReader {

    private LicenseNameUrlOverridesReader() {}

    /**
     * Reads the given file and returns a list of {@link LicenseNameUrlOverride} entries.
     * Returns an empty list if {@code overridesFile} is {@code null} or does not exist.
     *
     * @param overridesFile the XML file to read; may be {@code null}
     * @return a list of overrides; never {@code null}
     * @throws MojoExecutionException if the file cannot be read or parsed
     */
    public static List<LicenseNameUrlOverride> read(File overridesFile) throws MojoExecutionException {
        if (overridesFile == null || !overridesFile.exists()) {
            return Collections.emptyList();
        }
        try (InputStream in = Files.newInputStream(overridesFile.toPath())) {
            return read(in);
        } catch (Exception e) {
            throw new MojoExecutionException("Could not parse licenseNameUrlOverridesFile " + overridesFile, e);
        }
    }

    static List<LicenseNameUrlOverride> read(InputStream in)
            throws IOException, ParserConfigurationException, SAXException {
        final List<LicenseNameUrlOverride> result = new ArrayList<>();

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(in);
        doc.getDocumentElement().normalize();

        final NodeList overrideNodes = doc.getDocumentElement().getElementsByTagName("licenseNameUrlOverride");
        for (int i = 0; i < overrideNodes.getLength(); i++) {
            final Node node = overrideNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                result.add(parseOverride((Element) node));
            }
        }
        return result;
    }

    private static LicenseNameUrlOverride parseOverride(Element element) {
        final LicenseNameUrlOverride override = new LicenseNameUrlOverride();
        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if ("licenseNamePattern".equals(child.getNodeName())) {
                    override.setLicenseNamePattern(child.getTextContent());
                } else if ("url".equals(child.getNodeName())) {
                    override.setUrl(child.getTextContent());
                }
            }
        }
        return override;
    }
}
