package org.codehaus.mojo.license;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Displays all the available comment style to box file headers.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
@Mojo( name = "comment-style-list", requiresProject = false, requiresDirectInvocation = true )
public class CommentStyleListMojo
    extends AbstractLicenseMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * A flag to display also the content of each license.
     *
     * @since 1.0
     */
    @Parameter( property = "detail" )
    private boolean detail;

    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

    /**
     * All available header transformers.
     *
     * @since 1.0
     */
    @Component( role = FileHeaderTransformer.class )
    private Map<String, FileHeaderTransformer> transformers;

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkip() {
        // can't skip this goal since direct invocation is required
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init()
        throws Exception
    {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
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

        String pattern = " * %1$-" + maxLength + "s : %2$-" + maxDLength + "s, extensions : %3$s\n";

        buffer.append( "Available comment styles:\n\n" );
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
}
