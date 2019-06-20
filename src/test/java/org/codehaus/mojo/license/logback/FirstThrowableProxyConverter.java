package org.codehaus.mojo.license.logback;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;

/** Convert only the message of the first exception in a chain */
public class FirstThrowableProxyConverter extends ThrowableProxyConverter // NOSONAR squid:MaximumInheritanceDepth There is no way to influence the chain of super classes
{
    @Override
    protected String throwableProxyToString( IThrowableProxy tp )
    {
        return tp.getMessage() + CoreConstants.LINE_SEPARATOR;
    }
}
