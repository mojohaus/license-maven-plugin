package org.codehaus.mojo.license.spdx;

import java.util.Map;

import org.codehaus.mojo.license.spdx.SpdxLicenseInfo.Attachments.UrlInfo;
import org.codehaus.mojo.license.utils.FileUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpdxLicenseListTest {
    @Test
    void mimeTypes() {
        for (SpdxLicenseInfo lic : SpdxLicenseList.getLatest().getLicenses().values()) {
            for (UrlInfo urlInfo : lic.getAttachments().getUrlInfos().values()) {
                if (urlInfo.getMimeType() != null) {
                    assertNotNull(FileUtil.toExtension(urlInfo.getMimeType(), true));
                }
            }
        }
    }

    @Test
    void extraAliases() {
        Map<String, SpdxLicenseInfo> lics = SpdxLicenseList.getLatest().getLicenses();
        Map<String, UrlInfo> urlInfos = lics.get("Apache-2.0").getAttachments().getUrlInfos();
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/apache2.0"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/apache2.0.php"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/apache2.0.html"));

        urlInfos = lics.get("BSD-2-Clause").getAttachments().getUrlInfos();
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/bsd-license"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/bsd-license.php"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/bsd-license.html"));

        urlInfos = lics.get("CDDL-1.0").getAttachments().getUrlInfos();
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/cddl1"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/cddl1.php"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/cddl1.html"));

        urlInfos = lics.get("MIT").getAttachments().getUrlInfos();
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/mit-license"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/mit-license.php"));
        assertTrue(urlInfos.containsKey("https://opensource.org/licenses/mit-license.html"));
    }
}
