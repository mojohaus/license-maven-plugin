package org.codehaus.mojo.license.model;

import java.net.URL;

/**
 * A special class-path license repository defined on root java package.
 * Created on 21/05/16.
 *
 * @author Tony Chemit - dev@tchemit.fr
 * @since 1.9
 */
public class RootPackageClassPathLicenseRepository
    extends LicenseRepository
{

    @Override
    protected URL getDefinitionURL()
    {
        return getClass().getClassLoader().getResource( REPOSITORY_DEFINITION_FILE );
    }

    @Override
    protected URL getLicenseBaseURL( String licenseName )
    {
        return getClass().getClassLoader().getResource( licenseName );
    }

}
