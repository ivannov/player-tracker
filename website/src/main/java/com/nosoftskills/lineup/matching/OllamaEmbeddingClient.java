package com.nosoftskills.lineup.matching;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Computes name embeddings via a local Ollama instance. Never throws: if Ollama is unreachable,
 * not yet running, or returns something unusable, callers get an empty Optional and are expected
 * to fall back to trigram-only matching -- Ollama must never be a single point of failure for
 * basic name resolution.
 */
@ApplicationScoped
public class OllamaEmbeddingClient {

    private static final Logger LOG = Logger.getLogger(OllamaEmbeddingClient.class);

    private final OllamaRestApi restApi;
    private final String model;

    @Inject
    public OllamaEmbeddingClient(
            @RestClient OllamaRestApi restApi,
            @ConfigProperty(name = "lineup.ollama.embedding-model") String model) {
        this.restApi = restApi;
        this.model = model;
    }

    public Optional<double[]> embed(String text) {
        try {
            OllamaEmbeddingResponse response = restApi.embeddings(new OllamaEmbeddingRequest(model, text));
            if (response == null || response.embedding() == null || response.embedding().isEmpty()) {
                return Optional.empty();
            }
            double[] vector = new double[response.embedding().size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = response.embedding().get(i);
            }
            return Optional.of(vector);
        } catch (RuntimeException e) {
            LOG.warn("Ollama embedding request failed; falling back to trigram-only matching", e);
            return Optional.empty();
        }
    }
}
