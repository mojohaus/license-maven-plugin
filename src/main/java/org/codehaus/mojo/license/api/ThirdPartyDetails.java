package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 CodeLutin, Codehaus, Tony Chemit
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
 * Details of a artifact about his third-parties stuff.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public interface ThirdPartyDetails
{

    /**
     * @return the groupId of the dependency
     */
    String getGroupId();

    /**
     * @return the artifactId of the dependency
     */
    String getArtifactId();

    /**
     * @return the version of the dependency
     */
    String getVersion();

    /**
     * @return the type of the dependency
     */
    String getType();

    /**
     * @return the classifier of the dependency
     */
    String getClassifier();

    /**
     * @return the scope of the dependency
     */
    String getScope();

    /**
     * @return {@code true} if the project contains at least one license (from pom or third-party missing file).
     */
    boolean hasLicenses();

    /**
     * @return the licenses defined of the dependency
     */
    String[] getLicenses();

    /**
     * @return {@code true} if the project contains a license in his pom.
     */
    boolean hasPomLicenses();

    /**
     * @return the licenses defined in the pom of the project.
     */
    String[] getPomLicenses();

    /**
     * Sets the pom licenses.
     *
     * @param pomLicenses licenses loaded from the pom file
     */
    void setPomLicenses( String[] pomLicenses );

    /**
     * @return the licenses defined in the third-party file.
     */
    String[] getThirdPartyLicenses();

    /**
     * Sets the third-party licenses.
     *
     * @param thirdPartyLicenses licenses loaded from the third-party file
     */
    void setThirdPartyLicenses( String[] thirdPartyLicenses );

    /**
     * @return {@code true} if the project has his licenses defined in the third-party file.
     */
    boolean hasThirdPartyLicenses();
}
