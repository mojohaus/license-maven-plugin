package org.codehaus.mojo.license;

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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.codehaus.mojo.license.api.ArtifactFilters;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.download.Cache;
import org.codehaus.mojo.license.download.FileNameEntry;
import org.codehaus.mojo.license.download.LicenseDownloader;
import org.codehaus.mojo.license.download.LicenseDownloader.LicenseDownloadResult;
import org.codehaus.mojo.license.download.LicenseMatchers;
import org.codehaus.mojo.license.download.LicenseSummaryReader;
import org.codehaus.mojo.license.download.LicensedArtifact;
import org.codehaus.mojo.license.download.PreferredFileNames;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.download.UrlReplacements;
import org.codehaus.mojo.license.extended.InfoFile;
import org.codehaus.mojo.license.extended.spreadsheet.CalcFileWriter;
import org.codehaus.mojo.license.extended.spreadsheet.ExcelFileWriter;
import org.codehaus.mojo.license.spdx.SpdxLicenseList;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.ContentSanitizer;
import org.codehaus.mojo.license.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 23/05/16.
 *
 * @author Tony Chemit - chemit@codelutin.com
 */
public abstract class AbstractDownloadLicensesMojo extends AbstractLicensesXmlMojo
        implements MavenProjectDependenciesConfigurator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDownloadLicensesMojo.class);

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * List of Remote Repositories used by the resolver
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remoteRepositories;

    // CHECKSTYLE_OFF: LineLength
    /**
     * A file containing the license data (most notably license names and license URLs) missing in
     * {@code pom.xml} files of the dependencies.
     * <p>
     * Note that since 1.18, if you set {@link #errorRemedy} to {@code xmlOutput} the format of
     * {@link #licensesErrorsFile} is the same as the one of {@link #licensesConfigFile}. So you can use
     * {@link #licensesErrorsFile} as a base for {@link #licensesConfigFile}.
     * <p>
     * Since 1.18, the format of the file is as follows:
     * <pre>
     * {@code
     * <licenseSummary>
     *   <dependencies>
     *     <dependency>
     *       <groupId>\Qaopalliance\E</groupId><!-- A regular expression -->
     *       <artifactId>\Qaopalliance\E</artifactId><!-- A regular expression -->
     *       <!-- <version>.*</version> A version pattern is optional, .* being the default.
     *       <matchLicenses>
     *         <!-- Match a list of licenses with a single entry having name "Public Domain" -->
     *         <license>
     *           <name>\QPublic Domain\E</name><!-- A regular expression -->
     *         </license>
     *       </matchLicenses>
     *       <licenses approved="true" /><!-- Leave the matched dependency as is. -->
     *                                   <!-- In this particular case we approve that code in the Public -->
     *                                   <!-- Domain does not need any license URL. -->
     *     </dependency>
     *     <dependency>
     *         <groupId>\Qasm\E</groupId>
     *       <artifactId>\Qasm\E</artifactId>
     *       <matchLicenses>
     *         <!-- Match an empty list of licenses -->
     *       </matchLicenses>
     *       <!-- Replace the list of licenses in all matching dependencies with the following licenses -->
     *       <licenses>
     *         <license>
     *           <name>BSD 3-Clause ASM</name>
     *           <url>https://gitlab.ow2.org/asm/asm/raw/ASM_3_1_MVN/LICENSE.txt</url>
     *         </license>
     *       </licenses>
     *     </dependency>
     *     <dependency>
     *       <groupId>\Qca.uhn.hapi\E</groupId>
     *       <artifactId>.*</artifactId>
     *       <matchLicenses>
     *         <!-- Match a list of licenses with the following three entries in order: -->
     *         <license>
     *           <name>\QHAPI is dual licensed (MPL, GPL)\E</name>
     *           <comments>\QHAPI is dual licensed under both the Mozilla Public License and the GNU General Public License.\E\s+\QWhat this means is that you may choose to use HAPI under the terms of either license.\E\s+\QYou are both permitted and encouraged to use HAPI, royalty-free, within your applications,\E\s+\Qwhether they are free/open-source or commercial/closed-source, provided you abide by the\E\s+\Qterms of one of the licenses below.\E\s+\QYou are under no obligations to inform the HAPI project about what you are doing with\E\s+\QHAPI, but we would love to hear about it anyway!\E</comments>
     *         </license>
     *         <license>
     *           <name>\QMozilla Public License 1.1\E</name>
     *           <url>\Qhttp://www.mozilla.org/MPL/MPL-1.1.txt\E</url>
     *           <file>\Qmozilla public license 1.1 - index.0c5913925d40.txt\E</file>
     *         </license>
     *         <license>
     *           <name>\QGNU General Public License\E</name>
     *           <url>\Qhttp://www.gnu.org/licenses/gpl.txt\E</url>
     *           <file>\Qgnu general public license - gpl.txt\E</file>
     *         </license>
     *       </matchLicenses>
     *       <licenses approved="true" /><!-- It is OK that the first entry has no URL -->
     *     </dependency>
     *   </dependencies>
     * </licenseSummary>
     * }
     * </pre>
     * <p>
     * Before 1.18 the format was the same as the one of {@link #licensesOutputFile} and the groupIds and artifactIds
     * were matched as plain text rather than regular expressions. No other elements (incl. versions) were matched at
     * all. Since 1.18 the backwards compatibility is achieved by falling back to plain text matching of groupIds
     * and artifactIds if the given {@code <dependency>} does not contain the {@code <matchLicenses>} element.
     * <p>
     * Relationship to other parameters:
     * <ul>
     * <li>License names and license URLs {@link #licensesConfigFile} is applied before
     * {@link #licenseUrlReplacements}</li>
     * <li>{@link #licenseUrlReplacements} are applied before {@link #licenseUrlFileNames}</li>
     * <li>{@link #licenseUrlFileNames} have higher precedence than {@code <file>} elements in
     * {@link #licensesConfigFile}</li>
     * <li>{@link #licenseUrlFileNames} are ignored when {@link #organizeLicensesByDependencies} is {@code true}</li>
     * </ul>
     *
     * @since 1.0
     */
    @Parameter(property = "licensesConfigFile", defaultValue = "${project.basedir}/src/license/licenses.xml")
    protected File licensesConfigFile;
    // CHECKSTYLE_ON: LineLength

    /**
     * The directory to which the dependency licenses should be written.
     *
     * @since 1.0
     */
    @Parameter(
            property = "licensesOutputDirectory",
            defaultValue = "${project.build.directory}/generated-resources/licenses")
    protected File licensesOutputDirectory;

    /**
     * If {@code true}, the mojo will delete all files from {@link #licensesOutputDirectory} and then download them all
     * anew; otherwise the deletion before the download does not happen.
     * <p>
     * This may be useful if you have removed some dependencies and you want the stale license files to go away.
     * <b>
     * {@code cleanLicensesOutputDirectory = true} is not implied by {@link #forceDownload} because users may have
     * other files there in {@link #licensesOutputDirectory} that were not downloaded by the plugin.
     *
     * @see #removeOrphanLicenseFiles
     * @since 1.18
     */
    @Parameter(property = "license.cleanLicensesOutputDirectory", defaultValue = "false")
    private boolean cleanLicensesOutputDirectory;

    /**
     * If {@code true} the files referenced from {@link AbstractLicensesXmlMojo#licensesOutputFile} before executing
     * the mojo but not referenced from {@link AbstractLicensesXmlMojo#licensesOutputFile} after executing
     * the mojo will be deleted; otherwise neither before:after diffing nor any file deletions will happen.
     * <p>
     * Compared to {@link #cleanLicensesOutputDirectory} that removes all files from {@link #licensesOutputDirectory}
     * before downloading all licenses anew, the {@link #removeOrphanLicenseFiles} removes only files that
     * are certainly not needed anymore, e.g. due to a removal of a dependency. {@link #removeOrphanLicenseFiles} thus
     * allows to avoid downloading the license files of dependencies that were downloaded in the past and are still
     * available in {@link #licensesOutputDirectory}.
     *
     * @see #cleanLicensesOutputDirectory
     * @since 1.19
     */
    @Parameter(property = "license.removeOrphanLicenseFiles", defaultValue = "true")
    private boolean removeOrphanLicenseFiles;

    /**
     * A file containing dependencies whose licenses could not be downloaded for some reason. The format is similar to
     * {@link #licensesOutputFile} but the entries in {@link #licensesErrorsFile} have {@code <downloaderMessage>}
     * elements attached to them. Those should explain what kind of error happened during the processing of the given
     * dependency.
     *
     * @since 1.18
     */
    @Parameter(
            property = "license.licensesErrorsFile",
            defaultValue = "${project.build.directory}/generated-resources/licenses-errors.xml")
    private File licensesErrorsFile;

    /**
     * A file containing dependencies whose licenses could not be downloaded for some reason. The format is similar to
     * {@link #licensesExcelOutputFile} but the entries in {@link #licensesExcelErrorFile} have
     * {@code <downloaderMessage>} elements attached to them. Those should explain what kind of error happened during
     * the processing of the given dependency.
     *
     * @since 2.4.0
     */
    @Parameter(
            property = "license.licensesExcelErrorFile",
            defaultValue = "${project.build.directory}/generated-resources/licenses-errors.xlsx")
    private File licensesExcelErrorFile;

    /**
     * A file containing dependencies whose licenses could not be downloaded for some reason. The format is similar to
     * {@link #licensesCalcOutputFile} but the entries in {@link #licensesCalcErrorFile} have
     * {@code <downloaderMessage>} elements attached to them. Those should explain what kind of error happened during
     * the processing of the given dependency.
     *
     * @since 2.4.0
     */
    @Parameter(
            property = "license.licensesCalcErrorFile",
            defaultValue = "${project.build.directory}/generated-resources/licenses-errors.ods")
    private File licensesCalcErrorFile;

    /**
     * A filter to exclude some scopes.
     *
     * @since 1.0
     */
    @Parameter(property = "license.excludedScopes", defaultValue = "system")
    private String excludedScopes;

    /**
     * A filter to include only some scopes, if let empty then all scopes will be used (no filter).
     *
     * @since 1.0
     */
    @Parameter(property = "license.includedScopes")
    private String includedScopes;

    /**
     * A filter to exclude some types.
     *
     * @since 1.15
     */
    @Parameter(property = "license.excludedTypes")
    private String excludedTypes;

    /**
     * A filter to include only some types, if let empty then all types will be used (no filter).
     *
     * @since 1.15
     */
    @Parameter(property = "license.includedTypes")
    private String includedTypes;

    /**
     * A URL returning a plain text file that contains include/exclude artifact filters in the following format:
     * <pre>
     * {@code
     * # this is a comment
     * include gaPattern org\.my-org:my-artifact
     * include gaPattern org\.other-org:other-artifact
     * exclude gaPattern org\.yet-anther-org:.*
     * include scope compile
     * include scope test
     * exclude scope system
     * include type jar
     * exclude type war
     * }</pre>
     *
     * @since 1.18
     */
    @Parameter(property = "license.artifactFiltersUrl")
    private String artifactFiltersUrl;

    /**
     * Settings offline flag (will not download anything if setted to true).
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${settings.offline}")
    private boolean offline;

    /**
     * Before 1.18, {@link #quiet} having value {@code false} suppressed any license download related warnings in the
     * log. After 1.18 (incl.), the behavior depends on the value of {@link #errorRemedy}:
     * <table>
     *   <tr><th>quiet</th><th>errorRemedy</th><th>effective errorRemedy</th><tr>
     *   <tr><td>true</td><td>warn</td><td>ignore</td><tr>
     *   <tr><td>false</td><td>warn</td><td>warn</td><tr>
     *   <tr><td>true or false</td><td>ignore</td><td>ignore</td><tr>
     *   <tr><td>true or false</td><td>failFast</td><td>failFast</td><tr>
     *   <tr><td>true or false</td><td>xmlOutput</td><td>xmlOutput</td><tr>
     * </table>
     *
     * @since 1.0
     * @deprecated Use {@link #errorRemedy} instead
     */
    @Parameter(defaultValue = "false")
    private boolean quiet;

    /**
     * What to do on any license download related error. The possible values are:
     * <li>
     *   <ul>{@link ErrorRemedy#ignore}: all errors are ignored</ul>
     *   <ul>{@link ErrorRemedy#warn}: all errors are output to the log as warnings</ul>
     *   <ul>{@link ErrorRemedy#failFast}: a {@link MojoFailureException} is thrown on the first download related
     *      error</ul>
     *   <ul>{@link ErrorRemedy#xmlOutput}: error messages are added as {@code <downloaderMessages>} to
     *   {@link AbstractDownloadLicensesMojo#licensesErrorsFile}; in case there are error messages, the build will
     * fail after processing all dependencies</ul>
     * </li>
     * @since 1.18
     */
    @Parameter(property = "license.errorRemedy", defaultValue = "warn")
    protected ErrorRemedy errorRemedy;

    /**
     * If {@code true}, all encountered dependency license URLs are downloaded, no matter what is there in
     * {@link #licensesConfigFile} and {@link #licensesOutputFile}; otherwise {@link #licensesConfigFile},
     * {@link #licensesOutputFile} (eventually persisted from a previous build) and the content of
     * {@link #licensesOutputDirectory} are considered sources of valid information - i.e. only URLs that do not appear
     * to have been downloaded in the past will be downloaded.
     * <b>
     * If your {@link #licensesOutputDirectory} contains only license files downloaded by this plugin, you may consider
     * combining {@link #forceDownload} with setting {@link #cleanLicensesOutputDirectory} {@code true}
     *
     * @since 1.18
     */
    @Parameter(property = "license.forceDownload", defaultValue = "false")
    private boolean forceDownload;

    /**
     * Include transitive dependencies when downloading license files.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "true")
    private boolean includeTransitiveDependencies;

    /**
     * Exclude transitive dependencies from excluded artifacts.
     *
     * @since 1.13
     */
    @Parameter(property = "license.excludeTransitiveDependencies", defaultValue = "false")
    private boolean excludeTransitiveDependencies;

    /**
     * If {@code true} both optional and non-optional dependencies will be included in the list of artifacts for
     * creating the license report; otherwise only non-optional dependencies will be considered.
     *
     * @since 1.19
     */
    @Parameter(property = "license.includeOptional", defaultValue = "true")
    boolean includeOptional;

    /**
     * Get declared proxies from the {@code settings.xml} file.
     *
     * @since 1.4
     */
    @Parameter(defaultValue = "${settings.proxies}", readonly = true)
    private List<Proxy> proxies;

    /**
     * A flag to organize the licenses by dependencies. When this is done, each dependency will
     * get its full license file, even if already downloaded for another dependency.
     *
     * @since 1.9
     */
    @Parameter(property = "license.organizeLicensesByDependencies", defaultValue = "false")
    protected boolean organizeLicensesByDependencies;

    @Parameter(property = "license.sortByGroupIdAndArtifactId", defaultValue = "false")
    private boolean sortByGroupIdAndArtifactId;

    /**
     * A filter to exclude some GroupIds
     * This is a regular expression that is applied to groupIds (not an ant pattern).
     *
     * @since 1.11
     */
    @Parameter(property = "license.excludedGroups")
    private String excludedGroups;

    /**
     * A filter to include only some GroupIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter(property = "license.includedGroups")
    private String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter(property = "license.excludedArtifacts")
    private String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter(property = "license.includedArtifacts")
    private String includedArtifacts;

    /**
     * The Maven Project Object
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * List of regexps/replacements applied to the license urls prior to download.
     *
     * <p>License urls that match a regular expression will be replaced by the corresponding
     * replacement. Replacement is performed with {@link java.util.regex.Matcher#replaceAll(String)
     * java.util.regex.Matcher#replaceAll(String)} so you can take advantage of
     * capturing groups to facilitate flexible transformations.</p>
     *
     * <p>If the replacement element is omitted, this is equivalent to an empty replacement string.</p>
     *
     * <p>The replacements are applied in the same order as they are present in the configuration. The default
     * replacements (that can be activated via {@link #useDefaultUrlReplacements}) are appended to
     * {@link #licenseUrlReplacements}</p>
     *
     * <p>The {@code id} field of {@link LicenseUrlReplacement} is optional and is useful only if you want to override
     * some of the default replacements.</p>
     *
     * <pre>
     * {@code
     *
     * <licenseUrlReplacements>
     *   <licenseUrlReplacement>
     *     <regexp>\Qhttps://glassfish.java.net/public/CDDL+GPL_1_1.html\E</regexp>
     *     <replacement>https://oss.oracle.com/licenses/CDDL+GPL-1.1</replacement>
     *   </licenseUrlReplacement>
     *   <licenseUrlReplacement>
     *     <regexp>https://(.*)</regexp><!-- replace https with http -->
     *     <replacement>http://$1</replacement>
     *   </licenseUrlReplacement>
     *   <licenseUrlReplacement>
     *     <id>github.com-0</id><!-- An optional id to override the default replacement with the same id -->
     *     <regexp>^https?://github\.com/([^/]+)/([^/]+)/blob/(.*)$</regexp><!-- replace GitHub web UI with raw -->
     *     <replacement>https://raw.githubusercontent.com/$1/$2/$3</replacement>
     *   </licenseUrlReplacement>
     * </licenseUrlReplacements>
     * }
     * </pre>
     * <p>
     * Relationship to other parameters:
     * <ul>
     * <li>Default URL replacements can be unlocked by setting {@link #useDefaultUrlReplacements} to {@code true}.</li>
     * <li>License names and license URLs {@link #licensesConfigFile} is applied before
     * {@link #licenseUrlReplacements}</li>
     * <li>{@link #licenseUrlReplacements} are applied before {@link #licenseUrlFileNames}</li>
     * <li>{@link #licenseUrlFileNames} have higher precedence than {@code <file>} elements in
     * {@link #licensesConfigFile}</li>
     * <li>{@link #licenseUrlFileNames} are ignored when {@link #organizeLicensesByDependencies} is {@code true}</li>
     * </ul>
     *
     * @since 1.17
     */
    @Parameter
    protected List<LicenseUrlReplacement> licenseUrlReplacements;

    /**
     * If {@code true} the default license URL replacements be added to the internal {@link Map} of URL replacements
     * before adding {@link #licenseUrlReplacements} by their {@code id}; otherwise the default license URL replacements
     * will not be added to the internal {@link Map} of URL replacements.
     * <p>
     * Any individual URL replacement from the set of default URL replacements can be overriden via
     * {@link #licenseUrlReplacements} if the same {@code id} is used in {@link #licenseUrlReplacements}.
     * <p>
     * To view the list of default URL replacements, set {@link #useDefaultUrlReplacements} to {@code true} and run the
     * mojo with debug log level, e.g. using {@code -X} or
     * {-Dorg.slf4j.simpleLogger.log.org.codehaus.mojo.license=debug} on the command line.
     *
     * @since 1.20
     * @see #licenseUrlReplacements
     */
    @Parameter(property = "license.useDefaultUrlReplacements", defaultValue = "false")
    protected boolean useDefaultUrlReplacements;

    /**
     * A map that helps to select local files names for the content downloaded from license URLs.
     * <p>
     * Keys in the map are the local file names. These files will be created under {@link #licensesOutputDirectory}.
     * <p>
     * Values are white space ({@code " \t\n\r"}) separated lists of regular expressions that will be used to match
     * license URLs. The regular expressions are compiled using {@link Pattern#CASE_INSENSITIVE}. Note that various
     * characters that commonly occur in URLs have special meanings in regular extensions. Therefore, consider using
     * regex quoting as described in {@link Pattern} - e.g. {@code http://example\.com} or
     * {@code \Qhttp://example.com\E}
     * <p>
     * In addition to URL patterns, the list can optionally contain a sha1 checksum of the expected content. This is to
     * ensure that the content delivered by a URL does not change without notice. Note that strict checking
     * of the checksum happens only when {@link #forceDownload} is {@code true}. Otherwise the mojo assumes that the URL
     * -&gt; local name mapping is correct and downloads from the URL only if the local file does not exist.
     * <p>
     * A special value-less entry {@code <spdx/>} can be used to activate built-in license names that are based on
     * license IDs from <a href="https://spdx.org/licenses/">https://spdx.org/licenses</a>. The built-in SPDX mappings
     * can be overridden by the subsequent entries. To see which SPDX mappings are built-in, add the {@code <spdx/>}
     * entry and run the mojo with debug log level, e.g. using {@code -X} or
     * {-Dorg.slf4j.simpleLogger.log.org.codehaus.mojo.license=debug} on the command line.
     * <p>
     * An example:
     * <pre>
     * {@code
     * <licenseUrlFileNames>
     *   <spdx/><!-- A special element to activate built-in file name entries based on spdx.org license IDs -->
     *   <bsd-antlr.html>
     *       sha1:81ffbd1712afe8cdf138b570c0fc9934742c33c1
     *       https?://(www\.)?antlr\.org/license\.html
     *   </bsd-antlr.html>
     *   <cddl-gplv2-ce.txt>
     *       sha1:534a3fc9ae1076409bb00d01127dbba1e2620e92
     *       \Qhttps://raw.githubusercontent.com/javaee/activation/master/LICENSE.txt\E
     *   </cddl-gplv2-ce.txt>
     * </licenseUrlFileNames>
     * }
     * </pre>
     * <p>
     * Relationship to other parameters:
     * <ul>
     * <li>License names and license URLs {@link #licensesConfigFile} is applied before
     * {@link #licenseUrlReplacements}</li>
     * <li>{@link #licenseUrlReplacements} are applied before {@link #licenseUrlFileNames}</li>
     * <li>{@link #licenseUrlFileNames} have higher precedence than {@code <file>} elements in
     * {@link #licensesConfigFile}</li>
     * <li>{@link #licenseUrlFileNames} are ignored when {@link #organizeLicensesByDependencies} is {@code true}</li>
     * </ul>
     *
     * @since 1.18
     */
    @Parameter
    protected Map<String, String> licenseUrlFileNames;

    /**
     * A list of regexp:replacement pairs that should be applied to file names for storing licenses.
     * <p>
     * Note that these patterns are not applied to file names defined in {@link #licenseUrlFileNames}.
     *
     * @since 1.18
     */
    @Parameter
    protected List<LicenseUrlReplacement> licenseUrlFileNameSanitizers;

    /**
     * If {@code true}, {@link #licensesOutputFile} and {@link #licensesErrorsFile} will contain {@code <version>}
     * elements for each {@code <dependency>}; otherwise the {@code <version>} {@link #licensesOutputFile} and
     * {@link #licensesErrorsFile} elements will not be appended under {@code <dependency>} elements in
     * <b>
     * Might be useful if you want to keep the {@link #licensesOutputFile} under source control and you do not want to
     * see the changing dependency versions there.
     *
     * @since 1.18
     */
    @Parameter(property = "license.writeVersions", defaultValue = "true")
    private boolean writeVersions;

    /**
     * Connect timeout in milliseconds passed to the HTTP client when downloading licenses from remote URLs.
     *
     * @since 1.18
     */
    @Parameter(property = "license.connectTimeout", defaultValue = "5000")
    private int connectTimeout;

    /**
     * Socket timeout in milliseconds passed to the HTTP client when downloading licenses from remote URLs.
     *
     * @since 1.18
     */
    @Parameter(property = "license.socketTimeout", defaultValue = "5000")
    private int socketTimeout;

    /**
     * Connect request timeout in milliseconds passed to the HTTP client when downloading licenses from remote URLs.
     *
     * @since 1.18
     */
    @Parameter(property = "license.connectionRequestTimeout", defaultValue = "5000")
    private int connectionRequestTimeout;

    /**
     * A list of sanitizers to process the content of license files before storing them locally and before computing
     * their sha1 sums. Useful for removing parts of the content that change over time.
     * <p>
     * The content sanitizers are applied in alphabetical order by {@code id}.
     * <p>
     * Set {@link #useDefaultContentSanitizers} to {@code true} to apply the built-in content sanitizers.
     * <p>
     * An example:
     * <pre>
     * {@code
     * <licenseContentSanitizers>
     *   <licenseContentSanitizer>
     *     <id>fedoraproject.org-0</id>
     *     <urlRegexp>.*fedoraproject\\.org.*</urlRegexp><!-- Apply this sanitizer to URLs that contain fedora.org -->
     *     <contentRegexp>\"wgRequestId\":\"[^\"]*\"</contentRegexp><!-- wgRequestId value changes with every -->
     *                                                              <!-- request so we just remove it -->
     *     <contentReplacement>\"wgRequestId\":\"\"</contentReplacement>
     *   </licenseContentSanitizer>
     *   <licenseContentSanitizer>
     *     <id>opensource.org-0</id>
     *     <urlRegexp>.*opensource\\.org.*</urlRegexp><!-- Apply this sanitizer to URLs that contain opensource.org -->
     *     <contentRegexp>jQuery\\.extend\\(Drupal\\.settings[^\\n]+</contentRegexp><!-- Drupal\\.settings contain -->
     *                                                                              <!-- some clutter that changes -->
     *                                                                              <!-- often so we just remove it -->
     *     <contentReplacement></contentReplacement>
     *   </licenseContentSanitizer>
     * </licenseContentSanitizers>
     * }
     * </pre>
     *
     * @since 1.20
     * @see #useDefaultContentSanitizers
     */
    @Parameter(property = "license.licenseContentSanitizers")
    private List<LicenseContentSanitizer> licenseContentSanitizers;

    /**
     * If {@code true} the default content sanitizers will be added to the internal {@link Map} of sanitizes before
     * adding {@link #licenseContentSanitizers} by their {@code id}; otherwise the default content sanitizers will not
     * be added to the internal {@link Map} of sanitizes.
     * <p>
     * Any individual content sanitizer from the set of default sanitizers can be overriden via
     * {@link #licenseContentSanitizers} if the same {@code id} is used in {@link #licenseContentSanitizers}.
     * <p>
     * To view the list of default content sanitizers, set {@link #useDefaultContentSanitizers} to {@code true} and run
     * the mojo with debug log level, e.g. using {@code -X} or
     * {-Dorg.slf4j.simpleLogger.log.org.codehaus.mojo.license=debug} on the command line.
     *
     * @since 1.20
     * @see #licenseContentSanitizers
     */
    @Parameter(property = "license.useDefaultContentSanitizers", defaultValue = "false")
    private boolean useDefaultContentSanitizers;

    /**
     * Write Microsoft Office Excel file (XLSX) for goal license:aggregate-download-licenses.
     *
     * @since 2.4.0
     */
    @Parameter(property = "license.writeExcelFile", defaultValue = "false")
    private boolean writeExcelFile;

    /**
     * Write LibreOffice Calc file (ODS) for goal license:aggregate-download-licenses.
     *
     * @since 2.4.0
     */
    @Parameter(property = "license.writeCalcFile", defaultValue = "false")
    private boolean writeCalcFile;

    /**
     * To merge licenses in the Excel file.
     * <p>
     * Each entry represents a merge (first license is main license to keep), licenses are separated by {@code |}.
     * <p>
     * Example:
     * <p>
     * <pre>
     * &lt;licenseMerges&gt;
     * &lt;licenseMerge&gt;The Apache Software License|Version 2.0,Apache License, Version 2.0&lt;/licenseMerge&gt;
     * &lt;/licenseMerges&gt;
     * &lt;/pre&gt;
     *
     * @since 2.2.1
     */
    @Parameter
    List<String> licenseMerges;

    /**
     * The Excel output file used if {@link #writeExcelFile} is true,
     * containing a mapping between each dependency and its license information.
     * With extended information, if available.
     *
     * @see AbstractDownloadLicensesMojo#writeExcelFile
     * @see AggregateDownloadLicensesMojo#extendedInfo
     * @since 2.4.0
     */
    @Parameter(
            property = "license.licensesExcelOutputFile",
            defaultValue = "${project.build.directory}/generated-resources/licenses.xlsx")
    protected File licensesExcelOutputFile;

    /**
     * The Calc output file used if {@link #writeCalcFile} is true,
     * containing a mapping between each dependency and its license information.
     * With extended information, if available.
     *
     * @see AbstractDownloadLicensesMojo#writeCalcFile
     * @see AggregateDownloadLicensesMojo#extendedInfo
     * @since 2.4.0
     */
    @Parameter(
            property = "license.licensesCalcOutputFile",
            defaultValue = "${project.build.directory}/generated-resources/licenses.ods")
    protected File licensesCalcOutputFile;

    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------
    private PreferredFileNames preferredFileNames;
    /**
     * A map from the license URLs to file names (without path) where the
     * licenses were downloaded. This helps the plugin to avoid downloading
     * the same license multiple times.
     */
    private Cache cache;

    private int downloadErrorCount = 0;

    private ArtifactFilters artifactFilters;
    private final Set<String> orphanFileNames = new HashSet<>();

    private UrlReplacements urlReplacements;

    protected abstract boolean isSkip();

    protected MavenProject getProject() {
        return project;
    }

    protected abstract Map<String, LicensedArtifact> getDependencies();

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (isSkip()) {
            LOG.info("skip flag is on, will skip goal.");
            return;
        }

        this.errorRemedy = getEffectiveErrorRemedy(this.quiet, this.errorRemedy);
        this.preferredFileNames = PreferredFileNames.build(licensesOutputDirectory, licenseUrlFileNames);
        this.cache = new Cache(licenseUrlFileNames != null && !licenseUrlFileNames.isEmpty());
        this.urlReplacements = urlReplacements();

        initDirectories();

        final LicenseMatchers matchers = LicenseMatchers.load(licensesConfigFile);

        if (!forceDownload) {
            try {
                final List<ProjectLicenseInfo> projectLicenseInfos =
                        LicenseSummaryReader.parseLicenseSummary(licensesOutputFile);
                for (ProjectLicenseInfo dep : projectLicenseInfos) {
                    for (ProjectLicense lic : dep.getLicenses()) {
                        final String fileName = lic.getFile();
                        if (fileName != null) {
                            orphanFileNames.add(fileName);
                            final String url = lic.getUrl();
                            if (url != null) {
                                final File file = new File(licensesOutputDirectory, fileName);
                                if (file.exists()) {
                                    final LicenseDownloadResult entry =
                                            LicenseDownloadResult.success(file, FileUtil.sha1(file.toPath()), false);
                                    cache.put(url, entry);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to process license summary file: " + licensesOutputFile, e);
            }
        }

        final Map<String, LicensedArtifact> dependencies = getDependencies();

        // The resulting list of licenses after dependency resolution
        final List<ProjectLicenseInfo> depProjectLicenses = new ArrayList<>();

        try (LicenseDownloader licenseDownloader = new LicenseDownloader(
                findActiveProxy(),
                connectTimeout,
                socketTimeout,
                connectionRequestTimeout,
                contentSanitizers(),
                getCharset())) {
            for (LicensedArtifact artifact : dependencies.values()) {
                LOG.debug("Checking licenses for project {}", artifact);
                final ProjectLicenseInfo depProject = createDependencyProject(artifact);
                matchers.replaceMatches(depProject);

                /* Copy the messages and handle them via handleError() that may eventually add them back */
                final List<String> msgs = new ArrayList<>(depProject.getDownloaderMessages());
                depProject.getDownloaderMessages().clear();
                for (String msg : msgs) {
                    handleError(depProject, msg);
                }

                depProjectLicenses.add(depProject);
            }
            if (!offline) {
                /* First save the matching URLs into the cache */
                for (ProjectLicenseInfo depProject : depProjectLicenses) {
                    downloadLicenses(licenseDownloader, depProject, true);
                }
                LOG.debug("Finished populating cache");
                /*
                 * Then attempt to download the rest of the URLs using the available cache entries to select local
                 * file names based on file content sha1
                 */
                for (ProjectLicenseInfo depProject : depProjectLicenses) {
                    downloadLicenses(licenseDownloader, depProject, false);
                }
            }

            filterCopyrightLines(depProjectLicenses);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            if (sortByGroupIdAndArtifactId) {
                sortByGroupIdAndArtifactId(depProjectLicenses);
            }

            List<ProjectLicenseInfo> depProjectLicensesWithErrors = filterErrors(depProjectLicenses);
            writeLicenseSummaries(
                    depProjectLicenses, licensesOutputFile, licensesExcelOutputFile, licensesCalcOutputFile);
            if (!CollectionUtils.isEmpty(depProjectLicensesWithErrors)) {
                writeLicenseSummaries(
                        depProjectLicensesWithErrors,
                        licensesErrorsFile,
                        licensesExcelErrorFile,
                        licensesCalcErrorFile);
            }

            removeOrphanFiles(depProjectLicenses);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to write license summary file: " + licensesOutputFile, e);
        }

        if (downloadErrorCount > 0) {
            switch (errorRemedy) {
                case ignore:
                case failFast:
                    /* do nothing */
                    break;
                case warn:
                    LOG.warn("There were {} download errors - check the warnings above", downloadErrorCount);
                    break;
                case xmlOutput:
                    throw new MojoFailureException("There were " + downloadErrorCount + " download errors - check "
                            + licensesErrorsFile.getAbsolutePath());
                default:
                    throw new IllegalStateException(
                            "Unexpected value of " + ErrorRemedy.class.getName() + ": " + errorRemedy);
            }
        }
    }

    private void writeLicenseSummaries(
            List<ProjectLicenseInfo> depProjectLicenses, File outputFile, File excelOutputFile, File calcOutputFile)
            throws ParserConfigurationException, TransformerException, IOException {
        writeLicenseSummary(depProjectLicenses, outputFile, writeVersions);
        if (writeExcelFile) {
            ExcelFileWriter.write(depProjectLicenses, excelOutputFile);
        }
        if (writeCalcFile) {
            CalcFileWriter.write(depProjectLicenses, calcOutputFile);
        }
    }

    /**
     * Removes all extracted copyright lines if the license is an unmodified original license.<br/>
     * So no actual copyright lines are contained in the extracted copyright lines.
     *
     * @param depProjectLicenses Projects with extracted copyright lines.
     */
    private void filterCopyrightLines(List<ProjectLicenseInfo> depProjectLicenses) {
        for (ProjectLicenseInfo projectLicenseInfo : depProjectLicenses) {
            if (projectLicenseInfo.getExtendedInfo() == null
                    || CollectionUtils.isEmpty(
                            projectLicenseInfo.getExtendedInfo().getInfoFiles())) {
                continue;
            }
            List<InfoFile> infoFiles = projectLicenseInfo.getExtendedInfo().getInfoFiles();
            for (InfoFile infoFile : infoFiles) {
                if (cache.hasNormalizedContentChecksum(infoFile.getContentChecksum())) {
                    LOG.debug(
                            "Removed extracted copyright lines for {} ({})",
                            projectLicenseInfo.getExtendedInfo().getName(),
                            projectLicenseInfo.toGavString());
                    infoFile.setExtractedCopyrightLines(null);
                }
            }
        }
    }

    private UrlReplacements urlReplacements() {
        UrlReplacements.Builder b = UrlReplacements.builder().useDefaults(useDefaultUrlReplacements);
        if (licenseUrlReplacements != null) {
            for (LicenseUrlReplacement r : licenseUrlReplacements) {
                b.replacement(r.getId(), r.getRegexp(), r.getReplacement());
            }
        }
        return b.build();
    }

    private Map<String, ContentSanitizer> contentSanitizers() {
        Map<String, ContentSanitizer> result = new TreeMap<String, ContentSanitizer>();
        if (useDefaultContentSanitizers) {
            final Map<String, ContentSanitizer> defaultSanitizers =
                    SpdxLicenseList.getLatest().getAttachments().getContentSanitizers();
            result.putAll(defaultSanitizers);
            if (LOG.isDebugEnabled() && !defaultSanitizers.isEmpty()) {
                final StringBuilder sb = new StringBuilder() //
                        .append("Applied ") //
                        .append(defaultSanitizers.size()) //
                        .append(" licenseContentSanitizers:\n<licenseContentSanitizers>\n");
                for (ContentSanitizer sanitizer : defaultSanitizers.values()) {
                    sb.append("  <licenseContentSanitizer>\n") //
                            .append("    <id>") //
                            .append(sanitizer.getId()) //
                            .append("</id>\n") //
                            .append("    <urlRegexp>") //
                            .append(StringEscapeUtils.escapeJava(
                                    sanitizer.getUrlPattern().pattern())) //
                            .append("</urlRegexp>\n") //
                            .append("    <contentRegexp>") //
                            .append(StringEscapeUtils.escapeJava(
                                    sanitizer.getContentPattern().pattern())) //
                            .append("</contentRegexp>\n") //
                            .append("    <contentReplacement>") //
                            .append(StringEscapeUtils.escapeJava(sanitizer.getContentReplacement())) //
                            .append("</contentReplacement>\n") //
                            .append("  </licenseContentSanitizer>\n");
                }
                sb.append("</licenseContentSanitizers>");

                LOG.debug(sb.toString());
            }
        }
        if (licenseContentSanitizers != null) {
            for (LicenseContentSanitizer s : licenseContentSanitizers) {
                result.put(
                        s.getId(),
                        ContentSanitizer.compile(
                                s.getId(), s.getUrlRegexp(), s.getContentRegexp(), s.getContentReplacement()));
            }
        }

        return Collections.unmodifiableMap(result);
    }

    private void removeOrphanFiles(List<ProjectLicenseInfo> deps) {
        if (removeOrphanLicenseFiles) {
            for (ProjectLicenseInfo dep : deps) {
                for (ProjectLicense lic : dep.getLicenses()) {
                    orphanFileNames.remove(lic.getFile());
                }
            }

            for (String fileName : orphanFileNames) {
                final File file = new File(licensesOutputDirectory, fileName);
                if (file.exists()) {
                    LOG.info("Removing orphan license file \"{}\"", file);
                    file.delete();
                }
            }
        }
    }

    /**
     * Removes from the given {@code depProjectLicenses} those elements which have non-empty
     * {@link ProjectLicenseInfo#getDownloaderMessages()} and adds those to the resulting {@link List}.
     *
     * @param depProjectLicenses the list of {@link ProjectLicenseInfo}s to filter
     * @return a new {@link List} of {@link ProjectLicenseInfo}s containing only elements with non-empty
     *         {@link ProjectLicenseInfo#getDownloaderMessages()}
     */
    private List<ProjectLicenseInfo> filterErrors(List<ProjectLicenseInfo> depProjectLicenses) {
        final List<ProjectLicenseInfo> result = new ArrayList<>();
        final Iterator<ProjectLicenseInfo> it = depProjectLicenses.iterator();
        while (it.hasNext()) {
            final ProjectLicenseInfo dep = it.next();
            final List<String> messages = dep.getDownloaderMessages();
            if (CollectionUtils.isNotEmpty(messages)) {
                it.remove();
                result.add(dep);
            }
        }
        return result;
    }

    private static ErrorRemedy getEffectiveErrorRemedy(boolean quiet, ErrorRemedy errorRemedy) {
        switch (errorRemedy) {
            case warn:
                return quiet ? ErrorRemedy.ignore : ErrorRemedy.warn;
            default:
                return errorRemedy;
        }
    }

    private void sortByGroupIdAndArtifactId(List<ProjectLicenseInfo> depProjectLicenses) {
        Comparator<ProjectLicenseInfo> comparator = new Comparator<ProjectLicenseInfo>() {
            public int compare(ProjectLicenseInfo info1, ProjectLicenseInfo info2) {
                // ProjectLicenseInfo::getId() can not be used because . is before : thus a:b.c would be after a.b:c
                return (info1.getGroupId() + "+" + info1.getArtifactId())
                        .compareTo(info2.getGroupId() + "+" + info2.getArtifactId());
            }
        };
        Collections.sort(depProjectLicenses, comparator);
    }

    // ----------------------------------------------------------------------
    // MavenProjectDependenciesConfigurator Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean isIncludeTransitiveDependencies() {
        return includeTransitiveDependencies;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExcludeTransitiveDependencies() {
        return excludeTransitiveDependencies;
    }

    /** {@inheritDoc} */
    public ArtifactFilters getArtifactFilters() {
        if (artifactFilters == null) {
            artifactFilters = ArtifactFilters.of(
                    includedGroups,
                    excludedGroups,
                    includedArtifacts,
                    excludedArtifacts,
                    includedScopes,
                    excludedScopes,
                    includedTypes,
                    excludedTypes,
                    includeOptional,
                    artifactFiltersUrl,
                    getEncoding());
        }
        return artifactFilters;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isVerbose() {
        return getLog().isDebugEnabled();
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    private void initDirectories() throws MojoExecutionException {
        try {
            if (licensesOutputDirectory.exists()) {
                if (cleanLicensesOutputDirectory) {
                    LOG.info("Cleaning licensesOutputDirectory '{}'", licensesOutputDirectory);
                    FileUtils.cleanDirectory(licensesOutputDirectory);
                }
            } else {
                Files.createDirectories(licensesOutputDirectory.toPath());
            }

            Files.createDirectories(licensesOutputFile.getParentFile().toPath());
            Files.createDirectories(licensesErrorsFile.getParentFile().toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create a directory...", e);
        }
    }

    /** {@inheritDoc} */
    protected Path[] getAutodetectEolFiles() {
        return new Path[] {
            licensesConfigFile.toPath(), project.getBasedir().toPath().resolve("pom.xml")
        };
    }

    private Proxy findActiveProxy() throws MojoExecutionException {
        for (Proxy proxy : proxies) {
            if (proxy.isActive() && "http".equals(proxy.getProtocol())) {
                return proxy;
            }
        }
        return null;
    }

    /**
     * Create a simple DependencyProject object containing the GAV and license info from the Maven Artifact
     *
     * @param depMavenProject the dependency maven project
     * @return DependencyProject with artifact and license info
     * @throws MojoFailureException
     */
    private ProjectLicenseInfo createDependencyProject(LicensedArtifact depMavenProject) throws MojoFailureException {
        final ProjectLicenseInfo dependencyProject = new ProjectLicenseInfo(
                depMavenProject.getGroupId(),
                depMavenProject.getArtifactId(),
                depMavenProject.getVersion(),
                depMavenProject.getExtendedInfos());
        final List<org.codehaus.mojo.license.download.License> licenses = depMavenProject.getLicenses();
        for (org.codehaus.mojo.license.download.License license : licenses) {
            dependencyProject.addLicense(new ProjectLicense(
                    license.getName(), license.getUrl(), license.getDistribution(), license.getComments(), null));
        }
        List<String> msgs = depMavenProject.getErrorMessages();
        for (String msg : msgs) {
            dependencyProject.addDownloaderMessage(msg);
        }

        return dependencyProject;
    }

    /**
     * Determine filename to use for downloaded license file. The file name is based on the configured name of the
     * license (if available) and the remote filename of the license.
     *
     * @param depProject the project containing the license
     * @param url the license url
     * @param licenseName the license name
     * @param licenseFileName the file name where to save the license
     * @return A filename to be used for the downloaded license file
     * @throws URISyntaxException
     */
    private FileNameEntry getLicenseFileName(
            ProjectLicenseInfo depProject, final String url, final String licenseName, String licenseFileName)
            throws URISyntaxException {

        final URI licenseUrl = new URI(url);
        File licenseUrlFile = new File(licenseUrl.getPath());

        if (organizeLicensesByDependencies) {
            if (licenseFileName != null && !licenseFileName.isEmpty()) {
                return new FileNameEntry(
                        new File(licensesOutputDirectory, new File(licenseFileName).getName()), false, null);
            }
            licenseFileName = String.format(
                            "%s.%s%s.txt",
                            depProject.getGroupId(),
                            depProject.getArtifactId(),
                            licenseName != null ? "_" + licenseName : "")
                    .replaceAll("\\s+", "_");
        } else {
            final FileNameEntry preferredFileNameEntry = preferredFileNames.getEntryByUrl(url);
            if (preferredFileNameEntry != null) {
                return preferredFileNameEntry;
            }

            if (licenseFileName != null && !licenseFileName.isEmpty()) {
                return new FileNameEntry(
                        new File(licensesOutputDirectory, new File(licenseFileName).getName()), false, null);
            }

            licenseFileName = licenseUrlFile.getName();

            if (licenseName != null) {
                licenseFileName = licenseName + " - " + licenseUrlFile.getName();
            }

            // Normalize whitespace
            licenseFileName = licenseFileName.replaceAll("\\s+", " ");
        }

        // lower case and (back)slash removal
        licenseFileName = licenseFileName.toLowerCase(Locale.US).replaceAll("[\\\\/]+", "_");

        licenseFileName = sanitize(licenseFileName);

        return new FileNameEntry(new File(licensesOutputDirectory, licenseFileName), false, null);
    }

    private String sanitize(String licenseFileName) {
        if (licenseUrlFileNameSanitizers != null) {
            for (LicenseUrlReplacement sanitizer : licenseUrlFileNameSanitizers) {
                Pattern regexp = sanitizer.getPattern();
                String replacement = sanitizer.getReplacement() == null ? "" : sanitizer.getReplacement();
                if (regexp != null) {
                    licenseFileName = regexp.matcher(licenseFileName).replaceAll(replacement);
                }
            }
        }
        return licenseFileName;
    }

    /**
     * Download the licenses associated with this project
     *
     * @param depProject The project which generated the dependency
     * @param matchingUrlsOnly
     * @throws MojoFailureException
     */
    private void downloadLicenses(
            LicenseDownloader licenseDownloader, ProjectLicenseInfo depProject, boolean matchingUrlsOnly)
            throws MojoFailureException {
        LOG.debug("Downloading license(s) for project {}", depProject);

        List<ProjectLicense> licenses = depProject.getLicenses();

        if (matchingUrlsOnly
                && (depProject.getLicenses() == null || depProject.getLicenses().isEmpty())) {
            handleError(depProject, "No license information available for: " + depProject.toGavString());
            return;
        }

        int licenseIndex = 0;
        for (ProjectLicense license : licenses) {
            if (matchingUrlsOnly && StringUtils.isBlank(license.getUrl())) {
                handleError(
                        depProject,
                        "No URL for license at index " + licenseIndex + " in dependency " + depProject.toGavString());
            } else if (StringUtils.isNotBlank(license.getUrl())) {
                final String licenseUrl = urlReplacements.rewriteIfNecessary(license.getUrl());

                final LicenseDownloadResult cachedResult = cache.get(licenseUrl);
                try {

                    if (cachedResult != null) {
                        if (cachedResult.isPreferredFileName() == matchingUrlsOnly) {
                            if (organizeLicensesByDependencies) {
                                final FileNameEntry fileNameEntry = getLicenseFileName(
                                        depProject, licenseUrl, license.getName(), license.getFile());
                                final File cachedFile = cachedResult.getFile();
                                final LicenseDownloadResult byDepsResult;
                                final File byDepsFile = fileNameEntry.getFile();
                                if (cachedResult.isSuccess() && !cachedFile.equals(byDepsFile)) {
                                    if (!byDepsFile.exists()) {
                                        Files.copy(cachedFile.toPath(), byDepsFile.toPath());
                                    }
                                    byDepsResult = cachedResult.withFile(byDepsFile);
                                } else {
                                    byDepsResult = cachedResult;
                                }
                                handleResult(licenseUrl, byDepsResult, depProject, license);
                            } else {
                                handleResult(licenseUrl, cachedResult, depProject, license);
                            }
                        }
                    } else {
                        /* No cache entry for the current URL */
                        final FileNameEntry fileNameEntry =
                                getLicenseFileName(depProject, licenseUrl, license.getName(), license.getFile());

                        final File licenseOutputFile = fileNameEntry.getFile();
                        if (matchingUrlsOnly == fileNameEntry.isPreferred()) {
                            if (!licenseOutputFile.exists() || forceDownload) {
                                LicenseDownloadResult result =
                                        licenseDownloader.downloadLicense(licenseUrl, fileNameEntry);
                                if (!organizeLicensesByDependencies && result.isSuccess()) {
                                    /* check if we can re-use an existing file that has the same content */
                                    final String name = preferredFileNames.getFileNameBySha1(result.getSha1());
                                    if (name != null) {
                                        final File oldFile = result.getFile();
                                        if (!oldFile.getName().equals(name)) {
                                            LOG.debug(
                                                    "Found preferred name '{}' by SHA1 after downloading '{}'; "
                                                            + "renaming from '{}'",
                                                    name,
                                                    licenseUrl,
                                                    oldFile.getName());
                                            final File newFile = new File(licensesOutputDirectory, name);
                                            if (newFile.exists()) {
                                                oldFile.delete();
                                            } else {
                                                oldFile.renameTo(newFile);
                                            }
                                            result = result.withFile(newFile);
                                        }
                                    }
                                }
                                handleResult(licenseUrl, result, depProject, license);
                                cache.put(licenseUrl, result);
                            } else if (licenseOutputFile.exists()) {
                                final LicenseDownloadResult result = LicenseDownloadResult.success(
                                        licenseOutputFile,
                                        FileUtil.sha1(licenseOutputFile.toPath()),
                                        fileNameEntry.isPreferred());
                                handleResult(licenseUrl, result, depProject, license);
                                cache.put(licenseUrl, result);
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    String msg = "POM for dependency " + depProject.toGavString() + " has an invalid license URL: "
                            + licenseUrl;
                    handleError(depProject, msg);
                    LOG.debug(msg, e);
                } catch (FileNotFoundException e) {
                    String msg = "POM for dependency " + depProject.toGavString()
                            + " has a license URL that returns file not found: " + licenseUrl;
                    handleError(depProject, msg);
                    LOG.debug(msg, e);
                } catch (IOException e) {
                    String msg = "Unable to retrieve license from URL '" + licenseUrl + "' for dependency '"
                            + depProject.toGavString() + "': " + e.getMessage();
                    handleError(depProject, msg);
                    LOG.debug(msg, e);
                }
            }
            licenseIndex++;
        }
    }

    private void handleResult(
            String licenseUrl, LicenseDownloadResult result, ProjectLicenseInfo depProject, ProjectLicense license)
            throws MojoFailureException {
        if (result.isSuccess()) {
            license.setFile(result.getFile().getName());
        } else {
            handleError(depProject, result.getErrorMessage());
        }
    }

    private void handleError(ProjectLicenseInfo depProject, String msg) throws MojoFailureException {
        if (depProject.isApproved()) {
            LOG.debug("Suppressing manually approved license issue: {}", msg);
        } else {
            switch (errorRemedy) {
                case ignore:
                    /* do nothing */
                    break;
                case warn:
                    LOG.warn(msg);
                    break;
                case failFast:
                    throw new MojoFailureException(msg);
                case xmlOutput:
                    LOG.error(msg);
                    depProject.addDownloaderMessage(msg);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected value of " + ErrorRemedy.class.getName() + ": " + errorRemedy);
            }
            downloadErrorCount++;
        }
    }

    /**
     * What to do in case of a license download error.
     *
     * @since 1.18
     */
    public enum ErrorRemedy {
        /** All errors are ignored */
        ignore,
        /** All errors are output to the log as warnings */
        warn,
        /**
         * The first encountered error is logged and a {@link MojoFailureException} is thrown
         */
        failFast,
        /**
         * Error messages are added as {@code <downloaderMessages>} to
         * {@link AbstractDownloadLicensesMojo#licensesErrorsFile}; in case there are error messages, the build will
         * fail after processing all dependencies.
         */
        xmlOutput
    }
}
