
/* A script to generate SpdxLicenseListData */

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Files
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.Map.Entry
import java.util.AbstractMap.SimpleImmutableEntry
import groovy.json.JsonSlurper
import groovy.transform.Field
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.config.RequestConfig.Builder
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.ContentType;
import org.apache.http.HttpStatus
import org.apache.http.StatusLine
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.class)
@Field UrlChecker urlChecker

final Path basePath = basedir.toPath()
@Field final String licensesUrlString = 'https://raw.githubusercontent.com/spdx/license-list-data/v3.5/json/licenses.json'
final URL licensesUrl = new URL(licensesUrlString)
final Path spdxDir = basePath.resolve('src/main/java/org/codehaus/mojo/license/spdx')
final Path spdxTestDir = basePath.resolve('src/test/java/org/codehaus/mojo/license/spdx')
final Path licensesDir = basePath.resolve('target/spdx/licenses')

/* Licenses known to deliver different content over time, although they pass our simple test here */
final List<Pattern> instableContentUrls = [
    Pattern.compile('https?://(www\\.)?opensource\\.org.*'),
]

@Field final Map<String, Entry<Pattern, String>> urlReplacements = [
  /* Workaround for https://github.com/spdx/license-list-XML/issues/777 */
  'archive.org-0': new SimpleImmutableEntry<>(Pattern.compile('(archive\\.org/web/[0-9]+)/'), '$1id_/' ),
  'github.com/aws/mit-0': new SimpleImmutableEntry<>(Pattern.compile('.*github\\.com/aws/mit-0'), 'https://raw.githubusercontent.com/aws/mit-0/master/MIT-0' ),
  'github.com-0': new SimpleImmutableEntry<>(Pattern.compile('https?://github\\.com/([^/]+)/([^/]+)/blob/(.*)'), 'https://raw.githubusercontent.com/$1/$2/$3' ),
  'git.kernel.org-0': new SimpleImmutableEntry<>(Pattern.compile('https?://git\\.kernel\\.org/pub/scm/linux/([^/]+)/git/torvalds/linux\\.git/tree/(.*)'), 'https://git.kernel.org/pub/scm/linux/$1/git/torvalds/linux.git/plain/$2' ),
  'git.savannah.gnu.org-0': new SimpleImmutableEntry<>(Pattern.compile('https?://git\\.savannah\\.gnu\\.org/cgit/(.*)\\.git/tree/(.*)'), 'http://git.savannah.gnu.org/cgit/$1.git/plain/$2' ),
  'microsoft.com/opensource/licenses.mspx': new SimpleImmutableEntry<>(Pattern.compile('.*microsoft\\.com/opensource/licenses\\.mspx'), 'https://web.archive.org/web/20150619132250id_/http://www.microsoft.com/en-us/openness/licenses.aspx' ),
  'mozilla.org/MPL/MPL-1.1.txt': new SimpleImmutableEntry<>(Pattern.compile('.*\\Q.mozilla.org/MPL/MPL-1.1.txt\\E'), 'https://www.mozilla.org/media/MPL/1.1/index.0c5913925d40.txt' ),
  /* repository.jboss.com is known to be slow and timeouting */
  'repository.jboss.com/licenses': new SimpleImmutableEntry<>(Pattern.compile('https?://repository\\.jboss\\.com/licenses/(.*)'), 'https://web.archive.org/web/20171202125112id_/http://repository.jboss.com/licenses/$1' ),
] as TreeMap

@Field final Map<String, List<String>> additionalSeeAlso = [
    'Apache-1.0': [
        'https://apache.org/licenses/LICENSE-1.0.txt',
    ],
    'Apache-1.1': [
        'https://apache.org/licenses/LICENSE-1.1.txt',
    ],
    'Apache-2.0': [
        'https://opensource.org/licenses/apache2.0',
        'https://opensource.org/licenses/apache2.0.php',
        'https://opensource.org/licenses/apache2.0.html',
        'https://apache.org/licenses/LICENSE-2.0.txt',
    ],
    'BSD-2-Clause': [
        'https://opensource.org/licenses/bsd-license',
        'https://opensource.org/licenses/bsd-license.php',
        'https://opensource.org/licenses/bsd-license.html',
    ],
    'CDDL-1.0': [
        'https://opensource.org/licenses/cddl1',
        'https://opensource.org/licenses/cddl1.php',
        'https://opensource.org/licenses/cddl1.html',
    ],
    'MIT': [
        'https://opensource.org/licenses/mit-license',
        'https://opensource.org/licenses/mit-license.php',
        'https://opensource.org/licenses/mit-license.html',
    ]
]
['GPL', 'LGPL'].each { lic ->
    def licLower = lic.toLowerCase(Locale.US)
    ['1.0', '2.0', '2.1', '3.0'].each { v ->
        def urlInfix = '3.0'.equals(v) ? '' : 'old-licenses/'
        ['', '+', '-only', '-or-later'].each { suffix ->
            additionalSeeAlso.put("${lic}-${v}${suffix}".toString(), [
                "https://gnu.org/licenses/${urlInfix}${licLower}-${v}.txt".toString(),
                "https://fsf.org/licensing/licenses/${licLower}-${v}.txt".toString(),
            ])
        }
    }
}

@Field final Map<String, ContentSanitizer> contentSanitizers = [
    'opensource.org-0': new ContentSanitizer('.*opensource\\.org.*', 'jQuery\\.extend\\(Drupal\\.settings[^\n]+', ''),
    'opensource.org-1': new ContentSanitizer('.*opensource\\.org.*', 'value="form-[^"]*"', ''),
    'opensource.org-2': new ContentSanitizer('.*opensource\\.org.*', '<form action="/licenses/[^"]*"', '<form action=""'),
    'opencascade.org-0': new ContentSanitizer('.*opencascade\\.com.*', 'jQuery\\.extend\\(Drupal\\.settings[^\n]+', ''),
    'opencascade.org-1': new ContentSanitizer('.*opencascade\\.com.*', 'value="form-[^"]*"', ''),
    'data.norge.no-0': new ContentSanitizer('.*data\\.norge\\.no.*', 'jQuery\\.extend\\(Drupal\\.settings[^\n]+', ''),
    'data.norge.no-1': new ContentSanitizer('.*data\\.norge\\.no.*', 'value="form-[^"]*"', ''),
    'data.norge.no-2': new ContentSanitizer('.*data\\.norge\\.no.*', 'view-dom-id-[0-9a-f]{12}', ''),
    'directory.fsf.org-0': new ContentSanitizer('.*directory\\.fsf\\.org.*', '"wgRequestId":"[^"]*"', '"wgRequestId":""'),
    'directory.fsf.org-1': new ContentSanitizer('.*directory\\.fsf\\.org.*', '"wgBackendResponseTime":[0-9]+', '"wgBackendResponseTime":0'),
    'fedoraproject.org-0': new ContentSanitizer('.*fedoraproject\\.org.*', '"wgRequestId":"[^"]*"', '"wgRequestId":""'),
    'fedoraproject.org-1': new ContentSanitizer('.*fedoraproject\\.org.*', '"wgBackendResponseTime":[0-9]+', '"wgBackendResponseTime":0'),
    'zimbra.com-0': new ContentSanitizer('.*zimbra\\.com.*', 'Compiled on [^D]+ - Do not edit', ''),
    'romanrm.net-0': new ContentSanitizer('.*romanrm.net/mit-zero', 'src="/lib/exe/indexer\\.php\\?id=mit-zero&amp;[0-9]+"', ''),
    'users.on.net/~triforce-0': new ContentSanitizer('.*users\\.on\\.net/~triforce/glidexp/COPYING\\.txt', '[0-9]+ queries[^\\-]', ''),
    'users.on.net/~triforce-1': new ContentSanitizer('.*users\\.on\\.net/~triforce/glidexp/COPYING\\.txt', '<!-- [^\\-<>]+ in [^\\-<>]+ -->', ''),
    'creativecommons.org-0': new ContentSanitizer('.*creativecommons\\.org.*', '\n ', '\n'),
    'gianluca.dellavedova.org-0': new ContentSanitizer('.*gianluca\\.dellavedova\\.org.*', '<script src=\'https://r-login\\.wordpress\\.com[^\n]*', ''),
    'gianluca.dellavedova.org-1': new ContentSanitizer('.*gianluca\\.dellavedova\\.org.*', 'type="[0-9a-f]+-text/javascript"', 'type="text/javascript"'),
    'gianluca.dellavedova.org-2': new ContentSanitizer('.*gianluca\\.dellavedova\\.org.*', 'data-cf-modified-[^\\-]+-', 'data-cf-modified--'),
    'gianluca.dellavedova.org-3': new ContentSanitizer('.*gianluca\\.dellavedova\\.org.*', 'atatags-[^\\-"]+-[^\\-"]+', 'atatags--'),
    'eu-datagrid.web.cern.ch-0': new ContentSanitizer('.*eu-datagrid\\.web\\.cern\\.ch.*', 'wct=[^&"]+', 'wct='),
    'joinup.ec.europa.eu-0': new ContentSanitizer('.*joinup\\.ec\\.europa\\.eu.*', '<script type="text/javascript">window\\.NREUM[^\n]*</script>', ''),
    'artlibre.org-0': new ContentSanitizer('.*artlibre\\.org.*', '<!-- Dynamic page generated in [^\\-]+ -->', ''),
    'artlibre.org-1': new ContentSanitizer('.*artlibre\\.org.*', '<!-- Cached page generated by WP-Super-Cache on [^\\>]+ -->', ''),
    'tcl.tk-0': new ContentSanitizer('.*tcl\\.tk.*', 'email-protection#[0-9a-f]+', 'email-protection'),
    'tcl.tk-1': new ContentSanitizer('.*tcl\\.tk.*', 'data-cfemail="[^"]+"', 'data-cfemail=""'),
    'codeproject.com-0': new ContentSanitizer('.*codeproject\\.com.*', '>[^<]+members<', '><'),
    'codeproject.com-1': new ContentSanitizer('.*codeproject\\.com.*', '<div class="promo">[^\n]+', ''),
    'codeproject.com-2': new ContentSanitizer('.*codeproject\\.com.*', '<div class="msg-728x90"[^\n]+', ''),
    'codeproject.com-3': new ContentSanitizer('.*codeproject\\.com.*', '<br />\\s*[^\\s]+\\s*\\|\\s*[^\\s]+\\s*\\|', ''),
    'ohwr.org-0': new ContentSanitizer('.*ohwr\\.org.*', '<meta name="csrf-token"[^\n]+', ''),
] as TreeMap

if (Files.exists(licensesDir)) {
    licensesDir.toFile().deleteDir()
}
Files.createDirectories(licensesDir)

def engine = new groovy.text.SimpleTemplateEngine()
JsonSlurper jsonSlurper = new JsonSlurper()
Map<String, Object> spdx = jsonSlurper.parseText(licensesUrl.text)
try {
    urlChecker = new UrlChecker(instableContentUrls, urlReplacements, contentSanitizers, licensesDir)

    spdx['licenses'].each { lic ->
        final String licId = lic['licenseId']
        log.info("Starting "+ licId)
        lic['seeAlso']?.each { url ->
            url = url.trim()
            urlChecker.addUrl(url)
        }
        additionalSeeAlso.get(licId)?.each { url ->
            url = url.trim()
            println "adding additional URL "+ url
            urlChecker.addUrl(url)
        }
    }
    log.info("Rechecking")
    urlChecker.recheck()

    def model = [
        spdx: spdx,
        year: Calendar.getInstance().get(Calendar.YEAR),
        urlChecker: urlChecker,
        contentSanitizers: contentSanitizers,
        urlReplacements: urlReplacements,
        generator: this,
    ]

    def sourceTemplate = '''package org.codehaus.mojo.license.spdx;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) ${year} Codehaus
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

/**
 * A class generated by GenerateSpdxLicenseList.groovy from
 * <a href="${generator.licensesUrlString}">
 * ${generator.licensesUrlString}</a>
 */
class SpdxLicenseListData
{

    static SpdxLicenseList createList()
    {
        final SpdxLicenseList.Builder builder = SpdxLicenseList.builder();
<%
spdx.each { field, value ->
    switch (field) {
    case 'licenseListVersion':
    case 'releaseDate':
        println '        builder.' + field + '( "' + value + '" );'
        break
    case 'licenses':
        value.each { lic ->
            println ""
            println "        builder.license( SpdxLicenseInfo.builder()"
            lic.each { licField, licValue ->
                switch (licField) {
                    case 'referenceNumber':
                        /* ignore */
                        break
                    case 'isDeprecatedLicenseId':
                    case 'isOsiApproved':
                    case 'isFsfLibre':
                        println '            .' + licField + '( ' + licValue + ' )'
                        break
                    case 'detailsUrl':
                    case 'name':
                    case 'reference':
                    case 'licenseId':
                        println '            .' + licField + '( ' + generator.escapeString(licValue.trim()) + ' )'
                        break
                    case 'seeAlso':
                        licValue.each { seeAlso ->
                            final String url = seeAlso.trim()
                            println '            .seeAlso( ' + generator.escapeString(url) + ' )'
                        }
                        def moreSeeAlso = generator.additionalSeeAlso.get(lic['licenseId'])
                        moreSeeAlso = moreSeeAlso == null ? [] : moreSeeAlso

                        def urlInfos = urlChecker.getUrlInfos(licValue + moreSeeAlso);
                        if (!urlInfos.isEmpty()) {
                            println ''
                            urlInfos.each { url, sha1MimeTypeStable ->
                                final String sha1 = sha1MimeTypeStable.get(0)  == null ? 'null' : ('"' + sha1MimeTypeStable.get(0) + '"')
                                final String mimeType = sha1MimeTypeStable.get(1)  == null ? 'null' : ('"' + sha1MimeTypeStable.get(1) + '"')
                                println '            .urlInfo( "' + url + '", ' + sha1 + ', ' + mimeType + ', ' + sha1MimeTypeStable.get(2) + ', ' + sha1MimeTypeStable.get(3) + ' )'
                            }
                        }
                        break
                    default:
                        throw new IllegalStateException( "Unexpected field of SPDX license "+ licField )
                }
            }
            println "            .build()"
            println "        );"
        }
        println ""
        break
    default:
        throw new IllegalStateException( "Unexpected field of SPDX license list "+ field )
    }
}
println ''
urlReplacements.each { id, r ->
    println '        builder.urlReplacement( ' + generator.escapeString(id) + ', ' + generator.escapeString(r.getKey().pattern()) + ', ' + generator.escapeString(r.getValue()) + ' );'
}
println ''
contentSanitizers.each { id, cs ->
    println '        builder.contentSanitizer( ' + generator.escapeString(id) + ', ' + generator.escapeString(cs.getUrlPattern().pattern()) + ', ' + generator.escapeString(cs.getContentPattern().pattern()) + ', ' + generator.escapeString(cs.getContentReplacement()) + ' );'
}
println ''

%>        return builder.build();
    }
}
'''
    String source = engine.createTemplate(sourceTemplate).make(model)
    spdxDir.resolve("SpdxLicenseListData.java").toFile().write(source)


def testTemplate = '''package org.codehaus.mojo.license.spdx;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) ${year} Codehaus
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

/**
 * A class generated by GenerateSpdxLicenseList.groovy from
 * <a href="${generator.licensesUrlString}">
 * ${generator.licensesUrlString}</a>
 */
public class SpdxLicenseListDataTest
{
    @org.junit.Test
    public void getInstance()
    {
        SpdxLicenseList list = SpdxLicenseList.getLatest();
        org.junit.Assert.assertEquals( ${spdx.licenses.size()}, list.getLicenses().size() );
    }
}
'''

    String testSource = engine.createTemplate(testTemplate).make(model)
    Files.createDirectories(spdxTestDir)
    spdxTestDir.resolve("SpdxLicenseListDataTest.java").toFile().write(testSource)

} finally {
    urlChecker?.close()
}

public static String escapeString(String literal) {
    if (literal == null) {
        return 'null'
    }
    return '"'+ literal.replace('\n', '\\n').replace('\t', '\\t').replace('\r', '\\r').replace('\\', '\\\\').replace('"', '\\"') +'"'
}

class ContentSanitizer {
    private final Pattern urlPattern
    private final Pattern contentPattern
    private final String contentReplacement
    public ContentSanitizer( String urlPattern, String contentPattern, String contentReplacement ) {
        this.urlPattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE)
        this.contentPattern = Pattern.compile(contentPattern, Pattern.CASE_INSENSITIVE)
        this.contentReplacement = contentReplacement
    }
    public boolean applies(String url) {
        return urlPattern.matcher( url ).matches();
    }

    public String sanitize(String content) {
        if ( content == null ) {
            return null;
        }
        return contentPattern.matcher( content ).replaceAll( contentReplacement );
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public Pattern getContentPattern() {
        return contentPattern;
    }

    public String getContentReplacement() {
        return contentReplacement;
    }
}

class UrlChecker implements AutoCloseable {
    def log = LoggerFactory.getLogger(this.class)
    private final Map<String, Set<String>> sha1ToUrls = new LinkedHashMap<>()
    private final Map<String, Map.Entry<String, String>> urlToSha1MimeType = new LinkedHashMap<>()
    private final CloseableHttpClient client
    private final List<Pattern> instableContentUrls
    private final Map<String, Entry<Pattern, String>> urlReplacements
    private final Map<String, ContentSanitizer> contentSanitizers
    private final Path licensesDir
    private final Set<String> sanitizedUrls = new HashSet<>()

    public UrlChecker(List<Pattern> instableContentUrls, Map<String, Entry<Pattern, String>> urlReplacements,
            Map<String, ContentSanitizer> contentSanitizers, Path licensesDir) {
        this.instableContentUrls = instableContentUrls
        this.urlReplacements = urlReplacements
        this.contentSanitizers = contentSanitizers
        this.licensesDir = licensesDir
        final RequestConfig config = RequestConfig.copy(RequestConfig.DEFAULT)
                .setConnectTimeout(2000)
                .setSocketTimeout(2000)
                .setConnectionRequestTimeout(2000)
                .build();
        this.client = HttpClients.custom().setDefaultRequestConfig(config).build()
    }

    public void addUrl(String url) {
        urlReplacements.each { key, en ->
            final Pattern pat = en.getKey()
            final String replacement = en.getValue()
            url = pat.matcher(url).replaceAll(replacement)
        }
        if (!urlToSha1MimeType.containsKey(url)) {
            urlToSha1MimeType.put(url, get(url, false))
        }
    }

    public void recheck() {
        final Set<Map.Entry<String, Map.Entry<String, String>>> newUrlToSha1MimeType = new LinkedHashSet<>();
        final Iterator<Map.Entry<String, Map.Entry<String, String>>> it = urlToSha1MimeType.entrySet().iterator()
        while (it.hasNext()) {
            final Map.Entry<String, Map.Entry<String, String>> old = it.next()
            final String url = old.getKey()
            final Map.Entry<String, String> oldSha1MimeType = old.getValue()
            if (oldSha1MimeType != null) {
                final Map.Entry<String, String> newSha1 = get(url, true)
                if (!oldSha1MimeType.equals(newSha1)) {
                    log.warn("Volatile content from URL: "+ url + " old: "+ oldSha1MimeType + ", new "+ newSha1)
                    old.setValue(new SimpleImmutableEntry(null, oldSha1MimeType.getValue()))
                } else {
                    final String sha1 = oldSha1MimeType.getKey();
                    generalize(url, sha1, newUrlToSha1MimeType)
                }
            } else {
                //it.remove()
            }
        }
        for (Map.Entry<String, Map.Entry<String, String>> newEntry in newUrlToSha1MimeType) {
            urlToSha1MimeType.put(newEntry.getKey(), newEntry.getValue())
        }
    }

    // @return sha1, mimeType pair
    private Map.Entry<String, String> get(String url, boolean isRecheck) {
        CloseableHttpResponse response;
        try {
            response = client.execute( new HttpGet( url ) )
            final StatusLine statusLine = response.getStatusLine();
            if ( statusLine.getStatusCode() != HttpStatus.SC_OK )
            {
                log.warn("Got "+ statusLine + " for "+ url)
                return null;
            }
            log.info("Got "+ statusLine.getStatusCode() + " for "+ url)
            final HttpEntity entity = response.getEntity();
            if ( entity != null )
            {
                final ContentType contentType = ContentType.get( entity );
                final String mimeType = contentType != null ? contentType.getMimeType() : null
                final Charset charset = contentType != null ? (contentType.getCharset() == null ? StandardCharsets.UTF_8 : contentType.getCharset()) : StandardCharsets.UTF_8
                final Reader r = null
                final StringBuilder contentBuilder = new StringBuilder()
                try {
                    r = new InputStreamReader(entity.getContent(), charset)
                    char[] buffer = new char[8192]
                    int len = 0;
                    while ((len = r.read(buffer)) >= 0) {
                        contentBuilder.append(buffer, 0, len)
                    }
                }
                finally {
                    r?.close()
                }

                final String rawContent = contentBuilder.toString();
                final String urlFileName = urlToFileName(url)
                final String suffix = (isRecheck ? ".recheck.txt" : ".txt")
                byte[] bytes = rawContent.getBytes(charset);
                Files.write(licensesDir.resolve(urlFileName + ".raw" + suffix), bytes)
                String content = rawContent
                final List<String> sanitizers = new ArrayList<>()
                for (Map.Entry<String, ContentSanitizer> en in contentSanitizers.entrySet()) {
                    final ContentSanitizer sanitizer = en.getValue()
                    if (sanitizer.applies(url)) {
                        content = sanitizer.sanitize(content)
                        sanitizers.add(en.getKey())
                    }
                }
                if (!content.equals(rawContent)) {
                    log.info("Sanitized "+ url + " using "+ sanitizers)
                    bytes = content.getBytes(charset);
                    Files.write(licensesDir.resolve(urlFileName  + ".sanitized" + suffix), bytes)
                    sanitizedUrls.add(url)
                }
                final String sha1 = DigestUtils.sha1Hex(bytes)
                return new SimpleImmutableEntry(sha1, mimeType)
            } else {
                log.warn("Got no body for "+ url)
                return null;
            }
        } catch (Exception e) {
            log.warn("Could not get "+ url +": "+ e.getMessage())
            return null;
        } finally {
            response?.close()
        }
    }

    public Map<String, Tuple> getUrlInfos(List<String> urls) {
        final Map<String, Tuple> result = new LinkedHashMap<>()
        for (String url in urls) {
            url = url.trim()
            final boolean stable = isStable(url)
            final boolean sanitized = sanitizedUrls.contains(url)
            final Map.Entry<String, String> sha1MimeType = urlToSha1MimeType.get(url)
            if (sha1MimeType != null) {
                if (sha1MimeType.getKey() != null) {
                    final Set<String> sha1Urls = sha1ToUrls.get(sha1MimeType.getKey())
                    assert sha1Urls != null
                    assert !sha1Urls.isEmpty()
                    for (String shaUrl in sha1Urls) {
                        Map.Entry<String, String> smt = urlToSha1MimeType.get(shaUrl)
                        assert smt != null
                        final String sha1 = smt.getKey()
                        result.put(shaUrl, new Tuple(sha1, smt.getValue(), sha1 != null && stable, sanitized))
                    }
                } else {
                    result.put(url, new Tuple(null, null, false, sanitized))
                }
            }
        }
        return result;
    }

    public Map.Entry<String, String> getSha1MimeType(String url) {
        return urlToSha1MimeType.get(url);
    }

    private boolean tryAdd(String url, String sha1, Set<String> urls, Set<Map.Entry<String, Map.Entry<String, String>>> newUrlToSha1MimeType) {
        if (!urls.contains(url)) {
            final Map.Entry<String, String> newSha1MimeType = get(url, true)
            if (sha1.equals(newSha1MimeType?.getKey())) {
                log.info(" - generalized: "+ url)
                urls.add(url)
                newUrlToSha1MimeType.add(new SimpleImmutableEntry(url, new SimpleImmutableEntry(sha1, newSha1MimeType.getValue())))
                return true
            } else {
                log.warn(" - could not generalize: old sha1 "+ sha1 +" new sha1: "+ newSha1MimeType)
            }
        }
        return false
    }

    private boolean isStable(String url) {
        for (Pattern pat in instableContentUrls) {
            if (pat.matcher(url).matches()) {
                return false
            }
        }
        return true
    }

    public void generalize(String url, String sha1, Set<Map.Entry<String, Map.Entry<String, String>>> newUrlToSha1MimeType) {
        log.info("Generalizing "+ url)
        final Set<String> urls = sha1ToUrls.get(sha1)
        if (urls == null) {
            urls = new TreeSet<>()
            sha1ToUrls.put(sha1, urls)
        }
        urls.add(url)
        if (url.startsWith('http://')) {
            if (url.indexOf('://www.') >= 0) {
                tryAdd(url.replace('http://', 'https://'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('://www.', '://'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('http://', 'https://').replace('://www.', '://'), sha1, urls, newUrlToSha1MimeType);
            } else {
                tryAdd(url.replace('http://', 'https://'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('://', '://www.'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('http://', 'https://').replace('://', '://www.'), sha1, urls, newUrlToSha1MimeType);
            }
        } else if (url.startsWith('https://')) {
            if (url.indexOf('://www.') >= 0) {
                tryAdd(url.replace('https://', 'http://'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('://www.', '://'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('https://', 'http://').replace('://www.', '://'), sha1, urls, newUrlToSha1MimeType);
            } else {
                tryAdd(url.replace('https://', 'http://'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('://', '://www.'), sha1, urls, newUrlToSha1MimeType);
                tryAdd(url.replace('https://', 'http://').replace('://', '://www.'), sha1, urls, newUrlToSha1MimeType);
            }
        }
    }

    public void close() throws IOException {
        client.close();
    }

    private String urlToFileName(String url) {
        return url.replace('/', '!').replace('\\', '!')
    }
}

