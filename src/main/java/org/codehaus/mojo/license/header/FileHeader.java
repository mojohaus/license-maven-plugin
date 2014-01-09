package org.codehaus.mojo.license.header;

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

import org.codehaus.mojo.license.model.Copyright;

/**
 * Contract of a file header.
 * <p/>
 * A header has three sections like in this example :
 * <p/>
 * <pre>
 * Description
 * %--
 * Copyright (C) firstYear[ - lastYear] holder
 * %--
 * License
 * </pre>
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class FileHeader
{

    /**
     * Description of the project or module to add in header.
     */
    protected String description;

    /**
     * Copyright model.
     */
    protected Copyright copyright = new Copyright();

    /**
     * License used in the header.
     */
    protected String license;

    /**
     * @return the project name, or nay other common informations for all
     * files of a project (or module)
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return the copyright model.
     */
    public Copyright getCopyright()
    {
        return copyright;
    }

    /**
     * @return the license content (this is not the fully license content,
     * but just a per file license resume)
     */
    public String getLicense()
    {
        return license;
    }

    public void setCopyright( Copyright copyright )
    {
        this.copyright = copyright;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void setLicense( String license )
    {
        this.license = license;
    }
}
