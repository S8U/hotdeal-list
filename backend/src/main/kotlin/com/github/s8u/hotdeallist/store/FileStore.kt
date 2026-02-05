package com.github.s8u.hotdeallist.store

import java.io.InputStream

/**
 * 범용 파일 스토리지 인터페이스
 * Local, S3, R2 등 다양한 스토리지 백엔드에서 재사용 가능
 */
interface FileStore {

    /**
     * 파일 저장
     * @param path 저장 경로 (예: "image/12345.webp")
     * @param inputStream 파일 데이터
     * @param contentType MIME 타입 (예: "image/webp")
     * @return 저장된 파일의 전체 경로
     */
    fun store(path: String, inputStream: InputStream, contentType: String): String

    /**
     * 파일 조회
     * @param path 파일 경로
     * @return 파일 데이터 (없으면 null)
     */
    fun get(path: String): InputStream?

    /**
     * 파일 삭제
     * @param path 파일 경로
     * @return 삭제 성공 여부
     */
    fun delete(path: String): Boolean

    /**
     * 파일 존재 여부 확인
     * @param path 파일 경로
     * @return 존재 여부
     */
    fun exists(path: String): Boolean

    /**
     * 파일 접근 URL 반환
     * @param path 파일 경로
     * @return 접근 가능한 URL
     */
    fun getUrl(path: String): String
}
