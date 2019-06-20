package org.codehaus.mojo.license.logback;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import ch.qos.logback.core.AppenderBase;

public class ListAppender<E> extends AppenderBase<E>
{

    public final List<Pair<E, String>> list = new ArrayList<>();
    private Function<E, String> toString;

    public ListAppender( Function<E, String> toString )
    {
        this.toString = toString;
    }

    @Override
    protected void append( E e )
    {
        String string = toString.apply( e );
        list.add( Pair.of( e, string ) );
    }
}
