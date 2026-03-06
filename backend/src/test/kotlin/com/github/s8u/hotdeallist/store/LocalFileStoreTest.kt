package com.github.s8u.hotdeallist.store

import org.junit.jupiter.api.*
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalFileStoreTest {

    private lateinit var tempDir: Path
    private lateinit var localFileStore: LocalFileStore

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("localfilestore-test")
        localFileStore = LocalFileStore(tempDir, "http://localhost:8080/storage")
    }

    @AfterEach
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Nested
    @DisplayName("store")
    inner class Store {

        @Test
        @DisplayName("파일을 저장하고 경로를 반환한다")
        fun `should store file and return path`() {
            val content = "test image content".toByteArray()
            val inputStream = ByteArrayInputStream(content)

            val result = localFileStore.store("images/test.webp", inputStream, "image/webp")

            assertEquals("images/test.webp", result)
            assertTrue(Files.exists(tempDir.resolve("images/test.webp")))
        }

        @Test
        @DisplayName("중간 디렉토리가 없어도 자동 생성한다")
        fun `should create intermediate directories`() {
            val content = "content".toByteArray()
            val inputStream = ByteArrayInputStream(content)

            localFileStore.store("deep/nested/dir/file.txt", inputStream, "text/plain")

            assertTrue(Files.exists(tempDir.resolve("deep/nested/dir/file.txt")))
        }

        @Test
        @DisplayName("기존 파일을 덮어쓴다")
        fun `should overwrite existing file`() {
            val content1 = "original".toByteArray()
            val content2 = "updated".toByteArray()

            localFileStore.store("file.txt", ByteArrayInputStream(content1), "text/plain")
            localFileStore.store("file.txt", ByteArrayInputStream(content2), "text/plain")

            val stored = Files.readString(tempDir.resolve("file.txt"))
            assertEquals("updated", stored)
        }
    }

    @Nested
    @DisplayName("get")
    inner class Get {

        @Test
        @DisplayName("존재하는 파일을 읽는다")
        fun `should return input stream for existing file`() {
            val content = "test content".toByteArray()
            localFileStore.store("test.txt", ByteArrayInputStream(content), "text/plain")

            val result = localFileStore.get("test.txt")

            assertNotNull(result)
            assertEquals("test content", result.use { it.readBytes().decodeToString() })
        }

        @Test
        @DisplayName("존재하지 않는 파일은 null을 반환한다")
        fun `should return null for non-existing file`() {
            val result = localFileStore.get("nonexistent.txt")

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {

        @Test
        @DisplayName("존재하는 파일을 삭제한다")
        fun `should delete existing file`() {
            localFileStore.store("to-delete.txt", ByteArrayInputStream("content".toByteArray()), "text/plain")

            val result = localFileStore.delete("to-delete.txt")

            assertTrue(result)
            assertFalse(Files.exists(tempDir.resolve("to-delete.txt")))
        }

        @Test
        @DisplayName("존재하지 않는 파일 삭제 시 false를 반환한다")
        fun `should return false for non-existing file`() {
            val result = localFileStore.delete("nonexistent.txt")

            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("exists")
    inner class Exists {

        @Test
        @DisplayName("존재하는 파일은 true를 반환한다")
        fun `should return true for existing file`() {
            localFileStore.store("exists.txt", ByteArrayInputStream("content".toByteArray()), "text/plain")

            assertTrue(localFileStore.exists("exists.txt"))
        }

        @Test
        @DisplayName("존재하지 않는 파일은 false를 반환한다")
        fun `should return false for non-existing file`() {
            assertFalse(localFileStore.exists("nonexistent.txt"))
        }
    }

    @Nested
    @DisplayName("getUrl")
    inner class GetUrl {

        @Test
        @DisplayName("baseUrl과 경로를 조합하여 URL을 반환한다")
        fun `should return url with base url and path`() {
            val result = localFileStore.getUrl("images/test.webp")

            assertEquals("http://localhost:8080/storage/images/test.webp", result)
        }
    }
}
