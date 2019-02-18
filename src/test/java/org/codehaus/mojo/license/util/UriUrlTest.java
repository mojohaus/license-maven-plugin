package org.codehaus.mojo.license.util;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class UriUrlTest
{
    @Test
    public void uriUrlTest() throws MalformedURLException {
        final Path uncPath = Paths.get( "//server/dir/file.txt" );
        Assert.assertEquals( uncPath.toUri().toURL().toString(), uncPath.toUri().toString() );
    }
}
