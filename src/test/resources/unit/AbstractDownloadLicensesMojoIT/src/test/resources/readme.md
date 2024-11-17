# Directory content

The `sortedBy....xml` files are used by the `DownloadLicensesIT` integration test.
It checks if the test outcomes are equal to the test-files.

To re-generate it, comment the line `// saveDependencyInfos(dependencyInfos);` in and run the `DownloadLicensesIT`
integration test and copy the generated tmp files over the existing standard files.
