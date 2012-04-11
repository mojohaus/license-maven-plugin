/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 CodeLutin, Codehaus, Tony Chemit
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

package org.codehaus.mojo.license;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.plexus.i18n.I18N;

import java.util.Collection;
import java.util.Locale;

/**
 * Base class for report renderers.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public abstract class AbstractLicenseReportRenderer
        extends AbstractMavenReportRenderer {
    /**
     * Internationalization component.
     *
     * @since 1.1
     */
    protected final I18N i18n;

    /**
     * The locale we are rendering for.
     *
     * @since 1.1
     */
    protected final Locale locale;

    /**
     * The name of the bundle containing our I18n resources.
     *
     * @since 1.1
     */
    protected final String bundleName;

    public AbstractLicenseReportRenderer(org.apache.maven.doxia.sink.Sink sink, String bundleName, I18N i18n,
                                         Locale locale) {
        super(sink);
        this.bundleName = bundleName;
        this.i18n = i18n;
        this.locale = locale;
    }

    /** {@inheritDoc} */
    public String getTitle() {
        return getText("report.title");
    }

    /**
     * Gets the localized message for this report.
     *
     * @param key the message key.
     * @return the message.
     */
    public String getText(String key) {
        return i18n.getString(bundleName, locale, key);
    }

    protected void renderWarningIcon() {
        sink.figure();
        sink.figureGraphics("images/icon_warning_sml.gif");
        sink.figure_();
    }

    protected void renderErrorIcon() {
        sink.figure();
        sink.figureGraphics("images/icon_error_sml.gif");
        sink.figure_();
    }

    protected void renderSuccessIcon() {
        sink.figure();
        sink.figureGraphics("images/icon_success_sml.gif");
        sink.figure_();
    }

    protected void renderInfoIcon() {
        sink.figure();
        sink.figureGraphics("images/icon_info_sml.gif");
        sink.figure_();
    }

    protected String getGAV(ThirdPartyDetails details) {
        return ArtifactUtils.versionlessKey(details.getGroupId(), details.getArtifactId()) + ":" +
               details.getVersion();
    }

    protected void renderThirdPartySummaryTableHeader() {
        renderThirdPartySummaryTableHeader(true, true, true);
    }

    protected void renderThirdPartySummaryTableHeader(boolean includeScope, boolean includeClassifier,
                                                      boolean includeType) {
        sink.tableRow();
        sinkHeaderCellText(getText("report.status"));
        sinkHeaderCellText(getText("report.gav"));
//        sink.tableHeaderCell();
//        sink.text( getText( "report.artifactId" ) );
//        sink.tableHeaderCell_();
//        sink.tableHeaderCell();
//        sink.text( getText( "report.version" ) );
//        sink.tableHeaderCell_();
        if (includeScope) {
            sinkHeaderCellText(getText("report.scope"));
        }
        if (includeClassifier) {
            sinkHeaderCellText(getText("report.classifier"));
        }
        if (includeType) {
            sinkHeaderCellText(getText("report.type"));
        }
        sinkHeaderCellText(getText("report.licenses"));
        sink.tableRow_();
    }

    protected void renderThirdPartySummaryTableRow(ThirdPartyDetails details) {
        renderThirdPartySummaryTableRow(details, true, true, true);
    }

    protected void sinkHeaderCellText(String text) {
        sink.tableHeaderCell();
        sink.text(text);
        sink.tableHeaderCell_();
    }

    protected void sinkHeaderCellText(String width, String text) {
        sink.tableHeaderCell(width);
        sink.text(text);
        sink.tableHeaderCell_();
    }

    protected void sinkCellText(String width, String text) {
        sink.tableCell(width);
        sink.text(text);
        sink.tableCell_();
    }

    protected void sinkCellText(String text) {
        sink.tableCell();
        sink.text(text);
        sink.tableCell_();
    }

    protected void renderThirdPartySummaryTableRow(ThirdPartyDetails details, boolean includeScope,
                                                   boolean includeClassifier, boolean includeType) {
        sink.tableRow();
        sink.tableCell();
        if (details.hasPomLicenses()) {
            renderSuccessIcon();
        } else if (details.hasThirdPartyLicenses()) {
            renderWarningIcon();
        } else {
            renderErrorIcon();
        }

        sink.tableCell();
        String gav = getGAV(details);
        sink.link("./third-party-report.html#" + gav);
        sink.text(gav);
        sink.link_();

        sink.tableCell_();
        if (includeScope) {
            sinkCellText(details.getScope());
        }
        if (includeClassifier) {
            sinkCellText(details.getClassifier());
        }
        if (includeType) {
            sinkCellText(details.getType());
        }

        sink.tableCell();

        if (details.hasLicenses()) {
            String[] licenses = details.getLicenses();
            for (int i = 0; i < licenses.length; i++) {
                if (i > 0) {
                    sink.lineBreak();
                }
                sink.text(licenses[i]);
            }
        } else {
            sink.text("-");

        }
        sink.tableCell_();

        sink.tableRow_();
    }

    protected void safeBold() {
        try {
            sink.bold();
        } catch (NoSuchMethodError e) {
            // ignore Maven 2.1.0
        }
    }

    protected void safeBold_() {
        try {
            sink.bold_();
        } catch (NoSuchMethodError e) {
            // ignore Maven 2.1.0
        }
    }

    protected void safeItalic() {
        try {
            sink.italic();
        } catch (NoSuchMethodError e) {
            // ignore Maven 2.1.0
        }
    }

    protected void safeItalic_() {
        try {
            sink.italic_();
        } catch (NoSuchMethodError e) {
            // ignore Maven 2.1.0
        }
    }

    protected void renderThirdPartyDetailTable(ThirdPartyDetails details) {
        renderThirdPartyDetailTable(details, true, true, true);
    }

    protected void renderThirdPartyDetailTable(ThirdPartyDetails details, boolean includeScope,
                                               boolean includeClassifier, boolean includeType) {
        final String cellWidth = "80%";
        final String headerWidth = "20%";
        sink.table();
        sink.tableRows(new int[]{Parser.JUSTIFY_RIGHT, Parser.JUSTIFY_LEFT}, false);
        sink.tableRow();
        sinkHeaderCellText(headerWidth, getText("report.status"));
        sink.tableCell(cellWidth);
        if (details.hasPomLicenses()) {
            renderSuccessIcon();
            sink.nonBreakingSpace();
            sink.text(getText("report.status.licenseFromPom"));
        } else if (details.hasThirdPartyLicenses()) {
            renderWarningIcon();
            sink.nonBreakingSpace();
            sink.text(getText("report.status.licenseFromThirdParty"));
        } else {
            renderErrorIcon();
            sink.nonBreakingSpace();
            sink.text(getText("report.status.noLicense"));
        }
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sinkHeaderCellText(headerWidth, getText("report.gav"));
        sinkCellText(cellWidth, getGAV(details));
        sink.tableRow_();

        if (includeScope) {
            sink.tableRow();
            sinkHeaderCellText(headerWidth, getText("report.scope"));
            sinkCellText(cellWidth, details.getScope());
            sink.tableRow_();
        }
        if (includeClassifier) {
            sink.tableRow();
            sinkHeaderCellText(headerWidth, getText("report.classifier"));
            sinkCellText(cellWidth, details.getClassifier());
            sink.tableRow_();
        }
        if (includeType) {
            sink.tableRow();
            sinkHeaderCellText(headerWidth, getText("report.type"));
            sinkCellText(cellWidth, details.getType());
            sink.tableRow_();
        }
        String[] licenses = details.getLicenses();

        if (details.hasPomLicenses()) {
            sink.tableRow();
            sinkHeaderCellText(headerWidth, getText("report.licenses"));
            sink.tableCell(cellWidth);
            for (int i = 0; i < licenses.length; i++) {
                if (i > 0) {
                    sink.lineBreak();
                }
                sink.text(licenses[i]);

            }
            sink.tableCell_();
            sink.tableRow_();
        } else if (details.hasThirdPartyLicenses()) {
            sink.tableRow();
            sinkHeaderCellText(headerWidth, getText("report.licenses"));
            sink.tableCell(cellWidth);
            for (int i = 0; i < licenses.length; i++) {
                if (i > 0) {
                    sink.lineBreak();
                }
                sink.text(licenses[i]);

            }
            sink.tableCell_();
            sink.tableRow_();
        } else {
            sink.tableRow();
            sinkHeaderCellText(headerWidth, getText("report.licenses"));
            sinkCellText(cellWidth, getText("report.no.license"));
            sink.tableRow_();
        }
        sink.tableRows_();
        sink.table_();
    }

    protected void renderThirdPartySummaryTable(Collection<ThirdPartyDetails> collection) {
        renderThirdPartySummaryTable(collection, true, true, true);
    }

    protected void renderThirdPartySummaryTable(Collection<ThirdPartyDetails> collection, boolean includeScope,
                                                boolean includeClassifier, boolean includeType) {
        sink.table();
        renderThirdPartySummaryTableHeader(includeScope, includeClassifier, includeType);
        for (ThirdPartyDetails details : collection) {
            renderThirdPartySummaryTableRow(details, includeScope, includeClassifier, includeType);
        }
        renderThirdPartySummaryTableHeader(includeScope, includeClassifier, includeType);
        sink.table_();
    }

    protected void renderPropertySummaryTableHeader() {
        sink.tableRow();
        sinkHeaderCellText(getText("report.status"));
        sinkHeaderCellText(getText("report.property"));
        sinkHeaderCellText(getText("report.currentVersion"));
        sinkHeaderCellText(getText("report.nextVersion"));
        sinkHeaderCellText(getText("report.nextIncremental"));
        sinkHeaderCellText(getText("report.nextMinor"));
        sinkHeaderCellText(getText("report.nextMajor"));
        sink.tableRow_();
    }
}
