/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
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

package org.codehaus.mojo.license.header;

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
     * Copyright to string format
     */
    protected static final String COPYRIGHT_TO_STRING_FORMAT = "Copyright (C) %1$s %2$s";

    /**
     * Description of the project or module to add in header
     */
    protected String description;

    /**
     * Copyright holder
     */
    protected String copyrightHolder;

    /**
     * Copyright first year of application
     */
    protected Integer copyrightFirstYear;

    /**
     * Copyright last year of application (can be nullif copyright is
     * only on one year).
     */
    protected Integer copyrightLastYear;

    /**
     * License used in the header.
     */
    protected String license;

    /**
     * @return the project name, or nay other common informations for all
     *         files of a project (or module)
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return the copyright holder
     */
    public String getCopyrightHolder()
    {
        return copyrightHolder;
    }

    /**
     * @return the first year of the copyright
     */
    public Integer getCopyrightFirstYear()
    {
        return copyrightFirstYear;
    }

    /**
     * @return the last year of the copyright (if copyright affects only one
     *         year, can be equals to the {@link #getCopyrightFirstYear()}).
     */
    public Integer getCopyrightLastYear()
    {
        return copyrightLastYear;
    }

    /**
     * Produces a string representation of the copyright.
     * <p/>
     * If copyright acts on one year :
     * <pre>
     * Copyright (C) 2010 Holder
     * </pre>
     * <p/>
     * If copyright acts on more than one year :
     * <pre>
     * Copyright (C) 2010 - 2012 Holder
     * </pre>
     *
     * @return the String representation of the copyright
     */
    public String getCopyright()
    {
        String copyright;
        if ( getCopyrightLastYear() == null )
        {

            // copyright on one year
            copyright = String.format( COPYRIGHT_TO_STRING_FORMAT, getCopyrightFirstYear(), getCopyrightHolder() );
        }
        else
        {

            // copyright on more than one year
            copyright =
                String.format( COPYRIGHT_TO_STRING_FORMAT, getCopyrightFirstYear() + " - " + getCopyrightLastYear(),
                               getCopyrightHolder() );
        }
        return copyright;
    }

    /**
     * @return the license content (this is not the fully license content,
     *         but just a per file license resume)
     */
    public String getLicense()
    {
        return license;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void setCopyrightHolder( String copyrightHolder )
    {
        this.copyrightHolder = copyrightHolder;
    }

    public void setCopyrightFirstYear( Integer copyrightFirstYear )
    {
        this.copyrightFirstYear = copyrightFirstYear;
    }

    public void setCopyrightLastYear( Integer copyrightLastYear )
    {
        this.copyrightLastYear = copyrightLastYear;
    }

    public void setLicense( String license )
    {
        this.license = license;
    }
}
