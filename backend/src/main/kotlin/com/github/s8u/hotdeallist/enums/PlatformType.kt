package com.github.s8u.hotdeallist.enums

/**
 * 핫딜 플랫폼 타입
 */
enum class PlatformType(
    val communityName: String,
    val displayName: String,
) {
    COOLENJOY_JIRUM("쿨엔조이", "쿨엔조이 지름"),
    QUASARZONE_JIRUM("퀘이사존", "퀘이사존 지름"),
    QUASARZONE_TASEYO("퀘이사존", "퀘이사존 타세요"),
    RULIWEB_HOTDEAL("루리웹", "루리웹 핫딜"),
    PPOMPPU_PPOMPPU("뽐뿌", "뽐뿌 뽐뿌게시판"),
    PPOMPPU_HOTDEAL("뽐뿌", "뽐뿌 국내핫딜"),
    PPOMPPU_OVERSEAS_HOTDEAL("뽐뿌", "뽐뿌 해외핫딜"),
    CLIEN_ALTTEUL("클리앙", "클리앙 알뜰구매"),
}