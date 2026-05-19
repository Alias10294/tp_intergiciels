package org.isihop.fr.perTranslateService;

public record ParsedMessage(
        String from,
        String to,
        String content) 
{ }