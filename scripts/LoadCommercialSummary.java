import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class LoadCommercialSummary {
    private static final String ROOT = "C:/Users/hhy/ssafy/pjt11_buk03_15_09";
    private static final String URL = "jdbc:mysql://localhost:3306/ssafy_home"
            + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul";
    private static final String USER = "ssafy";
    private static final String PASSWORD = "ssafy";
    private static final int BATCH_SIZE = 1_000;
    private static final String[] REGIONS = {
            "seoul", "gyeonggi", "incheon", "busan", "daegu", "daejeon", "gwangju", "ulsan", "sejong",
            "gangwon", "chungbuk", "chungnam", "jeonbuk", "jeonnam", "gyeongbuk", "gyeongnam", "jeju"
    };

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("TRUNCATE TABLE commercials");
            statement.execute("TRUNCATE TABLE commercial_categories");

            String insertSql = """
                    INSERT INTO commercials (
                        shop_no, shop_name, branch_name, category_l_code, category_l_name,
                        category_m_code, category_m_name, category_s_code, category_s_name,
                        std_industry_code, std_industry_name, sido_code, sido_name,
                        sigungu_code, sigungu_name, haengjeong_code, haengjeong_name,
                        beopjeong_code, beopjeong_name, jibun_address, road_address,
                        building_name, longitude, latitude
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                for (String region : REGIONS) {
                    Path path = Path.of(ROOT, "data", "summary", "commercials_" + region + ".csv");
                    int loaded = loadCsv(path, insert);
                    connection.commit();
                    System.out.printf("[load] %-28s %,d rows%n", path.getFileName(), loaded);
                }
            }

            statement.executeUpdate("""
                    INSERT IGNORE INTO commercial_categories (category_code, category_name)
                    SELECT DISTINCT category_l_code, category_l_name
                    FROM commercials
                    WHERE category_l_code IS NOT NULL AND category_l_name IS NOT NULL
                    """);
            connection.commit();

            try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM commercials")) {
                if (rs.next()) {
                    System.out.println("[done] commercials rows = " + rs.getLong(1));
                }
            }
            try (ResultSet rs = statement.executeQuery("""
                    SELECT category_l_code, category_l_name, COUNT(*) AS cnt
                    FROM commercials
                    GROUP BY category_l_code, category_l_name
                    ORDER BY category_l_code
                    """)) {
                System.out.println("[category]");
                while (rs.next()) {
                    System.out.printf("  %s / %s = %,d%n", rs.getString(1), rs.getString(2), rs.getLong(3));
                }
            }
        }
    }

    private static int loadCsv(Path path, PreparedStatement insert) throws Exception {
        int loaded = 0;
        int pending = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = parseCsvLine(line);
                if (row.size() < 39) {
                    continue;
                }
                bindRow(insert, row);
                insert.addBatch();
                loaded++;
                pending++;
                if (pending >= BATCH_SIZE) {
                    insert.executeBatch();
                    pending = 0;
                }
            }
        }
        if (pending > 0) {
            insert.executeBatch();
        }
        return loaded;
    }

    private static void bindRow(PreparedStatement ps, List<String> row) throws Exception {
        setString(ps, 1, row.get(0));
        setString(ps, 2, row.get(1));
        setString(ps, 3, row.get(2));
        setString(ps, 4, row.get(3));
        setString(ps, 5, row.get(4));
        setString(ps, 6, row.get(5));
        setString(ps, 7, row.get(6));
        setString(ps, 8, row.get(7));
        setString(ps, 9, row.get(8));
        setString(ps, 10, row.get(9));
        setString(ps, 11, row.get(10));
        setString(ps, 12, row.get(11));
        setString(ps, 13, row.get(12));
        setString(ps, 14, row.get(13));
        setString(ps, 15, row.get(14));
        setString(ps, 16, row.get(15));
        setString(ps, 17, row.get(16));
        setString(ps, 18, row.get(17));
        setString(ps, 19, row.get(18));
        setString(ps, 20, row.get(24));
        setString(ps, 21, row.get(31));
        setString(ps, 22, row.get(30));
        setDecimal(ps, 23, row.get(37));
        setDecimal(ps, 24, row.get(38));
    }

    private static void setString(PreparedStatement ps, int index, String value) throws Exception {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
            return;
        }
        ps.setString(index, value);
    }

    private static void setDecimal(PreparedStatement ps, int index, String value) throws Exception {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.DECIMAL);
            return;
        }
        ps.setBigDecimal(index, new BigDecimal(value));
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
