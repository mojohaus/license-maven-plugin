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
package org.codehaus.mojo.license.model;

import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.utils.MojoHelper;

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

/**
 * Map of artifacts (stub in mavenproject) group by their license.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class LicenseMap
        extends TreeMap<String, SortedSet<MavenProject>> {

    private static final long serialVersionUID = 864199843545688069L;

    public static final String UNKNOWN_LICENSE_MESSAGE = "Unknown license";

    private final Comparator<MavenProject> projectComparator;

    /** Default contructor. */
    public LicenseMap() {
        projectComparator = MojoHelper.newMavenProjectComparator();
    }

    /**
     * Store in the license map a project to a given license.
     *
     * @param key   the license on which to associate the gieven project
     * @param value project to register in the license map
     * @return the set of projects using the given license
     */
    public SortedSet<MavenProject> put(String key, MavenProject value) {

        // handle multiple values as a set to avoid duplicates
        SortedSet<MavenProject> valueList = get(key);
        if (valueList == null) {

            valueList = new TreeSet<MavenProject>(projectComparator);
        }

        valueList.add(value);
        return put(key, valueList);
    }

    /**
     * Build a dependencies map from the license map, this is a map of license for each project registred in the
     * license map.
     *
     * @return the generated dependencies map
     */
    public SortedMap<MavenProject, String[]> toDependencyMap() {
        SortedMap<MavenProject, Set<String>> tmp = new TreeMap<MavenProject, Set<String>>(projectComparator);

        for (Map.Entry<String, SortedSet<MavenProject>> entry : entrySet()) {
            String license = entry.getKey();
            SortedSet<MavenProject> set = entry.getValue();
            for (MavenProject p : set) {
                Set<String> list = tmp.get(p);
                if (list == null) {
                    list = new HashSet<String>();
                    tmp.put(p, list);
                }
                list.add(license);
            }
        }

        SortedMap<MavenProject, String[]> result = new TreeMap<MavenProject, String[]>(projectComparator);
        for (Map.Entry<MavenProject, Set<String>> entry : tmp.entrySet()) {
            List<String> value = new ArrayList<String>(entry.getValue());
            Collections.sort(value);
            result.put(entry.getKey(), value.toArray(new String[value.size()]));
        }
        tmp.clear();
        return result;
    }
}
