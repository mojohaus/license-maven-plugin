package org.codehaus.mojo.license.utils;

import com.platformlib.process.api.ProcessInstance;
import com.platformlib.process.configurator.ProcessOutputConfigurator;
import com.platformlib.process.local.factory.LocalProcessBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LicenseRegistryClient {
    private final Logger LOGGER = LoggerFactory.getLogger(LicenseRegistryClient.class);
    private static final String LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY_NAME = "license-registry.git-repository";
    private final Map<String, String> cachedFiles;

    private static LicenseRegistryClient INSTANCE;

    private LicenseRegistryClient(final String gitRepository) {
        cachedFiles = initialize(gitRepository);
    }

    private Map<String, String> initialize(final String gitRepository) {
        Objects.requireNonNull(gitRepository);
        final Map<String, String> result = new HashMap<>();
        final Path tmpPath;
        try {
            tmpPath = Files.createTempDirectory("licenses-");
        } catch (final IOException ioException) {
            throw new IllegalStateException(ioException);
        }
        try {
            final ProcessInstance processInstance = LocalProcessBuilderFactory
                    .newLocalProcessBuilder()
                    .logger(configuration -> configuration.logger(LOGGER))
                    .processInstance(ProcessOutputConfigurator::unlimited)
                    .command("git")
                    .build()
                    .execute("clone", "--depth=1", gitRepository, tmpPath)
                    .toCompletableFuture()
                    .join();
            if (processInstance.getExitCode() != 0) {
                LOGGER.error("The git clone command stdout: {}", String.join("\n", processInstance.getStdOut()));
                LOGGER.error("The git clone command stderr: {}", String.join("\n", processInstance.getStdErr()));
                throw new IllegalStateException("Unable to clone git repository " + gitRepository);
            }
            LOGGER.info("Cloned {} into {}", gitRepository, tmpPath);
            Files.walk(tmpPath, 1).filter(Files::isRegularFile).forEach(path -> {
                try {
                    result.put(path.getFileName().toString(), new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                } catch (final IOException ioException) {
                    throw new IllegalStateException(ioException);
                }
            });
            Arrays.asList("templates", "licenses").forEach(source -> {
                try {
                    Files.walk(tmpPath.resolve(source), 1).filter(Files::isRegularFile).forEach(path -> {
                        try {
                            result.put(source + "/" + path.getFileName().toString(), new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                        } catch (final IOException ioException) {
                            throw new IllegalStateException(ioException);
                        }
                    });
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final IOException exception) {
            throw new IllegalStateException(exception);
        } finally {
            try {
                Files.walk(tmpPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (final IOException ioException) {
                LOGGER.warn("Unable to delete temporary resource", ioException);
            }
        }
        LOGGER.debug("License cached files are: " + String.join(",", result.keySet()));
        return result;
    }

    public synchronized static LicenseRegistryClient getInstance() {
        if (INSTANCE == null) {
            final String licenseRegistryGitRepository = Optional.ofNullable(
                    System.getProperty(LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY_NAME,
                            System.getenv(LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY_NAME))
            ).orElseThrow(() -> new IllegalArgumentException("Either Environment variable or JVM argument for set 'license-registry.git-repository' must be provided"));
            INSTANCE = new LicenseRegistryClient(licenseRegistryGitRepository);
        }
        return INSTANCE;
    }

    public String getFileContent(final String fileName) {
        if (!cachedFiles.containsKey(fileName)) {
            throw new IllegalArgumentException("The file '" + fileName + "' hasn't been found");
        }
        return cachedFiles.get(fileName);
    }
}
