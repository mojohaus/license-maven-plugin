// every dependency keeps its own copy, even though both licenses have the same normalized text
File dir = new File(basedir, 'target/licenses')
['localhost.jettison_company_license.txt', 'localhost.json_company_license.txt'].each { name ->
    File copy = new File(dir, name)
    assert copy.isFile() : "Missing ${copy}; found ${dir.list()}"
    assert copy.text.toLowerCase().contains('company license')
}

File summary = new File(basedir, 'target/licenses.xml')
assert summary.text.contains('localhost.jettison_company_license.txt')
assert summary.text.contains('localhost.json_company_license.txt')

return true
