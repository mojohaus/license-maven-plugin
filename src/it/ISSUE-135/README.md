# Integration test for issue #135

## description

When adding a license header to a java file that has a "package" and "import" on the same line; e.g. something like this:

```java
package org.example.test; import java.io.File;
```

the content after the package declaration (the import) is wrongly truncated.

## test scenario

- The original java file is located at src/main/original/java
- It is copied to directory target/generated-sources
- The directory target/generated-sources is added as a source of the project
- The update-file-header goal is executed on that directory
- the build fails with compile errors
