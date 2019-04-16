
/*
 * A script to start LittleProxy on port 18080.
 * Do not forget to kill it by System.setProperty('LittleProxy.stop', 'true') in the same JVM
 */

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.HttpFiltersAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.DefaultFullHttpResponse;


class KillerThread extends Thread {
    private final long timeoutMs = 20 * 1000 * 60
    private final long startTime = System.currentTimeMillis()
    private final HttpProxyServer server;
    public KillerThread( HttpProxyServer server )
    {
        super();
        this.server = server;
    }
    @Override
    public void run()
    {
        while (true) {
            if (System.getProperty("LittleProxy.stop") != null) {
                println "LittleProxy stopped via LittleProxy.stop system property"
                break
            }
            if (server.stopped.get()) {
                println "LittleProxy stopped by itself"
                break
            }
            if (System.currentTimeMillis() > startTime + timeoutMs) {
                println "LittleProxy timeouted"
                break
            }
            try
            {
                Thread.sleep( 100 );
                //println "LittleProxy is running"
            }
            catch ( InterruptedException e )
            {
                Thread.currentThread().interrupt();
            }
        }
        server.stop();
    }
}

final HttpProxyServer server =
        DefaultHttpProxyServer.bootstrap()
            .withPort(18080)
            .withFiltersSource(new HttpFiltersSourceAdapter() {
                @Override
                public int getMaximumResponseBufferSizeInBytes() {
                    return 2 * 1024 * 1024;
                }
                public HttpFilters filterRequest(HttpRequest req, ChannelHandlerContext ctx) {
                    if (io.netty.handler.codec.http.HttpMethod.GET == req.getMethod()
                            && 'http://www.apache.org/licenses/LICENSE-2.0.txt'.equals(req.getUri())) {
                        return new HttpFiltersAdapter(req) {
                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                return super.clientToProxyRequest(httpObject)
                            }

                            @Override
                            public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                println "LittleProxy overwrites the response body for URL 'http://www.apache.org/licenses/LICENSE-2.0.txt'"
                                final ByteBuf buffer = Unpooled.copiedBuffer('Proxied via LittleProxy', java.nio.charset.StandardCharsets.UTF_8);
                                final HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, io.netty.handler.codec.http.HttpResponseStatus.OK, buffer);
                                response.trailingHeaders().add(httpObject.headers())
                                return response;
                            }
                        };
                    }
                }
            })
            .start();
new KillerThread(server).start();
