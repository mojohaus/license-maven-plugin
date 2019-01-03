package org.codehaus.mojo.license.model;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2016 Tony Chemit
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
