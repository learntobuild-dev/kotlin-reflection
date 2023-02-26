package com.globomantics.plugins

import com.globomantics.services.Repository
import com.globomantics.services.ServiceProvider
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.example.ISBNValidationResult
import org.example.ISBNValidator
import java.sql.Connection
import java.sql.DriverManager
import kotlin.reflect.typeOf

@Serializable
data class Book(
    val title: String,
    val isbn: String,
    val authors: String,
    val category: Int?
)

@Serializable
data class User(val name: String)

@Serializable
data class Category(val name: String)

fun buildDatabaseConnection(): Connection {
    Class.forName("org.sqlite.JDBC")
    return DriverManager.getConnection("jdbc:sqlite:test1.db")
}

fun buildServiceProvider(): ServiceProvider {
    val result = ServiceProvider()
    result.add(typeOf<Connection>(), buildDatabaseConnection())
    return result
}

fun Application.configureRouting() {
    routing {
        route("/book") {
            get {
                val repository = buildServiceProvider().getService<Repository>()
                val books = repository.getAllBooks()
                call.respond(books)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val repository = buildServiceProvider().getService<Repository>()
                    val book = repository.getBook(id)
                    call.respond(book)
                }
            }
            put("/{id}") {
                val bookId = call.parameters["id"]?.toIntOrNull()
                val operation = call.parameters["operation"]
                val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                if (bookId == null || operation == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid bookId or userId")
                } else {
                    val context = buildServiceProvider().getService<Repository>()
                    context.setRenterId(
                        bookId,
                        if (operation == "rent") {
                            userId
                        } else if (operation == "return") {
                            null
                        } else {
                            throw Exception("Invalid operation $operation")
                        })
                }
            }
            post {
                val book = call.receive<Book>()
                val result = ISBNValidator.validate(book.isbn)
                val errorCodeField = ISBNValidationResult::class.java.getDeclaredField("errorCode")
                if (!errorCodeField.trySetAccessible()) {
                    call.respond(HttpStatusCode.InternalServerError)
                } else {
                    if (errorCodeField.getInt(result) != 0) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid ISBN")
                    } else {
                        val repository = buildServiceProvider().getService<Repository>()
                        repository.addBook(book)
                    }
                }
            }
        }
        route("/user") {
            get {
                val repository = buildServiceProvider().getService<Repository>()
                val users = repository.getAllUsers()
                call.respond(users)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val repository = buildServiceProvider().getService<Repository>()
                    val user = repository.getUser(id)
                    call.respond(user)
                }
            }
            post {
                val user = call.receive<User>()
                val repository = buildServiceProvider().getService<Repository>()
                repository.addUser(user)
            }
        }
        route("/category") {
            get {
                val context = buildServiceProvider().getService<Repository>()
                val categories = context.getAllCategories()
                call.respond(categories)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val context = buildServiceProvider().getService<Repository>()
                    val category = context.getCategory(id)
                    call.respond(category)
                }
            }
            post {
                val category = call.receive<Category>()
                val repository = buildServiceProvider().getService<Repository>()
                repository.addCategory(category)
            }
        }
    }
}
