package com.example.plugins

import com.example.datamodel.BookDbModel
import com.example.datamodel.CategoryDbModel
import com.example.datamodel.DatabaseContext
import com.example.datamodel.UserDbModel
import com.example.services.Database
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val title: String,
    val isbn: String,
    val authors: Array<String>,
    val category: Int
)

@Serializable
data class User(val name: String)

@Serializable
data class Category(val name: String)

fun Application.configureRouting() {
    routing {
        route("/book") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val books = DatabaseContext.getEntities<BookDbModel>(connection, null)
                call.respond(
                    books.map {
                        Book(
                            it.title,
                            it.isbn,
                            it.authors.split(",").toTypedArray(),
                            it.category ?: -1
                        )
                    }.toTypedArray()
                )
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    val books = DatabaseContext.getEntities<BookDbModel>(connection, Pair(BookDbModel::id, id))
                    call.respond(
                        books.map {
                            Book(
                                it.title,
                                it.isbn,
                                it.authors.split(",").toTypedArray(),
                                it.category ?: -1
                            )
                        }.toTypedArray()
                    )
                }
            }
            put("/{id}") {
                val bookId = call.parameters["id"]?.toIntOrNull()
                val operation = call.parameters["operation"]
                val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                if (bookId == null || operation == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid bookId or userId")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    DatabaseContext.updateEntities<BookDbModel>(
                        connection,
                        Pair(BookDbModel::id, bookId),
                    ) {
                        if (operation == "rent") {
                            it.withRenterId(userId)
                        } else if (operation == "return") {
                            it.withRenterId(null)
                        } else {
                            throw Exception("Invalid operation $operation")
                        }
                    }
                }
            }
            post {
                val book = call.receive<Book>()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                DatabaseContext.addEntity(
                    connection,
                    BookDbModel(
                        book.title,
                        book.isbn,
                        book.authors.joinToString(","),
                        null,
                        book.category
                    )
                )
            }
        }
        route("/user") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val users = DatabaseContext.getEntities<UserDbModel>(connection, null)
                call.respond(users.map { User(it.name) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    val users = DatabaseContext.getEntities<UserDbModel>(connection, Pair(UserDbModel::id, id))
                    call.respond(users.map { User(it.name) }.toTypedArray())
                }
            }
            post {
                val name = call.receiveText()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                DatabaseContext.addEntity(connection, UserDbModel(name))
            }
        }
        route("/category") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val categories = DatabaseContext.getEntities<CategoryDbModel>(connection, null)
                call.respond(categories.map { Category(it.name) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    val categories = DatabaseContext.getEntities<CategoryDbModel>(connection, Pair(CategoryDbModel::id, id))
                    call.respond(categories.map { Category(it.name) }.toTypedArray())
                }
            }
            post {
                val name = call.receiveText()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                DatabaseContext.addEntity(connection, CategoryDbModel(name))
            }
        }
    }
}
