package org.codehaus.mojo.license;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.mojo.license.LicenseMojoUtils.LoggerFacade;

public class MockLogger
    implements LoggerFacade
{
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile( "\\Q{}\\E" );

    private static final String format( String template, Object param )
    {
        return PLACEHOLDER_PATTERN.matcher( template )
                        .replaceFirst( Matcher.quoteReplacement( String.valueOf( param ) ) );
    }

    private final StringBuilder log = new StringBuilder();

    @Override
    public void debug( String template, Object param )
    {
        log.append( "DEBUG " ).append( format( template, param ) ).append( "\n" );
    }

    @Override
    public void warn( String string )
    {
        log.append( "WARN " ).append( string ).append( "\n" );
    }

    @Override
    public void warn( String template, Object param )
    {
        log.append( "WARN " ).append( format( template, param ) ).append( "\n" );
    }

    public String dump()
    {
        return log.toString();
    }

    public void reset()
    {
        log.setLength( 0 );
    }

}
