package ru.netology;


import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class Request {
    private final String[] requestLine;
    private final List<String> headers;
    private final String body;


    //for method POST
    public Request(String[] requestLine, List<String> headers, String body) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;
    }

    //for method GET
    public Request(String[] requestLine, List<String> headers) {
        this.requestLine = requestLine;
        this.headers = headers;
        body = "";
    }

    public List<NameValuePair> getQueryParams() {
        final List query;
        try {
            query = new URIBuilder(getPath(), Charset.defaultCharset())
                    .getQueryParams();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return query;
    }

    public List<NameValuePair> getQueryParam(String name) {
        return getQueryParams().stream()
                .filter(p -> p.getName().equals(name))
                .collect(Collectors.toList());
    }


    public String[] getRequestLine() {
        return requestLine;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return requestLine[0];
    }

    public String getPath() {
        return requestLine[1];
    }


    public String getBody() {
        return body;
    }

}
