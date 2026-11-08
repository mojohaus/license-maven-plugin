package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 Captain-P-Goldfish, Falco Nikolas
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
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.maven.settings.Proxy;

/**
 * Applies a proxy from {@code settings.xml} to an Apache HttpClient, so that every download of the plugin goes through
 * the same proxy code path.
 */
public final class HttpClientProxy {

    private HttpClientProxy() {}

    /**
     * Finds the proxy to use among the proxies declared in {@code settings.xml}.
     *
     * @param proxies the declared proxies, may be {@code null}
     * @return the first active {@code http} proxy, or {@code null} if there is none
     */
    public static Proxy findActiveProxy(Collection<Proxy> proxies) {
        if (proxies != null) {
            for (Proxy proxy : proxies) {
                if (proxy.isActive() && "http".equals(proxy.getProtocol())) {
                    return proxy;
                }
            }
        }
        return null;
    }

    /**
     * Configures the proxy, its credentials and its non-proxy hosts on the given builders. Does nothing if
     * {@code proxy} is {@code null}. The caller still has to pass the built {@code requestConfig} to
     * {@link HttpClientBuilder#setDefaultRequestConfig(RequestConfig)}.
     *
     * @param proxy the proxy, may be {@code null}
     * @param requestConfig the request configuration to set the proxy host on
     * @param clientBuilder the client builder to set credentials and the route planner on
     */
    public static void configure(Proxy proxy, RequestConfig.Builder requestConfig, HttpClientBuilder clientBuilder) {
        if (proxy == null) {
            return;
        }
        requestConfig.setProxy(new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getProtocol()));
        if (proxy.getUsername() != null && proxy.getPassword() != null) {
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            final Credentials creds = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
            credsProvider.setCredentials(new AuthScope(proxy.getHost(), proxy.getPort()), creds);
            clientBuilder.setDefaultCredentialsProvider(credsProvider);
        }
        final String rawNonProxyHosts = proxy.getNonProxyHosts();
        if (rawNonProxyHosts != null) {
            final String[] nonProxyHosts = rawNonProxyHosts.split("|");
            if (nonProxyHosts.length > 0) {
                final List<Pattern> nonProxyPatterns = new ArrayList<>();
                for (String nonProxyHost : nonProxyHosts) {
                    final Pattern pat = Pattern.compile(
                            nonProxyHost.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*"), Pattern.CASE_INSENSITIVE);
                    nonProxyPatterns.add(pat);
                }
                final HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
                final HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost) {

                    @Override
                    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context)
                            throws HttpException {
                        for (Pattern pattern : nonProxyPatterns) {
                            if (pattern.matcher(target.getHostName()).matches()) {
                                return null;
                            }
                        }
                        return super.determineProxy(target, request, context);
                    }
                };
                clientBuilder.setRoutePlanner(routePlanner);
            }
        }
    }
}
