package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 Codehaus
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

import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.api.DefaultThirdPartyDetails;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.plexus.i18n.I18N;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LicenseLinkRenderingTest {

    private static ThirdPartyDetails details(License... licenses) {
        final MavenProject project = new MavenProject();
        for (License license : licenses) {
            project.getModel().addLicense(license);
        }
        return new DefaultThirdPartyDetails(project);
    }

    private static License license(String name, String url) {
        final License license = new License();
        license.setName(name);
        license.setUrl(url);
        return license;
    }

    @Test
    void licenseUrlIsMatchedByName() {
        final ThirdPartyDetails details = details(
                license("Apache License, Version 2.0", " https://www.apache.org/licenses/LICENSE-2.0.txt "),
                license("No URL", null));

        assertEquals(
                "https://www.apache.org/licenses/LICENSE-2.0.txt",
                details.getLicenseUrl("Apache License, Version 2.0"));
        assertNull(details.getLicenseUrl("No URL"));
        assertNull(details.getLicenseUrl("Unknown"));
        assertNull(details.getLicenseUrl(null));
    }

    @Test
    void rendersLinkWhenUrlIsKnown() {
        final Sink sink = mock(Sink.class);
        final ThirdPartyDetails details = details(license("MIT", "https://opensource.org/licenses/MIT"));

        renderer(sink).renderLicense(details, "MIT");

        final InOrder order = inOrder(sink);
        order.verify(sink).link("https://opensource.org/licenses/MIT");
        order.verify(sink).text("MIT");
        order.verify(sink).link_();
    }

    @Test
    void rendersPlainTextWhenNoUrlIsKnown() {
        final Sink sink = mock(Sink.class);

        renderer(sink).renderLicense(details(license("MIT", null)), "MIT");

        verify(sink).text("MIT");
        verify(sink, never()).link(org.mockito.ArgumentMatchers.anyString());
    }

    private static ThirdPartyReportRenderer renderer(Sink sink) {
        return new ThirdPartyReportRenderer(sink, mock(I18N.class), "third-party-report", Locale.ENGLISH, null);
    }
}
