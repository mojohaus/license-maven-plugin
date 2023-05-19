package org.codehaus.mojo.license.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dependency {
    private String groupId;
    private String artifactId;
    private String version;

    @JsonProperty("description")
    private String description;
    @JsonAlias("File-Name-Off-Version")
    @JsonProperty("fileNameOffVersion")
    private String fileNameOffVersion;
    @JsonProperty("licenseLink")
    private String licenseLink;
    @JsonProperty("licenseType")
    private String licenseType;
    @JsonProperty("modification")
    private String modification;
    @JsonProperty("name")
    private String name;
    @JsonProperty("system")
    private String system;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileNameOffVersion() {
        return fileNameOffVersion;
    }

    public void setFileNameOffVersion(String fileNameOffVersion) {
        this.fileNameOffVersion = fileNameOffVersion;
    }

    public String getLicenseLink() {
        return licenseLink;
    }

    public void setLicenseLink(String licenseLink) {
        this.licenseLink = licenseLink;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getModification() {
        return modification;
    }

    public void setModification(String modification) {
        this.modification = modification;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public void update(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
}
