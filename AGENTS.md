# Agent guide for license-maven-plugin

This file is for automated agents and human contributors working on this repository. It records
what the code and the POMs do not make obvious.

## Layout

The repository is a single Maven module, the plugin itself.

|         Path         |                                       Contents                                       |
|----------------------|--------------------------------------------------------------------------------------|
| `src/main/java`      | The goals (`*Mojo.java`) and the shared code in `api`, `download`, `header`, `model` |
| `src/main/java11`    | Java 11 code packed into `META-INF/versions/11` of a multi-release jar               |
| `src/main/resources` | License texts in `META-INF/licenses`, FreeMarker templates for the third-party files |
| `src/it`             | Integration tests, one directory per test                                            |
| `src/license-test`   | A `license.properties` file the `run-its` profile attaches for the ITs               |
| `src/build/scripts`  | `GenerateSpdxLicenseList.groovy`, which regenerates `SpdxLicenseListData.java`       |
| `src/site`           | The site at <https://www.mojohaus.org/license-maven-plugin/>                         |

- `src/main/java/org/codehaus/mojo/license/spdx/SpdxLicenseListData.java` is generated. Do not edit
  it by hand; run `mvn groovy:execute@GenerateSpdxLicenseList`, which downloads the SPDX license
  list at the version set in the script.

## Build gates

- `mvn verify` runs Spotless, Checkstyle and animal-sniffer. Spotless runs in `apply` mode locally
  and in `check` mode when `CI` is set. It formats Java with palantir-java-format, sorts the POM
  and also reformats Markdown, including the tables in this file.
- Main code must use the Java 8 API only: animal-sniffer checks it against the `java18`
  signature, and CI builds on JDK 8 as well as newer JDKs. Code that needs a Java 11 API goes
  into `src/main/java11`; the `java11-mrjar` profile compiles it on JDK 11 and later, and
  surefire then tests the Java 11 classes.
- The plugin compiles against Maven 3.9.16 (`mavenVersion`) and declares a literal
  `<prerequisites><maven>3.9.0</maven>`; `minimalMavenBuildVersion` holds the build to the same
  floor. Keep the two apart, so raising `mavenVersion` does not raise the prerequisite.
- Java sources start with the `package` line followed by the LGPL `#%L` header, as in the
  existing files.

## Unit tests

- JUnit 5 with Mockito 4. There is no plugin testing harness: tests construct the mojos and
  helpers directly and mock their collaborators.
- Prefer a unit test. Add an integration test only for what needs a real Maven build: dependency
  resolution, reactor and aggregate goals, the site report, or files written into a project.

## Integration tests

- Run them with the `run-its` profile. Each directory under `src/it` is a separate Maven build
  that resolves from `target/local-repo`, configured by `src/it/settings.xml`. Artifacts not
  installed there come from Maven Central.
- Run a subset by directory name:

  ```shell
  mvn -P run-its verify -Dinvoker.test='ISSUE-80,MLICENSE-3*'
  ```
- Scripts are named `prebuild.groovy` and `postbuild.groovy`. Write them as `assert` statements.
  Invoker fails a script whose last expression is anything other than `true` or `null`, so a
  trailing `list.each { }` fails the test; use a `for` loop.
- The scripts can use WireMock, Apache POI and odfdom, which are dependencies of the invoker
  plugin. Download tests start a WireMock server in `prebuild.groovy` and stop it in
  `postbuild.groovy`.
- `download-licenses-basic`, `download-licenses-configured` and `download-licenses-force` are
  excluded in the POM because they download from remote servers.
- Tests that need Java 11 set `invoker.java.version = 11+` in `invoker.properties`.
- They run in parallel (`invoker.parallelThreads`), so a test must not depend on another test's
  output. The forked builds get `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 -XX:+UseSerialGC`, so
  every JVM prints a `Picked up JAVA_TOOL_OPTIONS` line on stderr, and
  `MAVEN_OPTS=-Duser.country=US -Duser.language=en`.

## Fixtures and dependencies

- POMs under `src/it` pin old dependency versions on purpose, including some with known
  vulnerabilities: the postbuild assertions and expected XML files contain those versions
  verbatim. Do not update them. Dependabot excludes `src/it` from version updates, but security
  updates can still propose them; close those pull requests.
- Deliberate pins in `.github/dependabot.yml`: WireMock stays on 2.x and Mockito below 5 because
  the build and the integration tests run on JDK 8, Maven Resolver below 2 because 2.x targets
  Maven 4, and SLF4J below 2 because the Maven 3 runtime does not support SLF4J 2 providers.

## Pull requests

- Pull requests are squash-merged or rebased; merge commits are disabled. Release Drafter builds
  the release notes from the titles and labels, so the title should describe the change for a
  user.

