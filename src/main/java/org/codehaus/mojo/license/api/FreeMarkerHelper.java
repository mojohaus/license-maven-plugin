package org.codehaus.mojo.license.api;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * A helper to deal with freemarker templating
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role="org.codehaus.mojo.license.api.FreeMarkerHelper" role-hint="default"
 * @since 1.1
 */
public class FreeMarkerHelper {

    /**
     * Shared freemarker configuration.
     * <p/>
     * <b>Note: </b> The configuration is auto loading as needed.
     */
    protected Configuration freemarkerConfiguration;


    public Template getTemplate(String templateName) throws IOException {
        Template template =
                getFreemarkerConfiguration().getTemplate(templateName);

        if (template == null) {
            throw new IOException("Could not find template " + templateName);
        }
        return template;
    }

    public String renderTemplate(String templateName,
                                 Map<String, Object> parameters) throws IOException {
        Template template = getTemplate(templateName);
        StringWriter out = new StringWriter();
        try {
            template.process(parameters, out);
        } catch (TemplateException e) {
            throw new IOException("Could not render template " +
                                  templateName + " for reason " + e.getMessage());
        }
        return out.toString();
    }

    protected Configuration getFreemarkerConfiguration() {
        if (freemarkerConfiguration == null) {
            freemarkerConfiguration = new Configuration();
            TemplateLoader loader = new ClassTemplateLoader(getClass(), "/");
            freemarkerConfiguration.setTemplateLoader(loader);
            BeansWrapper objectWrapper = new DefaultObjectWrapper();
            freemarkerConfiguration.setObjectWrapper(objectWrapper);
        }
        return freemarkerConfiguration;
    }
}
