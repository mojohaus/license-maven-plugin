// the copy keeps the extension of the source file
File copy = new File(basedir, 'target/licenses/company license - company-license.html')
assert copy.isFile() : "Missing ${copy}; found ${new File(basedir, 'target/licenses').list()}"
assert copy.text.contains('The Company License, for integration testing.')

File summary = new File(basedir, 'target/licenses.xml')
assert summary.text.contains('company license - company-license.html')

return true
