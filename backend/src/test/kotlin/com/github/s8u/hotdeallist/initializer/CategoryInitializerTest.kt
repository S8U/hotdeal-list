package com.github.s8u.hotdeallist.initializer

import com.github.s8u.hotdeallist.entity.Category
import com.github.s8u.hotdeallist.repository.CategoryRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.boot.DefaultApplicationArguments

@ExtendWith(MockitoExtension::class)
class CategoryInitializerTest {

    @Mock
    lateinit var categoryRepository: CategoryRepository

    private lateinit var initializer: CategoryInitializer

    private var savedCategories = mutableListOf<Category>()
    private var idCounter = 1L

    @BeforeEach
    fun setUp() {
        initializer = CategoryInitializer(categoryRepository)
        savedCategories.clear()
        idCounter = 1L

        // findByCode는 항상 null 반환 (신규 생성)
        whenever(categoryRepository.findByCode(any())).thenReturn(null)
        whenever(categoryRepository.count()).thenReturn(0L)

        // save 호출 시 id를 부여하여 반환
        whenever(categoryRepository.save(any<Category>())).thenAnswer { invocation ->
            val category = invocation.getArgument<Category>(0)
            Category::class.java.superclass.getDeclaredField("id").apply {
                isAccessible = true
                set(category, idCounter++)
            }
            savedCategories.add(category)
            category
        }
    }

    @Test
    fun `run 실행 시 카테고리가 저장된다`() {
        initializer.run(DefaultApplicationArguments())

        assertTrue(savedCategories.isNotEmpty())
        verify(categoryRepository, atLeast(1)).save(any<Category>())
    }

    @Test
    fun `L1 카테고리 8개가 depth 0으로 생성된다`() {
        initializer.run(DefaultApplicationArguments())

        val l1Codes = listOf("electronics", "auto_tools", "fashion", "beauty", "food", "living", "hobby", "etc")
        val l1Categories = savedCategories.filter { it.depth == 0 }

        assertEquals(8, l1Categories.size)
        l1Codes.forEach { code ->
            assertTrue(l1Categories.any { it.code == code }, "L1 category '$code' should exist")
        }
    }

    @Test
    fun `L1 카테고리의 parentId는 null이다`() {
        initializer.run(DefaultApplicationArguments())

        val l1Categories = savedCategories.filter { it.depth == 0 }
        l1Categories.forEach { category ->
            assertNull(category.parentId, "L1 category '${category.code}' should have null parentId")
        }
    }

    @Test
    fun `하위 카테고리는 부모 ID를 가진다`() {
        initializer.run(DefaultApplicationArguments())

        val childCategories = savedCategories.filter { it.depth > 0 }
        childCategories.forEach { category ->
            assertNotNull(category.parentId, "Child category '${category.code}' should have parentId")
        }
    }

    @Test
    fun `smartphone 카테고리는 mobile의 하위이다`() {
        initializer.run(DefaultApplicationArguments())

        val mobile = savedCategories.find { it.code == "mobile" }
        val smartphone = savedCategories.find { it.code == "smartphone" }

        assertNotNull(mobile)
        assertNotNull(smartphone)
        assertEquals(mobile!!.id, smartphone!!.parentId)
    }

    @Test
    fun `기존 카테고리가 있으면 업데이트한다`() {
        val existingCategory = Category(
            code = "electronics",
            name = "구 전자",
            depth = 0
        )
        Category::class.java.superclass.getDeclaredField("id").apply {
            isAccessible = true
            set(existingCategory, 100L)
        }

        whenever(categoryRepository.findByCode("electronics")).thenReturn(existingCategory)
        whenever(categoryRepository.save(existingCategory)).thenReturn(existingCategory)

        initializer.run(DefaultApplicationArguments())

        assertEquals("전자·가전", existingCategory.name)
        assertEquals("Electronics", existingCategory.nameEn)
    }

    @Test
    fun `sortOrder는 순서대로 증가한다`() {
        initializer.run(DefaultApplicationArguments())

        for (i in 1 until savedCategories.size) {
            assertTrue(
                savedCategories[i].sortOrder > savedCategories[i - 1].sortOrder,
                "sortOrder should increase: ${savedCategories[i - 1].code}(${savedCategories[i - 1].sortOrder}) < ${savedCategories[i].code}(${savedCategories[i].sortOrder})"
            )
        }
    }

    @Test
    fun `etc 카테고리 하위에 subscription gift_card gifticon point가 있다`() {
        initializer.run(DefaultApplicationArguments())

        val etc = savedCategories.find { it.code == "etc" }
        assertNotNull(etc)

        val etcChildren = savedCategories.filter { it.parentId == etc!!.id }
        val expectedCodes = listOf("subscription", "gift_card", "gifticon", "point")

        expectedCodes.forEach { code ->
            assertTrue(etcChildren.any { it.code == code }, "etc should have child '$code'")
        }
    }
}
