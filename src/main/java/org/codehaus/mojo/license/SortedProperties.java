/*
 * #%L
 * Maven helper plugin
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2009 - 2010 Tony Chemit, CodeLutin
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


import java.io.*;
import java.util.*;

/**
 * Permet d'avoir les fichiers de proprietes tries.
 *
 * @author ruchaud <ruchaud@codelutin.com>
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class SortedProperties
    extends Properties
{

    private static final long serialVersionUID = -1147150444452577558L;

    /**
     * l'encoding a utiliser pour lire et ecrire le properties.
     */
    protected String encoding;

    /**
     * un drapeau pour savoir s'il faut enlever l'entete generere
     */
    protected boolean removeHeader;


    public SortedProperties( String encoding )
    {
        this( encoding, true );
    }


    public SortedProperties( String encoding, boolean removeHeader )
    {
        this.encoding = encoding;
        this.removeHeader = removeHeader;
    }

    public SortedProperties( Properties defaults )
    {
        super( defaults );
    }

    @Override
    public Enumeration<Object> keys()
    {
        List<Object> objects = Collections.list( super.keys() );
        Vector<Object> result;
        try
        {
            // Attention, si les clef ne sont pas des string, ca ne marchera pas
            List<String> list = toGenericList( objects, String.class );
            Collections.sort( list );
            result = new Vector<Object>( list );
        }
        catch ( IllegalArgumentException e )
        {
            // keys are not string !!!
            // can not sort keys
            result = new Vector<Object>( objects );
        }
        return result.elements();
    }

    /**
     * Charge le properties a partir d'un fichier.
     *
     * @param src le fichier src a charger en utilisant l'encoding declare
     * @return l'instance du properties
     * @throws java.io.IOException if any io pb
     */
    public SortedProperties load( File src )
        throws IOException
    {
        FileInputStream reader = new FileInputStream( src );
        try
        {
            load( reader );
        }
        finally
        {
            reader.close();
        }
        return this;
    }

    /**
     * Sauvegarde le properties dans un fichier, sans commentaire et en utilisant l'encoding declare.
     *
     * @param dst the fichier de destination
     * @throws java.io.IOException if any io pb
     */
    public void store( File dst )
        throws IOException
    {
        OutputStream writer = new FileOutputStream( dst );
        try
        {
            store( writer, null );
        }
        finally
        {
            writer.close();
        }
    }

    /**
     * Permet de convertir une liste non typee, en une liste typee.
     * <p/>
     * La liste en entree en juste bien castee.
     * <p/>
     * On effectue une verification sur le typage des elements de la liste.
     * <p/>
     * Note : <b>Aucune liste n'est creee, ni recopiee</b>
     *
     * @param <O>  le type des objets de la liste
     * @param list la liste a convertir
     * @param type le type des elements de la liste
     * @return la liste typee
     * @throws IllegalArgumentException si un element de la liste en entree
     *                                  n'est pas en adequation avec le type
     *                                  voulue.
     */
    @SuppressWarnings( { "unchecked" } )
    static public <O> List<O> toGenericList( List<?> list, Class<O> type )
        throws IllegalArgumentException
    {
        if ( list.isEmpty() )
        {
            return (List<O>) list;
        }
        for ( Object o : list )
        {
            if ( !type.isAssignableFrom( o.getClass() ) )
            {
                throw new IllegalArgumentException(
                    "can not cast List with object of type " + o.getClass() + " to " + type + " type!" );
            }
        }
        return (List<O>) list;
    }

}
