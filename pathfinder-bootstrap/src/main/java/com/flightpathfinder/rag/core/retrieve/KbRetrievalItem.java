package com.flightpathfinder.rag.core.retrieve;

public record KbRetrievalItem(
        String intentId,
        String intentName,
        String question,
        String collectionName,
        String topic,
        String source,
        String documentId,
        String title,
        String content,
        double score,
        int rank) {

    public KbRetrievalItem {
        intentId = intentId == null ? "" : intentId.trim();
        intentName = intentName == null ? "" : intentName.trim();
        question = question == null ? "" : question.trim();
        collectionName = collectionName == null ? "" : collectionName.trim();
        topic = topic == null ? "" : topic.trim();
        source = source == null ? "" : source.trim();
        documentId = documentId == null ? "" : documentId.trim();
        title = title == null ? "" : title.trim();
        content = content == null ? "" : content.trim();
    }
}
