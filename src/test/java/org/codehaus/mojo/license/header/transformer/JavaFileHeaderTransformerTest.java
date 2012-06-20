package org.codehaus.mojo.license.header.transformer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link JavaFileHeaderTransformer}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.2
 */
public class JavaFileHeaderTransformerTest
{

    private static final String PACKAGE = "package org.codehaus.mojo.license.header.transformer;";

    private static final String CONTENT = "content";

    private static final String HEADER = "header";

    @Test
    public void testAddHeader()
    {
        JavaFileHeaderTransformer transformer = new JavaFileHeaderTransformer();

        String content = PACKAGE + "\n" + CONTENT;

        transformer.setAddJavaLicenseAfterPackage( false );

        String result = transformer.addHeader( HEADER, content );
        Assert.assertEquals( HEADER + content, result );

        transformer.setAddJavaLicenseAfterPackage( true );

        result = transformer.addHeader( HEADER, content );
        Assert.assertEquals( PACKAGE + "\n" + HEADER + CONTENT, result );

    }

}
