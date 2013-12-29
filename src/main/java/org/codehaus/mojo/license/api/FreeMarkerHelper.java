package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2011 CodeLutin, Codehaus, Tony Chemit
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

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * A helper to deal with freemarker templating.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public class FreeMarkerHelper
{

    public static final String TEMPLATE = "template";

    /**
     * Shared freemarker configuration.
     * <p/>
     * <b>Note: </b> The configuration is auto loading as needed.
     */
    protected final Configuration freemarkerConfiguration;

    protected final TemplateLoader templateLoader;

    /**
     * @return a default helper, if template is a file, then will use it as it, otherwise will have a look into
     * classloader in current package.
     */
    public static FreeMarkerHelper newDefaultHelper()
    {
        ClassTemplateLoader templateLoader = new ClassTemplateLoader( FreeMarkerHelper.class, "/" );
        FreeMarkerHelper result = new FreeMarkerHelper( templateLoader );
        return result;
    }

    /**
     *
     * @param stringTemplate
     * @return a helper, if template is a file, then will use it as it, otherwise will use the given template
     */
    public static FreeMarkerHelper newHelperFromContent( String stringTemplate )
    {

        StringTemplateLoader templateLoader = new StringTemplateLoader();
        templateLoader.putTemplate( TEMPLATE, stringTemplate );

        FreeMarkerHelper result = new FreeMarkerHelper( templateLoader );
        return result;
    }

    protected FreeMarkerHelper( TemplateLoader templateLoader )
    {
        this.templateLoader = templateLoader;
        freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setTemplateLoader( templateLoader );
        BeansWrapper objectWrapper = new DefaultObjectWrapper();
        freemarkerConfiguration.setObjectWrapper( objectWrapper );
    }

    public Template getTemplate( String templateName )
        throws IOException
    {

        File file = new File( templateName );
        if ( file.exists() )
        {

            // this is a file
            freemarkerConfiguration.setTemplateLoader( new FileTemplateLoader( file.getParentFile() ) );
            templateName = file.getName();
        }
        else
        {

            // just use the classloader
            freemarkerConfiguration.setTemplateLoader( templateLoader );
        }
        Template template = freemarkerConfiguration.getTemplate( templateName );

        if ( template == null )
        {
            throw new IOException( "Could not find template " + templateName );
        }
        return template;
    }

    public String renderTemplate( String templateName, Map<String, Object> parameters )
        throws IOException
    {

        Template template = getTemplate( templateName );
        return renderTemplate( template, parameters );
    }

    public String renderTemplate( Template template, Map<String, Object> parameters )
        throws IOException
    {

        StringWriter out = new StringWriter();
        try
        {
            template.process( parameters, out );
        }
        catch ( TemplateException e )
        {
            throw new IOException( "Could not render template " +
                                       template.getName() + " for reason " + e.getMessage() );
        }
        return out.toString();
    }
}
