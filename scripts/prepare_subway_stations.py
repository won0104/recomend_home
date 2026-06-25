"""서울 도시·광역철도 역사 데이터를 추천 서비스용 CSV로 전처리한다.

원본 역 정보에는 법정동코드가 없으므로, 지번·도로명 주소를 기존 dongcodes 데이터와
비교해 FK로 사용할 dong_code를 채운다. 정확히 매칭된 행만 import CSV에 포함한다.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import time
from collections import Counter
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen

import pandas as pd


WORKSPACE = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = Path("/Users/chaewon/Downloads/전국 도시광역철도 역사 역사정보_20260102.xlsx")
DONG_SQL = WORKSPACE / "sql" / "load_dongcodes.sql"
OUTPUT_DIR = WORKSPACE / "data"
IMPORT_CSV = OUTPUT_DIR / "subway_stations_seoul_for_import.csv"
UNMATCHED_CSV = OUTPUT_DIR / "subway_stations_seoul_unmatched.csv"
SUMMARY_FILE = OUTPUT_DIR / "subway_stations_seoul_mapping_summary.txt"
SECRET_PROPERTIES = WORKSPACE / "backend" / "src" / "main" / "resources" / "application-secret.properties"

SOURCE_COLUMNS = [
    "운영노선",
    "역 번호",
    "역명(한글)",
    "역 위치(위도)",
    "역 위치(경도)",
    "역 주소(지번주소)",
    "역 주소(도로명 주소)",
]
IMPORT_COLUMNS = ["line_name", "station_no", "station_name", "latitude", "longitude", "dong_code"]


def normalize(value: str | None) -> str:
    """주소와 dongcodes의 명칭을 공백·기호 차이 없이 비교할 형태로 바꾼다."""
    normalized_value = (value or "")
    normalized_value = normalized_value.replace("서울시", "서울특별시")
    normalized_value = normalized_value.replace("전북특별자치도", "전라북도")
    normalized_value = normalized_value.replace("강원특별자치도", "강원도")
    return re.sub(r"[^0-9가-힣A-Za-z]", "", normalized_value)


def split_sql_tuples(values: str) -> list[str]:
    """dongcodes INSERT VALUES 부분을 개별 튜플 문자열로 분리한다."""
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


def parse_dongcodes() -> list[dict[str, str]]:
    """프로젝트의 dongcodes SQL에서 법정동명과 코드를 읽는다."""
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
                "normalized_sido": normalize(sido_name),
                "normalized_gugun": normalize(gugun_name),
                "normalized_dong": normalize(dong_name),
            }
        )

    return sorted(dongcodes, key=lambda item: len(item["normalized_dong"]), reverse=True)


def match_dong_code(address: str, dongcodes: list[dict[str, str]]) -> tuple[str, str]:
    """주소에 시·군·구·법정동명이 모두 있는 경우에만 정확 매칭한다."""
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
    most_specific = [
        candidate
        for candidate in candidates
        if len(candidate["normalized_dong"]) == longest_name_length
    ]
    if len(most_specific) == 1:
        return most_specific[0]["dong_code"], "ADDRESS_EXACT"
    return "", "AMBIGUOUS"


def as_text(value: object) -> str:
    """엑셀의 빈 값과 숫자를 CSV에 안전한 문자열로 바꾼다."""
    if pd.isna(value):
        return ""
    return str(value).strip()


def is_seoul_address(address: str) -> bool:
    return "서울특별시" in address or "서울시" in address


def get_kakao_rest_api_key() -> str:
    """환경 변수 또는 프로젝트의 로컬 secret 파일에서 키를 읽되, 출력하지 않는다."""
    if api_key := os.environ.get("KAKAO_REST_API_KEY", "").strip():
        return api_key

    if not SECRET_PROPERTIES.is_file():
        raise FileNotFoundError(
            "KAKAO_REST_API_KEY가 없고 application-secret.properties 파일도 찾을 수 없습니다."
        )

    for line in SECRET_PROPERTIES.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("KAKAO_REST_API_KEY="):
            api_key = line.split("=", 1)[1].strip()
            if api_key:
                return api_key
    raise ValueError("application-secret.properties에 KAKAO_REST_API_KEY가 없습니다.")


def reverse_geocode_legal_dong_code(latitude: str, longitude: str, api_key: str) -> tuple[str, str]:
    """카카오 역지오코딩에서 법정동(region_type=B) 코드만 가져온다."""
    query = urlencode(
        {
            "x": longitude,
            "y": latitude,
            "input_coord": "WGS84",
            "output_coord": "WGS84",
        }
    )
    request = Request(
        f"https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?{query}",
        headers={"Authorization": f"KakaoAK {api_key}"},
    )
    try:
        with urlopen(request, timeout=30) as response:  # nosec B310: 공공 좌표 역지오코딩 고정 URL
            payload = json.load(response)
    except Exception as exception:  # API 오류는 import 대상에서 제외하고 audit 파일에 남긴다.
        return "", f"KAKAO_API_ERROR:{type(exception).__name__}"

    legal_dong = next(
        (document for document in payload.get("documents", []) if document.get("region_type") == "B"),
        None,
    )
    if not legal_dong or not legal_dong.get("code"):
        return "", "NO_LEGAL_DONG_RESULT"
    return str(legal_dong["code"]), "COORDINATE_REVERSE"


def main() -> None:
    parser = argparse.ArgumentParser(description="서울 도시·광역철도 역 데이터를 DB import CSV로 변환합니다.")
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE, help="도시·광역철도 역사정보 원본 XLSX 경로")
    parser.add_argument(
        "--recover-with-kakao",
        action="store_true",
        help="주소에 동명이 없는 행을 카카오 좌표 역지오코딩으로 보강합니다.",
    )
    args = parser.parse_args()

    if not args.source.is_file():
        raise FileNotFoundError(f"원본 XLSX 파일을 찾을 수 없습니다: {args.source}")

    OUTPUT_DIR.mkdir(exist_ok=True)
    source = pd.read_excel(args.source, sheet_name="1.역사정보", usecols=SOURCE_COLUMNS, dtype=str)
    source = source.fillna("")
    dongcodes = parse_dongcodes()
    project_dong_codes = {dongcode["dong_code"] for dongcode in dongcodes}
    kakao_api_key = get_kakao_rest_api_key() if args.recover_with_kakao else ""

    imported_rows: list[dict[str, str]] = []
    unmatched_rows: list[dict[str, str]] = []
    method_counts: Counter[str] = Counter()
    seoul_row_count = 0
    missing_coordinate_count = 0

    for _, source_row in source.iterrows():
        land_address = as_text(source_row["역 주소(지번주소)"])
        road_address = as_text(source_row["역 주소(도로명 주소)"])
        address = " ".join(filter(None, [land_address, road_address]))
        if not is_seoul_address(address):
            continue

        seoul_row_count += 1
        latitude = as_text(source_row["역 위치(위도)"])
        longitude = as_text(source_row["역 위치(경도)"])
        dong_code, match_method = match_dong_code(address, dongcodes)

        if not latitude or not longitude:
            match_method = "MISSING_COORDINATE"
            missing_coordinate_count += 1
        elif match_method != "ADDRESS_EXACT" and args.recover_with_kakao:
            recovered_code, recovery_method = reverse_geocode_legal_dong_code(latitude, longitude, kakao_api_key)
            if recovery_method == "COORDINATE_REVERSE" and recovered_code in project_dong_codes:
                dong_code, match_method = recovered_code, recovery_method
            elif recovery_method == "COORDINATE_REVERSE":
                match_method = "REVERSE_DONG_CODE_NOT_IN_PROJECT"
            else:
                match_method = recovery_method
            time.sleep(0.12)
        method_counts[match_method] += 1

        prepared_row = {
            "line_name": as_text(source_row["운영노선"]),
            "station_no": as_text(source_row["역 번호"]),
            "station_name": as_text(source_row["역명(한글)"]),
            "latitude": latitude,
            "longitude": longitude,
            "dong_code": dong_code,
        }

        if match_method in {"ADDRESS_EXACT", "COORDINATE_REVERSE"}:
            imported_rows.append(prepared_row)
        else:
            unmatched_rows.append(
                {
                    **prepared_row,
                    "land_address": land_address,
                    "road_address": road_address,
                    "match_method": match_method,
                }
            )

    with IMPORT_CSV.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=IMPORT_COLUMNS)
        writer.writeheader()
        writer.writerows(imported_rows)

    unmatched_columns = [*IMPORT_COLUMNS, "land_address", "road_address", "match_method"]
    with UNMATCHED_CSV.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=unmatched_columns)
        writer.writeheader()
        writer.writerows(unmatched_rows)

    coordinate_recovery_failure_count = sum(
        count
        for method, count in method_counts.items()
        if method.startswith("KAKAO_API_ERROR")
        or method in {"NO_LEGAL_DONG_RESULT", "REVERSE_DONG_CODE_NOT_IN_PROJECT"}
    )
    summary = [
        f"서울 주소 기준 원본 역사 수: {seoul_row_count}",
        f"DB import 가능(주소 기반 법정동 정확 매핑): {method_counts['ADDRESS_EXACT']}",
        f"DB import 가능(좌표 역지오코딩 법정동 매핑): {method_counts['COORDINATE_REVERSE']}",
        f"DB import 제외(주소 매핑 모호·미매칭): {method_counts['AMBIGUOUS'] + method_counts['UNMATCHED']}",
        f"DB import 제외(좌표 역지오코딩 실패·코드 검증 실패): {coordinate_recovery_failure_count}",
        f"DB import 제외(좌표 누락): {missing_coordinate_count}",
        f"DB import CSV: {IMPORT_CSV.name}",
        f"확인 필요 행 CSV: {UNMATCHED_CSV.name}",
    ]
    SUMMARY_FILE.write_text("\n".join(summary) + "\n", encoding="utf-8")
    print("\n".join(summary))


if __name__ == "__main__":
    main()
