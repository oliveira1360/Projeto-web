package org.example.auth

import org.example.controllers.ApiMediaTypes
import org.example.controllers.ProblemDetail
import org.example.controllers.ProblemTypes
import org.example.controllers.UnauthorizedException
import org.example.controllers.createProblemDetail
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler

@ExceptionHandler(UnauthorizedException::class)
fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ProblemDetail> =
    ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
        .body(
            createProblemDetail(
                type = ProblemTypes.UNAUTHORIZED,
                title = "Unauthorized",
                status = HttpStatus.UNAUTHORIZED,
                detail = ex.message ?: "Invalid or missing authentication",
                instance = "/auth",
            ),
        )
