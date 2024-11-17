package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2013 CodeLutin, Codehaus, Tony Chemit
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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Object to convert in mojo a parameter from a some simple String to a List.
 *
 * See (http://jira.codehaus.org/browse/MLICENSE-53).
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.4
 */
public class StringToList {
    /**
     * Regular expression to split license list with a pipe character: "|".
     */
    public static final String LIST_OF_LICENSES_REG_EX = "\\s*\\|\\s*";

    /**
     * List of data.
     */
    private final List<String> data;

    public StringToList() {
        data = new ArrayList<>();
    }

    /**
     * @param data a list of licenses or a URL
     */
    public StringToList(String data) throws MojoExecutionException {
        this();
        if (!UrlRequester.isStringUrl(data)) {
            String[] splited = trimmedStringSplit(data);
            for (String s : splited) {
                addEntryToList(s);
            }
        } else {
            for (String license : UrlRequester.downloadList(data)) {
                if (data != null && StringUtils.isNotBlank(license) && !this.data.contains(license)) {
                    this.data.add(license);
                }
            }
        }
    }

    /**
     * Splits a String by the pipe character and trims it.<br>
     * It allows, for example, to write lengthy license merge lists not written as a 400 characters long, hard to read
     * line, but as a list of lines separated by a pipe character.<br>
     * Example, instead of writing the following XML:
     * <pre><code>
     * <tag>Righteous license 2.1|The righteous extra long named super rights granting license version 2.1|Hidden right license 2.1|The completely unfair "all your rights belong to us" license version 2.1</tag>
     * </code></pre>
     * write:
     * <pre><code>
     * <tag>Righteous license 2.1
     *     |The righteous extra long named super rights granting license version 2.1
     *     |Hidden right license 2.1
     *     |The completely unfair "all your rights belong to us" license version 2.1
     * </tag>
     * </code></pre>
     * @param stringToSplit String to split.
     * @return Split and trimmed string.
     */
    public static String[] trimmedStringSplit(String stringToSplit) {
        return stringToSplit
                .trim()
                // Replace newlines
                .replace('\n', ' ')
                // Replace multiple spaces with one.
                .replaceAll(" +", " ")
                .split(LIST_OF_LICENSES_REG_EX);
    }

    public List<String> getData() {
        return data;
    }

    protected void addEntryToList(String data) {
        this.data.add(data);
    }

    public boolean contains(String name) {
        return data.contains(name);
    }
}
