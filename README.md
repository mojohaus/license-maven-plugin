license-maven-plugin
====================

It is a fork of original mojohaus's [license-maven-plugin](https://github.com/mojohaus/license-maven-plugin).
#### Overview
The plugin provides the following basic functionality:
1. generates **THIRD-PARTY.txt** with list of all 3rd-party dependencies that are included in build with their licenses (maven plugin goal *add-thirdparty-properties*)
2. downloads files with license text (one per each license) (maven plugin goal *download-licenses*)
3. performs check for forbidden licenses (i.e. licenses that are not in whitelist) <license-registry repository>/licenses-whitelist.txt   

By default, all files are placed in *${project.build.directory}/generated-resources/licenses* directory
**NOTE:** by default these operations are bound to *process-resources* phase
#### Usage example

##### license-maven-plugin configuration

* As a starting template for plugin configuration in pom.xml you can use the following snippet:
```xml
<plugin>
    <groupId>org.octopusden.octopus</groupId>
    <artifactId>license-maven-plugin</artifactId>
    <version>${license-maven-plugin.version}</version>
    <configuration>
        <acceptPomPackaging>true</acceptPomPackaging>
        <excludedScopes>test,provided</excludedScopes>
        <failIfWarning>false</failIfWarning>
        <failOnMissing>${license.failOnMissing}</failOnMissing>
        <failOnBlacklist>${license.failOnBlacklist}</failOnBlacklist>
        <excludedGroups>YOUR_CORPORATE.*|javax.mail.*</excludedGroups>
        <useMissingFile>false</useMissingFile>
        <useRepositoryMissingFiles>false</useRepositoryMissingFiles>
        <licensesOutputDirectory>${license.output.directory}</licensesOutputDirectory>
        <outputDirectory>${license.output.directory}</outputDirectory>
        <skip>${license.skip}</skip>
    </configuration>
    <executions>
        <execution>
            <id>license-check</id>
            <phase>generate-resources</phase>
            <goals>
                <goal>add-third-party</goal>
                <goal>download-licenses</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>org.apache.maven.doxia</groupId>
            <artifactId>doxia-core</artifactId>
            <version>1.2</version>
        </dependency>
    </dependencies>
</plugin>
```
* Note that license-maven-plugin works only with artifacts specified as dependency (including transitive) therefore ensure that all external libraries are specified as dependencies. 
* You should enable license-maven-plugin in your module that builds distribution
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.octopusden.octopus</groupId>
            <artifactId>license-maven-plugin</artifactId>
        </plugin>
        ...
    </plugins>
</build>
```
        
* Add generated license files into distribution (applied only for explicitly distributed components)
* The parameter **license-registry.git-repository** is mandatory parameter specifies the URL of the license repository. 
It can be provided as either an environment variable or as JVM argument, for example ```mvn clean install -Dlicense-registry.git-repository=<repository_url>```

* Example for assembling with maven-war-plugin: 
```xml
<plugin>
    <artifactId>maven-war-plugin</artifactId>
    <configuration>
        <webResources>
            ...
            <resource>
                <directory>${license.output.directory}</directory>
                <targetPath>${license.distribution.path}</targetPath>
                <filtering>false</filtering>
            </resource>
        </webResources>
    </configuration>
</plugin>
```
        
* Example for assembling with maven-assembly-plugin (add following in assembly descriptor):
```xml
<fileSet>
    <includes>
        <include>**</include>
    </includes>
    <directory>${license.output.directory}</directory>
    <outputDirectory>${license.distribution.path}</outputDirectory>
</fileSet>
```

#### How to run

###### Run license check during build
By default license plugin is disabled. You should reset *license.skip* property to enable it 
Execute 
```
mvn  install -Dlicense.skip=false
```
After that you should look at *${project.build.directory}/generated-resources/licenses*

**NOTE** If you run plugin from internal network w/o direct access to Internet then specify also `-Dlicense.proxy=http://proxy:800`


###### Running separate operations (on developer machine)
You can run separate operations from command line:

- to generate **THIRD-PARTY.txt** file with list of used licenses
    For multimodule project:
```
mvn org.octopusden.octopus:license-maven-plugin:<VERSION>:aggregate-add-third-party \
  -Dlicense.acceptPomPackaging \
  -Dlicense.failOnBlacklist \
  -Dlicense.failOnMissing
```

    For single module:
```
mvn org.octopusden.octopus:license-maven-plugin:<VERSION>:add-third-party \
  -Dlicense.acceptPomPackaging \
  -Dlicense.failOnBlacklist \
  -Dlicense.failOnMissing
```


- to download all licenses

For multimodule project: 
```$xslt
mvn org.octopusden.octopus:license-maven-plugin:<VERSION>:aggregate-download-licenses
```

For single module: 
```$xslt
mvn org.octopusden.octopus:license-maven-plugin:<VERSION>:download-licenses
```

#### Parameters information

- `-Dlicense.acceptPomPackaging` fetching licensees projects with `pom` packaging
- `-Dlicense.failOnBlacklist` let task fail on not acceptable licenses
- `-Dlicense.failOnMissing` let task fail on dependencies with no licenses
- `-Dlicense.skip` disables\enables license plugin
- `-Dlicense.proxy` url to proxy if there is no direct access to WWW (example `http://proxy:800`)


#### Result processing
If your build successfully passes, you are most likely clean. Congratulations!

If your build fails with message like this:
``` 
[INFO] Included licenses (whitelist): [BSD, CDDL, Eclipse Public License - v 1.0, Public Domain, The Apache Software License, Version 2.0]
[ERROR] There is 1 forbidden licenses used:
[ERROR] License MIT used by 2 dependencies:
...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO]
[INFO] My parent ........................... SUCCESS [1.016s]
[INFO] My module 1 ......................... FAILURE [3.063s]
[INFO] My module 2 ......................... SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
```

You should manually check all rejected libraries.
There are two possible failures in verification process:

##### License cannot be resolved for particular library. 
In this case you'll see message like this: "License Unknown used by <N> dependencies..."
```
[WARNING] License "Unknown license" used by 1 dependencies:
-xdb6 (com.oracle:xdb6:10.2.0.4 - no url defined)
```
In this case you should manually determine license for this library and manually register it in
<license-registry repository>/thirdparty-licenses.properties. 
Another option is to replace library with another one having suitable license.
##### License is resolved but is not added to whitelist. 
In this case you'll see message like this: 
"License <licensename> used by <N> dependencies..."
Example:
```
   [WARNING] License "ICU License" used by 1 dependencies:
    -ICU4J (com.ibm.icu:icu4j:57.1 - http://icu-project.org/)
```

1. If the name of the license differs only in spelling from already registered one, then add it to the list of synonyms
in <license-registry repository>/merges.txt in '|'-separated string (one line per each license)
2. In case of not supported license, do one of the following:

* Replace library with another one having suitable license.
* Decide if this license is indeed prohibited. If not, whitelist this license.

#### Third party licenses repository

Default URLs for licenses are specified <license-registry repository>/licenses.properties.

#### Registering license synonyms
Different name for same license can be specified in <license-registry repository>/merges.txt
in '|'-separated string (one string per each license)