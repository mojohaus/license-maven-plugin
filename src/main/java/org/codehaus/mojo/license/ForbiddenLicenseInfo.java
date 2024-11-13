package org.codehaus.mojo.license;

import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Container of a project, and a list of all license it has, which are listed as forbidden.
 * <p>
 * TODO: Change this to a record at JDK 16+.
 */
public class ForbiddenLicenseInfo {
    private final ProjectLicenseInfo projectLicenseInfo;
    private final List<ProjectLicense> forbiddenLicenses;

    public ForbiddenLicenseInfo(ProjectLicenseInfo projectLicenseInfo, List<ProjectLicense> forbiddenLicenses) {
        this.projectLicenseInfo = projectLicenseInfo;
        this.forbiddenLicenses = forbiddenLicenses;
        Objects.requireNonNull(projectLicenseInfo, "projectLicenseInfo cannot be null");
        Objects.requireNonNull(forbiddenLicenses, "forbiddenLicenses cannot be null");
    }

    public ProjectLicenseInfo getProjectLicenseInfo() {
        return projectLicenseInfo;
    }

    public List<ProjectLicense> getForbiddenLicenses() {
        return forbiddenLicenses;
    }

    @Override
    public String toString() {
        return MessageFormat.format("Project \"{0}\" has the following forbidden licenses:\n{1}.",
            projectLicenseInfo,
            forbiddenLicenses.stream()
                .map(projectLicense -> MessageFormat.format("\t- {0}", projectLicense.getName()))
                .collect(Collectors.joining("\n"))
        );
    }
}
