package org.example.controllers

import org.example.Either
import org.example.Failure
import org.example.Success
import org.example.UserAuthService
import org.example.UserError
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateUserDTO
import org.example.dto.inputDto.LoginUserDTO
import org.example.dto.inputDto.UpdateUserDTO
import org.example.entity.User
import org.example.entity.toEmail
import org.example.entity.toName
import org.example.entity.toPassword
import org.example.entity.toUrlOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userServices: UserAuthService,
) {

    /*
    curl -X POST "http://localhost:8080/create/user" -H "Content-Type: application/json" -d "{\"name\":\"John Doe\",\"nickName\":\"john\",\"email\":\"john@example.com\",\"password\":\"Secret1\"}"
     */
    @PostMapping("/user/create")
    fun createUser(
        @RequestBody body: CreateUserDTO
    ):  ResponseEntity<*> {
        val name = body.name.toName()
        val nickName = body.nickName.toName()
        val email = body.email.toEmail()
        val password = body.password.toPassword()
        val imageUrl = body.imageUrl.toUrlOrNull()

        val result: Either<UserError, User> =
            userServices.createUser(name, nickName, email, password, imageUrl)

        return when(result){
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)

        }

    }

    @PostMapping("/user/login")
    fun loginUser(
        @RequestBody body: LoginUserDTO
    ) : ResponseEntity<*>{
        val result = userServices.createToken(body.email, body.password)
        return when(result){
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.ACCEPTED).body(result.value)
        }
    }

    @GetMapping("/user/info")
    fun getUserInfo(
         user: AuthenticatedUserDto
    ): ResponseEntity<*>  {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(" ${user.user.id}, \n ${user.user.email},\n ${user.user.name}")
    }

    @PostMapping("/user/update")
    fun updateUser(
        user: AuthenticatedUserDto,
        @RequestBody body: UpdateUserDTO
    ): ResponseEntity<*>{
        val result = userServices.updateUser(user.user.id, body.name, body.nickName, body.password, body.imageUrl)
        when(result){
            is Failure -> return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.value)
        }
    }
}