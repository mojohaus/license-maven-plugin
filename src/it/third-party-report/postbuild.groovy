File target = new File(basedir, "target");
File defaultThirdPartyFile = new File(target, "generated-sources/license/THIRD-PARTY.txt");
File reportThirdPartyFile = new File(target, "site/third-party-report.html");

assert target.isDirectory();
assert defaultThirdPartyFile.isFile();
assert reportThirdPartyFile.isFile();

String[] notExpectedContents = ['Apache License 2.0'];

String[] expectedContents = ['The Apache Software License, Version 2.0',
'org.javassist:javassist:3.18.1-GA',
'commons-logging:commons-logging:1.1.1',
'commons-primitives:commons-primitives:1.0'];

content = reportThirdPartyFile.text;

for (expected in notExpectedContents) {
  assert !content.contains(expected)
}

for (expected in expectedContents) {
  assert content.contains(expected)
}
