To check that third-party report works fine.

Check then that issues:

o MLICENSE-68
o MLICENSE-67 (see why project.getArtifacts() is empty, but project.getDependencyArtifacts() are filled),
              if not includeTransitiveDependencies then it works.