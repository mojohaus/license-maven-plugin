package org.codehaus.mojo.license.spdx;

import java.util.Map;

import org.codehaus.mojo.license.spdx.SpdxLicenseInfo.Attachments.UrlInfo;
import org.codehaus.mojo.license.utils.FileUtil;
import org.junit.Assert;
import org.junit.Test;

public class SpdxLicenseListTest {
    @Test
    public void mimeTypes() {
        for (SpdxLicenseInfo lic : SpdxLicenseList.getLatest().getLicenses().values()) {
            for (UrlInfo urlInfo : lic.getAttachments().getUrlInfos().values()) {
                if (urlInfo.getMimeType() != null) {
                    Assert.assertNotNull(FileUtil.toExtension(urlInfo.getMimeType(), true));
                }
            }
        }
    }

    @Test
    public void extraAliases() {
        Map<String, SpdxLicenseInfo> lics = SpdxLicenseList.getLatest().getLicenses();
        Map<String, UrlInfo> urlInfos = lics.get("Apache-2.0").getAttachments().getUrlInfos();
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/apache2.0"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/apache2.0.php"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/apache2.0.html"));

        urlInfos = lics.get("BSD-2-Clause").getAttachments().getUrlInfos();
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/bsd-license"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/bsd-license.php"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/bsd-license.html"));

        urlInfos = lics.get("CDDL-1.0").getAttachments().getUrlInfos();
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/cddl1"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/cddl1.php"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/cddl1.html"));

        urlInfos = lics.get("MIT").getAttachments().getUrlInfos();
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/mit-license"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/mit-license.php"));
        Assert.assertTrue(urlInfos.containsKey("https://opensource.org/licenses/mit-license.html"));
    }
}
