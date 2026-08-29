package com.github.s8u.hotdeallist.controller

import com.github.s8u.hotdeallist.dto.request.AdminOpsRequest
import com.github.s8u.hotdeallist.dto.response.AdminOpsResponse
import com.github.s8u.hotdeallist.service.AdminOpsService
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import java.net.InetAddress
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

    @PostMapping("/run")
    fun run(
        @RequestBody request: AdminOpsRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AdminOpsResponse> {
        verifyInternalAccess(httpRequest)
        return ResponseEntity.ok(adminOpsService.execute(request))
    }

    // 외부 접근은 Nginx에서 차단하고 Docker bridge를 통한 호스트 요청만 허용한다.
    private fun verifyInternalAccess(httpRequest: HttpServletRequest) {
        val remoteAddr = httpRequest.remoteAddr
        if (!isInternalAddress(remoteAddr)) {
            logger.warn("Admin ops access denied remoteAddr={}", remoteAddr)
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    private fun isInternalAddress(remoteAddr: String?): Boolean {
        if (remoteAddr.isNullOrBlank()) return false

        return runCatching {
            val address = InetAddress.getByName(remoteAddr)
            address.isLoopbackAddress || address.isSiteLocalAddress
        }.getOrDefault(false)
    }
}
