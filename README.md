# license-maven-plugin

This is the [license-maven-plugin](http://www.mojohaus.org/license-maven-plugin/). It has been forked and extended with the option
to extract more license information from the dependencies. It can also write a report to an Excel file.

Information about the source project (the license stays of course the same):

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

## How to configure the extended Excel report

The projects `pom.xml` must have the lines marked with "`<!-- New -->`" to use the new options (`extendedInfo`
and `writeExcelFile`):

```xml
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>license-maven-plugin</artifactId>
                    <version>2.3.1-SNAPSHOT</version>
                    <configuration>
                        <!-- New -->
                        <extendedInfo>true</extendedInfo>
                        <!-- New -->
                        <writeExcelFile>true</writeExcelFile>
                        <!-- New -->
                        <writeCalcFile>true</writeCalcFile>

                        <!-- Not needed, but a suggestion -->
                        <!-- Makes it more readable, licenses are the same with different names -->
                        <licenseMerges>
                            <licenseMerge>Apache License, Version 2.0|Apache 2.0|Apache 2|Apache License 2.0|The Apache
                                Software License, Version 2.0|Apache
                                License, version 2.0|AL 2.0|ASF 2.0
                            </licenseMerge>
                            <licenseMerge>MIT License|The MIT License|The MIT License (MIT)</licenseMerge>
                            <licenseMerge>GNU Lesser General Public License|GNU Lesser General Public Licence|GNU LESSER
                                GENERAL PUBLIC LICENSE|GNU Lesser
                                Public License|Lesser General Public License (LGPL)
                            </licenseMerge>
                            <licenseMerge>GNU General Lesser Public License (LGPL) version 3.0|GNU LESSER GENERAL PUBLIC
                                LICENSE, version 3 (LGPL-3.0)
                            </licenseMerge>
                            <!-- Problematic: While the maven plugins may have a simple declaration of "BSD" for their license, they may mean "New BSD" license, at least that's what they state on their websites. -->
                            <licenseMerge>The BSD License|BSD License|BSD</licenseMerge>
                            <licenseMerge>The BSD 3-Clause License|The New BSD License|New BSD License</licenseMerge>
                        </licenseMerges>
```
Since this hasn't been published to the official Maven repository yet, you must compile it yourself and install it to
your local repository.

If you want to include it in your project and make sure all dependencies are packed together and
solved, install it by adding:
```xml
    <properties>
        <third.party.dir>  <Your-Dir-Where-The-JAR-Is-Located>  </third.party.dir>
    </properties>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <artifactId>maven-install-plugin</artifactId>
                    <version>3.1.1</version>
                    <configuration>
                        <updateReleaseInfo>true</updateReleaseInfo>
                    </configuration>
                </plugin>
                ...
        </pluginManagement>

        <plugins>
            <plugin>
                <artifactId>maven-install-plugin</artifactId>
                <inherited>false</inherited>
                <executions>
                    <execution>
                        <id>install-own-maven-license-plugin</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>install-file</goal>
                        </goals>
                        <configuration>
                            <file>${third.party.dir}/license-maven-plugin-2.3.1-SNAPSHOT.jar</file>
                            <groupId>org.codehaus.mojo</groupId>
                            <artifactId>license-maven-plugin</artifactId>
                            <version>2.3.1-SNAPSHOT</version>
                            <packaging>jar</packaging>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            ...
```
Then, to install the plugin a `mvn validate` is enough.

After that, generate the report with `mvn license:aggregate-download-licenses`.

Under `./target/generated-resources/licenses.xlsx` is then the generated Excel-Report with the extended information.
