package org.codehaus.mojo.license.nexus;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.License;

import java.io.IOException;
import java.util.*;

/**
 * Created
 * on 31.01.2018.
 */
public class LicenseProcessor {

    private static final List<String> IGNORED_LICENSES = Collections.unmodifiableList(Arrays.asList("Not-Declared", "Not Declared", "UNSPECIFIED", "No-Sources", "No Sources"));

    private Log log;
    private String proxyUrl;

//    private final String proxyHost = "proxy";
//    private final int proxyPort = 800;

    public LicenseProcessor(Log log, String proxyUrl) {
        this.log = log;
        this.proxyUrl = proxyUrl;
    }

    public Log getLog() {
        return log;
    }

    public List<License> getLicencesByProject(MavenProject depMavenProject) {
        try {
            String url = getUrl(depMavenProject);
            getLog().info("Executing " + url);
            Request request = request(url);
            if (this.proxyUrl != null) {
                request = request.viaProxy(proxyUrl);
            }
            Response response = request.execute();
            HttpResponse httpResponse = response.returnResponse();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                String responseStr = EntityUtils.toString(httpResponse.getEntity());
                System.out.println(responseStr);
                List<License> mavenLicenses = getLicensesFromJSON(responseStr);
                if (mavenLicenses.isEmpty()) {
                    getLog().info("No licenses found in Nexus for " + toString(depMavenProject) + " in Nexus" );
                } else {
                    getLog().info("Nexus licenses found for " + toString(depMavenProject) + ": " + toString(mavenLicenses));
                }

                return mavenLicenses;
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                getLog().info("license for " + toString(depMavenProject) + ":" + depMavenProject.getVersion() + " is not found in Nexus");
            } else {
                getLog().info("Unknown status code for " + toString(depMavenProject) +  " : " + statusCode);
            }
        } catch (IOException e) {
            getLog().error(e.getMessage());
            e.printStackTrace();
        }
        return Collections.<License>emptyList();
    }

    private String toString(List<License> mavenLicenses) {
        List<String> licenseNameList = new ArrayList<>();
        for (License license:mavenLicenses) {
            licenseNameList.add(license.getName());
        }
        return StringUtils.join(licenseNameList, ",");
    }

    private String toString(MavenProject depMavenProject) {
        return depMavenProject.getGroupId() + ":" + depMavenProject.getArtifact();
    }

    private Request request(String url) {
        return Request.Get(url)
                .addHeader("Connection",  "Keep-Alive")
                .addHeader("User-Agent", "Nexus/2.14.5-02 (PRO; Linux; 3.10.0-327.36.1.el7.x86_64; amd64; 1.8.0_102)")
                .addHeader("Accept-Encoding", "gzip,deflate");
    }

    List<License> getLicensesFromJSON(String responseStr) throws IOException {
        ComponentInfo componentInfo = parseJSON(responseStr);
        List<String> declaredLicenseIds = parseLicenseList(componentInfo.getDeclaredLicenses());
        List<String> observedLicenseIds = parseLicenseList(componentInfo.getObservedLicenses());
        Set<String> uniqueLicences = new HashSet<>();
        uniqueLicences.addAll(declaredLicenseIds);
        uniqueLicences.addAll(observedLicenseIds);
        ArrayList<License> mavenLicenses = new ArrayList<>();
        for (String licenseId : uniqueLicences) {
            License e = new License();
            e.setName(licenseId);
            mavenLicenses.add(e);
        }
        return mavenLicenses;
    }

    List<String> parseLicenseList(List<ComponentInfo.License> declaredLicenses) {
        List<String> allLicences = new ArrayList<>();
        for (ComponentInfo.License declaredLicense : declaredLicenses) {
            String licenseId = declaredLicense.getLicenseId();
            List<String> licenses = parseLicense(licenseId);
            allLicences.addAll(licenses);
        }
        return allLicences;
    }

    public List<String> parseLicense(String licenseStr) {
        String[] items = licenseStr.split(" or ");
        List<String> list = new ArrayList<>();
        for (String item : items) {
            if (StringUtils.isNotBlank(item) && !IGNORED_LICENSES.contains(item.trim())) {
                list.add(item);
            }
        }
        return list;
    }

    private String getUrl(MavenProject depMavenProject) {
        String groupId = depMavenProject.getGroupId();
        String version = depMavenProject.getVersion();
        String packaging = depMavenProject.getPackaging();
        String artifactId = depMavenProject.getArtifactId();
        return "https://rhc-pro.sonatype.com/rest/rhc/extras/componentDetails/b82cd057df4f463e9a0ffe5b782a2163/5af35056b4d257e4b64b9e8069c0746e8b08629f?componentIdentifier=%7B%22format%22:%22maven%22,%22coordinates%22:%7B%22artifactId%22:%22" + artifactId + "%22,%22extension%22:%22" + packaging + "%22,%22groupId%22:%22" + groupId + "%22,%22version%22:%22" + version + "%22%7D%7D&licenseId=803a54e2d20bb68c7aca5955ff1ccf24732351ff";
    }

    ComponentInfo parseJSON(String data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(data, ComponentInfo.class);
    }



    /*
            org.apache.HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
            client.getHttpConnectionManager().
                    getParams().setConnectionTimeout(30000);
            String url = getUrl(depMavenProject);
            GetMethod get = new GetMethod(url);
            try {
                int result = client.executeMethod(get);
                String response = get.getResponseBodyAsString();
                System.out.println(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
    */
}
