package com.globomantics.plugins

import com.example.services.Mapper
import com.globomantics.datamodel.BookDbModel
import com.globomantics.datamodel.CategoryDbModel
import com.globomantics.datamodel.DatabaseContext
import com.globomantics.datamodel.UserDbModel
import com.globomantics.services.Database
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.example.ISBNValidationResult
import org.example.ISBNValidator

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

fun Application.configureRouting() {
    routing {
        route("/book") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val books = DatabaseContext.getEntities<BookDbModel>(connection, null)
                call.respond(books.map { Mapper.map<BookDbModel, Book>(it) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    val books = DatabaseContext.getEntities<BookDbModel>(connection, Pair(BookDbModel::id, id))
                    call.respond(books.map {Mapper.map<BookDbModel, Book>(it) }.toTypedArray())
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
                val result = ISBNValidator.validate(book.isbn)
                val errorCodeField = ISBNValidationResult::class.java.getDeclaredField("errorCode")
                if (!errorCodeField.trySetAccessible()) {
                    call.respond(HttpStatusCode.InternalServerError)
                } else {
                    if (errorCodeField.getInt(result) != 0) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid ISBN")
                    } else {
                        val connection = Database.getConnection()
                        DatabaseContext.ensureCreated(connection)
                        DatabaseContext.addEntity(connection, Mapper.map<Book, BookDbModel>(book))
                    }
                }
            }
        }
        route("/user") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val users = DatabaseContext.getEntities<UserDbModel>(connection, null)
                call.respond(users.map { Mapper.map<UserDbModel, User>(it) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    val users = DatabaseContext.getEntities<UserDbModel>(connection, Pair(UserDbModel::id, id))
                    call.respond(users.map { Mapper.map<UserDbModel, User>(it) }.toTypedArray())
                }
            }
            post {
                val user = call.receive<User>()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                DatabaseContext.addEntity(
                    connection,
                    Mapper.map<User, UserDbModel>(user))
            }
        }
        route("/category") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val categories = DatabaseContext.getEntities<CategoryDbModel>(connection, null)
                call.respond(categories.map { Mapper.map<CategoryDbModel, Category>(it) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    val categories = DatabaseContext.getEntities<CategoryDbModel>(connection, Pair(CategoryDbModel::id, id))
                    call.respond(categories.map { Mapper.map<CategoryDbModel, Category>(it) }.toTypedArray())
                }
            }
            post {
                val category = call.receive<Category>()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                DatabaseContext.addEntity(
                    connection,
                    Mapper.map<Category, CategoryDbModel>(category))
            }
        }
    }
}
