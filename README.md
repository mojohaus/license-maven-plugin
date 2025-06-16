# license-maven-plugin

This is the [license-maven-plugin](http://www.mojohaus.org/license-maven-plugin/).

[![GitHub CI](https://github.com/mojohaus/license-maven-plugin/actions/workflows/maven.yml/badge.svg)](https://github.com/mojohaus/license-maven-plugin/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/license-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.mojo/license-maven-plugin)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/mojohaus/license-maven-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![The GNU Lesser General Public License, Version 3.0](https://img.shields.io/badge/license-LGPL3-blue.svg)](http://www.gnu.org/licenses/lgpl-3.0.txt)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site -DperformRelease scm-publish:publish-scm
```

