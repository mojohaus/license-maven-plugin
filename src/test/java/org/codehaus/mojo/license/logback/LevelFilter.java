package org.codehaus.mojo.license.logback;

import com.google.common.base.Predicate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LevelFilter implements Predicate<ILoggingEvent>
{

    enum Comparison
    {
        GREATER_OR_EQUAL
        {
            @Override
            public boolean matches( Level l1, Level l2 )
            {
                return l1.levelInt >= l2.levelInt;
            }
        };

        public abstract boolean matches( Level l1, Level l2 );
    }

    private Level level;
    private Comparison comparison;

    public LevelFilter( Comparison comparison, Level level )
    {
        this.level = level;
        this.comparison = comparison;
    }

    @Override
    public boolean apply( ILoggingEvent input )
    {
        return comparison.matches( input.getLevel(), level );
    }

    public static LevelFilter greaterOrEqual( Level level )
    {
        return new LevelFilter( Comparison.GREATER_OR_EQUAL, level );
    }
}
