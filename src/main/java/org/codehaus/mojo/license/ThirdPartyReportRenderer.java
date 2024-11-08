package org.codehaus.mojo.license;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.doxia.sink.Sink;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.plexus.i18n.I18N;

/**
 * Generates a report of third parties of the project.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.1
 */
public class ThirdPartyReportRenderer extends AbstractLicenseReportRenderer {
    private final Collection<ThirdPartyDetails> details;

    public ThirdPartyReportRenderer(
            Sink sink, I18N i18n, String outputName, Locale locale, Collection<ThirdPartyDetails> details) {
        super(sink, outputName, i18n, locale);
        this.details = details;
    }

    protected Collection<ThirdPartyDetails> getThirdPartiesPomLicense() {
        Collection<ThirdPartyDetails> result = new ArrayList<>();
        for (ThirdPartyDetails detail : details) {
            if (detail.hasPomLicenses()) {
                result.add(detail);
            }
        }
        return result;
    }

    protected Collection<ThirdPartyDetails> getThirdPartiesThirdPartyLicense() {
        Collection<ThirdPartyDetails> result = new ArrayList<>();
        for (ThirdPartyDetails detail : details) {
            if (detail.hasThirdPartyLicenses()) {
                result.add(detail);
            }
        }
        return result;
    }

    protected Collection<ThirdPartyDetails> getThirdPartiesNoLicense() {
        Collection<ThirdPartyDetails> result = new ArrayList<>();
        for (ThirdPartyDetails detail : details) {
            if (!detail.hasLicenses()) {
                result.add(detail);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    protected void renderBody() {

        startSection(getText("report.overview.title"));
        paragraph(getText("report.overview.text"));

        renderSummaryTotalsTable(details);

        renderSummaryTable(details);

        endSection();

        Collection<ThirdPartyDetails> dependencies;

        // With no licenses dependencies

        dependencies = getThirdPartiesNoLicense();
        startSection(getText("report.detail.title.noLicense"));
        sink.paragraph();
        sink.text(getText("report.detail.text.noLicense"));
        if (CollectionUtils.isEmpty(dependencies)) {
            sink.lineBreak();
            sink.text(getText("report.detail.text.emptyList"));
        }
        sink.paragraph_();

        for (final ThirdPartyDetails detail : dependencies) {
            renderThirdPartyDetail(detail);
        }
        endSection();

        // With third-party licenses dependencies

        dependencies = getThirdPartiesThirdPartyLicense();
        startSection(getText("report.detail.title.thirdPartyLicense"));
        sink.paragraph();
        sink.text(getText("report.detail.text.thirdPartyLicense"));
        if (CollectionUtils.isEmpty(dependencies)) {
            sink.lineBreak();
            sink.text(getText("report.detail.text.emptyList"));
        }
        sink.paragraph_();

        for (final ThirdPartyDetails detail : dependencies) {
            renderThirdPartyDetail(detail);
        }
        endSection();

        // With no pom dependencies

        dependencies = getThirdPartiesPomLicense();
        startSection(getText("report.detail.title.pomLicense"));
        sink.paragraph();
        sink.text(getText("report.detail.text.pomLicense"));
        if (CollectionUtils.isEmpty(dependencies)) {
            sink.lineBreak();
            sink.text(getText("report.detail.text.emptyList"));
        }
        sink.paragraph_();

        for (final ThirdPartyDetails detail : dependencies) {
            renderThirdPartyDetail(detail);
        }
        endSection();
    }

    private void renderSummaryTotalsTable(Collection<ThirdPartyDetails> allThirdParties) {
        int numWithPomLicense = 0;
        int numWithThirdPartyLicense = 0;
        int numWithNoLicense = 0;
        for (ThirdPartyDetails detail : allThirdParties) {
            if (detail.hasPomLicenses()) {
                numWithPomLicense++;
            } else if (detail.hasThirdPartyLicenses()) {
                numWithThirdPartyLicense++;
            } else {
                numWithNoLicense++;
            }
        }
        startTable();

        sink.tableRow();
        sink.tableCell();
        renderInfoIcon();
        sink.tableCell_();
        tableCell(getText("report.overview.numThirdParties"));
        tableCell(Integer.toString(allThirdParties.size()));
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        renderSuccessIcon();
        sink.tableCell_();
        tableCell(getText("report.overview.numWithPomLicenses"));
        tableCell(Integer.toString(numWithPomLicense));
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        renderWarningIcon();
        sink.tableCell_();
        tableCell(getText("report.overview.numWithThirdPartyLicenses"));
        tableCell(Integer.toString(numWithThirdPartyLicense));
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        renderErrorIcon();
        sink.tableCell_();
        tableCell(getText("report.overview.numWithNoLicense"));
        tableCell(Integer.toString(numWithNoLicense));
        sink.tableRow_();
        endTable();
    }

    private void renderSummaryTable(Collection<ThirdPartyDetails> contents) {
        startSection(getText("report.overview.thirdParty"));

        if (contents.isEmpty()) {
            paragraph(getText("report.overview.nothirdParty"));
        } else {
            renderThirdPartySummaryTable(contents);
        }
        endSection();
    }

    private void renderThirdPartyDetail(ThirdPartyDetails detail) {
        startSection(getGAV(detail));
        renderThirdPartyDetailTable(detail);

        sink.link("#" + getText("report.overview.title"));
        sink.text(getText("report.back.to.top.page"));
        sink.link_();
        sink.lineBreak();
        endSection();
    }
}
