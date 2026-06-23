package com.ssafy.prj.batch.domain;

/**
 * 카카오 주소 검색으로 좌표를 보강할 아파트 한 건이다.
 *
 * <p>매매 거래 건수가 아니라 {@code apt_seq} 기준으로 중복을 제거한 대상이다.
 * 따라서 같은 아파트에서 거래가 여러 번 발생해도 좌표 API는 한 번만 호출한다.</p>
 */
public record HouseCoordinateTarget(String aptSeq, String address) {
}
