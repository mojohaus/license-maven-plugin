file = new File(basedir, 'target/generated-sources/license/THIRD-PARTY.txt');
assert file.exists();
content = file.text;
assert !content.contains('the project has no dependencies.');
//artifact ids of both libraries must exist in the output
assert content.contains('smack-core');
assert content.contains('smack-extensions');