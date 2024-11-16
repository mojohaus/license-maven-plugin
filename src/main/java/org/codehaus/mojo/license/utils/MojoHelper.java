package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2020 CodeLutin, Codehaus, Tony Chemit
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mojo helper methods.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class MojoHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MojoHelper.class);

    /**
     * Add the directory as a resource of the given project.
     *
     * @param dir      the directory to add
     * @param project  the project to update
     * @param includes the includes of the resource
     * @return {@code true} if the resources was added (not already existing)
     */
    public static boolean addResourceDir(File dir, MavenProject project, String... includes) {
        List<?> resources = project.getResources();
        return addResourceDir(dir, project, resources, includes);
    }

    /**
     * Add the directory as a resource in the given resource list.
     *
     * @param dir       the directory to add
     * @param project   the project involved
     * @param resources the list of existing resources
     * @param includes  includes of the new resources
     * @return {@code true} if the resource was added (not already existing)
     */
    public static boolean addResourceDir(File dir, MavenProject project, List<?> resources, String... includes) {
        String newresourceDir = dir.getAbsolutePath();
        boolean shouldAdd = true;
        for (Object o : resources) {
            Resource r = (Resource) o;
            if (!r.getDirectory().equals(newresourceDir)) {
                continue;
            }

            for (String i : includes) {
                if (!r.getIncludes().contains(i)) {
                    r.addInclude(i);
                }
            }
            shouldAdd = false;
            break;
        }
        if (shouldAdd) {
            Resource r = new Resource();
            r.setDirectory(newresourceDir);
            for (String i : includes) {
                if (!r.getIncludes().contains(i)) {
                    r.addInclude(i);
                }
            }
            project.addResource(r);
        }
        return shouldAdd;
    }

    public static Comparator<MavenProject> newMavenProjectComparator() {
        return new Comparator<MavenProject>() {
            /**
             * {@inheritDoc}
             */
            public int compare(MavenProject o1, MavenProject o2) {

                String id1 = getArtifactId(o1.getArtifact());
                String id2 = getArtifactId(o2.getArtifact());
                return id1.compareTo(id2);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Comparator<MavenProject> newMavenProjectComparatorByName() {
        Comparator<MavenProject> comparatorByName = new Comparator<MavenProject>() {
            /**
             * {@inheritDoc}
             */
            public int compare(MavenProject o1, MavenProject o2) {

                String id1 = getProjectName(o1);
                String id2 = getProjectName(o2);
                return id1.compareToIgnoreCase(id2);
            }
        };
        // in case 2 project names are equal, compare by maven coordinates, to avoid losing
        // projects with same name and different coordinates
        return new ComparatorChain(Arrays.asList(comparatorByName, newMavenProjectComparator()));
    }

    protected static final double[] TIME_FACTORS = {1000000, 1000, 60, 60, 24};

    protected static final String[] TIME_UNITES = {"ns", "ms", "s", "m", "h", "d"};

    public static String convertTime(long value) {
        return convert(value, TIME_FACTORS, TIME_UNITES);
    }

    public static String convert(long value, double[] factors, String[] unites) {
        long sign = value == 0 ? 1 : value / Math.abs(value);
        int i = 0;
        double tmp = Math.abs(value);
        while (i < factors.length && i < unites.length && tmp > factors[i]) {
            tmp = tmp / factors[i++];
        }

        tmp *= sign;
        String result;
        result = MessageFormat.format("{0,number,0.###}{1}", tmp, unites[i]);
        return result;
    }

    /**
     * suffix a given {@code baseUrl} with the given {@code suffix}
     *
     * @param baseUrl base url to use
     * @param suffix  suffix to add
     * @return the new url
     */
    public static URL getUrl(URL baseUrl, String suffix) {
        String url = baseUrl.toString() + "/" + suffix;
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("could not obtain url " + url, ex);
        }
    }

    public static String getArtifactId(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append(artifact.getGroupId());
        sb.append("--");
        sb.append(artifact.getArtifactId());
        sb.append("--");
        sb.append(artifact.getVersion());
        return sb.toString();
    }

    public static String getArtifactName(MavenProject project) {
        StringBuilder sb = new StringBuilder();
        if (project.getName().startsWith("Unnamed -")) {

            // as in Maven 3, let's use the artifact id
            sb.append(project.getArtifactId());
        } else {
            sb.append(project.getName());
        }
        sb.append(" (");
        sb.append(project.getGroupId());
        sb.append(":");
        sb.append(project.getArtifactId());
        sb.append(":");
        sb.append(project.getVersion());
        sb.append(" - ");
        String url = project.getUrl();
        sb.append(url == null ? "no url defined" : url);
        sb.append(")");

        return sb.toString();
    }

    public static String getProjectName(MavenProject project) {
        String sb;
        if (project.getName().startsWith("Unnamed")) {

            // as in Maven 3, let's use the artifact id
            sb = project.getArtifactId();
        } else {
            sb = project.getName();
        }

        return sb;
    }

    public static List<String> getParams(String params) {
        String[] split = params == null ? new String[0] : params.split(",");
        return Arrays.asList(split);
    }

    /**
     * {@link MavenProject#getDependencyArtifacts()} is deprecated.
     *
     * <p>
     * This method checks if the dependency artifacts is {@code null} and returns an empty {@code HashSet} to avoid the
     * {@code NullPointerException}s caused by the {@link MavenProject#getDependencyArtifacts()} returning {@code null}.
     * </p>
     *
     * @param project the MavenProject to retrieve artifacts from
     * @return a HashSet of dependencies or an empty set
     */
    public static Set<Artifact> getDependencyArtifacts(MavenProject project) {
        if (project == null || project.getDependencyArtifacts() == null) {
            LOG.warn("");
            LOG.warn("Non-transitive dependencies cannot be found. ");
            LOG.warn("");
            return Collections.emptySet();
        }
        return project.getDependencyArtifacts();
    }
}
