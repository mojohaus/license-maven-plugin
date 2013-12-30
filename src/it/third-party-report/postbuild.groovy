def assertExistsDirectory(file)
{
  if ( !file.exists() || !file.isDirectory() )
  {
    println(file.getAbsolutePath() + " file is missing or is not a directory.")
    return false
  }
  return true
}

def assertExistsFile(file)
{
  if ( !file.exists() || file.isDirectory() )
  {
    println(file.getAbsolutePath() + " file is missing or a directory.")
    return false
  }
  return true
}

def assertNotExistsFile(file)
{
  if ( file.exists() )
  {
    println(file.getAbsolutePath() + " file should not exists.")
    return false
  }
  return true
}

def assertNotContains( content, expected )
{
  if ( content.contains( expected ) )
  {
    println( expected + " was found in \n :" + content )
    return false
  }
  return true
}

def assertContains( content, expected )
{
  if ( !content.contains( expected ) )
  {
    println( expected + " was not found in \n :" + content )
    return false
  }
  return true
}

File target = new File(basedir, "target");
File defaultThirdPartyFile = new File(target, "generated-sources/license/THIRD-PARTY.txt");
File reportThirdPartyFile = new File(target, "site/third-party-report.html");

assert assertExistsDirectory(target);
assert assertExistsFile(defaultThirdPartyFile);
assert assertExistsFile(reportThirdPartyFile);

String[] notExpectedContents = ['Apache License 2.0'];

String[] expectedContents = ['The Apache Software License, Version 2.0',
'org.javassist:javassist:3.18.1-GA',
'commons-logging:commons-logging:1.1.1',
'commons-primitives:commons-primitives:1.0'];

content = reportThirdPartyFile.text;

notExpectedContents.each {
  assert assertNotContains( content, it )
}

expectedContents.each {
  assert assertContains( content, it )
}
return true;