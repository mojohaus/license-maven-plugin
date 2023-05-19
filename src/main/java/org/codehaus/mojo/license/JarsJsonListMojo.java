package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generates JSON file with list of dependencies from jars folder.
 *
 */
@Mojo( name = "jars-json-list", requiresProject = false, requiresDirectInvocation = false, defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class JarsJsonListMojo
    extends AbstractMojo
{

    private static final Pattern JAR_VERSION_PATTERN = Pattern.compile("-r?\\d");
    private static final Pattern POM_XML_ENTRY = Pattern.compile("^META-INF/maven/.+/pom\\.xml$");
    private static final Map<String, XPathExpression[]> POM_INFO_XPATHS = getPomInfoXPathExpressions();

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    @Parameter( property = "jarDirectory", defaultValue = "dist/lib" )
    private String jarDirectory;

    @Parameter( property = "jarInfoFile", defaultValue = "jar-info.properties" )
    private String jarInfoFile;

    @Parameter( property = "outputFile", defaultValue = "third-party.json" )
    private String outputFile;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("JARs directory: " + jarDirectory);
            getLog().info("JAR information file: " + jarInfoFile);
            getLog().info("Output file: " + outputFile);

            Properties artifacts = new Properties();
            artifacts.load(new FileReader(jarInfoFile));

            List<Map<String, String>> infos = Files.list(Paths.get(jarDirectory))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .map(path -> extractJarInfo(path, artifacts))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            JSONArray listJson = new JSONArray();
            infos.forEach(info -> {
                JSONObject infoJson = new JSONObject();
                info.forEach(infoJson::put);
                listJson.put(infoJson);
            });

            Files.write(Paths.get(outputFile), listJson.toString(2).getBytes());
        } catch (IOException ex) {
            throw new RuntimeException("Failed read/write file", ex);
        }
    }

    private static Map<String, XPathExpression[]> getPomInfoXPathExpressions() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Map<String, XPathExpression[]> exprs = new LinkedHashMap<>();
        try {
            exprs.put("name",  new XPathExpression[]{xpath.compile("/project/name")});
            exprs.put("groupId", new XPathExpression[]{
                    xpath.compile("/project/groupId"),
                    xpath.compile("/project/parent/groupId")
            });
            exprs.put("artifactId", new XPathExpression[]{xpath.compile("/project/artifactId")});
            exprs.put("version", new XPathExpression[]{
                    xpath.compile("/project/version"),
                    xpath.compile("/project/parent/version")
            });
        } catch (XPathExpressionException ex) {
            throw new RuntimeException("Failed to compile XPath expression", ex);
        }
        return exprs;
    }

    private Map<String,String> extractJarInfo(Path jarPath, Properties artifacts) {
        String jarName = jarPath.getFileName().toString();
        getLog().info("Processing " + jarName);

        try {
            ZipFile zipFile = new ZipFile(jarPath.toFile());
            for (ZipEntry entry : Collections.list(zipFile.entries())) {
                if (entry.isDirectory())
                    continue;

                String entryName = entry.getName();
                if (POM_XML_ENTRY.matcher(entryName).matches()) {
                    getLog().debug("Found pom.xml");
                    Map<String, String> mavenCoordinates = getPomInfo(zipFile, entry);
                    getLog().debug("Maven coordinates: " + mavenCoordinates);
                    return mavenCoordinates;
                }
            }

            Map<String, String> jarCoordinates = getJarInfo(jarName, artifacts);
            getLog().debug("Jar coordinates: " + jarCoordinates);
            return jarCoordinates;
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            getLog().error("Exception raised during processing of " + jarName, ex);
        }
        return null;
    }

    private Map<String, String> getPomInfo(ZipFile zipFile, ZipEntry entry) throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> pomInfo = new LinkedHashMap<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zipFile.getInputStream(entry));
        POM_INFO_XPATHS.forEach((field, exprs) -> {
            for (XPathExpression expr : exprs) {
                try {
                    String value = expr.evaluate(doc);
                    if (value != null && !value.isEmpty()) {
                        pomInfo.put(field, value);
                        break;
                    }
                } catch (XPathExpressionException ex) {
                    getLog().error("Failed to evaluate XPath " + expr, ex);
                }
            }
        });
        pomInfo.put("packaging", "jar");
        return pomInfo;
    }

    private static Map<String, String> getJarInfo(String jarName, Properties artifacts) {
        String artifactId, version;
        String artifact = jarName.substring(0, jarName.length() - 4);
        Matcher matcher = JAR_VERSION_PATTERN.matcher(artifact);
        if (matcher.find()) {
            int verIdx = matcher.start();
            artifactId = artifact.substring(0, verIdx);
            version = artifact.substring(verIdx + 1);
        } else {
            artifactId = artifact;
            version = getAttribute(artifacts, artifactId, "version", null);
        }
        String name = getAttribute(artifacts, artifactId, "name", null);
        String groupId = getAttribute(artifacts, artifactId, "groupId", null);
        artifactId = getAttribute(artifacts, artifactId, "artifactId", artifactId);
        Map<String, String> jarCoordinates = new LinkedHashMap<>();
        jarCoordinates.put("name", name);
        jarCoordinates.put("groupId", groupId);
        jarCoordinates.put("artifactId", artifactId);
        jarCoordinates.put("version", version);
        jarCoordinates.put("packaging", "jar");
        return jarCoordinates;
    }

    private static String getAttribute(Properties artifacts, String artifactId, String attribute, String defaultValue) {
        String key = artifactId + "." + attribute;
        if (artifacts.containsKey(key)) {
            return artifacts.getProperty(key);
        }
        return defaultValue;
    }

}
