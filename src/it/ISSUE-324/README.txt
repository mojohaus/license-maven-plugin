Demonstrates that the aggregateAddThirdParty plugin only resolves the actually used version of a dependency, and not all versions that are present in transitive dependencies.

The project contains a child2 that depends on hbase-annotations:2.1.3. It also contains child1 which depends on hive-hcatalog-core:2.3.4. The Hive dependency also depends on hbase-annotations, but in a much older version, and through a longer dependency chain. The old hbase-annotations artifact depends on jdk.tools, which is not present in Java 9+.

The plugin execution will fail if the plugin attempts to resolve jdk.tools on Java 9+. This test is therefore only meaningful on Java 9+, and will pass spuriously on earlier versions.