package com.duosecurity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.duosecurity.exception.DuoException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

class DuoConnectorPersistentConnectionTest {

    private static final String API_HOST = "my_api_host.com";
    private static final String[] CA_CERT = {
        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
    };

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private DuoConnector createConnector(boolean usePersistentConnections) throws DuoException {
        DuoConnector connector = new DuoConnector(API_HOST, null, null, CA_CERT,
                usePersistentConnections);
        OkHttpClient existingClient = (OkHttpClient) connector.retrofit.callFactory();
        connector.retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(JacksonConverterFactory.create())
                .client(existingClient)
                .build();
        return connector;
    }

    @Test
    void persistentConnectionsDisabled_sendsConnectionCloseHeader() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"stat\":\"OK\",\"message\":\"success\",\"message_detail\":\"\"}"));

        DuoConnector connector = createConnector(false);
        connector.duoHealthcheck("client_id", "client_assertion");

        RecordedRequest recorded = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("close", recorded.getHeader("Connection"));
    }

    @Test
    void persistentConnectionsEnabled_doesNotSendConnectionCloseHeader() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"stat\":\"OK\",\"message\":\"success\",\"message_detail\":\"\"}"));

        DuoConnector connector = createConnector(true);
        connector.duoHealthcheck("client_id", "client_assertion");

        RecordedRequest recorded = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("Keep-Alive", recorded.getHeader("Connection"));
    }
}
