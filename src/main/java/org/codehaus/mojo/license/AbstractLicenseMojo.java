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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Abstract license mojo.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public abstract class AbstractLicenseMojo
        extends AbstractMojo {

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Flag to activate verbose mode.
     * <p/>
     * <b>Note:</b> Verbose mode is always on if you starts a debug maven instance
     * (says via {@code -X}).
     *
     * @since 1.0
     */
    @Parameter(property = "license.verbose", defaultValue = "${maven.verbose}")
    private boolean verbose;

    /**
     * Encoding used to read and writes files.
     * <p/>
     * <b>Note:</b> If nothing is filled here, we will use the system
     * property {@code file.encoding}.
     *
     * @since 1.0
     */
    @Parameter(property = "license.encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

    /**
     * Current maven session. (used to launch certain mojo once by build).
     *
     * @since 1.0
     */
    @Component
    private MavenSession session;

    /**
     * The reacted project.
     *
     * @since 1.0
     */
    @Component
    private MavenProject project;

    // ----------------------------------------------------------------------
    // Abstract methods
    // ----------------------------------------------------------------------

    /**
     * When is sets to {@code true}, will skip execution.
     * <p/>
     * This will take effect in at the very begin of the {@link #execute()}
     * before any initialisation of goal.
     *
     * @return {@code true} if goal will not be executed
     */
    public abstract boolean isSkip();

    /**
     * Method to initialize the mojo before doing any concrete actions.
     * <p/>
     * <b>Note:</b> The method is invoked before the {@link #doAction()} method.
     *
     * @throws Exception if any
     */
    protected abstract void init()
            throws Exception;

    /**
     * Do plugin action.
     * <p/>
     * The method {@link #execute()} invoke this method only and only if :
     * <ul>
     * <li>{@link #checkPackaging()} returns {@code true}.</li>
     * <li>method {@link #init()} returns {@code true}.</li>
     * </ul>
     *
     * @throws Exception if any
     */
    protected abstract void doAction()
            throws Exception;

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public final void execute()
            throws MojoExecutionException, MojoFailureException {
        try {
            if ( getLog().isDebugEnabled() )
            {
                // always be verbose in debug mode
                setVerbose( true );
            }

            boolean mustSkip = isSkip();

            if ( mustSkip )
            {
                getLog().info( "skip flag is on, will skip goal." );
                return;
            }

            // check if project packaging is compatible with the mojo

            boolean canContinue = checkPackaging();
            if ( !canContinue )
            {
                getLog().warn( "The goal is skip due to packaging '" + getProject().getPackaging() + "'" );
                return;
            }

            // init the mojo

            try
            {
                checkEncoding();

                init();

            }
            catch ( MojoFailureException e )
            {
                throw e;
            }
            catch ( MojoExecutionException e )
            {
                throw e;
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException(
                    "could not init goal " + getClass().getSimpleName() + " for reason : " + e.getMessage(), e );
            }

            // check if mojo can be skipped

            canContinue = checkSkip();
            if ( !canContinue )
            {
                if ( isVerbose() )
                {
                    getLog().info( "Goal will not be executed." );
                }
                return;
            }

            // can really execute the mojo

            try
            {
                doAction();
            }
            catch ( MojoFailureException e )
            {
                throw e;
            }
            catch ( MojoExecutionException e )
            {
                throw e;
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException(
                    "could not execute goal " + getClass().getSimpleName() + " for reason : " + e.getMessage(), e );
            }
        } finally {
            afterExecute();
        }
    }

    // ----------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------

    /** @return the enconding used to read and write files. */
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

    /** @return the current maven project */
    public final MavenProject getProject() {
        return project;
    }

    /** @return {@code true} if verbose flag is on, {@code false} otherwise */
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

    /** @return the {@link MavenSession}. */
    public final MavenSession getSession() {
        return session;
    }

    /** @return the build timestamp (used to have a unique timestamp all over a build). */
    public final long getBuildTimestamp() {
        return session.getStartTime().getTime();
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /** A call back to execute after the {@link #execute()} is done. */
    protected void afterExecute() {
        // by default do nothing
    }

    /**
     * Check if the project packaging is acceptable for the mojo.
     * <p/>
     * By default, accept all packaging types.
     * <p/>
     * <b>Note:</b> This method is the first instruction to be executed in
     * the {@link #execute()}.
     * <p/>
     * <b>Tip:</b> There is two method to simplify the packaging check :
     * <p/>
     * {@link #acceptPackaging(String...)}
     * <p/>
     * and
     * <p/>
     * {@link #rejectPackaging(String...)}
     *
     * @return {@code true} if can execute the goal for the packaging of the
     *         project, {@code false} otherwise.
     */
    protected boolean checkPackaging() {
        // by default, accept every type of packaging
        return true;
    }

    /**
     * Checks if the mojo execution should be skipped.
     *
     * @return {@code false} if the mojo should not be executed.
     */
    protected boolean checkSkip() {
        // by default, never skip goal
        return true;
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
     * <p/>
     * If no encoding was filled, then use the default for system
     * (via {@code file.encoding} environement property).
     */
    protected void checkEncoding() {

        if (isVerbose()) {
            getLog().info("Will check encoding : " + getEncoding());
        }
        if (StringUtils.isEmpty(getEncoding())) {
            getLog().warn("File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING +
                          ", i.e. build is platform dependent!");
            setEncoding(ReaderFactory.FILE_ENCODING);
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
            getLog().info("add resource " + dir + " with includes " + Arrays.toString(includes));
        }
    }

    /** @return {@code true} if project is not a pom, {@code false} otherwise. */
    protected boolean hasClassPath() {
        return rejectPackaging("pom");
    }

}
