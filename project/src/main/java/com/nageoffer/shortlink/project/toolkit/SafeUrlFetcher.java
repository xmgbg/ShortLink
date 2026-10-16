/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.project.toolkit;

import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Restricts server-side URL fetching to public HTTP(S) resources.
 */
public final class SafeUrlFetcher {

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BODY_SIZE = 1024 * 1024;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private SafeUrlFetcher() {
    }

    public static Document fetch(String url) throws IOException {
        URI current = parseAndValidate(url);
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            Connection.Response response = Jsoup.connect(current.toString())
                    .followRedirects(false)
                    .timeout(3000)
                    .maxBodySize(MAX_BODY_SIZE)
                    .userAgent("ShortLinkBot/1.0")
                    .execute();
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return response.parse();
            }
            if (statusCode < 300 || statusCode >= 400 || !response.hasHeader("Location")) {
                throw new IOException("Remote server returned HTTP " + statusCode);
            }
            current = parseAndValidate(current.resolve(response.header("Location")).toString());
        }
        throw new IOException("Too many redirects");
    }

    private static URI parseAndValidate(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new ClientException("仅支持公开的 HTTP 或 HTTPS 地址");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isPrivateAddress(address)) {
                    throw new ClientException("不允许访问本机或内网地址");
                }
            }
            return uri;
        } catch (URISyntaxException | IOException ex) {
            throw new ClientException("链接格式或域名解析错误");
        }
    }

    private static boolean isPrivateAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || uniqueLocalIpv6;
    }
}
