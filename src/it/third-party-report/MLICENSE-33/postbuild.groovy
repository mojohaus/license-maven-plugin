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

def assertContent(file, content, wantedText)
{
  if ( !content.contains(wantedText) )
  {
    println(file.getAbsolutePath() + " should contains content " + wantedText)
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

//String content = reportThirdPartyFile.text
//
//String[] expectedContents = ["commons-logging:commons-logging:1.1.1"]
//
//expectedContents.each {
//  assert assertContent(otherThirdPartyFile, content, it)
//}

return true;