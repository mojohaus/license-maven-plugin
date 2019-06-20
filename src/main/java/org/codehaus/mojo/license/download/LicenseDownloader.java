package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 CodeLutin, Codehaus, Tony Chemit
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Proxy;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.ContentSanitizer;
import org.codehaus.mojo.license.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for downloading remote license files.
 *
 * @author pgier
 * @since 1.0
 */
public class LicenseDownloader implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger( LicenseDownloader.class );

    private static final Pattern EXTENSION_PATTERN = Pattern.compile( "\\.[a-z]{1,4}$", Pattern.CASE_INSENSITIVE );

    private final CloseableHttpClient client;

    private final Map<String, ContentSanitizer> contentSanitizers;
    private final Charset charset;

    public LicenseDownloader( Proxy proxy, int connectTimeout, int socketTimeout, int connectionRequestTimeout,
                              Map<String, ContentSanitizer> contentSanitizers, Charset charset )
    {
        this.contentSanitizers = contentSanitizers;
        this.charset = charset;
        final Builder configBuilder = RequestConfig.copy( RequestConfig.DEFAULT ) //
                        .setConnectTimeout( connectTimeout ) //
                        .setSocketTimeout( socketTimeout ) //
                        .setConnectionRequestTimeout( connectionRequestTimeout );

        if ( proxy != null )
        {
            configBuilder.setProxy( new HttpHost( proxy.getHost(), proxy.getPort(), proxy.getProtocol() ) );
        }

        HttpClientBuilder clientBuilder = HttpClients.custom().setDefaultRequestConfig( configBuilder.build() );
        if ( proxy != null )
        {
            if ( proxy.getUsername() != null && proxy.getPassword() != null )
            {
                final CredentialsProvider credsProvider = new BasicCredentialsProvider();
                final Credentials creds = new UsernamePasswordCredentials( proxy.getUsername(), proxy.getPassword() );
                credsProvider.setCredentials( new AuthScope( proxy.getHost(), proxy.getPort() ), creds );
                clientBuilder.setDefaultCredentialsProvider( credsProvider );
            }
            final String rawNonProxyHosts = proxy.getNonProxyHosts();
            if ( rawNonProxyHosts != null )
            {
                final String[] nonProxyHosts = rawNonProxyHosts.split( "|" );
                if ( nonProxyHosts.length > 0 )
                {
                    final List<Pattern> nonProxyPatterns = new ArrayList<>();
                    for ( String nonProxyHost : nonProxyHosts )
                    {
                        final Pattern pat =
                            Pattern.compile( nonProxyHost.replaceAll( "\\.", "\\\\." ).replaceAll( "\\*", ".*" ),
                                             Pattern.CASE_INSENSITIVE );
                        nonProxyPatterns.add( pat );
                    }
                    final HttpHost proxyHost = new HttpHost( proxy.getHost(), proxy.getPort() );
                    final HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner( proxyHost )
                    {

                        @Override
                        protected HttpHost determineProxy( HttpHost target, HttpRequest request, HttpContext context )
                            throws HttpException
                        {
                            for ( Pattern pattern : nonProxyPatterns )
                            {
                                if ( pattern.matcher( target.getHostName() ).matches() )
                                {
                                    return null;
                                }
                            }
                            return super.determineProxy( target, request, context );
                        }

                    };
                    clientBuilder.setRoutePlanner( routePlanner );
                }
            }
        }
        this.client = clientBuilder.build();
    }

    /**
     * Downloads a license file from the given {@code licenseUrlString} stores it locally and
     * returns the local path where the license file was stored. Note that the
     * {@code outputFile} name can be further modified by this method, esp. the file extension
     * can be adjusted based on the mime type of the HTTP response.
     *
     * @param licenseUrlString the URL
     * @param outputFile a hint where to store the license file
     * @return the path to the file where the downloaded license file was stored
     * @throws IOException
     * @throws URISyntaxException
     * @throws MojoFailureException
     */
    public LicenseDownloadResult downloadLicense( String licenseUrlString, FileNameEntry fileNameEntry )
        throws IOException, URISyntaxException, MojoFailureException
    {
        final File outputFile = fileNameEntry.getFile();
        if ( licenseUrlString == null || licenseUrlString.length() == 0 )
        {
            throw new IllegalArgumentException( "Null URL for file " + outputFile.getPath() );
        }

        List<ContentSanitizer> sanitizers = filterSanitizers( licenseUrlString );

        if ( licenseUrlString.startsWith( "file://" ) )
        {
            LOG.debug( "Downloading '{}' -> '{}'", licenseUrlString, outputFile );
            Path in = Paths.get( new URI( licenseUrlString ) );
            if ( sanitizers.isEmpty() )
            {
                Files.copy( in, outputFile.toPath() );
                return LicenseDownloadResult.success( outputFile, FileUtil.sha1( in ), fileNameEntry.isPreferred() );
            }
            else
            {
                try ( BufferedReader r = Files.newBufferedReader( in, charset ) )
                {
                    return sanitize( r, outputFile, charset, sanitizers, fileNameEntry.isPreferred() );
                }
            }
        }
        else
        {
            LOG.debug( "About to download '{}'", licenseUrlString );
            try ( CloseableHttpResponse response = client.execute( new HttpGet( licenseUrlString ) ) )
            {
                final StatusLine statusLine = response.getStatusLine();
                if ( statusLine.getStatusCode() != HttpStatus.SC_OK )
                {
                    return LicenseDownloadResult.failure( "'" + licenseUrlString + "' returned "
                        + statusLine.getStatusCode()
                        + ( statusLine.getReasonPhrase() != null ? " " + statusLine.getReasonPhrase() : "" ) );
                }

                final HttpEntity entity = response.getEntity();
                if ( entity != null )
                {
                    final ContentType contentType = ContentType.get( entity );

                    File updatedFile = fileNameEntry.isPreferred() ? outputFile
                                    : updateFileExtension( outputFile,
                                                           contentType != null ? contentType.getMimeType() : null );
                    LOG.debug( "Downloading '{}' -> '{}'{}",
                        licenseUrlString,
                        updatedFile,
                        fileNameEntry.isPreferred() ? " (preferred file name)" : "" );

                    if ( sanitizers.isEmpty() )
                    {
                        try ( InputStream in = entity.getContent();
                                        FileOutputStream fos = new FileOutputStream( updatedFile ) )
                        {
                            final MessageDigest md = MessageDigest.getInstance( "SHA-1" );
                            final byte[] buf = new byte[1024];
                            int len;
                            while ( ( len = in.read( buf ) ) >= 0 )
                            {
                                md.update( buf, 0, len );
                                fos.write( buf, 0, len );
                            }
                            final String actualSha1 = Hex.encodeHexString( md.digest() );
                            final String expectedSha1 = fileNameEntry.getSha1();
                            if ( expectedSha1 != null && !expectedSha1.equals( actualSha1 ) )
                            {
                                throw new MojoFailureException( "URL '" + licenseUrlString
                                    + "' returned content with unexpected sha1 '" + actualSha1 + "'; expected '"
                                    + expectedSha1 + "'. You may want to (a) re-run the current mojo"
                                    + " with -Dlicense.forceDownload=true or (b) change the expected sha1 in"
                                    + " the licenseUrlFileNames entry '" + fileNameEntry.getFile().getName()
                                    + "' or (c) split the entry so that"
                                    + " its URLs return content with different sha1 sums." );
                            }
                            return LicenseDownloadResult.success( updatedFile, actualSha1,
                                                                  fileNameEntry.isPreferred() );
                        }
                        catch ( NoSuchAlgorithmException e )
                        {
                            throw new RuntimeException( e );
                        }
                    }
                    else
                    {
                        final Charset cs = contentType != null
                                        ? ( contentType.getCharset() == null ? this.charset : contentType.getCharset() )
                                        : this.charset;
                        try ( BufferedReader r =
                                        new BufferedReader( new InputStreamReader( entity.getContent(), cs ) ) )
                        {
                            return sanitize( r, updatedFile, cs, sanitizers, fileNameEntry.isPreferred() );
                        }
                    }
                }
                else
                {
                    return LicenseDownloadResult.failure( "'" + licenseUrlString + "' returned no body." );
                }
            }
        }
    }

    static LicenseDownloadResult sanitize( BufferedReader r, File out, Charset charset,
                                           List<ContentSanitizer> sanitizers, boolean preferredFileName )
        throws IOException
    {
        final StringBuilder contentBuilder = new StringBuilder();
        // CHECKSTYLE_OFF: MagicNumber
        char[] buffer = new char[8192];
        // CHECKSTYLE_ON: MagicNumber
        int len = 0;
        while ( ( len = r.read( buffer ) ) >= 0 )
        {
            contentBuilder.append( buffer, 0, len );
        }

        String content = contentBuilder.toString();
        for ( ContentSanitizer sanitizer : sanitizers )
        {
            content = sanitizer.sanitize( content );
        }
        byte[] bytes = content.getBytes( charset );
        Files.write( out.toPath(), bytes );
        final String sha1 = DigestUtils.sha1Hex( bytes );
        return LicenseDownloadResult.success( out, sha1, preferredFileName );
    }

    List<ContentSanitizer> filterSanitizers( String licenseUrlString )
    {
        ArrayList<ContentSanitizer> result = new ArrayList<>();
        for ( ContentSanitizer s : contentSanitizers.values() )
        {
            if ( s.applies( licenseUrlString ) )
            {
                result.add( s );
            }
        }
        return result;
    }

    static File updateFileExtension( File outputFile, String mimeType )
    {
        String realExtension = FileUtil.toExtension( mimeType, false );
        if ( realExtension == null )
        {
            /* default extension is .txt */
            realExtension = ".txt";
        }

        final String oldFileName = outputFile.getName();
        if ( !oldFileName.endsWith( realExtension ) )
        {
            final String newFileName = EXTENSION_PATTERN.matcher( oldFileName ).replaceAll( "" ) + realExtension;
            return new File( outputFile.getParentFile(), newFileName );
        }
        return outputFile;
    }

    @Override
    public void close() throws IOException
    {
        client.close();
    }

    /**
     * A result of a license download operation.
     *
     * @since 1.18
     */
    public static class LicenseDownloadResult
    {
        public static LicenseDownloadResult success( File file, String sha1, boolean preferredFileName )
        {
            return new LicenseDownloadResult( file, sha1, preferredFileName, null );
        }

        public static LicenseDownloadResult failure( String errorMessage )
        {
            return new LicenseDownloadResult( null, null, false, errorMessage );
        }

        private LicenseDownloadResult( File file, String sha1, boolean preferredFileName, String errorMessage )
        {
            super();
            this.file = file;
            this.errorMessage = errorMessage;
            this.sha1 = sha1;
            this.preferredFileName = preferredFileName;
        }

        private final File file;

        private final String errorMessage;

        private final String sha1;

        private final boolean preferredFileName;

        public File getFile()
        {
            return file;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }

        public boolean isSuccess()
        {
            return errorMessage == null;
        }

        public boolean isPreferredFileName()
        {
            return preferredFileName;
        }

        public String getSha1()
        {
            return sha1;
        }

        public LicenseDownloadResult withFile( File otherFile )
        {
            return new LicenseDownloadResult( otherFile, sha1, preferredFileName, errorMessage );
        }
    }

}
