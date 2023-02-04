package ru.netology;


public class Main {
    public static void main(String[] args) {
        ServerHTTP serverHTTP = new ServerHTTP();
        serverHTTP.addHandlers(ServerHTTP.GET, "/messages", (request, out) -> {
            serverHTTP.successfulRequestForGET(out);
        });
        serverHTTP.addHandlers(ServerHTTP.POST, "/messages", (request, out) -> {
            serverHTTP.successfulRequestForPOST(out);
        });
        serverHTTP.startServer(9999);
    }
}
