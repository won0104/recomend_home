package com.ssafy.prj.batch.pdf;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssafy.prj.batch.domain.EstateMarketItem;
import com.ssafy.prj.batch.domain.EstateMarketReport;

@Service
public class EstateMarketPdfService {

    @Value("${batch.estate.pdf-dir:${user.home}/ssafy-home-reports}")
    private String pdfDir;

    public String createPdf(EstateMarketReport report, List<EstateMarketItem> items) throws Exception {
        Path dir = Path.of(pdfDir);
        Files.createDirectories(dir);
        Path output = dir.resolve(report.getReportId() + ".pdf");

        try (OutputStream os = Files.newOutputStream(output)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            Path malgun = Path.of("C:/Windows/Fonts/malgun.ttf");
            if (Files.exists(malgun)) {
                builder.useFont(malgun.toFile(), "MalgunGothic");
            }
            builder.withHtmlContent(buildHtml(report, items), dir.toUri().toString());
            builder.toStream(os);
            builder.run();
        }
        return output.toAbsolutePath().toString();
    }

    private String buildHtml(EstateMarketReport report, List<EstateMarketItem> items) {
        StringBuilder rows = new StringBuilder();
        int limit = Math.min(items == null ? 0 : items.size(), 30);
        for (int i = 0; i < limit; i++) {
            EstateMarketItem item = items.get(i);
            rows.append("<tr>")
                    .append(td(item.getDealDate()))
                    .append(td(item.getAptName()))
                    .append(td(item.getDong()))
                    .append(td(String.format("%,.2f", item.getArea())))
                    .append(td(String.valueOf(item.getFloor())))
                    .append(td(String.format("%,d", item.getDealAmount())))
                    .append("</tr>");
        }

        String volumeChart = buildVolumeChart(items);

        return """
                <!DOCTYPE html>
                <html lang="ko" xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    body { font-family: 'MalgunGothic', sans-serif; color: #1f2937; margin: 36px; }
                    h1 { font-size: 24px; margin: 0 0 8px; }
                    h2 { font-size: 16px; margin: 28px 0 10px; border-bottom: 1px solid #d1d5db; padding-bottom: 6px; }
                    .meta { color: #6b7280; font-size: 12px; margin-bottom: 20px; }
                    .cards { display: table; width: 100%%; border-spacing: 8px; margin: 12px 0 20px; }
                    .card { display: table-cell; border: 1px solid #d1d5db; padding: 12px; width: 25%%; }
                    .label { color: #6b7280; font-size: 11px; }
                    .value { font-size: 18px; font-weight: 700; margin-top: 4px; }
                    .chart { border: 1px solid #e5e7eb; background: #fbfdff; padding: 14px; }
                    .bar-row { display: table; width: 100%%; margin: 7px 0; }
                    .bar-label { display: table-cell; width: 88px; font-size: 11px; color: #374151; vertical-align: middle; }
                    .bar-wrap { display: table-cell; vertical-align: middle; }
                    .bar-bg { width: 100%%; height: 14px; background: #e5e7eb; }
                    .bar-fill { height: 14px; background: #2563eb; }
                    .bar-count { display: table-cell; width: 44px; font-size: 11px; color: #111827; text-align: right; vertical-align: middle; }
                    .chart-note { color: #6b7280; font-size: 11px; margin-top: 8px; }
                    .ai-summary { line-height: 1.65; background: #f9fafb; padding: 14px; border: 1px solid #e5e7eb; }
                    .ai-summary h1 { font-size: 18px; margin: 0 0 12px; }
                    .ai-summary h2 { font-size: 15px; margin: 16px 0 8px; border-bottom: 0; padding-bottom: 0; }
                    .ai-summary p { margin: 6px 0; }
                    .ai-summary ul, .ai-summary ol { margin: 6px 0 8px 20px; padding: 0; }
                    .ai-summary li { margin: 4px 0; }
                    table { width: 100%%; border-collapse: collapse; font-size: 11px; }
                    th, td { border: 1px solid #d1d5db; padding: 7px; text-align: left; }
                    th { background: #f3f4f6; }
                  </style>
                </head>
                <body>
                  <h1>%s</h1>
                  <div class="meta">reportId=%s | lawdCd=%s | dealYmd=%s | generatedAt=%s</div>
                  <div class="cards">
                    <div class="card"><div class="label">거래 건수</div><div class="value">%,d</div></div>
                    <div class="card"><div class="label">평균 거래금액</div><div class="value">%,d만원</div></div>
                    <div class="card"><div class="label">최고 거래금액</div><div class="value">%,d만원</div></div>
                    <div class="card"><div class="label">최저 거래금액</div><div class="value">%,d만원</div></div>
                  </div>
                  <h2>법정동별 거래량 비교</h2>
                  %s
                  <h2>AI 요약</h2>
                  <div class="ai-summary">%s</div>
                  <h2>정제 거래 데이터</h2>
                  <table>
                    <thead>
                      <tr><th>거래일</th><th>아파트</th><th>법정동</th><th>면적</th><th>층</th><th>금액(만원)</th></tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                </body>
                </html>
                """.formatted(escape(report.getTitle()), escape(report.getReportId()), escape(report.getLawdCd()),
                escape(report.getDealYmd()),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                report.getItemCount(), report.getAvgDealAmount(), report.getMaxDealAmount(),
                report.getMinDealAmount(), volumeChart, markdownToHtml(report.getAiSummary()), rows).stripLeading();
    }

    private String buildVolumeChart(List<EstateMarketItem> items) {
        if (items == null || items.isEmpty()) {
            return "<div class=\"chart\"><div class=\"chart-note\">표시할 거래 데이터가 없습니다.</div></div>";
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EstateMarketItem item : items) {
            String dong = item.getDong() == null || item.getDong().isBlank() ? "미상" : item.getDong().trim();
            counts.put(dong, counts.getOrDefault(dong, 0) + 1);
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));

        int max = entries.stream().mapToInt(Map.Entry::getValue).max().orElse(1);
        int total = items.size();
        int limit = Math.min(entries.size(), 12);

        StringBuilder chart = new StringBuilder("<div class=\"chart\">");
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            int width = Math.max(4, Math.round(entry.getValue() * 100f / max));
            chart.append("<div class=\"bar-row\">")
                    .append("<div class=\"bar-label\">").append(escape(entry.getKey())).append("</div>")
                    .append("<div class=\"bar-wrap\"><div class=\"bar-bg\"><div class=\"bar-fill\" style=\"width:")
                    .append(width)
                    .append("%;\"></div></div></div>")
                    .append("<div class=\"bar-count\">")
                    .append(entry.getValue())
                    .append("건</div>")
                    .append("</div>");
        }
        chart.append("<div class=\"chart-note\">총 ")
                .append(total)
                .append("건 기준, 거래량 상위 ")
                .append(limit)
                .append("개 법정동 표시</div>")
                .append("</div>");
        return chart.toString();
    }

    private String td(String value) {
        return "<td>" + escape(value) + "</td>";
    }

    private String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        boolean inUl = false;
        boolean inOl = false;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                continue;
            }

            if (line.startsWith("## ")) {
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                html.append("<h2>").append(inlineMarkdown(line.substring(3))).append("</h2>");
            } else if (line.startsWith("# ")) {
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                html.append("<h1>").append(inlineMarkdown(line.substring(2))).append("</h1>");
            } else if (line.matches("\\d+\\.\\s+.*")) {
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (!inOl) {
                    html.append("<ol>");
                    inOl = true;
                }
                html.append("<li>").append(inlineMarkdown(line.replaceFirst("^\\d+\\.\\s+", ""))).append("</li>");
            } else if (line.startsWith("- ")) {
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                if (!inUl) {
                    html.append("<ul>");
                    inUl = true;
                }
                html.append("<li>").append(inlineMarkdown(line.substring(2))).append("</li>");
            } else {
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                html.append("<p>").append(inlineMarkdown(line)).append("</p>");
            }
        }
        if (inUl) {
            html.append("</ul>");
        }
        if (inOl) {
            html.append("</ol>");
        }
        return html.toString();
    }

    private String inlineMarkdown(String value) {
        String escaped = escape(value);
        return escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
