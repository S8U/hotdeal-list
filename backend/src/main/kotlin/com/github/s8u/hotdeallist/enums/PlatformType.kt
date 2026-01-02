package com.github.s8u.hotdeallist.enums

/**
 * 핫딜 플랫폼 타입
 */
enum class PlatformType(
    val displayName: String
) {
    // 쿨엔조이
    COOLENJOY_JIRUM("쿨엔조이 지름"),

    // 퀘이사존
    QUASARZONE_JIRUM("퀘이사존 지름"),
    QUASARZONE_TASEYO("퀘이사존 타세요"),

    // 루리웹
    RULIWEB_HOTDEAL("루리웹 핫딜"),

    // 뽐뿌
    PPOMPPU_HOTDEAL("뽐뿌 국내핫딜"),
    PPOMPPU_OVERSEAS_HOTDEAL("뽐뿌 해외핫딜"),

    // 클리앙
    CLIEN_ALTTEUL("클리앙 알뜰구매"),

    // 조드
    ZOD_DEAL("조드 특가");

}