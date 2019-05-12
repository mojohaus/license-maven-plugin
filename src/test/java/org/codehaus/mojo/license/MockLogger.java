package org.codehaus.mojo.license;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

public class MockLogger implements Log
{
    private boolean debugEnabled = true;
    private boolean infoEnabled = true;
    private boolean warnEnabled = true;
    private boolean errorEnabled = true;
    private List<String> messages = new ArrayList<>();

    @Override
    public boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    @Override
    public void debug( CharSequence content )
    {
        add( debugEnabled, "DEBUG", content );
    }

    @Override
    public void debug( CharSequence content, Throwable error )
    {
        add( debugEnabled, "DEBUG", content, error );
    }

    @Override
    public void debug( Throwable error )
    {
        add( debugEnabled, "DEBUG", error );
    }

    @Override
    public boolean isInfoEnabled()
    {
        return infoEnabled;
    }

    @Override
    public void info( CharSequence content )
    {
        add( infoEnabled, "INFO", content );
    }

    @Override
    public void info( CharSequence content, Throwable error )
    {
        add( infoEnabled, "INFO", content, error );
    }

    @Override
    public void info( Throwable error )
    {
        add( infoEnabled, "INFO", error );
    }

    @Override
    public boolean isWarnEnabled()
    {
        return warnEnabled;
    }

    @Override
    public void warn( CharSequence content )
    {
        add( warnEnabled, "WARN", content );
    }

    @Override
    public void warn( CharSequence content, Throwable error )
    {
        add( warnEnabled, "WARN", content, error );
    }

    @Override
    public void warn( Throwable error )
    {
        add( warnEnabled, "WARN", error );
    }

    @Override
    public boolean isErrorEnabled()
    {
        return errorEnabled;
    }

    @Override
    public void error( CharSequence content )
    {
        add( errorEnabled, "ERROR", content );
    }

    @Override
    public void error( CharSequence content, Throwable error )
    {
        add( errorEnabled, "ERROR", content, error );
    }

    @Override
    public void error( Throwable error )
    {
        add( errorEnabled, "ERROR", error );
    }

    // Builder pattern initialization

    public MockLogger enableDebug( boolean enable )
    {
        debugEnabled = enable;
        return this;
    }

    public MockLogger enableInfo( boolean enable )
    {
        infoEnabled = enable;
        return this;
    }

    public MockLogger enableWarn( boolean enable )
    {
        warnEnabled = enable;
        return this;
    }

    public MockLogger enableError( boolean enable )
    {
        errorEnabled = enable;
        return this;
    }

    private void add(boolean enabled, String level, Throwable error) {
        add(enabled, level, null, error);
    }

    private void add(boolean enabled, String level, CharSequence content) {
        add(enabled, level, content, null);
    }

    private void add(boolean enabled, String level, CharSequence content, Throwable error) {
        if (!enabled) {
            return;
        }

        StringBuilder buffer = new StringBuilder(level);
        if (content != null) {
            buffer.append( ' ' ).append(content);
        }

        if (error != null) {
            buffer.append( "\n" ).append(error.toString());
        }

        messages.add( buffer.toString() );
    }

    public List<String> getMessages()
    {
        return messages;
    }

    public String dump()
    {
        return StringUtils.join( messages.iterator(), "\n" );
    }

    public void reset()
    {
        messages.clear();
    }
}
