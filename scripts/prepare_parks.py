"""공원 CSV에 법정동 코드를 추가하는 주소 기반 전처리 스크립트."""

from __future__ import annotations

import csv
import re
from collections import Counter
from pathlib import Path


WORKSPACE = Path(__file__).resolve().parents[1]
SOURCE_CSV = Path("/Users/chaewon/Downloads/전국도시공원정보표준데이터.csv")
DONG_SQL = WORKSPACE / "sql" / "load_dongcodes.sql"
OUTPUT_DIR = WORKSPACE / "data"
OUTPUT_CSV = OUTPUT_DIR / "parks_prepared.csv"
IMPORT_CSV = OUTPUT_DIR / "parks_for_import.csv"
UNMATCHED_CSV = OUTPUT_DIR / "parks_unmatched.csv"
SUMMARY_FILE = OUTPUT_DIR / "parks_mapping_summary.txt"


def normalize(value: str | None) -> str:
    normalized_value = (value or "")
    normalized_value = normalized_value.replace("전북특별자치도", "전라북도")
    normalized_value = normalized_value.replace("강원특별자치도", "강원도")
    return re.sub(r"[^0-9가-힣A-Za-z]", "", normalized_value)


def parse_dongcodes() -> list[dict[str, str]]:
    insert_line = next(
        line for line in DONG_SQL.open(encoding="utf-8") if line.startswith("INSERT INTO `dongcodes`")
    )
    values = insert_line.split(" VALUES ", 1)[1].rstrip(";\n")
    dongcodes: list[dict[str, str]] = []

    for tuple_values in split_sql_tuples(values):
        row = next(csv.reader([tuple_values], quotechar="'", skipinitialspace=True))
        code, sido_name, gugun_name, dong_name = ["" if value == "NULL" else value for value in row]
        if not dong_name:
            continue
        dongcodes.append(
            {
                "dong_code": code,
                "sido_name": sido_name,
                "gugun_name": gugun_name,
                "dong_name": dong_name,
                "normalized_sido": normalize(sido_name),
                "normalized_gugun": normalize(gugun_name),
                "normalized_dong": normalize(dong_name),
            }
        )

    return sorted(dongcodes, key=lambda item: len(item["normalized_dong"]), reverse=True)


def split_sql_tuples(values: str) -> list[str]:
    tuples: list[str] = []
    tuple_start: int | None = None
    depth = 0
    in_quote = False

    for index, character in enumerate(values):
        if character == "'":
            in_quote = not in_quote
            continue
        if in_quote:
            continue
        if character == "(":
            if depth == 0:
                tuple_start = index + 1
            depth += 1
        elif character == ")":
            depth -= 1
            if depth == 0 and tuple_start is not None:
                tuples.append(values[tuple_start:index])
                tuple_start = None

    return tuples


def match_dong_code(address: str, dongcodes: list[dict[str, str]]) -> tuple[str, str]:
    normalized_address = normalize(address)
    candidates: list[dict[str, str]] = []

    for dongcode in dongcodes:
        if dongcode["normalized_dong"] not in normalized_address:
            continue
        if dongcode["normalized_gugun"] and dongcode["normalized_gugun"] not in normalized_address:
            continue
        if dongcode["normalized_sido"] and dongcode["normalized_sido"] not in normalized_address:
            continue
        candidates.append(dongcode)

    if not candidates:
        return "", "UNMATCHED"

    longest_name_length = max(len(candidate["normalized_dong"]) for candidate in candidates)
    most_specific_candidates = [
        candidate
        for candidate in candidates
        if len(candidate["normalized_dong"]) == longest_name_length
    ]
    if len(most_specific_candidates) == 1:
        return most_specific_candidates[0]["dong_code"], "ADDRESS_EXACT"
    return "", "AMBIGUOUS"


def main() -> None:
    OUTPUT_DIR.mkdir(exist_ok=True)
    dongcodes = parse_dongcodes()

    with SOURCE_CSV.open(encoding="cp949", newline="") as source_file:
        reader = csv.DictReader(source_file)
        fieldnames = (reader.fieldnames or []) + ["dong_code", "dong_match_method"]
        rows = list(reader)

    unmatched_rows: list[dict[str, str]] = []
    method_counts: Counter[str] = Counter()
    for row in rows:
        address = " ".join(
            filter(None, [row.get("소재지지번주소", ""), row.get("소재지도로명주소", "")])
        )
        dong_code, method = match_dong_code(address, dongcodes)
        row["dong_code"] = dong_code
        row["dong_match_method"] = method
        method_counts[method] += 1
        if method != "ADDRESS_EXACT":
            unmatched_rows.append(row)

    with OUTPUT_CSV.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    with UNMATCHED_CSV.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(unmatched_rows)

    import_fieldnames = [
        "park_name",
        "road_address",
        "latitude",
        "longitude",
        "park_area",
        "dong_code",
    ]
    with IMPORT_CSV.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=import_fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(
                {
                    "park_name": row["공원명"],
                    "road_address": row["소재지도로명주소"],
                    "latitude": row["위도"],
                    "longitude": row["경도"],
                    "park_area": row["공원면적"],
                    "dong_code": row["dong_code"],
                }
            )

    summary = [
        f"전체 공원 수: {len(rows)}",
        f"주소 기반 법정동 코드 매핑: {method_counts['ADDRESS_EXACT']}",
        f"모호한 주소 매핑: {method_counts['AMBIGUOUS']}",
        f"미매칭 주소: {method_counts['UNMATCHED']}",
    ]
    SUMMARY_FILE.write_text("\n".join(summary) + "\n", encoding="utf-8")
    print("\n".join(summary))


if __name__ == "__main__":
    main()
