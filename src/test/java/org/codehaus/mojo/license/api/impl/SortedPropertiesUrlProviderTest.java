package org.codehaus.mojo.license.api.impl;

import static org.junit.Assert.*;

import org.codehaus.mojo.license.utils.SortedProperties;
import org.junit.Test;

public class SortedPropertiesUrlProviderTest {

    @Test
    public void testUrl() throws Exception
    {
        String url = "classpath:overrides.properties";
        SortedProperties actual = new SortedPropertiesUrlProvider( url )
                .get();
        assertEquals( SortedPropertiesFileProviderTest.EXPECTED_OVERRIDES, actual.toString() );
    }
}
