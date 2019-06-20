package org.codehaus.mojo.license.logback;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

public class RedirectLogger
{

    private static class LogInfo
    {
        private Logger logger;
        private boolean additive;
        private AppenderAttachableImpl<ILoggingEvent> aai;
        private boolean installed = false;

        public LogInfo( Class<?> type )
        {
            this( LoggerFactory.getLogger( type ) );
        }

        public LogInfo( org.slf4j.Logger logger )
        {
            this.logger = (Logger) logger;
            additive = this.logger.isAdditive();
            aai = getPrivateField( this.logger, "aai" );
        }

        public void install( Appender<ILoggingEvent> a )
        {
            if ( installed )
            {
                throw new IllegalStateException( "Already installed" );
            }

            AppenderAttachableImpl<ILoggingEvent> replacement = new AppenderAttachableImpl<>();
            replacement.addAppender( a );

            logger.setAdditive( false );
            setPrivateField( logger, "aai", replacement );
            installed = true;
        }

        public void deinstall()
        {
            if ( !installed )
            {
                throw new IllegalStateException( "Not installed" );
            }

            logger.setAdditive( additive );
            setPrivateField( logger, "aai", aai );
            installed = false;
        }

        private void setPrivateField( Object instance, String name, Object value )
        {
            try
            {
                Field f = instance.getClass().getDeclaredField( name );
                f.setAccessible( true );

                f.set( instance, value );
            }
            catch ( Exception e )
            {
                throw new AssertionError( e );
            }
        }

        private <T> T getPrivateField( Object instance, String name )
        {
            try
            {
                Field f = instance.getClass().getDeclaredField( name );
                f.setAccessible( true );

                @SuppressWarnings("unchecked")
                T tmp = (T) f.get( instance );
                return tmp;
            }
            catch ( Exception e )
            {
                throw new AssertionError( e );
            }
        }
    }

    private List<LogInfo> infos = Lists.newArrayList();
    private Function<ILoggingEvent, String> toString = new EventToString();
    private ListAppender<ILoggingEvent> appender;

    public RedirectLogger( Class<?>... types )
    {
        infos = createLoggers( types );
    }

    public RedirectLogger( org.slf4j.Logger... loggers )
    {
        infos = createLoggers( loggers );
    }

    private List<LogInfo> createLoggers( org.slf4j.Logger[] loggers )
    {
        List<LogInfo> result = Lists.newArrayList();

        for ( org.slf4j.Logger logger : loggers )
        {
            result.add( new LogInfo( logger ) );
        }

        return result;
    }

    public RedirectLogger toString( Function<ILoggingEvent, String> toString )
    {
        this.toString = toString;
        return this;
    }

    private List<LogInfo> createLoggers( Class<?>[] types )
    {
        List<LogInfo> result = Lists.newArrayList();

        for ( Class<?> type : types )
        {
            result.add( new LogInfo( type ) );
        }

        return result;
    }

    public void install()
    {
        if ( infos.isEmpty() )
        {
            throw new IllegalStateException( "No loggers defined" );
        }

        appender = new ListAppender<>( toString );

        appender.start();

        for ( LogInfo item : infos )
        {
            item.install( appender );
        }
    }

    public void deinstall()
    {
        if ( infos.isEmpty() )
        {
            throw new IllegalStateException( "No loggers defined" );
        }

        for ( LogInfo item : infos )
        {
            item.deinstall();
        }

        appender.stop();
    }

    public String dump()
    {
        return dump( Level.INFO );
    }

    public String dump( Level level )
    {
        return dump( LevelFilter.greaterOrEqual( level ) );
    }

    public String dump( Predicate<ILoggingEvent> filter )
    {
        StringBuilder buffer = new StringBuilder();

        for ( Pair<ILoggingEvent, String> event : appender.list )
        {
            if ( filter.test( event.getKey() ) )
            {
                buffer.append( event.getValue() );
            }
        }

        return buffer.toString().replace( "\r\n", "\n" );
    }
}
