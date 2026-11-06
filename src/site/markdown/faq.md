---

title: Frequently Asked Questions
---------------------------------

<!--
#%L
License Maven Plugin
%%
Copyright (C) 2010 - 2011 Codehaus, Tony Chemit
%%
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Lesser Public License for more details.

You should have received a copy of the GNU General Lesser Public
License along with this program.  If not, see
<http://www.gnu.org/licenses/lgpl-3.0.html>.
#L%
-->

# Frequently Asked Questions

**General**

1. [What is the difference between this plugin (the Codehaus license-maven-plugin) and the maven-license-plugin hosted at Google Code?](#google-code-maven-license-plugin)
2. [What is the difference between this plugin (the Codehaus license-maven-plugin) and the maven-license-plugin hosted at Code Lutin?](#code-lutin-maven-license-plugin)
3. [Why sql style should not be used with mysql sql files ?](#mysql-file-header)
4. [How to deal with hardcoded encoding ISO-8859-1 for properties files when updating file headers?](#properties_files)

## General

<a id="google-code-maven-license-plugin"></a>

### What is the difference between this plugin (the Codehaus license-maven-plugin) and the maven-license-plugin hosted at Google Code?

The Codehaus plugin and the Google Code plugin were developed separately but with similar goals. The two plugins have several overlapping features including verifying and updating license information in source files. The Codehaus plugin also provides the ability to download the licenses of third party dependencies and create and XML report listing each project dependency and its license. If you are not sure which one to use you should try both to see which one best fits your needs.

<a id="code-lutin-maven-license-plugin"></a>

### What is the difference between this plugin (the Codehaus license-maven-plugin) and the maven-license-plugin hosted at Code Lutin?

The [Code Lutin maven-license-plugin](http://maven-site.nuiton.org/maven-license-plugin/) is an older version of the Codehaus license-maven-plugin. The source code of the Code Lutin plugin was donated to Codehaus and merged with some existing license plugin code. The original creator joined then the Codehaus team to continue the project development.

<a id="mysql-file-header"></a>

### Why sql style should not be used with mysql sql files ?

Mysql does not accept **`---`** as comment (which is used to mark license header for normal sql files). You should use the *mysql* header style instead, with configuration:

```xml
<extraExtensions>
  <sql>mysql</sql>
</extraExtensions>
```

[Bug detail.](http://jira.codehaus.org/browse/MLICENSE-56)

<a id="properties_files"></a>

### How to deal with hardcoded encoding ISO-8859-1 for properties files when updating file headers?

Even if in modern JDK (since 1.6), there is a support for UTF-8 encoding on properties files, encoding ISO-8859-1 is still used on thoses files.

To keep this encoding for thoses files, you need then to execute twice the update-file-header plugin like this:

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>license-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>update-file-header-UTF-8</id>
      <goals><goal>update-file-header</goal></goals>
      <configuration>
        <excludes>
          <exclude>**/*.properties</exclude>
        </excludes>
        <encoding>UTF-8</encoding>
      </configuration>
    </execution>
    <execution>
      <id>update-file-header-ISO-8859-1</id>
      <goals><goal>update-file-header</goal></goals>
      <configuration>
        <includes>
          <include>**/*.properties</include>
        </includes>
        <encoding>ISO-8859-1</encoding>
      </configuration>
    </execution>
  </executions>
</plugin>
```

