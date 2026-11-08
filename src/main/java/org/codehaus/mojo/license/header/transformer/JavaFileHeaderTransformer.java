package org.codehaus.mojo.license.header.transformer;

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

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation of {@link FileHeaderTransformer} for java format.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
@Named("java")
@Singleton
public class JavaFileHeaderTransformer extends AbstractFileHeaderTransformer {

    /**
     * Flag to add the license header after the {@code package} statement.
     *
     * @since 1.2
     */
    protected boolean addJavaLicenseAfterPackage;

    /**
     * Flag to use comment start tag with a no reformat syntax {@code /*-}.
     *
     * See http://www.oracle.com/technetwork/java/javase/documentation/codeconventions-141999.html#350
     *
     * @since 1.9
     */
    protected boolean useNoReformatCommentStartTag;

    /**
     * Default constructor.
     */
    public JavaFileHeaderTransformer() {
        super("java", "header transformer with java comment style", "/*", " */", " * ");
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDefaultAcceptedExtensions() {
        return new String[] {
            "java", "groovy", "css", "jccs", "cs", "as", "aj", "c", "h", "cpp", "js", "json", "ts", "go", "kt"
        };
    }

    /**
     * Sets the value to the property {@link #addJavaLicenseAfterPackage}.
     *
     * @param addJavaLicenseAfterPackage the new value to set
     * @since 1.2
     */
    public void setAddJavaLicenseAfterPackage(boolean addJavaLicenseAfterPackage) {
        this.addJavaLicenseAfterPackage = addJavaLicenseAfterPackage;
    }

    /**
     * Sets the value of the property {@link #useNoReformatCommentStartTag}.
     *
     * @param useNoReformatCommentStartTag the new value to set
     * @since 1.9
     */
    public void setUseNoReformatCommentStartTag(boolean useNoReformatCommentStartTag) {
        this.useNoReformatCommentStartTag = useNoReformatCommentStartTag;
    }

    @Override
    public String getCommentStartTag() {
        return useNoReformatCommentStartTag ? "/*-" : super.getCommentStartTag();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String addHeader(String header, String content) {

        if (!addJavaLicenseAfterPackage) {
            return super.addHeader(header, content);
        }

        String result;

        String prolog = null;
        int startProlog = findPackageStatement(content);
        if (startProlog > -1) {

            // package was detected
            int endProlog = content.indexOf(";", startProlog);

            if (endProlog > -1) {

                // prolog end was detected

                int lastIndex = endProlog + 1;

                // Include existing end of line in prolog

                if (lastIndex < content.length() && content.charAt(lastIndex) == '\r') {
                    lastIndex++;
                }

                if (lastIndex < content.length() && content.charAt(lastIndex) == '\n') {
                    lastIndex++;
                }

                // the prolog includes the whole package definition

                prolog = content.substring(0, lastIndex);
            }
        }

        if (prolog == null) {

            // no prolog detected
            result = super.addHeader(header, content);
        } else {

            // prolog detected
            content = content.substring(prolog.length());
            result = super.addHeader(prolog + getLineSeparator() + header, content);
        }
        return result;
    }

    /**
     * Finds the {@code package} keyword that starts the package declaration, skipping comments, string and character
     * literals and identifiers that merely contain the word, such as {@code packages} in a Javadoc comment.
     *
     * @param content the Java source
     * @return the index of the keyword, or {@code -1} if the source has no package declaration
     */
    static int findPackageStatement(String content) {
        int length = content.length();
        int i = 0;
        while (i < length) {
            char c = content.charAt(i);
            if (c == '/' && i + 1 < length && content.charAt(i + 1) == '*') {
                int end = content.indexOf("*/", i + 2);
                if (end < 0) {
                    return -1;
                }
                i = end + 2;
            } else if (c == '/' && i + 1 < length && content.charAt(i + 1) == '/') {
                int end = content.indexOf('\n', i + 2);
                if (end < 0) {
                    return -1;
                }
                i = end + 1;
            } else if (c == '"' || c == '\'') {
                i = skipLiteral(content, i);
            } else if (Character.isJavaIdentifierStart(c)) {
                int end = i + 1;
                while (end < length && Character.isJavaIdentifierPart(content.charAt(end))) {
                    end++;
                }
                if (content.substring(i, end).equals("package")) {
                    return i;
                }
                i = end;
            } else {
                i++;
            }
        }
        return -1;
    }

    private static int skipLiteral(String content, int start) {
        char quote = content.charAt(start);
        int i = start + 1;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote) {
                return i + 1;
            } else {
                i++;
            }
        }
        return content.length();
    }
}
