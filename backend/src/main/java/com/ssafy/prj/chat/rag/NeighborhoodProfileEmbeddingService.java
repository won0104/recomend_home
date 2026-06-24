package com.ssafy.prj.chat.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class NeighborhoodProfileEmbeddingService {

    private final VectorStore vectorStore;

    public NeighborhoodProfileEmbeddingService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(List<NeighborhoodProfileDocumentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        List<Document> documents = requests.stream()
            .filter(request -> !isBlank(request.content()))
            .map(this::toDocument)
            .toList();
        if (documents.isEmpty()) {
            return 0;
        }
        vectorStore.add(documents);
        return documents.size();
    }

    private Document toDocument(NeighborhoodProfileDocumentRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        putIfPresent(metadata, "sido", request.sido());
        putIfPresent(metadata, "sigungu", request.sigungu());
        putIfPresent(metadata, "dong", request.dong());
        putIfPresent(metadata, "lawdCd", request.lawdCd());
        putIfPresent(metadata, "category", request.category());
        return new Document(request.content(), metadata);
    }

    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (!isBlank(value)) {
            metadata.put(key, value.trim());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
