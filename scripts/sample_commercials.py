"""
상가(상권) CSV 경량화 — 100줄에 1줄씩 샘플링

원본 :  data/소상공인시장진흥공단_상가(상권)정보_20260331/*.csv
결과 :  data/summary/commercials_<region>.csv  (헤더 + 100행마다 1행, 영문 파일명)

사용법 :
    cd C:\\SSAFY\\pjt06_buk03_15_12
    python scripts\\sample_commercials.py

표준 라이브러리만 사용하므로 별도 설치 불필요.
샘플링 비율은 SAMPLE_EVERY 상수로 조정 가능 (기본 100).
"""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SRC_DIR = PROJECT_ROOT / "data" / "소상공인시장진흥공단_상가(상권)정보_20260331"
DST_DIR = PROJECT_ROOT / "data" / "summary"

SAMPLE_EVERY = 100  # 100줄당 1줄 (1%)

# 한글 시도명 → 영문 식별자
REGION_MAP = {
    "강원": "gangwon",
    "경기": "gyeonggi",
    "경남": "gyeongnam",
    "경북": "gyeongbuk",
    "광주": "gwangju",
    "대구": "daegu",
    "대전": "daejeon",
    "부산": "busan",
    "서울": "seoul",
    "세종": "sejong",
    "울산": "ulsan",
    "인천": "incheon",
    "전남": "jeonnam",
    "전북": "jeonbuk",
    "제주": "jeju",
    "충남": "chungnam",
    "충북": "chungbuk",
}


def to_ascii_name(src_name: str) -> str:
    """원본 파일명에서 시도명을 추출해 commercials_<region>.csv 형태로 변환.
    예: 소상공인시장진흥공단_상가(상권)정보_서울_202603.csv -> commercials_seoul.csv
    매핑이 없으면 안전한 ASCII 슬러그로 대체.
    """
    m = re.search(r"정보_([가-힣]+)_\d+", src_name)
    if m and m.group(1) in REGION_MAP:
        return f"commercials_{REGION_MAP[m.group(1)]}.csv"
    # 매핑 실패 시 한글/괄호 등을 _ 로 치환한 슬러그
    stem = src_name.rsplit(".", 1)[0]
    slug = re.sub(r"[^A-Za-z0-9_]+", "_", stem).strip("_").lower()
    return f"{slug or 'commercials'}.csv"


def sample_csv(src: Path, dst: Path) -> tuple[int, int]:
    """src CSV 를 읽어 헤더 + 매 SAMPLE_EVERY 번째 데이터 행만 dst 로 기록.
    반환값 : (원본 데이터 행 수, 출력 데이터 행 수)
    """
    src_rows = 0
    out_rows = 0

    # newline="" 로 열어야 csv 모듈이 줄바꿈을 정확히 처리
    with src.open("r", encoding="utf-8-sig", newline="") as fin, \
         dst.open("w", encoding="utf-8", newline="") as fout:
        reader = csv.reader(fin)
        writer = csv.writer(fout, quoting=csv.QUOTE_MINIMAL)

        try:
            header = next(reader)
        except StopIteration:
            return (0, 0)
        writer.writerow(header)

        for idx, row in enumerate(reader):
            src_rows += 1
            # idx 는 데이터 행 0-base. 0, 100, 200, ... 행만 유지
            if idx % SAMPLE_EVERY == 0:
                writer.writerow(row)
                out_rows += 1

    return (src_rows, out_rows)


def main() -> int:
    if not SRC_DIR.is_dir():
        sys.stderr.write(f"[error] 원본 폴더를 찾을 수 없습니다 : {SRC_DIR}\n")
        return 1

    DST_DIR.mkdir(parents=True, exist_ok=True)

    csv_paths = sorted(p for p in SRC_DIR.iterdir() if p.suffix.lower() == ".csv")
    if not csv_paths:
        sys.stderr.write(f"[error] CSV 파일이 없습니다 : {SRC_DIR}\n")
        return 1

    print(f"[info] 원본 폴더 : {SRC_DIR}")
    print(f"[info] 결과 폴더 : {DST_DIR}")
    print(f"[info] 샘플링 비율 : 1 / {SAMPLE_EVERY}")
    print(f"[info] 총 {len(csv_paths)} 개 파일 처리\n")

    total_src = 0
    total_out = 0
    for src in csv_paths:
        dst_name = to_ascii_name(src.name)
        dst = DST_DIR / dst_name
        src_rows, out_rows = sample_csv(src, dst)
        total_src += src_rows
        total_out += out_rows
        ratio = (out_rows / src_rows * 100.0) if src_rows else 0.0
        print(f"  {src.name:<70s} -> {dst_name:<28s} {src_rows:>10,} -> {out_rows:>8,}  ({ratio:5.2f}%)")

    print()
    print(f"[done] 원본 합계 : {total_src:>12,} 행")
    print(f"[done] 결과 합계 : {total_out:>12,} 행")
    print(f"[done] 비율      : {(total_out / total_src * 100.0) if total_src else 0:.2f} %")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
