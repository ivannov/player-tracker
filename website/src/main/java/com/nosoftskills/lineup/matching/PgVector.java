package com.nosoftskills.lineup.matching;

/**
 * Formats a Java vector as a pgvector text literal (e.g. "[0.1,0.2,0.3]") for use with
 * {@code CAST(? AS vector)} in native queries -- there's no Hibernate ORM mapping for the
 * players.name_embedding column, so all reads/writes of it go through native SQL.
 */
final class PgVector {

    private PgVector() {
    }

    static String toLiteral(double[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
