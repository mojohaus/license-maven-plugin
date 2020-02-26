package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 MojoHaus
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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.download.LicenseSummaryWriter;
import org.codehaus.mojo.license.download.LicensedArtifactResolver;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common parent for {@link LicensesXmlInsertVersionsMojo} and {@link AbstractDownloadLicensesMojo}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.19
 */
public abstract class AbstractLicensesXmlMojo
    extends AbstractMojo
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractLicensesXmlMojo.class );

    /**
     * The output file containing a mapping between each dependency and it's license information.
     *
     * @since 1.0
     */
    @Parameter( property = "licensesOutputFile",
                    defaultValue = "${project.build.directory}/generated-resources/licenses.xml" )
    protected File licensesOutputFile;

    /**
     * An end of line constant name denoting the EOL string to use when redering the {@code licenses.xml} file. Possible
     * values are {@code LF}, {@code CRLF}, {@code AUTODETECT} and {@code PLATFORM}.
     * <p>
     * When the value {@code AUTODETECT} is used, the mojo will use whatever EOL value is used in the first existing of
     * the following files: {@link #licensesConfigFile}, <code>${basedir}/pom.xml</code>.
     * <p>
     * The value {@code PLATFORM} is deprecated but still kept for backwards compatibility reasons.
     *
     * @since 1.17
     */
    @Parameter( property = "licensesOutputFileEol", defaultValue = "AUTODETECT" )
    protected Eol licensesOutputFileEol;

    /**
     * Encoding used to (1) read the file specified in {@link #licensesConfigFile} and (2) write the file specified in
     * {@link #licensesOutputFile}.
     *
     * @since 1.17
     */
    @Parameter( property = "licensesOutputFileEncoding", defaultValue = "${project.build.sourceEncoding}" )
    private String licensesOutputFileEncoding;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true )
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * The Maven Project Object
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    /**
     * Licensed artifact resolver.
     *
     * @since 1.0
     */
    @Component
    protected LicensedArtifactResolver licensedArtifactResolver;

    private Charset charset;

    /** {@inheritDoc} */
    public String getEncoding()
    {
        initEncoding();
        return licensesOutputFileEncoding;
    }

    Charset getCharset()
    {
        initEncoding();
        return charset;
    }

    private void initEncoding()
    {
        if ( charset == null )
        {
            if ( licensesOutputFileEncoding == null )
            {
                licensesOutputFileEncoding = System.getProperty( "file.encoding" );
                LOG.warn( "Using the default system encoding for reading or writing licenses.xml file."
                    + " This makes your build platform dependent. You should set either"
                    + " project.build.sourceEncoding or licensesOutputFileEncoding" );
            }
            charset = Charset.forName( licensesOutputFileEncoding );

            if ( licensesOutputFileEol == Eol.AUTODETECT )
            {
                final Path[] paths = getAutodetectEolFiles();
                Path autodetectFromFile = null;
                for ( Path path : paths )
                {
                    if ( Files.exists( path ) )
                    {
                        autodetectFromFile = path;
                        break;
                    }
                }
                if ( autodetectFromFile != null )
                {
                    try
                    {
                        licensesOutputFileEol = Eol.autodetect( autodetectFromFile, charset );
                    }
                    catch ( IOException e )
                    {
                        throw new RuntimeException( "Cannot autodetect end of line from file \"" + autodetectFromFile
                            + "\"", e );
                    }
                }
            }
        }
    }

    protected Path[] getAutodetectEolFiles()
    {
        return new Path[] { licensesOutputFile.toPath() };
    }

    protected void writeLicenseSummary( List<ProjectLicenseInfo> deps, File licensesOutputFile, boolean writeVersions )
        throws ParserConfigurationException, TransformerException, IOException
    {
        initEncoding();
        LicenseSummaryWriter.writeLicenseSummary( deps, licensesOutputFile, charset, licensesOutputFileEol,
                                                  writeVersions );
    }

}
