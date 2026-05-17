package org.isihop.fr.perTranslateService;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TranslationListener {

    private static final Logger logger = LoggerFactory.getLogger(TranslationListener.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MessageParser messageParser;
    private final RestClient restClient;

    @Value("${application.topicin}")
    private String topicIn;

    @Value("${libretranslate.url}")
    private String libreTranslateUrl;

    @Value("${libretranslate.source}")
    private String sourceLanguage;

    @Value("${libretranslate.target}")
    private String targetLanguage;

    @Value("${libretranslate.format}")
    private String format;

    public TranslationListener(
            KafkaTemplate<String, String> kafkaTemplate,
            MessageParser messageParser,
            RestClient.Builder restClientBuilder
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.messageParser = messageParser;
        this.restClient = restClientBuilder
                .build();
    }

    @KafkaListener(topics = "${application.topicout}")
    public void consume(String rawMessage) {
        logger.info("Message reçu depuis topicout : {}", rawMessage);

        Optional<ParsedMessage> optionalMessage = messageParser.parse(rawMessage);

        if (optionalMessage.isEmpty()) {
            return;
        }

        ParsedMessage message = optionalMessage.get();
        String translatedContent = translateOrFallback(message.content());
        String translatedMessage = messageParser.format(message, translatedContent);

        try {
            kafkaTemplate.send(topicIn, message.to(), translatedMessage).get();
            logger.info("Message traduit envoyé vers topicin pour {} : {}", message.to(), translatedMessage);
        } catch (Exception e) {
            logger.error("Impossible d'envoyer le message traduit vers Kafka : {}", translatedMessage, e);
        }
    }

    private String translateOrFallback(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        try {
            TranslationRequest request = new TranslationRequest(
                    text,
                    sourceLanguage,
                    targetLanguage,
                    format
            );

            TranslationResponse response = restClient.post()
                    .uri(libreTranslateUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TranslationResponse.class);

            if (response == null || response.translatedText() == null || response.translatedText().isBlank()) {
                logger.warn("Réponse LibreTranslate vide, message original conservé");
                return text;
            }

            return response.translatedText();

        } catch (RestClientException e) {
            logger.error("Erreur pendant l'appel LibreTranslate, message original conservé : {}", text, e);
            return text;
        }
    }
}