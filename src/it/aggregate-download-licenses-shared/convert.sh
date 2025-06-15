#!/bin/bash
# Converts output XML file virtually into a HTML table via XSLT,
# to easily check if the template to check against, is itself correct.
# Then starts a webserver to view the HTML view of the XML files.

function convertToHtml() {
    # If there is "xsltproc" installed, use it to convert the XML to an actual HTML file.
    # Only with the persisted HTML file the included JavaScript column sorting works.
    if which xsltproc; then
        html="../${1}/${2}.html"
        xsltproc --output "$html" "sortedByHtml.xslt" "../${1}/${2}.xml"
        if which tidy; then
            echo "Format HTML"
            tidy -i --quiet yes --mute --show-warnings=false --show-errors 0 -o "${html}" "$html"
        fi
        echo "HTML link (XSLT conversion): http://0.0.0.0:8000/${1}/${2}.html"
    fi
    echo "XML link (XSLT conversion): http://0.0.0.0:8000/${1}/${2}.xml"
}

convertToHtml "aggregate-download-licenses-sort-by-dependencyName" sortedByDependencyName
convertToHtml "aggregate-download-licenses-sort-by-licenseMatch" sortedByLicenseMatch
convertToHtml "aggregate-download-licenses-sort-by-licenseName" sortedByLicenseName
convertToHtml "aggregate-download-licenses-sort-by-pluginId" sortedByDependencyPluginId

pushd ..
echo "Stop the server with Ctrl+C"
python3 -m http.server
popd || exit