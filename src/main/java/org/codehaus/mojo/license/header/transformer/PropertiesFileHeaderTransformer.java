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
 * Implementation of {@link FileHeaderTransformer} for properties format.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
@Named("properties")
@Singleton
public class PropertiesFileHeaderTransformer extends AbstractFileHeaderTransformer {

    /**
     * Default constructor.
     */
    public PropertiesFileHeaderTransformer() {
        super("properties", "header transformer with properties file comment style", "###", "###", "# ");
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDefaultAcceptedExtensions() {
        return new String[] {"properties", "sh", "py", "rb", "pl", "pm"};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String addHeader(String header, String content) {

        String result;

        String prolog = null;
        int startProlog = content.indexOf("#!");
        if (startProlog > -1) {

            // shebang was detected

            int endProlog = content.indexOf('\n', startProlog);

            if (endProlog > -1) {

                // prolog end was detected
                prolog = content.substring(0, endProlog + 1);
            }
        }

        if (prolog == null) {

            // no shebang detected
            result = super.addHeader(header, content);
        } else {

            // shebang detected
            content = content.substring(prolog.length());
            result = super.addHeader(prolog + getLineSeparator() + header, content);
        }
        return result;
    }
}
