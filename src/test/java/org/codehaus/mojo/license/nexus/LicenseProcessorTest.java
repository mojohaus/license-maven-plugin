package org.codehaus.mojo.license.nexus;

import org.apache.maven.model.License;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * Created on 30.01.2018.
 */
public class LicenseProcessorTest {

    private LicenseProcessor licenseProcessor = new LicenseProcessor(null, null);

    @Test
    public void test() throws IOException {
        String data = loadToString("licenseInfo.json");
        ComponentInfo componentInfo = licenseProcessor.parseJSON(data);
        System.out.println(componentInfo);
    }

    private String loadToString(String fileName) throws IOException {
        InputStream resourceAsStream = LicenseProcessorTest.class.getResourceAsStream("/" + fileName);
        return IOUtil.toString(resourceAsStream);
    }

    @Test
    public void testParseList() {
        assertEquals(Collections.singletonList("Apache-2.0"), licenseProcessor.parseLicense("Apache-2.0"));
        assertEquals(0, licenseProcessor.parseLicense("").size());
        assertEquals(0, licenseProcessor.parseLicense("Not-Declared").size());
        assertEquals(0, licenseProcessor.parseLicense("Not Declared").size());
        assertEquals(0, licenseProcessor.parseLicense("No-Sources").size());
        assertEquals(0, licenseProcessor.parseLicense("No Sources").size());
        assertEquals(asList("Apache-2.0", "LGPL-2.1+", "MPL-1.1"), licenseProcessor.parseLicense("Apache-2.0 or LGPL-2.1+ or MPL-1.1"));
    }

    @Test
    public void testLoadLicenses() throws IOException {
        String data = loadToString("observedLicenseInfo.json");
        List<License> licenses = licenseProcessor.getLicensesFromJSON(data);
        assertEquals(1, licenses.size());
        assertEquals("Apache-2.0", licenses.get(0).getName());
    }

    @Test
    public void testUnspecified() throws IOException {
        String data = loadToString("unspecified.json");
        List<License> licenses = licenseProcessor.getLicensesFromJSON(data);
        assertEquals(0, licenses.size());
    }

    private static String loadFromFile(File file) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }


}