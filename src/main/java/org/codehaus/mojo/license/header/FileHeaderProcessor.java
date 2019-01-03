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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.nuiton.processor.Processor;

/**
 * File header processor.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class FileHeaderProcessor extends Processor
{
    /**
     * internal file header filter.
     */
    private final FileHeaderFilter filter;

    public FileHeaderProcessor( FileHeaderFilter filter, FileHeader fileHeader, FileHeaderTransformer transformer )
    {
        if ( filter == null )
        {
            throw new IllegalStateException( "no file header filter set." );
        }
        if ( fileHeader == null )
        {
            throw new IllegalStateException( "no file header set." );
        }
        if ( transformer == null )
        {
            throw new IllegalStateException( "no file header transformer set." );
        }
        this.filter = filter;
        setInputFilter( filter );
        filter.setFileHeader( fileHeader );
        filter.setTransformer( transformer );
        filter.resetContent();
    }

    public String addHeader( String content )
    {
        return filter.getTransformer().addHeader( filter.getFullHeaderContent(), content );
    }

    /**
     * @return {@code true} if processed file was touched (says the header was
     * fully found), {@code false} otherwise
     * @see FileHeaderFilter#isTouched()
     */
    public boolean isTouched()
    {
        return filter.isTouched();
    }

    /**
     * @return {@code true} if processed file was modified (says the header was
     * fully found and content changed), {@code false} otherwise
     * @see FileHeaderFilter#isModified()
     */
    public boolean isModified()
    {
        return filter.isModified();
    }

    /**
     * @return {@code true} if header of header was detected
     * @see FileHeaderFilter#isDetectHeader()
     */
    public boolean isDetectHeader()
    {
        return filter.isDetectHeader();
    }

    public synchronized void process( String inputContent, File outputFile, String encoding ) throws IOException
    {

        filter.reset();

        Reader input = new InputStreamReader( new ByteArrayInputStream( inputContent.getBytes( encoding ) ), encoding );
        try
        {
            Writer output = new OutputStreamWriter( new FileOutputStream( outputFile ), encoding );
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

    public String getFileHeaderDescription()
    {
        return filter.getFileHeader().getDescription();
    }

    public void updateDescription( String description )
    {
        filter.getFileHeader().setDescription( description );
        filter.resetContent();
    }
}
