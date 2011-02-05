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

package org.codehaus.mojo.license;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;

import java.util.*;

/**
 * Displays all the available comment style to box file headers.
 *
 * @author themit <chemit@codelutin.com>
 * @requiresProject false
 * @requiresDirectInvocation
 * @goal comment-style-list
 * @since 1.0
 */
public class CommentStyleListMojo
    extends AbstractLicenseMojo
{

    /**
     * A flag to display also the content of each license.
     *
     * @parameter expression="${detail}"
     * @since 1.0
     */
    private boolean detail;

    /**
     * All available header transformers.
     *
     * @component role="org.codehaus.mojo.license.header.transformer.FileHeaderTransformer"
     * @since 1.0
     */
    private Map<String, FileHeaderTransformer> transformers;

    @Override
    protected void init()
        throws Exception
    {
        //nothing to do
    }

    @Override
    public void doAction()
        throws MojoExecutionException, MojoFailureException
    {

        StringBuilder buffer = new StringBuilder();
        if ( isVerbose() )
        {
            buffer.append( "\n\n-------------------------------------------------------------------------------\n" );
            buffer.append( "                           license-maven-plugin\n" );
            buffer.append( "-------------------------------------------------------------------------------\n\n" );
        }
        List<String> names = new ArrayList<String>( transformers.keySet() );
        Collections.sort( names );

        int maxLength = 0;
        int maxDLength = 0;
        for ( String name : names )
        {
            if ( name.length() > maxLength )
            {
                maxLength = name.length();
            }
            FileHeaderTransformer transformer = transformers.get( name );
            if ( transformer.getDescription().length() > maxDLength )
            {
                maxDLength = transformer.getDescription().length();
            }
        }

        String pattern = "  - %1$-" + maxLength + "s : %2$-" + maxDLength + "s, extensions : %3$s\n";

        buffer.append( "List of available comment styles:\n\n" );
        for ( String transformerName : names )
        {
            FileHeaderTransformer transformer = transformers.get( transformerName );
            buffer.append( String.format( pattern, transformerName, transformer.getDescription(),
                                          Arrays.toString( transformer.getDefaultAcceptedExtensions() ) ) );
            if ( detail )
            {
                buffer.append( "\n   example : \n" );
                buffer.append( transformer.boxComment( "header", true ) );
                buffer.append( '\n' );
            }
        }

        getLog().info( buffer.toString() );
    }

    public boolean isDetail()
    {
        return detail;
    }

    public void setDetail( boolean detail )
    {
        this.detail = detail;
    }

    public Map<String, FileHeaderTransformer> getTransformers()
    {
        return transformers;
    }

    public void setTransformers( Map<String, FileHeaderTransformer> transformers )
    {
        this.transformers = transformers;
    }
}
