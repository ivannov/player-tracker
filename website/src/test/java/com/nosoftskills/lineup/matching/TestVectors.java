package com.nosoftskills.lineup.matching;

/** Builds 768-dim test vectors (matching players.name_embedding vector(768)) from a few values. */
final class TestVectors {

    private TestVectors() {
    }

    static double[] embedding(double... leadingValues) {
        double[] vector = new double[768];
        System.arraycopy(leadingValues, 0, vector, 0, leadingValues.length);
        return vector;
    }
}
