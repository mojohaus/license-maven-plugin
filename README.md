# License Maven Plugin

[![GitHub CI](https://github.com/mojohaus/license-maven-plugin/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/mojohaus/license-maven-plugin/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/license-maven-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.mojo/license-maven-plugin)
[![The GNU Lesser General Public License, Version 3.0](https://img.shields.io/badge/license-LGPL3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0.txt)

The License Maven Plugin manages the licenses of a Maven project and its dependencies. It adds, updates and checks
license headers in source files, creates the project license file, downloads the license texts of the
dependencies, and builds the third-party license list and report.

It requires Maven 3.9.0 or later and Java 8 or later. To list the licenses that the plugin can put into file
headers, run:

```shell
mvn org.codehaus.mojo:license-maven-plugin:license-list
```

The [plugin site](https://www.mojohaus.org/license-maven-plugin/) lists every goal, with a
[usage guide](https://www.mojohaus.org/license-maven-plugin/usage.html), examples and an
[FAQ](https://www.mojohaus.org/license-maven-plugin/faq.html).

## Contributing

[AGENTS.md](AGENTS.md) describes the build gates, how the unit and integration tests are written
and run, and the test fixtures that must not be updated. It is written for contributors and
coding agents alike.

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site -DperformRelease scm-publish:publish-scm
```

