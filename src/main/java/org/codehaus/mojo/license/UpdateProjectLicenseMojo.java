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

package org.codehaus.mojo.license;

import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.utils.FileUtil;

import java.io.File;

/**
 * Updates (or creates) the main project license file according to the given
 * license defines as {@link #licenseName}.
 * <p/>
 * Can also generate a bundled license file (to avoid collision names in
 * class-path). This file is by default generated in
 * {@code META-INF class-path directory}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @goal update-project-license
 * @phase generate-resources
 * @requiresProject true
 * @since 1.0
 */
public class UpdateProjectLicenseMojo
        extends AbstractLicenseNameMojo {

    /**
     * Project license file to synchronize with main license defined in
     * descriptor file.
     *
     * @parameter expression="${license.licenceFile}" default-value="${basedir}/LICENSE.txt"
     * @required
     * @since 1.0
     */
    protected File licenseFile;

    /**
     * The directory where to generate license resources.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @parameter expression="${license.outputDirectory}"  default-value="target/generated-sources/license"
     * @since 1.0
     */
    protected File outputDirectory;

    /**
     * A flag to copy the main license file in a bundled place.
     * <p/>
     * This is usefull for final application to have a none confusing location
     * to seek for the application license.
     * <p/>
     * If Sets to {@code true}, will copy the license file to the
     * {@link #bundleLicensePath} to {@link #outputDirectory}.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @parameter expression="${license.generateBundle}"  default-value="false"
     * @since 1.0
     */
    protected boolean generateBundle;

    /**
     * The path of the bundled license file to produce when
     * {@link #generateBundle} is on.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @parameter expression="${license.bundleLicensePath}"  default-value="META-INF/${project.artifactId}-LICENSE.txt"
     * @since 1.0
     */
    protected String bundleLicensePath;

    /**
     * A flag to force to generate project license file even if it is up-to-date.
     *
     * @parameter expression="${license.force}"  default-value="false"
     * @since 1.0.0
     */
    protected boolean force;

    /**
     * A flag to skip the goal.
     *
     * @parameter expression="${license.skipUpdateProjectLicense}" default-value="false"
     * @since 1.0
     */
    protected boolean skipUpdateProjectLicense;

    /** Flag to known if generate is needed. */
    private boolean doGenerate;

    /** {@inheritDoc} */
    @Override
    protected void init()
            throws Exception {

        if (isSkip()) {
            return;
        }

        super.init();

        // must generate if file does not exist or pom never thant license file
        if (licenseFile != null) {
            File pomFile = getProject().getFile();

            this.doGenerate = force || !licenseFile.exists() || licenseFile.lastModified() <= pomFile.lastModified();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doAction()
            throws Exception {

        License license = getLicense();

        if (doGenerate) {

            getLog().info("Will create or update license file [" + license.getName() + "] to " + licenseFile);
            if (isVerbose()) {
                getLog().info("detail of license :\n" + license);
            }

            if (licenseFile.exists() && isKeepBackup()) {
                if (isVerbose()) {
                    getLog().info("backup " + licenseFile);
                }
                // copy it to backup file
                FileUtil.backupFile(licenseFile);
            }
        }

        // obtain license content
        String licenseContent = license.getLicenseContent(getEncoding());

        if (doGenerate) {

            // writes it root main license file
            FileUtil.writeString(licenseFile, licenseContent, getEncoding());
        }

        if (hasClassPath()) {

            // copy the license file to the resources directory
            File resourceTarget = new File(outputDirectory, licenseFile.getName());
            FileUtil.copyFile(this.licenseFile, resourceTarget);

            addResourceDir(outputDirectory, "**/" + resourceTarget.getName());

            if (generateBundle) {

                // creates the bundled license file
                File bundleTarget = FileUtil.getFile(outputDirectory, bundleLicensePath);
                FileUtil.copyFile(licenseFile, bundleTarget);

                if (!resourceTarget.getName().equals(bundleTarget.getName())) {

                    addResourceDir(outputDirectory, "**/" + bundleTarget.getName());
                }
            }
        }
    }

    @Override
    public boolean isSkip() {
        return skipUpdateProjectLicense;
    }

    @Override
    public void setSkip(boolean skipUpdateProjectLicense) {
        this.skipUpdateProjectLicense = skipUpdateProjectLicense;
    }

}
