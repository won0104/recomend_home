package com.ssafy.prj.batch.bus;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** 서울 버스 정류소 위치 API XML 응답을 안전하게 읽는다. */
@Component
public class BusStopApiResponseParser {

    /**
     * API 한 페이지를 읽는다.
     *
     * <p>서울 Open API의 XCRD는 경도, YCRD는 위도다. 추천 서비스와 카카오 API가 모두
     * WGS84 위도·경도를 사용하므로 이 순서를 여기에서 명확하게 고정한다.</p>
     */
    public BusStopApiPage parse(String xml) throws Exception {
        Document document = parseXml(xml);
        validateResult(document);

        int totalCount = parseTotalCount(firstText(document, "list_total_count"));
        NodeList rows = document.getElementsByTagName("row");
        List<BusStopCoordinate> stops = new ArrayList<>(rows.getLength());

        for (int index = 0; index < rows.getLength(); index++) {
            Element row = (Element) rows.item(index);
            String stopsNo = firstText(row, "STOPS_NO");
            String longitudeText = firstText(row, "XCRD");
            String latitudeText = firstText(row, "YCRD");

            if (stopsNo.isBlank() || longitudeText.isBlank() || latitudeText.isBlank()) {
                throw new IllegalStateException("Bus stop API row is missing STOPS_NO, XCRD, or YCRD");
            }

            try {
                BigDecimal longitude = new BigDecimal(longitudeText);
                BigDecimal latitude = new BigDecimal(latitudeText);
                validateWgs84Coordinate(latitude, longitude, stopsNo);
                stops.add(new BusStopCoordinate(stopsNo, latitude, longitude));
            } catch (NumberFormatException exception) {
                throw new IllegalStateException("Bus stop API has invalid coordinate. stopsNo=" + stopsNo, exception);
            }
        }

        return new BusStopApiPage(totalCount, stops);
    }

    /** 외부 XML에서 DTD를 읽지 않게 막아 XXE 공격을 예방한다. */
    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    /** 서울 Open API는 정상 응답일 때 RESULT/CODE에 INFO-000을 반환한다. */
    private void validateResult(Document document) {
        String resultCode = firstText(document, "CODE");
        if (!"INFO-000".equals(resultCode)) {
            throw new IllegalStateException("Seoul bus stop API failed. code=" + resultCode
                + ", message=" + firstText(document, "MESSAGE"));
        }
    }

    private int parseTotalCount(String totalCount) {
        try {
            int parsed = Integer.parseInt(totalCount);
            if (parsed < 0) {
                throw new IllegalStateException("Bus stop API list_total_count must not be negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Bus stop API has invalid list_total_count: " + totalCount, exception);
        }
    }

    /** 서울 위치 API가 WGS84가 아닌 다른 좌표계를 반환했을 때 잘못된 법정동 매핑을 막는다. */
    private void validateWgs84Coordinate(BigDecimal latitude, BigDecimal longitude, String stopsNo) {
        boolean isWgs84 = latitude.compareTo(new BigDecimal("33")) >= 0
            && latitude.compareTo(new BigDecimal("39")) <= 0
            && longitude.compareTo(new BigDecimal("124")) >= 0
            && longitude.compareTo(new BigDecimal("132")) <= 0;
        if (!isWgs84) {
            throw new IllegalStateException("Bus stop API coordinate is not WGS84. stopsNo=" + stopsNo);
        }
    }

    private String firstText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String firstText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
