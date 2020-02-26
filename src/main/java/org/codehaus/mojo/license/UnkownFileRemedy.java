/******************************************************************************
 * COMINTO GmbH
 * Klosterstr. 49
 * 40211 Düsseldorf
 * Germany
 *
 * (c) Copyright 2020 by COMINTO GmbH
 * ALL RIGHTS RESERVED
 *
 ******************************************************************************/
package org.codehaus.mojo.license;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * What to do in case of a file not found in project.
 *
 * @since 2.0.1
 */
public enum UnkownFileRemedy
{
    /** Unkown files will be logged debug */
    debug,
    /** Unkown files are output to the log as warnings */
    warn,
    /**
     * The first encountered unkown file is logged and a {@link MojoExecutionException} is thrown
     */
    failFast,
}