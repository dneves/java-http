package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;

public class App {
    public static void main( String[] args ) {
        Logger logger = LogManager.getLogger( "CONSOLE_JSON_APPENDER" );
        logger.warn( "starting up server ...");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final OkHttpClient httpClient = new OkHttpClient();

        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler( Handlers.routing()
                        .get("/", new HttpHandler() {
                            @Override
                            public void handleRequest(HttpServerExchange exchange) throws Exception {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                exchange.getResponseSender().send("get /");
                            }
                        })
                        .get("/{id}", new HttpHandler() {
                            @Override
                            public void handleRequest(HttpServerExchange exchange) throws Exception {
                                Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
                                Deque<String> ids = queryParameters.get("id");
                                String id = ids.getFirst();

                                Request request = new Request.Builder()
                                        .url("https://reqres.in/api/users/"+id)
                                        .build();

                                try (Response response = httpClient.newCall(request).execute()) {
                                    if (!response.isSuccessful()) {
                                        throw new IOException("Unexpected code " + response);
                                    }

                                    ResponseBody responseBody = response.body();
                                    if ( responseBody == null ) {
                                        throw new IOException( "expecting a body");
                                    }

                                    Map< String, Object > fromJson = gson.<Map< String, Object >>fromJson(responseBody.string(), Map.class);


                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                    exchange.getResponseSender().send( gson.toJson( fromJson ) );

                                }

                            }
                        })
                        .post("/", new HttpHandler() {
                            @Override
                            public void handleRequest(HttpServerExchange exchange) throws Exception {
                                exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                    @Override
                                    public void handle(HttpServerExchange httpServerExchange, String body) {
                                        Map<String, Object> json = gson.<Map< String, Object>>fromJson(body, Map.class);

                                        httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                        httpServerExchange.getResponseSender().send("post / : " + json.get( "name" ) );
                                    }
                                }, java.nio.charset.StandardCharsets.UTF_8);

                            }
                        })
                )
                .build();
        server.start();
    }
}
