package org.codehaus.mojo.license.spdx;

import org.codehaus.mojo.license.spdx.SpdxLicenseInfo.Attachments.UrlInfo;
import org.codehaus.mojo.license.utils.FileUtil;
import org.junit.Assert;
import org.junit.Test;

public class SpdxLicenseListTest
{
    @Test
    public void mimeTypes()
    {
        for ( SpdxLicenseInfo lic : SpdxLicenseList.getLatest().getLicenses().values() )
        {
            for ( UrlInfo urlInfo : lic.getAttachments().getUrlInfos().values() )
            {
                if ( urlInfo.getMimeType() != null )
                {
                    Assert.assertNotNull( FileUtil.toExtension( urlInfo.getMimeType(), true ));
                }
            }
        }
    }
}
