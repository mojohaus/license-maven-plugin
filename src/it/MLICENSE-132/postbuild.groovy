file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
assert content.contains('(APACHE) Spring O/X Mapping (org.springframework.ws:spring-oxm:1.5.8 - no url defined)');

file = new File(basedir, 'build.log');
assert file.exists();
content = file.text;
assert content.contains('licenseMerges will be overridden by licenseMergesUrl.');
return true;
