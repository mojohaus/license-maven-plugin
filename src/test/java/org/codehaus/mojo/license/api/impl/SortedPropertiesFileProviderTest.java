package org.codehaus.mojo.license.api.impl;

import static org.junit.Assert.*;

import java.io.File;

import org.codehaus.mojo.license.LicenseMojoUtilsTest;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.junit.Test;

public class SortedPropertiesFileProviderTest {

    public static final String EXPECTED_OVERRIDES =
            "{org.jboss.xnio--xnio-api--3.3.6.Final=The Apache Software License, Version 2.0}";

    @Test
    public void testFile() throws Exception
    {
        File file = LicenseMojoUtilsTest.OVERRIDES_PROPERTIES;
        SortedProperties actual = new SortedPropertiesFileProvider( file, "UTF-8" )
                .get();
        assertEquals( EXPECTED_OVERRIDES, actual.toString() );
    }
}
