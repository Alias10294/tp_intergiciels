package org.isihop.fr.perTranslateService;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageParser {

    private static final Logger logger = LoggerFactory.getLogger(MessageParser.class);

    public Optional<ParsedMessage> parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            logger.warn("Message vide ignoré");
            return Optional.empty();
        }

        String[] parts = rawMessage.split("#", 3);

        if (parts.length != 3) {
            logger.warn("Message ignoré, format invalide : {}", rawMessage);
            return Optional.empty();
        }

        if (!parts[0].startsWith("FROM:") || !parts[1].startsWith("TO:")) {
            logger.warn("Message ignoré, entêtes invalides : {}", rawMessage);
            return Optional.empty();
        }

        String from = parts[0].substring("FROM:".length()).trim();
        String to = parts[1].substring("TO:".length()).trim();
        String content = cleanContent(parts[2]);

        if (from.isBlank() || to.isBlank()) {
            logger.warn("Message ignoré, expéditeur ou destinataire vide : {}", rawMessage);
            return Optional.empty();
        }

        return Optional.of(new ParsedMessage(from, to, content));
    }

    public String format(ParsedMessage message, String translatedContent) {
        return "FROM:" + message.from()
                + "#TO:" + message.to()
                + "#" + translatedContent;
    }

    private String cleanContent(String content) {
        String cleaned = content == null ? "" : content.trim();

        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned;
    }
}