package com.github.s8u.hotdeallist.controller

import com.github.s8u.hotdeallist.dto.request.AdminOpsRequest
import com.github.s8u.hotdeallist.dto.response.AdminOpsResponse
import com.github.s8u.hotdeallist.service.AdminOpsService
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Hidden
@RestController
@RequestMapping("/admin/ops")
class AdminOpsController(
    private val adminOpsService: AdminOpsService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val LOCALHOST_ADDRESSES = setOf("127.0.0.1", "0:0:0:0:0:0:0:1", "::1")
    }

    @PostMapping("/run")
    fun run(
        @RequestBody request: AdminOpsRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AdminOpsResponse> {
        verifyLocalhost(httpRequest)
        return ResponseEntity.ok(adminOpsService.execute(request))
    }

    private fun verifyLocalhost(httpRequest: HttpServletRequest) {
        val remoteAddr = httpRequest.remoteAddr
        if (remoteAddr !in LOCALHOST_ADDRESSES) {
            logger.warn("Admin ops access denied remoteAddr={}", remoteAddr)
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }
}
