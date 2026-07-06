package com.nekoflow.backend.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalizacao de texto para busca: minusculo, sem acentos, apenas alfanumerico
 * (ASCII) separado por espaco. Usada tanto para montar a coluna anime.search_index
 * (na escrita) quanto para normalizar a query do usuario (na leitura), garantindo
 * que ambos os lados batam. Preserva a busca acento-insensivel que existia antes.
 */
public final class SearchText {

    private SearchText() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return stripped.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{Alnum}]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }
}
