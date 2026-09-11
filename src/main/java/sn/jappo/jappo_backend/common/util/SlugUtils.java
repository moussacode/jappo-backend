package sn.jappo.jappo_backend.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtils {

    // Motif Regex pour nettoyer les espaces et caractères spéciaux
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    /**
     * Transforme une chaîne de caractères (ex: "Pépinière & Co ! ") en slug (ex: "pepiniere-co")
     *
     * @param input Le texte brut
     * @return Le slug propre pour une URL
     */
    public static String makeSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // 1. Remplacer les espaces par des tirets
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");

        // 2. Transformer les lettres accentuées en lettres simples (ex: "é" -> "e", "ç" -> "c")
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");

        // 3. Passer tout en minuscules et retirer les tirets inutiles au début/fin
        return slug.toLowerCase(Locale.ENGLISH)
                   .replaceAll("^-+", "")  // Supprime les tirets au début
                   .replaceAll("-+$", ""); // Supprime les tirets à la fin
    }
}