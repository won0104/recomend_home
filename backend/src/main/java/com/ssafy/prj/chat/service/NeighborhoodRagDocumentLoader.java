package com.ssafy.prj.chat.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NeighborhoodRagDocumentLoader implements ApplicationRunner {
    private final VectorStore vectorStore;

    public NeighborhoodRagDocumentLoader(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStore = vectorStoreProvider.getIfAvailable();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (vectorStore == null) {
            return;
        }
        try {
            vectorStore.add(List.of(
                    doc("category-school", "SCHOOL은 학교 카테고리입니다. 학교, 학원, 교육 인프라 접근성을 중요하게 보는 사용자에게 사용합니다."),
                    doc("category-cafe", "CAFE는 카페/외식 카테고리입니다. 카페, 음식점, 식당, 외식 생활 편의성을 의미합니다."),
                    doc("category-culture", "CULTURE는 운동/문화 카테고리입니다. 체육시설, 운동, 문화생활, 여가시설 접근성을 의미합니다."),
                    doc("category-shopping", "SHOPPING은 장보기/생활편의 카테고리입니다. 마트, 시장, 쇼핑, 생활 편의시설 접근성을 의미합니다."),
                    doc("category-park", "PARK는 공원/산책 카테고리입니다. 공원, 녹지, 산책하기 좋은 환경을 의미합니다."),
                    doc("category-medical", "MEDICAL은 의료 카테고리입니다. 병원, 의원, 약국 같은 의료 인프라 접근성을 의미합니다."),
                    doc("category-pet", "PET는 반려동물 카테고리입니다. 반려동물과 함께 생활하기 좋은 환경, 산책, 동물병원 접근성 등을 함께 고려할 수 있습니다."),
                    doc("priority-score", "생활환경 우선순위는 최대 3개까지 받습니다. 1순위는 0.5, 2순위는 0.3, 3순위는 0.2 같은 방식으로 점수 계산에 반영할 수 있습니다."),
                    doc("chat-flow", "동네추천 챗봇은 기준 위치, 장소 후보 선택, 반경, 예산, 생활환경 우선순위 확인 순서로 정보를 받은 뒤 기존 추천 서비스를 호출합니다.")));
        } catch (Exception ignored) {
        }
    }

    private Document doc(String id, String text) {
        return new Document(id, text, Map.of("domain", "neighborhood-chat", "source", "bootstrap"));
    }
}
