package com.github.s8u.hotdeallist.dto.response

import com.github.s8u.hotdeallist.enums.AdminOpsStage

data class AdminOpsResponse(
    val elapsedMs: Long,
    val dryRun: Boolean,
    val stages: List<AdminOpsStage>,
    val totals: Totals,
    val byStage: Map<String, Map<String, Int>>,
    val errors: List<Error>
) {
    data class Totals(
        val targeted: Int,
        val processed: Int,
        val skipped: Int,
        val failed: Int
    )

    data class Error(
        val rawId: Long? = null,
        val hotdealId: Long? = null,
        val stage: String,
        val message: String
    )
}
