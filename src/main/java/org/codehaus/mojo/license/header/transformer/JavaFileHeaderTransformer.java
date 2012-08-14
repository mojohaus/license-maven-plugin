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

/**
 * Implementation of {@link FileHeaderTransformer} for java format.
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role-hint="java"
 * @since 1.0
 */
public class JavaFileHeaderTransformer
    extends AbstractFileHeaderTransformer
{

    /**
     * Flag to add the license header after the {@code package} statement.
     *
     * @since 1.2
     */
    protected boolean addJavaLicenseAfterPackage;

    /**
     * Default constructor.
     */
    public JavaFileHeaderTransformer()
    {
        super( "java", "header transformer with java comment style", "/*", " */", " * " );
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDefaultAcceptedExtensions()
    {
        return new String[]{ "java", "groovy", "css", "cs", "as", "aj", "c", "h", "cpp", "js", "json"

        };
    }

    /**
     * Sets the value to the property {@link #addJavaLicenseAfterPackage}.
     *
     * @param addJavaLicenseAfterPackage the new value to set
     * @since 1.2
     */
    public void setAddJavaLicenseAfterPackage( boolean addJavaLicenseAfterPackage )
    {
        this.addJavaLicenseAfterPackage = addJavaLicenseAfterPackage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String addHeader( String header, String content )
    {

        if ( !addJavaLicenseAfterPackage )
        {
            return super.addHeader( header, content );
        }

        String result;

        String prolog = null;
        int startProlog = content.indexOf( "package" );
        if ( startProlog > -1 )
        {

            // package was detected
            int endProlog = content.indexOf( ";", startProlog );

            if ( endProlog > -1 )
            {

                // prolog end was detected

                int lastIndex = endProlog + 1;

                prolog = content.substring( 0, lastIndex );

                // prolog goes to next line
                prolog += "\n";

                if ( lastIndex == content.length() )
                {

                    // adding a new empty end line to the content
                    content += "\n";
                }


            }
        }

        if ( prolog == null )
        {

            // no prolog detected
            result = super.addHeader( header, content );
        }
        else
        {

            // prolog detected
            content = content.substring( prolog.length() );
            result = super.addHeader( prolog + header, content );
        }
        return result;
    }

}
