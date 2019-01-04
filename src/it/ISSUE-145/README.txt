Demonstrates that the aggregateAddThirdParty goal does not cause Maven to drop dependencies.

The project has parent A and children B and C with B depending on C. The bug in the plugin caused B to lose its dependency on C at build time, because the plugin modified Maven's list of dependencies for B. The test verifies that the dependency is preserved.