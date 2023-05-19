package org.codehaus.mojo.license;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.codehaus.mojo.license.api.*;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.codehaus.mojo.license.utils.*;
import org.codehaus.plexus.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created on 23/05/16.
 *
 * @author Tony Chemit - chemit@codelutin.com
 */
public abstract class AbstractDownloadLicensesMojo
        extends AbstractMojo
        implements MavenProjectDependenciesConfigurator {
    public static final String LICENSE_MAP_KEY = "licenseMap";

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * @since 1.14.25
     */
    @Parameter(property = "license.skip", defaultValue = "false")
    private boolean skip;

    /**
     * A flag to fail the build if at least one dependency was detected without a license.
     *
     * @since 1.14
     */
    @Parameter( property = "license.failOnMissing", defaultValue = "false" )
    boolean failOnMissing;

    /**
     * A flag to fail the build if at least one dependency was blacklisted.
     *
     * @since 1.14
     */
    @Parameter( property = "license.failOnBlacklist", defaultValue = "false" )
    boolean failOnBlacklist;


    /**
     * Location of the local repository.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    private List remoteRepositories;

    /**
     * Input file containing a mapping between each dependency and it's license information.
     *
     * @since 1.0
     */
    @Parameter(property = "licensesConfigFile", defaultValue = "${project.basedir}/src/license/licenses.xml")
    private File licensesConfigFile;

    /**
     * The directory to which the dependency licenses should be written.
     *
     * @since 1.0
     */
    @Parameter(property = "licensesOutputDirectory",
            defaultValue = "${project.build.directory}/generated-resources/licenses")
    private File licensesOutputDirectory;

    /**
     * The output file containing a mapping between each dependency and it's license information.
     *
     * @since 1.0
     */
    @Parameter(property = "licensesOutputFile",
            defaultValue = "${project.build.directory}/generated-resources/licenses.xml")
    private File licensesOutputFile;

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
    @Parameter(property = "license.includedScopes", defaultValue = "")
    private String includedScopes;

    /**
     * Settings offline flag (will not download anything if setted to true).
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${settings.offline}")
    private boolean offline;

    /**
     * Don't show warnings about bad or missing license files.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "false")
    private boolean quiet;

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
     * Get declared proxies from the {@code settings.xml} file.
     *
     * @since 1.4
     */
    @Parameter(defaultValue = "${settings.proxies}", readonly = true)
    private List<Proxy> proxies;

    @Parameter(property="license.proxy", readonly = true)
    private String proxyUrl;

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
    @Parameter(property = "license.excludedGroups", defaultValue = "")
    private String excludedGroups;

    /**
     * A filter to include only some GroupIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter(property = "license.includedGroups", defaultValue = "")
    private String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter(property = "license.excludedArtifacts", defaultValue = "")
    private String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter(property = "license.includedArtifacts", defaultValue = "")
    private String includedArtifacts;

    /**
     * The Maven Project Object
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter
    List<String> licenseMerges = new ArrayList<>();


    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

    /**
     * Dependencies tool.
     *
     * @since 1.0
     */
    @Component
    private DependenciesTool dependenciesTool;

    /**
     * Third party tool (much of the logic of these mojos is implemented here).
     *
     * @since 1.0
     */
    @Component
    private ThirdPartyTool thirdPartyTool;


    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * Keeps a collection of the URLs of the licenses that have been downlaoded. This helps the plugin to avoid
     * downloading the same license multiple times.
     */
    private Set<String> downloadedLicenseURLs = new HashSet<String>();

    /**
     * Proxy Login/Password encoded(only if usgin a proxy with authentication).
     *
     * @since 1.4
     */
    private String proxyLoginPasswordEncoded;

    /**
     * Encoding used to read and writes files.
     * <p>
     * <b>Note:</b> If nothing is filled here, we will use the system
     * property {@code file.encoding}.
     *
     * @since 1.0
     */
    @Parameter(property = "license.encoding", defaultValue = "${project.build.sourceEncoding}")
    String encoding;

    protected abstract boolean isSkip();

    public final boolean isSkipAll() {
        return skip || isSkip();
    }


    protected MavenProject getProject() {
        return project;
    }

    public String getEncoding() {
        return encoding;
    }

    protected abstract SortedMap<String, MavenProject> getDependencies();

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    protected SortedMap<String, MavenProject> getDependencies(MavenProject project) {
        return dependenciesTool.loadProjectDependencies(project, this, localRepository, remoteRepositories, null);
    }

    /**
     * {@inheritDoc}
     */
    public void execute()
            throws MojoExecutionException, MojoFailureException {

        initDirectories();

        if (isSkipAll()) {
            getLog().info("skip flag is on, will skip goal. skipAll=" + skip);
            return;
        }

        initProxy();

        Map<String, ProjectLicenseInfo> configuredDepLicensesMap = new HashMap<String, ProjectLicenseInfo>();

        // License info from previous build
        if (licensesOutputFile.exists()) {
            loadLicenseInfo(configuredDepLicensesMap, licensesOutputFile, true);
        }

        // Manually configured license info, loaded second to override previously loaded info
        if (licensesConfigFile.exists()) {
            loadLicenseInfo(configuredDepLicensesMap, licensesConfigFile, false);
        }

        SortedMap<String, MavenProject> projectDependenciesMap = getDependencies();
        Set<MavenProject> dependencies = new HashSet<>(projectDependenciesMap.values());

        final LicenseMap licenseMap;
        if (getPluginContext().containsKey(LICENSE_MAP_KEY)) {
            getLog().info("Loading licenseMap from cache");
            licenseMap = (LicenseMap) getPluginContext().get(LICENSE_MAP_KEY);
        } else {
            licenseMap = calculateLicenseMap(projectDependenciesMap, dependencies);
        }

        Properties licenseProperties = new Properties();

        try {
            getLog().info("Loading license urls from licenses.properties");
            licenseProperties.load(new StringReader(LicenseRegistryClient.getInstance().getFileContent("licenses.properties")));
        } catch (IOException e) {
            throw new MojoExecutionException("Can't fetch external third-party dependencies from licenses.properties", e );
        }

        Map<String, String> licenseUrlMap = new TreeMap<>();
        Set<String> unresolvedLicenses = new TreeSet<>();
        for (String licenseName : licenseMap.keySet()) {
            if (licenseMap.get(licenseName).isEmpty()) {
                getLog().warn("No projects for " + licenseName);
            }
            String licenseURL;
            if (licenseProperties.containsKey(licenseName)) {
                licenseURL = licenseProperties.getProperty(licenseName);
                getLog().info("license for " + licenseName + " is found = '" + licenseURL + "'");
            } else {
                licenseURL = getLicenseUrlFromProjects(licenseMap, licenseName, dependencies);
                getLog().info("URL from projects for " + licenseName + " = '" + licenseURL + "'");
            }
            if (licenseURL != null) {
                licenseUrlMap.put(licenseName, licenseURL);
            } else {
                unresolvedLicenses.add(licenseName);
            }
        }

        Map<String, String> failedLicenses = new HashMap<>();
        for (String license : licenseUrlMap.keySet()) {
            String licenseUrl = licenseUrlMap.get(license);
            if (!downloadLicense(license, licenseUrl)) {
                failedLicenses.put(license, licenseUrl);
            }
        }
        if (!unresolvedLicenses.isEmpty()) {
            StringBuilder message = new StringBuilder("\n");
            for (String lic : unresolvedLicenses) {
                message.append(lic).append(" -> ").append(licenseMap.get(lic)).append("\n");
            }
            if (failOnMissing) {
                throw new MojoFailureException("URLs are not defined for the following licenses: " + message);
            } else {
                getLog().warn("URLs are not defined for the following licenses: " + message);
            }
        }
        if (!failedLicenses.isEmpty()) {
            throw new MojoFailureException("Failed to download licenses by the following urls: " + failedLicenses.toString());
        }
    }

    private LicenseMap calculateLicenseMap(SortedMap<String, MavenProject> projectDependenciesMap, Set<MavenProject> dependencies) throws MojoFailureException {
        ThirdPartyHelper thirdPartyHelper =
                new DefaultThirdPartyHelper(project, getEncoding(), isVerbose(), dependenciesTool, thirdPartyTool, localRepository,
                        project.getRemoteArtifactRepositories(), getLog());
        LicenseMap licenseMap = thirdPartyHelper.createLicenseMap(dependencies, proxyUrl);

        overrideLicenses(licenseMap, projectDependenciesMap);

        thirdPartyHelper.mergeLicenses(licenseMerges, licenseMap);
        return licenseMap;
    }

    void overrideLicenses(LicenseMap licenseMap, SortedMap<String, MavenProject> projectDependencies) throws MojoFailureException {
//        thirdPartyTool.overrideLicenses( licenseMap1, projectDependencies, getEncoding(), overrideFile );

        try {
            thirdPartyTool.overrideLicenses( licenseMap, projectDependencies, getEncoding(), "thirdparty-licenses.properties");
        } catch (IOException e) {
            throw new MojoFailureException("Can't fetch external third-party license info from thirdparty-licenses.properties", e );
        }
    }


    private boolean downloadLicense(String license, String licenseUrl) {
        try {
            String licenseFileName = getLicenseFileName(null, license, licenseUrl);

            File licenseOutputFile = new File(licensesOutputDirectory, licenseFileName);
            if (licenseOutputFile.exists()) {
                return true;
            }

            if (!downloadedLicenseURLs.contains(licenseUrl) || organizeLicensesByDependencies) {
                getLog().info("Downloading " + license + " from " + licenseUrl);
                LicenseDownloader licenseDownloader = new LicenseDownloader(proxyUrl);
                licenseDownloader.downloadLicense(licenseUrl, proxyLoginPasswordEncoded, licenseOutputFile);
                downloadedLicenseURLs.add(licenseUrl);
                return true;
            }
        } catch (MalformedURLException e) {
            if (!quiet) {
                getLog().warn(license + " has an invalid license URL: " +
                        licenseUrl);
            }
        } catch (FileNotFoundException e) {
            if (!quiet) {
                getLog().warn(license +
                        " has a license URL that returns file not found: " + licenseUrl);
            }
        } catch (IOException e) {
            getLog().warn("Unable to retrieve license " + license + " by " + licenseUrl);
            getLog().warn(e.getMessage());
        }
        return false;
    }

    private String getLicenseUrlFromProjects(LicenseMap licenseMap, String licenseName, Set<MavenProject> dependencies) {
        List<String> licenseSynonims = findLicenseSynonims(licenseName);
        getLog().debug("synonyms for " + licenseName + " are " + licenseSynonims);
        SortedSet<MavenProject> mavenProjects = licenseMap.get(licenseName);
        for (MavenProject mavenProject : mavenProjects) {
            MavenProject fullMavenProject = findMavenProject(dependencies, mavenProject);
            List<License> licenses = fullMavenProject.getModel().getLicenses();
            for (License licenseFromDep : licenses) {
                if (licenseSynonims.contains(licenseFromDep.getName()) && licenseFromDep.getUrl() != null) {
                    return licenseFromDep.getUrl();
                }
            }
        }
        return null;
    }

    private List<String> findLicenseSynonims(String licenseName) {
        for (String licengeMergeString : licenseMerges) {
            String[] items = licengeMergeString.split("\\s*\\|\\s*");
            for (String item : items) {
                if (licenseName.equals(item)) {
                    return Arrays.asList(items);
                }
            }
        }
        return Collections.singletonList(licenseName);
    }

    private MavenProject findMavenProject(Set<MavenProject> dependencies, MavenProject mavenProject) {
        for (MavenProject p : dependencies) {
            if (p.getGroupId().equals(mavenProject.getGroupId()) &&
                    p.getArtifact().equals(mavenProject.getArtifact()) &&
                    p.getVersion().equals(mavenProject.getVersion()) &&
                    p.getPackaging().equals(mavenProject.getPackaging())
                    ) {
                return p;
            }
        }
        getLog().error("maven project not found for " + mavenProject);
        return mavenProject;
    }

    private List<ProjectLicenseInfo> sortByGroupIdAndArtifactId(List<ProjectLicenseInfo> depProjectLicenses) {
        List sorted = new ArrayList(depProjectLicenses);
        Comparator<? super ProjectLicenseInfo> comparator = new Comparator<ProjectLicenseInfo>() {
            public int compare(ProjectLicenseInfo info1, ProjectLicenseInfo info2) {
                //ProjectLicenseInfo::getId() can not be used because . is before : thus a:b.c would be after a.b:c
                return (info1.getGroupId() + "+" + info1.getArtifactId()).compareTo(info2.getGroupId() + "+" + info2.getArtifactId());
            }
        };
        Collections.sort(sorted, comparator);
        return sorted;
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

    /**
     * {@inheritDoc}
     */
    public List<String> getExcludedScopes() {
        return MojoHelper.getParams(excludedScopes);
    }

    public void setExcludedScopes(String excludedScopes) {
        this.excludedScopes = excludedScopes;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getIncludedScopes() {
        return MojoHelper.getParams(includedScopes);
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getIncludedArtifacts() {
        return includedArtifacts;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getIncludedGroups() {
        return includedGroups;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getExcludedGroups() {
        return excludedGroups;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getExcludedArtifacts() {
        return excludedArtifacts;
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

    private void initDirectories()
            throws MojoExecutionException {
        try {
            FileUtil.createDirectoryIfNecessary(licensesOutputDirectory);

            FileUtil.createDirectoryIfNecessary(licensesOutputFile.getParentFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create a directory...", e);
        }
    }

    private void initProxy()
            throws MojoExecutionException {
        Proxy proxyToUse = null;
        for (Proxy proxy : proxies) {
            if (proxy.isActive() && "http".equals(proxy.getProtocol())) {

                // found our proxy
                proxyToUse = proxy;
                break;
            }
        }
        if (proxyToUse != null) {

            System.getProperties().put("proxySet", "true");
            System.setProperty("proxyHost", proxyToUse.getHost());
            System.setProperty("proxyPort", String.valueOf(proxyToUse.getPort()));
            if (proxyToUse.getNonProxyHosts() != null) {
                System.setProperty("nonProxyHosts", proxyToUse.getNonProxyHosts());
            }
            if (proxyToUse.getUsername() != null) {
                String loginPassword = proxyToUse.getUsername() + ":" + proxyToUse.getPassword();
                proxyLoginPasswordEncoded = new String(Base64.encodeBase64(loginPassword.getBytes()));
            }
        }
    }

    /**
     * Load the license information contained in a file if it exists. Will overwrite existing license information in the
     * map for dependencies with the same id. If the config file does not exist, the method does nothing.
     *
     * @param configuredDepLicensesMap A map between the dependencyId and the license info
     * @param licenseConfigFile        The license configuration file to load
     * @param previouslyDownloaded     Whether these licenses were already downloaded
     * @throws MojoExecutionException if could not load license infos
     */
    private void loadLicenseInfo(Map<String, ProjectLicenseInfo> configuredDepLicensesMap, File licenseConfigFile,
                                 boolean previouslyDownloaded)
            throws MojoExecutionException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(licenseConfigFile);
            List<ProjectLicenseInfo> licensesList = LicenseSummaryReader.parseLicenseSummary(fis);
            for (ProjectLicenseInfo dep : licensesList) {
                configuredDepLicensesMap.put(dep.getId(), dep);
                if (previouslyDownloaded) {
                    for (License license : dep.getLicenses()) {
                        // Save the URL so we don't download it again
                        downloadedLicenseURLs.add(license.getUrl());
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to parse license summary output file: " + licenseConfigFile, e);
        } finally {
            FileUtil.tryClose(fis);
        }
    }

    /**
     * Returns the project ID for the artifact
     *
     * @param artifact the artifact
     * @return groupId:artifactId
     */
    private String getArtifactProjectId(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    /**
     * Create a simple DependencyProject object containing the GAV and license info from the Maven Artifact
     *
     * @param depMavenProject the dependency maven project
     * @return DependencyProject with artifact and license info
     */
    private ProjectLicenseInfo createDependencyProject(MavenProject depMavenProject) {
        ProjectLicenseInfo dependencyProject =
                new ProjectLicenseInfo(depMavenProject.getGroupId(), depMavenProject.getArtifactId(),
                        depMavenProject.getVersion());
        List<?> licenses = depMavenProject.getLicenses();
        for (Object license : licenses) {
            License license1 = (License) license;
            getLog().info("Add license " + license1.getName() + ", " + license1.getUrl() + " to " + depMavenProject);
            dependencyProject.addLicense(license1);
        }
        return dependencyProject;
    }

    /**
     * Determine filename to use for downloaded license file. The file name is based on the configured name of the
     * license (if available) and the remote filename of the license.
     *
     * @param depProject the project containing the license
     * @param license    the license
     * @return A filename to be used for the downloaded license file
     * @throws MalformedURLException if the license url is malformed
     */
    private String getLicenseFileName(ProjectLicenseInfo depProject, License license)
            throws MalformedURLException {
        return getLicenseFileName(depProject, license.getName(), license.getUrl());
    }

    private String getLicenseFileName(ProjectLicenseInfo depProject, String licenseName, String licenseUrlStr) throws MalformedURLException {
        final File licenseUrlFile;
        if (licenseUrlStr.startsWith("http")) {
            URL licenseUrl = new URL(licenseUrlStr.replace("/raw?ref=master", "").replace("%2F", "/"));
            licenseUrlFile = new File(licenseUrl.getPath());
        } else {
            licenseUrlFile = new File(licenseUrlStr);
        }
        String licenseFileName;
        String defaultExtension = ".txt";

        if (organizeLicensesByDependencies) {
            licenseFileName = String.format("%s.%s%s", depProject.getGroupId(), depProject.getArtifactId(),
                    licenseName != null
                            ? "_" + licenseName
                            : "").toLowerCase().replaceAll("\\s+", "_");
        } else {
            licenseFileName = licenseUrlFile.getName();

            if (licenseName != null) {
                licenseFileName = licenseName.replaceAll("/", "_") + " - " + licenseUrlFile.getName();
            }

            // Check if the file has a valid file extention
            int extensionIndex = licenseFileName.lastIndexOf(".");
            if (extensionIndex == -1 || extensionIndex > (licenseFileName.length() - 3)) {
                // This means it isn't a valid file extension, so append the default
                licenseFileName = licenseFileName + defaultExtension;
            }

            // Force lower case so we don't end up with multiple copies of the same license
            licenseFileName = licenseFileName.toLowerCase();
        }
        return licenseFileName;
    }

}
