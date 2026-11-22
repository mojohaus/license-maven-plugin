package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 Codehaus
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.maven.model.Organization;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.license.header.transformer.AptFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.FreeMarkerFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.HtmlFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.JavaFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.JspFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.MySqlFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.PhpFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.PropertiesFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.RstFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.SqlFileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.XmlFileHeaderTransformer;
import org.mockito.Mockito;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Configures a mojo the way Maven does, from the generated plugin descriptor: a parameter takes the value given
 * in the plugin configuration, else the one of its property expression, else its default value. The tests of the
 * goals therefore see the defaults that are declared on the {@code @Parameter} annotations.
 */
final class MojoTestSupport {

    private MojoTestSupport() {}

    /** The comment styles that the plugin registers by name. */
    static Map<String, FileHeaderTransformer> transformers() {
        Map<String, FileHeaderTransformer> transformers = new TreeMap<>();
        transformers.put("apt", new AptFileHeaderTransformer());
        transformers.put("ftl", new FreeMarkerFileHeaderTransformer());
        transformers.put("html", new HtmlFileHeaderTransformer());
        transformers.put("java", new JavaFileHeaderTransformer());
        transformers.put("jsp", new JspFileHeaderTransformer());
        transformers.put("mysql", new MySqlFileHeaderTransformer());
        transformers.put("php", new PhpFileHeaderTransformer());
        transformers.put("properties", new PropertiesFileHeaderTransformer());
        transformers.put("rst", new RstFileHeaderTransformer());
        transformers.put("sql", new SqlFileHeaderTransformer());
        transformers.put("xml", new XmlFileHeaderTransformer());
        return transformers;
    }

    static MavenProjectHelper projectHelper() {
        return Mockito.mock(MavenProjectHelper.class);
    }

    /**
     * Creates the project that a fixture's {@code pom.xml} would give.
     *
     * @param basedir the project directory
     * @param name the project name
     * @param organization the organization name, or {@code null}
     * @param inceptionYear the inception year, or {@code null}
     * @return the project, with UTF-8 as source encoding
     */
    static MavenProject project(
            Path basedir, String artifactId, String version, String name, String organization, String inceptionYear) {
        MavenProject project = new MavenProject();
        project.setFile(basedir.resolve("pom.xml").toFile());
        project.setGroupId("org.codehaus.mojo.license.test");
        project.setArtifactId(artifactId);
        project.setVersion(version);
        project.setName(name);
        project.setInceptionYear(inceptionYear);
        if (organization != null) {
            Organization org = new Organization();
            org.setName(organization);
            project.setOrganization(org);
        }
        project.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");
        return project;
    }

    /**
     * Sets the parameters of {@code mojo} for the goal {@code goal}.
     *
     * @param mojo the mojo to configure
     * @param goal the goal of the mojo in the plugin descriptor
     * @param project the project the mojo runs on
     * @param configuration the plugin configuration, by parameter name; array parameters take a {@code String[]}
     * @throws Exception if the descriptor cannot be read or a parameter cannot be set
     */
    static void configure(Object mojo, String goal, MavenProject project, Map<String, Object> configuration)
            throws Exception {
        setField(mojo, "project", project);
        Element mojoElement = null;
        // other jars on the class path, such as xmlbeans, carry a META-INF/maven/plugin.xml too
        Path classes = Paths.get(AbstractLicenseMojo.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        Path descriptor = classes.resolve("META-INF/maven/plugin.xml");
        if (!Files.isRegularFile(descriptor)) {
            throw new IllegalStateException("The plugin descriptor is generated in the process-classes phase");
        }
        try (InputStream in = Files.newInputStream(descriptor)) {
            NodeList mojos = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(in)
                    .getElementsByTagName("mojo");
            for (int i = 0; i < mojos.getLength() && mojoElement == null; i++) {
                Element candidate = (Element) mojos.item(i);
                if (goal.equals(text(candidate, "goal"))) {
                    mojoElement = candidate;
                }
            }
        }
        if (mojoElement == null) {
            throw new IllegalArgumentException("No goal " + goal + " in the plugin descriptor");
        }

        // the parameters that the plugin configuration may set, whether or not they have a default
        Set<String> declared = new HashSet<>();
        NodeList declaredParameters =
                ((Element) mojoElement.getElementsByTagName("parameters").item(0)).getElementsByTagName("name");
        for (int i = 0; i < declaredParameters.getLength(); i++) {
            declared.add(declaredParameters.item(i).getTextContent().trim());
        }
        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            if (!declared.contains(entry.getKey())) {
                throw new IllegalArgumentException(entry.getKey() + " is not a parameter of " + goal);
            }
            setField(mojo, entry.getKey(), entry.getValue());
        }

        // the others take the value of their property expression, else their default
        Element parameters =
                (Element) mojoElement.getElementsByTagName("configuration").item(0);
        for (Node node = parameters.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element)) {
                continue;
            }
            Element parameter = (Element) node;
            String name = parameter.getTagName();
            if (name.equals("project")
                    || name.equals("session")
                    || name.equals("proxies")
                    || configuration.containsKey(name)) {
                continue;
            }
            String expression = parameter.getTextContent().trim();
            String text = expression.isEmpty() ? null : evaluate(expression, project);
            if (text == null && parameter.hasAttribute("default-value")) {
                text = evaluate(parameter.getAttribute("default-value"), project);
            }
            Object value = convert(text, parameter.getAttribute("implementation"));
            if (value != null) {
                setField(mojo, name, value);
            }
        }
    }

    private static String text(Element parent, String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent().trim();
    }

    private static String evaluate(String expression, MavenProject project) {
        if (!expression.startsWith("${") || !expression.endsWith("}")) {
            return expression;
        }
        String key = expression.substring(2, expression.length() - 1);
        switch (key) {
            case "project.name":
                return project.getName();
            case "project.inceptionYear":
                return project.getInceptionYear();
            case "project.organization.name":
                return project.getOrganization() == null
                        ? null
                        : project.getOrganization().getName();
            default:
                Properties properties = project.getProperties();
                return properties.getProperty(key);
        }
    }

    private static Object convert(String text, String type) {
        if (text == null) {
            return null;
        }
        switch (type) {
            case "boolean":
                return Boolean.valueOf(text);
            case "java.lang.Integer":
                return Integer.valueOf(text);
            case "java.lang.String":
                return text;
            default:
                throw new IllegalArgumentException("Cannot convert '" + text + "' to " + type);
        }
    }

    static void setField(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                // look in the superclass
            }
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Copies a fixture directory from the test resources.
     *
     * @param resource the directory below {@code /update-file-header}
     * @param target the directory to copy into
     * @throws IOException if the copy fails
     */
    static void copyFixture(String resource, Path target) throws IOException {
        File source;
        try {
            source = new File(MojoTestSupport.class
                    .getResource("/update-file-header/" + resource)
                    .toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        Path root = source.toPath();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(root.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(root.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
