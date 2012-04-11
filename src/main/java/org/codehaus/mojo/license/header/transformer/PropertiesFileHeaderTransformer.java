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

package org.codehaus.mojo.license.header.transformer;

/**
 * Implementation of {@link FileHeaderTransformer} for properties format.
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role-hint="properties"
 * @since 1.0
 */
public class PropertiesFileHeaderTransformer
        extends AbstractFileHeaderTransformer {

    public static final String NAME = "properties";

    public static final String DESCRIPTION = "header transformer with properties file comment style";

    public static final String COMMENT_LINE_PREFIX = "# ";

    public static final String COMMENT_START_TAG = "###";

    public static final String COMMENT_END_TAG = "###";

    protected final String linesep;

    public PropertiesFileHeaderTransformer() {
        super(NAME, DESCRIPTION, COMMENT_START_TAG, COMMENT_END_TAG, COMMENT_LINE_PREFIX);
        linesep = System.getProperty("line.separator");
    }

    public String[] getDefaultAcceptedExtensions() {
        return new String[]{NAME, "sh", "py", "rb", "pl", "pm"};
    }

    @Override
    public String addHeader(String header, String content) {

        String result;

        String prolog = null;
        int startProlog = content.indexOf("#!");
        if (startProlog > -1) {

            // shebang was detected

            int endProlog = content.indexOf(linesep);

            if (endProlog > -1) {

                // prolog end was detected
                prolog = content.substring(0, endProlog + linesep.length());
            }
        }

        if (prolog == null) {

            // no shebang detected
            result = super.addHeader(header, content);
        } else {

            // shebang detected
            content = content.substring(prolog.length());
            result = super.addHeader(prolog + '\n' + header, content);
        }
        return result;
    }
}
