package org.codehaus.mojo.license.model;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.utils.MojoHelper;

/**
 * Map of artifacts (stub in mavenproject) grouped by their license.
 *
 * <ul>
 *     <li><code>key</code> is the license on which to associate the given project.</li>
 *     <li><code>value</code> list of projects belonging to the license.</li>
 * </ul>
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class LicenseMap extends TreeMap<String, SortedSet<MavenProject>> {

    private static final long serialVersionUID = 864199843545688069L;

    public static final String UNKNOWN_LICENSE_MESSAGE = "Unknown license";

    private final Comparator<MavenProject> projectComparator;

    /**
     * Default constructor.
     */
    public LicenseMap() {
        this(MojoHelper.newMavenProjectComparator());
    }

    public LicenseMap(Comparator<MavenProject> projectComparator) {
        this.projectComparator = projectComparator;
    }

    /**
     * Store in the license map a project to a given license.
     *
     * @param key   the license on which to associate the given project
     * @param value project to register in the license map
     * @return the set of projects using the given license
     */
    public SortedSet<MavenProject> put(String key, MavenProject value) {

        // handle multiple values as a set to avoid duplicates
        SortedSet<MavenProject> valueList = get(key);
        if (valueList == null) {

            valueList = new TreeSet<>(projectComparator);
        }

        valueList.add(value);
        return put(key, valueList);
    }

    /**
     * Store in the license other licenseMap.
     *
     * @param licenseMap license map to put
     */
    public void putAll(LicenseMap licenseMap) {
        for (Map.Entry<String, SortedSet<MavenProject>> entry : licenseMap.entrySet()) {

            String key = entry.getKey();

            // handle multiple values as a set to avoid duplicates
            SortedSet<MavenProject> valueList = get(key);
            if (valueList == null) {

                valueList = new TreeSet<>(projectComparator);
            }

            valueList.addAll(entry.getValue());
            put(key, valueList);
        }
    }

    /**
     * Build a dependencies map from the license map, this is a map of license for each project registered in the
     * license map.
     *
     * @return the generated dependencies map
     */
    public SortedMap<MavenProject, String[]> toDependencyMap() {
        SortedMap<MavenProject, Set<String>> tmp = new TreeMap<>(projectComparator);

        for (Map.Entry<String, SortedSet<MavenProject>> entry : entrySet()) {
            String license = entry.getKey();
            SortedSet<MavenProject> set = entry.getValue();
            for (MavenProject p : set) {
                Set<String> list = tmp.get(p);
                if (list == null) {
                    list = new HashSet<>();
                    tmp.put(p, list);
                }
                list.add(license);
            }
        }

        SortedMap<MavenProject, String[]> result = new TreeMap<>(projectComparator);
        for (Map.Entry<MavenProject, Set<String>> entry : tmp.entrySet()) {
            List<String> value = new ArrayList<>(entry.getValue());
            Collections.sort(value);
            result.put(entry.getKey(), value.toArray(new String[value.size()]));
        }
        tmp.clear();
        return result;
    }

    public LicenseMap toLicenseMapOrderByName() {
        LicenseMap result = new LicenseMap(MojoHelper.newMavenProjectComparatorByName());
        result.putAll(this);
        return result;
    }

    /**
     * Remove project from all licenses it is mapped to.
     * @param project
     * @return a List of license names that the given project was mapped to
     */
    public List<String> removeProject(MavenProject project) {
        List<String> removedFrom = new ArrayList<>();
        for (Map.Entry<String, SortedSet<MavenProject>> entry : entrySet()) {
            SortedSet<MavenProject> projects = entry.getValue();
            for (MavenProject mavenProject : projects) {
                if (project.equals(mavenProject)) {
                    removedFrom.add(entry.getKey());
                    break;
                }
            }
        }

        for (String r : removedFrom) {
            get(r).remove(project);
        }

        return removedFrom;
    }
}
