package com.nosoftskills.lineup.matching;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OllamaEmbeddingClientTest {

    // Injects the real CDI-managed client, configured (application.properties) to point at
    // localhost:11434 -- nothing listens there in the test sandbox, so this exercises a genuine
    // "Ollama unreachable" fallback rather than a mock.
    @Inject
    OllamaEmbeddingClient unreachableClient;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void embedParsesEmbeddingVectorFromOllamaResponse() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/embeddings", exchange -> {
            byte[] body = "{\"embedding\":[0.1,0.2,0.3]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        OllamaRestApi restApi = RestClientBuilder.newBuilder()
                .baseUri(URI.create("http://localhost:" + server.getAddress().getPort()))
                .build(OllamaRestApi.class);
        OllamaEmbeddingClient client = new OllamaEmbeddingClient(restApi, "nomic-embed-text");

        Optional<double[]> result = client.embed("Ivan Ivanov");

        assertTrue(result.isPresent());
        assertArrayEquals(new double[] {0.1, 0.2, 0.3}, result.get(), 1e-9);
    }

    @Test
    void embedReturnsEmptyWhenOllamaIsUnreachableInsteadOfThrowing() {
        Optional<double[]> result = unreachableClient.embed("Ivan Ivanov");

        assertTrue(result.isEmpty());
    }

    @Test
    void embedReturnsEmptyWhenServerRespondsWithNoEmbedding() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/embeddings", exchange -> {
            byte[] body = "{\"embedding\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        OllamaRestApi restApi = RestClientBuilder.newBuilder()
                .baseUri(URI.create("http://localhost:" + server.getAddress().getPort()))
                .build(OllamaRestApi.class);
        OllamaEmbeddingClient client = new OllamaEmbeddingClient(restApi, "nomic-embed-text");

        Optional<double[]> result = client.embed("Ivan Ivanov");

        assertTrue(result.isEmpty());
    }
}
