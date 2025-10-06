package org.example

import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User

interface RepositoryUser : Repository<User> {
    fun findByEmail(email: Email): User?
    fun createUser(name: Name, nickName: Name, email: Email, password: Password, imageUrl: URL? = null): User

}