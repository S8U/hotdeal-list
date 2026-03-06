package com.github.s8u.hotdeallist.controller

import com.github.s8u.hotdeallist.entity.Category
import com.github.s8u.hotdeallist.repository.CategoryRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockKExtension::class)
class CategoryControllerTest {

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(CategoryController(categoryRepository))
            .build()
    }

    private fun createCategory(
        id: Long,
        parentId: Long?,
        code: String,
        name: String,
        nameEn: String? = null,
        depth: Int = 0,
        sortOrder: Int = 0
    ): Category {
        val category = Category(
            parentId = parentId,
            code = code,
            name = name,
            nameEn = nameEn,
            depth = depth,
            sortOrder = sortOrder
        )
        // BaseEntity의 id는 reflection으로 설정
        val idField = category.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(category, id)
        return category
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 목록을 반환한다")
    fun `should return empty list when no categories`() {
        every { categoryRepository.findAll() } returns emptyList()

        mockMvc.get("/api/v1/categories")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    @DisplayName("트리 구조로 카테고리를 반환한다")
    fun `should return categories in tree structure`() {
        val categories = listOf(
            createCategory(1L, null, "electronics", "전자·가전", "Electronics", depth = 0, sortOrder = 1),
            createCategory(2L, null, "fashion", "패션·의류", "Fashion", depth = 0, sortOrder = 2),
            createCategory(3L, 1L, "mobile", "모바일", "Mobile", depth = 1, sortOrder = 1),
            createCategory(4L, 1L, "computer", "컴퓨터", "Computer", depth = 1, sortOrder = 2),
            createCategory(5L, 3L, "smartphone", "스마트폰", "Smartphone", depth = 2, sortOrder = 1)
        )
        every { categoryRepository.findAll() } returns categories

        mockMvc.get("/api/v1/categories")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].code") { value("electronics") }
                jsonPath("$[0].name") { value("전자·가전") }
                jsonPath("$[0].children.length()") { value(2) }
                jsonPath("$[0].children[0].code") { value("mobile") }
                jsonPath("$[0].children[0].children.length()") { value(1) }
                jsonPath("$[0].children[0].children[0].code") { value("smartphone") }
                jsonPath("$[1].code") { value("fashion") }
                jsonPath("$[1].children.length()") { value(0) }
            }
    }

    @Test
    @DisplayName("자식 카테고리를 sortOrder 기준으로 정렬한다")
    fun `should sort children by sortOrder`() {
        val categories = listOf(
            createCategory(1L, null, "electronics", "전자·가전", depth = 0, sortOrder = 1),
            createCategory(2L, 1L, "computer", "컴퓨터", depth = 1, sortOrder = 2),
            createCategory(3L, 1L, "mobile", "모바일", depth = 1, sortOrder = 1),
            createCategory(4L, 1L, "av", "영상·음향", depth = 1, sortOrder = 3)
        )
        every { categoryRepository.findAll() } returns categories

        mockMvc.get("/api/v1/categories")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].children[0].code") { value("mobile") }
                jsonPath("$[0].children[1].code") { value("computer") }
                jsonPath("$[0].children[2].code") { value("av") }
            }
    }

    @Test
    @DisplayName("루트 카테고리만 있을 때 자식 없이 반환한다")
    fun `should return root categories without children`() {
        val categories = listOf(
            createCategory(1L, null, "electronics", "전자·가전", depth = 0, sortOrder = 1),
            createCategory(2L, null, "fashion", "패션·의류", depth = 0, sortOrder = 2)
        )
        every { categoryRepository.findAll() } returns categories

        mockMvc.get("/api/v1/categories")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].children.length()") { value(0) }
                jsonPath("$[1].children.length()") { value(0) }
            }
    }
}
