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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.doxia.sink.Sink;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.plexus.i18n.I18N;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * Generates a report of third parties of the project.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public class ThirdPartyReportRenderer
    extends AbstractLicenseReportRenderer
{
    private final Collection<ThirdPartyDetails> details;

    public ThirdPartyReportRenderer( Sink sink, I18N i18n, String outputName, Locale locale,
                                     Collection<ThirdPartyDetails> details )
    {
        super( sink, outputName, i18n, locale );
        this.details = details;
    }

    protected Collection<ThirdPartyDetails> getThirdPartiesPomLicense()
    {
        Collection<ThirdPartyDetails> result = new ArrayList<ThirdPartyDetails>();
        for ( ThirdPartyDetails detail : details )
        {
            if ( detail.hasPomLicenses() )
            {
                result.add( detail );
            }
        }
        return result;
    }

    protected Collection<ThirdPartyDetails> getThirdPartiesThirdPartyLicense()
    {
        Collection<ThirdPartyDetails> result = new ArrayList<ThirdPartyDetails>();
        for ( ThirdPartyDetails detail : details )
        {
            if ( detail.hasThirdPartyLicenses() )
            {
                result.add( detail );
            }
        }
        return result;
    }

    protected Collection<ThirdPartyDetails> getThirdPartiesNoLicense()
    {
        Collection<ThirdPartyDetails> result = new ArrayList<ThirdPartyDetails>();
        for ( ThirdPartyDetails detail : details )
        {
            if ( !detail.hasLicenses() )
            {
                result.add( detail );
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    protected void renderBody()
    {

        sink.section1();
        sink.sectionTitle1();
        sink.text( getText( "report.overview.title" ) );
        sink.sectionTitle1_();
        sink.paragraph();
        sink.text( getText( "report.overview.text" ) );
        sink.paragraph_();

        renderSummaryTotalsTable( details );

        renderSummaryTable( "report.overview.thirdParty", details, "report.overview.nothirdParty" );

        sink.section1_();

        Collection<ThirdPartyDetails> dependencies;

        // With no licenses dependencies

        dependencies = getThirdPartiesNoLicense();
        sink.section1();
        sink.sectionTitle1();
        sink.text( getText( "report.detail.title.noLicense" ) );
        sink.sectionTitle1_();
        sink.paragraph();
        sink.text( getText( "report.detail.text.noLicense" ) );
        if ( CollectionUtils.isEmpty( dependencies ) )
        {
            sink.lineBreak();
            sink.text( getText( "report.detail.text.emptyList" ) );
        }
        sink.paragraph_();

        for ( final ThirdPartyDetails detail : dependencies )
        {
            renderThirdPartyDetail( detail );
        }
        sink.section1_();

        // With third-party licenses dependencies

        dependencies = getThirdPartiesThirdPartyLicense();
        sink.section1();
        sink.sectionTitle1();
        sink.text( getText( "report.detail.title.thirdPartyLicense" ) );
        sink.sectionTitle1_();
        sink.paragraph();
        sink.text( getText( "report.detail.text.thirdPartyLicense" ) );
        if ( CollectionUtils.isEmpty( dependencies ) )
        {
            sink.lineBreak();
            sink.text( getText( "report.detail.text.emptyList" ) );
        }
        sink.paragraph_();

        for ( final ThirdPartyDetails detail : dependencies )
        {
            renderThirdPartyDetail( detail );
        }
        sink.section1_();

        // With no pom dependencies

        dependencies = getThirdPartiesPomLicense();
        sink.section1();
        sink.sectionTitle1();
        sink.text( getText( "report.detail.title.pomLicense" ) );
        sink.sectionTitle1_();
        sink.paragraph();
        sink.text( getText( "report.detail.text.pomLicense" ) );
        if ( CollectionUtils.isEmpty( dependencies ) )
        {
            sink.lineBreak();
            sink.text( getText( "report.detail.text.emptyList" ) );
        }
        sink.paragraph_();

        for ( final ThirdPartyDetails detail : dependencies )
        {
            renderThirdPartyDetail( detail );
        }
        sink.section1_();
    }

    private void renderSummaryTotalsTable( Collection<ThirdPartyDetails> allThirdParties )
    {
        int numWithPomLicense = 0;
        int numWithThirdPartyLicense = 0;
        int numWithNoLicense = 0;
        for ( ThirdPartyDetails details : allThirdParties )
        {
            if ( details.hasPomLicenses() )
            {
                numWithPomLicense++;
            }
            else if ( details.hasThirdPartyLicenses() )
            {
                numWithThirdPartyLicense++;
            }
            else
            {
                numWithNoLicense++;
            }
        }
        sink.table();

        sink.tableRow();
        sink.tableCell();
        renderInfoIcon();
        sink.tableCell_();
        sinkCellText( getText( "report.overview.numThirdParties" ) );
        sinkCellText( Integer.toString( allThirdParties.size() ) );
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        renderSuccessIcon();
        sink.tableCell_();
        sinkCellText( getText( "report.overview.numWithPomLicenses" ) );
        sinkCellText( Integer.toString( numWithPomLicense ) );
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        renderWarningIcon();
        sink.tableCell_();
        sinkCellText( getText( "report.overview.numWithThirdPartyLicenses" ) );
        sinkCellText( Integer.toString( numWithThirdPartyLicense ) );
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        renderErrorIcon();
        sink.tableCell_();
        sinkCellText( getText( "report.overview.numWithNoLicense" ) );
        sinkCellText( Integer.toString( numWithNoLicense ) );
        sink.tableRow_();
        sink.table_();
    }

    private void renderSummaryTable( String titleKey, Collection<ThirdPartyDetails> contents, String emptyKey )
    {
        sink.section2();
        sink.sectionTitle2();
        sink.text( getText( titleKey ) );
        sink.sectionTitle2_();

        if ( contents.isEmpty() )
        {
            sink.paragraph();
            sink.text( getText( emptyKey ) );
            sink.paragraph_();
        }
        else
        {
            renderThirdPartySummaryTable( contents );
        }
        sink.section2_();
    }

    private void renderThirdPartyDetail( ThirdPartyDetails details )
    {
        sink.section2();
        sink.sectionTitle2();
        sink.text( getGAV( details ) );
        sink.sectionTitle2_();
        renderThirdPartyDetailTable( details );

        sink.link( "./third-party-report.html#" + getText( "report.overview.title" ) );
        sink.text( getText( "report.back.to.top.page" ) );
        sink.link_();
        sink.lineBreak();
        sink.section2_();
    }


}
