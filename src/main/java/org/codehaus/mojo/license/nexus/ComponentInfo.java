package org.codehaus.mojo.license.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Created on 30.01.2018.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentInfo {

    private List<License> declaredLicenses;
    private List<License> observedLicenses;

    public List<License> getDeclaredLicenses() {
        return declaredLicenses;
    }

    public void setDeclaredLicenses(List<License> declaredLicenses) {
        this.declaredLicenses = declaredLicenses;
    }

    public List<License> getObservedLicenses() {
        return observedLicenses;
    }

    public void setObservedLicenses(List<License> observedLicenses) {
        this.observedLicenses = observedLicenses;
    }

    @Override
    public String toString() {
        return "ComponentInfo{" +
                "declaredLicenses=" + declaredLicenses +
                ", observedLicenses=" + observedLicenses +
                '}';
    }

    public static class License {
        private String licenseId;
        private String licenseName;

        public String getLicenseId() {
            return licenseId;
        }

        public void setLicenseId(String licenseId) {
            this.licenseId = licenseId;
        }

        public String getLicenseName() {
            return licenseName;
        }

        public void setLicenseName(String licenseName) {
            this.licenseName = licenseName;
        }

        @Override
        public String toString() {
            return "License{" +
                    "licenseId='" + licenseId + '\'' +
                    ", licenseName='" + licenseName + '\'' +
                    '}';
        }
    }
}

