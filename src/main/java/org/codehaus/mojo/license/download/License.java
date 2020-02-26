package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 MojoHaus
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

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.20
 */
public class License
{

    /**
     * The full legal name of the license.
     */
    private final String name;

    /**
     * The official url for the license text.
     */
    private final String url;

    /**
     * The primary method by which this project may be distributed.
     * <dl>
     * <dt>repo</dt>
     * <dd>may be downloaded from the Maven repository</dd>
     * <dt>manual</dt>
     * <dd>user must manually download and install the dependency.</dd>
     * </dl>
     */
    private final String distribution;

    /**
     * Addendum information pertaining to this license.
     */
    private final String comments;

    public License( String name, String url, String distribution, String comments )
    {
        super();
        this.name = name;
        this.url = url;
        this.distribution = distribution;
        this.comments = comments;
    }

    public String getName()
    {
        return name;
    }

    public String getUrl()
    {
        return url;
    }

    public String getDistribution()
    {
        return distribution;
    }

    public String getComments()
    {
        return comments;
    }

}
