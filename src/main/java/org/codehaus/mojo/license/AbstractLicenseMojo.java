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

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract license mojo.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public abstract class AbstractLicenseMojo extends AbstractMojo {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLicenseMojo.class);

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Flag to activate verbose mode.
     *
     * <b>Note:</b> Verbose mode is always on if you starts a debug maven instance
     * (says via {@code -X}).
     *
     * @since 1.0
     */
    @Parameter(property = "license.verbose", defaultValue = "${maven.verbose}")
    boolean verbose;

    /**
     * Encoding used to read and writes files.
     *
     * <b>Note:</b> If nothing is filled here, we will use the system
     * property {@code file.encoding}.
     *
     * @since 1.0
     */
    @Parameter(property = "license.encoding", defaultValue = "${project.build.sourceEncoding}")
    String encoding;

    /**
     * The reacted project.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    // ----------------------------------------------------------------------
    // Abstract methods
    // ----------------------------------------------------------------------

    /**
     * When is sets to {@code true}, will skip execution.
     *
     * This will take effect in at the very begin of the {@link #execute()}
     * before any initialisation of goal.
     *
     * @return {@code true} if goal will not be executed
     */
    public abstract boolean isSkip();

    /**
     * Method to initialize the mojo before doing any concrete actions.
     *
     * <b>Note:</b> The method is invoked before the {@link #doAction()} method.
     *
     * @throws Exception if any
     */
    protected abstract void init() throws Exception;

    /**
     * Do plugin action.
     *
     * The method {@link #execute()} invoke this method only and only if :
     * <ul>
     * <li>{@link #checkPackaging()} returns {@code true}.</li>
     * <li>method {@link #init()} returns {@code true}.</li>
     * </ul>
     *
     * @throws Exception if any
     */
    protected abstract void doAction() throws Exception;

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public final void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (getLog().isDebugEnabled()) {
                // always be verbose in debug mode
                setVerbose(true);
            }

            boolean mustSkip = isSkip();

            if (mustSkip) {
                LOG.info("skip flag is on, will skip goal.");
                return;
            }

            // check if project packaging is compatible with the mojo

            boolean canContinue = checkPackaging();
            if (!canContinue) {
                LOG.info("The goal is skip due to packaging '{}'", getProject().getPackaging());
                return;
            }

            // init the mojo

            try {
                checkEncoding();

                init();

            } catch (MojoFailureException | MojoExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "could not init goal " + getClass().getSimpleName() + " for reason : " + e.getMessage(), e);
            }

            // check if mojo can be skipped
            if (shouldSkip()) {
                LOG.info("All files are up to date, skip goal execution.");
                return;
            }

            // can really execute the mojo

            try {
                doAction();
            } catch (MojoFailureException | MojoExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "could not execute goal " + getClass().getSimpleName() + " for reason : " + e.getMessage(), e);
            }
        } finally {
            afterExecute();
        }
    }

    // ----------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------

    /**
     * @return the enconding used to read and write files.
     */
    public final String getEncoding() {
        return encoding;
    }

    /**
     * Sets new encoding used to read and write files.
     *
     * @param encoding new encodnignt ing to use
     */
    public final void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the current maven project
     */
    public final MavenProject getProject() {
        return project;
    }

    /**
     * @return {@code true} if verbose flag is on, {@code false} otherwise
     */
    public final boolean isVerbose() {
        return verbose;
    }

    /**
     * Sets new value to {@link #verbose} flag.
     *
     * @param verbose new value to set
     */
    public final void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * A call back to execute after the {@link #execute()} is done.
     */
    protected void afterExecute() {
        // by default do nothing
    }

    /**
     * Check if the project packaging is acceptable for the mojo.
     *
     * By default, accept all packaging types.
     *
     * <b>Note:</b> This method is the first instruction to be executed in
     * the {@link #execute()}.
     *
     * <b>Tip:</b> There is two method to simplify the packaging check :
     *
     * {@link #acceptPackaging(String...)}
     *
     * and
     *
     * {@link #rejectPackaging(String...)}
     *
     * @return {@code true} if can execute the goal for the packaging of the
     * project, {@code false} otherwise.
     */
    protected boolean checkPackaging() {
        // by default, accept every type of packaging
        return true;
    }

    /**
     * Checks if the mojo execution should be skipped.
     *
     * @return {@code true} if the mojo should not be executed.
     */
    protected boolean shouldSkip() {
        // by default, never skip goal
        return false;
    }

    /**
     * Accept the project's packaging between some given.
     *
     * @param packages the accepted packaging
     * @return {@code true} if the project's packaging is one of the given ones.
     */
    protected boolean acceptPackaging(String... packages) {
        String projectPackaging = getProject().getPackaging();

        for (String p : packages) {
            if (p.equals(projectPackaging)) {
                // accept packaging
                return true;
            }
        }
        // reject packaging
        return false;
    }

    /**
     * Accept the project's packaging if not in given one.
     *
     * @param packages the rejecting packagings
     * @return {@code true} if the project's packaging is not in the given ones.
     */
    protected boolean rejectPackaging(String... packages) {
        String projectPackaging = getProject().getPackaging();

        for (String p : packages) {
            if (p.equals(projectPackaging)) {
                // reject this packaging
                return false;
            }
        }
        // accept packaging
        return true;
    }

    /**
     * Method to be invoked in init phase to check sanity of {@link #getEncoding()}.
     *
     * If no encoding was filled, then use the default for system
     * (via {@code file.encoding} environement property).
     */
    protected void checkEncoding() {

        if (isVerbose()) {
            LOG.info("Will check encoding: {}", getEncoding());
        }
        if (StringUtils.isEmpty(getEncoding())) {
            String sysEncoding = Charset.defaultCharset().displayName();
            LOG.warn(
                    "File encoding has not been set, using platform encoding {}, i.e. build is platform dependent!",
                    sysEncoding);
            setEncoding(sysEncoding);
        }
    }

    /**
     * Add a new resource location to the maven project
     * (in not already present).
     *
     * @param dir      the new resource location to add
     * @param includes files to include
     */
    protected void addResourceDir(File dir, String... includes) {
        boolean added = MojoHelper.addResourceDir(dir, getProject(), includes);
        if (added && isVerbose()) {
            LOG.info("add resource {} with includes {}", dir, (Object) includes);
        }
    }

    /**
     * @return {@code true} if project is not a pom, {@code false} otherwise.
     */
    protected boolean hasClassPath() {
        return rejectPackaging("pom");
    }
}
