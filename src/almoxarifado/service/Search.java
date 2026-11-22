package almoxarifado.service;

import java.text.Normalizer;

/** Busca textual sem acento e sem diferenciar maiúsculas (porta do StringUtil.deaccent da API). */
final class Search {
    private Search() {
    }

    static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** true se a query for vazia ou se todas as suas palavras aparecerem em text. */
    static boolean matchesAllTokens(String text, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedText = normalize(text);
        for (String token : normalize(query).trim().split("\\s+")) {
            if (!normalizedText.contains(token)) {
                return false;
            }
        }
        return true;
    }
}
