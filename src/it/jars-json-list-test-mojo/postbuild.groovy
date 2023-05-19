def assertExistsFile(file)
{
  if ( !file.exists() || file.isDirectory() )
  {
    println(file.getAbsolutePath() + " file is missing or a directory.")
    return false
  }
  return true
}

File expected = new File(basedir, "expected.json");
File actual = new File(basedir, "actual.json");

assert assertExistsFile(actual);

assert actual.text == expected.text;

return true;