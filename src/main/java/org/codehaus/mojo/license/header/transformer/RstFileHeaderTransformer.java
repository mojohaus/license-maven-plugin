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

package org.codehaus.mojo.license.header.transformer;

/**
 * Implementation of {@link FileHeaderTransformer} for apt format.
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role-hint="rst"
 * @since 1.0
 */
public class RstFileHeaderTransformer
    extends AbstractFileHeaderTransformer
{

    public static final String NAME = "rst";

    public static final String DESCRIPTION = "header transformer with rst comment style";

    public static final String COMMENT_LINE_PREFIX = ".. * ";

    public static final String COMMENT_START_TAG = ".. -";

    public static final String COMMENT_END_TAG = ".. -";

    public RstFileHeaderTransformer()
    {
        super( NAME, DESCRIPTION, COMMENT_START_TAG, COMMENT_END_TAG, COMMENT_LINE_PREFIX );
    }

    public String[] getDefaultAcceptedExtensions()
    {
        return new String[]{ "rst" };
    }
}
