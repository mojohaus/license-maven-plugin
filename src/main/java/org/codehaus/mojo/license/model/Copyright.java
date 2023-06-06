package org.codehaus.mojo.license.model;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2014 CodeLutin, Codehaus, Tony Chemit
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

import java.util.Calendar;
import java.util.Date;

/**
 * Model of a copyright.
 *
 * Created on 1/8/14.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.6
 */
public class Copyright
{

    /**
     * Default copyright to string format.
     */
    protected static final String DEFAULT_COPYRIGHT_TO_STRING_FORMAT = "Copyright (C) %1$s %2$s";

    /**
     * Copyright to string format.
     */
    protected String copyrightToStringFormat;

    /**
     * Copyright holder.
     */
    protected String holder;

    /**
     * Copyright first year of application.
     */
    protected int firstYear;

    /**
     * Copyright last year of application (can be null if copyright is
     * only on one year).
     */
    protected Integer lastYear;

    public static Copyright newCopyright( String copyrightToStringFormat,
            Integer inceptionYear, Integer lastYear, String holder )
    {
        int firstYear = inceptionYear == null ? lastYear : inceptionYear;
        if ( lastYear == null || firstYear >= lastYear )
        {
            lastYear = null;
        }
        if ( copyrightToStringFormat == null )
        {
            return newCopyright( DEFAULT_COPYRIGHT_TO_STRING_FORMAT, firstYear, lastYear, holder );
        }
        Copyright result = new Copyright( copyrightToStringFormat, firstYear, lastYear, holder );
        return result;
    }

    public static Copyright newCopyright( Integer inceptionYear, Integer lastYear, String holder )
    {
        return newCopyright( DEFAULT_COPYRIGHT_TO_STRING_FORMAT, inceptionYear, lastYear, holder );
    }

    public static Copyright newCopyright( String copyrightToStringFormat, Integer inceptionYear, String holder )
    {

        Calendar cal = Calendar.getInstance();
        cal.setTime( new Date() );
        Integer lastYear = cal.get( Calendar.YEAR );

        return newCopyright( copyrightToStringFormat, inceptionYear, lastYear, holder );
    }

    public static Copyright newCopyright( Integer inceptionYear, String holder )
    {

        Calendar cal = Calendar.getInstance();
        cal.setTime( new Date() );
        Integer lastYear = cal.get( Calendar.YEAR );

        return newCopyright( inceptionYear, lastYear, holder );
    }

    public Copyright()
    {
    }

    public Copyright( Copyright copyright )
    {
        this( copyright.getCopyrightToStringFormat(), copyright.getFirstYear(),
                copyright.getLastYear(), copyright.getHolder() );
    }

    public Copyright( String copyrightToStringFormat, int firstYear, Integer lastYear, String holder )
    {
        this.copyrightToStringFormat = copyrightToStringFormat;
        this.firstYear = firstYear;
        this.lastYear = lastYear;
        this.holder = holder;
    }

    /**
     * @return the copyright to string format
     */
    public String getCopyrightToStringFormat()
    {
        return copyrightToStringFormat;
    }

    /**
     * @return the copyright holder
     */
    public String getHolder()
    {
        return holder;
    }

    /**
     * @return the first year of the copyright
     */
    public Integer getFirstYear()
    {
        return firstYear;
    }

    /**
     * @return the last year of the copyright (if copyright affects only one
     * year, can be equals to the {@link #getFirstYear()}).
     */
    public Integer getLastYear()
    {
        return lastYear;
    }

    /**
     * Produces a string representation of the copyright year range.
     *
     * If copyright acts on one year :
     * <pre>
     * 2010
     * </pre>
     *
     * If copyright acts on more than one year :
     * <pre>
     * 2010 - 2012
     * </pre>
     *
     * @return the String representation of the copyright year range.
     */
    public String getYears()
    {
        String years;
        if ( getLastYear() == null )
        {

            // copyright on one year
            years = String.valueOf( getFirstYear() );
        }
        else
        {

            // copyright on more than one year
            years = getFirstYear() + " - " + getLastYear();
        }
        return years;
    }

    /**
     * Produces a string representation of the copyright.
     *
     * If copyright acts on one year :
     * <pre>
     * Copyright (C) 2010 Holder
     * </pre>
     *
     * If copyright acts on more than one year :
     * <pre>
     * Copyright (C) 2010 - 2012 Holder
     * </pre>
     *
     * @return the String representation of the copyright
     */
    public String getText()
    {
        String copyright = String.format( getCopyrightToStringFormat(), getYears(), getHolder() );
        return copyright;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof Copyright ) )
        {
            return false;
        }

        Copyright copyright = (Copyright) o;

        if ( firstYear != copyright.firstYear )
        {
            return false;
        }
        if ( holder != null ? !holder.equals( copyright.holder ) : copyright.holder != null )
        {
            return false;
        }
        if ( lastYear != null ? !lastYear.equals( copyright.lastYear ) : copyright.lastYear != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = holder != null ? holder.hashCode() : 0;
        result = 31 * result + firstYear;
        result = 31 * result + ( lastYear != null ? lastYear.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return getText();
    }
}
