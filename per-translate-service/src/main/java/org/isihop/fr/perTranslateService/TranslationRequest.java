package org.isihop.fr.perTranslateService;

public record TranslationRequest(
        String q,
        String source,
        String target,
        String format
) {
}