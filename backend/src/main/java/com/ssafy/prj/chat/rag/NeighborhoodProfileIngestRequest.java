package com.ssafy.prj.chat.rag;

import java.util.List;

public record NeighborhoodProfileIngestRequest(
    List<NeighborhoodProfileDocumentRequest> documents
) {
}
