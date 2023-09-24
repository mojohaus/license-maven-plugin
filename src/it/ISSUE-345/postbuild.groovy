// Check that the licenseMergesUrl is used.
file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY.txt')
assert file.exists()
content = file.text
assert !content.contains('the project has no dependencies.')
assert content.contains('(APACHE) Commons Logging (commons-logging:commons-logging:1.1.1 - http://commons.apache.org/logging)')

// Since no licenseMerges was provided then there should be no warning.
file = new File(basedir, 'build.log')
assert file.exists()
content = file.text
assert !content.contains('licenseMerges will be overridden by licenseMergesUrl.')
return true
