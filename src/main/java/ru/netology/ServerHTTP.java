package ru.netology;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerHTTP {
    public static final String GET = "GET";
    public static final String POST = "POST";

    private final int limit = 4096;
    private final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
    private final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
    private final int threadPools = 64;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap();

    public void addHandlers(String method, String path, Handler handler) {
        handlers.putIfAbsent(method, new ConcurrentHashMap<>());
        var methodMap = handlers.get(method);
        methodMap.put(path, handler);
    }

    public void notFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void successfulRequestForGET(BufferedOutputStream out) throws IOException {
        String msg = "Hello from GET /messages";
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + "text/plain" + "\r\n" +
                        "Content-Length: " + msg.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(msg.getBytes());
        out.flush();
    }

    public static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void successfulRequestForPOST(BufferedOutputStream out) throws IOException {
        String msg = "Hello from POST /messages";
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + "text/plain" + "\r\n" +
                        "Content-Length: " + msg.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(msg.getBytes());
        out.flush();
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private String findBody(BufferedInputStream in, BufferedOutputStream out, int requestLineEnd, byte[] buffer, int read) throws IOException {
        final var headers = findHeaders(in, out, requestLineEnd, buffer, read);
        final var contentLength = extractHeader(headers, "Content-Length");
        if (contentLength.isPresent()) {
            final var length = Integer.parseInt(contentLength.get());
            final var bodyBytes = in.readNBytes(length);
            final var body = new String(bodyBytes);
            return body;
        }
        return null;
    }

    private int findRequestLine(int read, byte[] buffer) throws IOException {
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        return requestLineEnd;
    }

    private List<String> findHeaders(BufferedInputStream in, BufferedOutputStream out, int requestLineEnd, byte[] buffer, int read) throws IOException {
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            return null;
        }
        in.reset();
        in.skip(headersStart);
        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        //System.out.println(headers);
        return headers;
    }

    private int actionHandlers(BufferedInputStream in, BufferedOutputStream out, byte[] buffer, int read) {
        final int success = 1;
        final int error = -1;
        try {
            final var requestLineEnd = findRequestLine(read, buffer);
            if (requestLineEnd == -1) {
                notFound(out);
                return error;
            }
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                notFound(out);
                return error;
            }
            Request request = new Request(requestLine, findHeaders(in, out, requestLineEnd, buffer, read));

            //вывод query
            request.getQueryParams().stream()
                    .forEach(System.out::println);

            var methodMap = handlers.get(request.getMethod());
            if (methodMap == null) {
                notFound(out);
                return error;
            }
            var handler = methodMap.get(request.getPath());
            if (handler == null) {
                notFound(out);
                return error;
            }
            handler.handle(request, out);


        } catch (IOException e) {
            e.printStackTrace();
        }

        return success;
    }

    private void connection(ServerSocket serverSocket) {
        while (true) {
            try (final var socket = serverSocket.accept();
                 final var in = new BufferedInputStream(socket.getInputStream());
                 final var out = new BufferedOutputStream(socket.getOutputStream())
            ) {
                in.mark(limit);
                final byte[] buffer = new byte[limit];
                final int read = in.read(buffer);
                if (actionHandlers(in, out, buffer, read) == -1) {
                    continue;
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void startServer(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                ExecutorService executorService = Executors.newFixedThreadPool(threadPools);
                executorService.submit(new Thread(() -> connection(serverSocket)));
                executorService.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
