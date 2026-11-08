// The header of an HTML file keeps its blank lines when update-file-header runs a second time with trimHeaderLine.
file = new File(basedir, 'src/files/test.html')
assert file.isFile()
content = file.text.replace('\r\n', '\n')

assert content.startsWith('<!--')
assert content.count('#%L') == 1
assert content.contains('Second name')
assert !content.contains('First name')
assert content.contains('Copyright (C)')
assert content.contains('any later version.\n\n  This program is distributed')
assert content.contains('<p>Test</p>')
return true
