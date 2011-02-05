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

import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.nuiton.processor.Processor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * File header processor.
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role="org.nuiton.processor.Processor" role-hint="file-header"
 * @since 1.0
 */
public class FileHeaderProcessor
    extends Processor
{

    /**
     * processor configuration
     */
    protected FileHeaderProcessorConfiguration configuration;

    /**
     * internal file header filter
     */
    protected FileHeaderFilter filter;

    public FileHeaderProcessor()
    {
    }

    public FileHeaderProcessorConfiguration getConfiguration()
    {
        return configuration;
    }

    public FileHeaderFilter getFilter()
    {
        return filter;
    }

    /**
     * @return {@code true} if processed file was touched (says the header was
     *         fully found), {@code false} otherwise
     * @see FileHeaderFilter#isTouched()
     */
    public boolean isTouched()
    {
        return getFilter() != null && getFilter().isTouched();
    }

    /**
     * @return {@code true} if processed file was modified (says the header was
     *         fully found and content changed), {@code false} otherwise
     * @see FileHeaderFilter#isModified()
     */
    public boolean isModified()
    {
        return getFilter() != null && getFilter().isModified();
    }

    /**
     * @return {@code true} if header of header was detected
     * @see FileHeaderFilter#isDetectHeader()
     */
    public boolean isDetectHeader()
    {
        return getFilter() != null && getFilter().isDetectHeader();
    }

    public void process( File filein, File fileout )
        throws IOException, IllegalStateException
    {

        checkInit();
        reset();

        FileReader input = new FileReader( filein );
        try
        {
            FileWriter output = new FileWriter( fileout );
            try
            {
                process( input, output );
            }
            finally
            {
                output.close();
            }
        }
        finally
        {
            input.close();
        }
    }

    public void populateFilter()
    {
        FileHeader fileHeader = getConfiguration().getFileHeader();
        boolean change = false;

        FileHeaderFilter filter = getFilter();

        if ( !fileHeader.equals( filter.getFileHeader() ) )
        {

            // change file header

            filter.setFileHeader( fileHeader );
            change = true;
        }
        FileHeaderTransformer transformer = getConfiguration().getTransformer();
        if ( !transformer.equals( filter.getTransformer() ) )
        {

            // change file transformer

            filter.setTransformer( transformer );
            change = true;
        }
        if ( change )
        {

            // something has changed, must reset content cache
            filter.resetContent();
        }
    }

    public void setConfiguration( FileHeaderProcessorConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public void setFilter( FileHeaderFilter filter )
    {
        this.filter = filter;
        setInputFilter( filter );
    }

    public void reset()
    {
        if ( filter != null )
        {
            filter.reset();
        }
    }

    protected FileHeader getFileHeader()
    {
        return getConfiguration().getFileHeader();
    }

    protected FileHeaderTransformer getTransformer()
    {
        return getConfiguration().getTransformer();
    }

    protected void checkInit()
        throws IllegalStateException
    {
        if ( getConfiguration() == null )
        {
            throw new IllegalStateException( "no configuration set." );
        }
        if ( getFileHeader() == null )
        {
            throw new IllegalStateException( "no file header set." );
        }
        if ( getTransformer() == null )
        {
            throw new IllegalStateException( "no file header transformer set." );
        }
        if ( getFilter() == null )
        {
            throw new IllegalStateException( "no file header filter set." );
        }
    }
}
