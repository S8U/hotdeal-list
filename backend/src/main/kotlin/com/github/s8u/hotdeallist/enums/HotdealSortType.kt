package com.github.s8u.hotdeallist.enums

import org.springframework.data.domain.Sort

enum class HotdealSortType(val field: String, val direction: Sort.Direction) {
    LATEST("createdAt", Sort.Direction.DESC),
    POPULAR("likeCount", Sort.Direction.DESC),
    COMMENTS("commentCount", Sort.Direction.DESC),
    VIEWS("viewCount", Sort.Direction.DESC)
}
