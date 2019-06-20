package org.codehaus.mojo.license.logback;

import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;

public class EventToString implements Function<ILoggingEvent, String> {

    private PatternLayout patternLayout = new PatternLayout();
    
    public EventToString() {
        this("%level %msg%n%ex");
    }
    
    public EventToString(String pattern) {
        Context context = (Context) LoggerFactory.getILoggerFactory();
        
        patternLayout.setContext(context);
        patternLayout.setPattern(pattern);
        patternLayout.setOutputPatternAsHeader(false);
        patternLayout.getInstanceConverterMap().put("ex", FirstThrowableProxyConverter.class.getName());
        
        patternLayout.start();
    }
    
    @Override
    public String apply(ILoggingEvent input) {
        
        if (null == input) {
            return "null";
        }
        
        String result = patternLayout.doLayout(input);
        return result;
    }

}
