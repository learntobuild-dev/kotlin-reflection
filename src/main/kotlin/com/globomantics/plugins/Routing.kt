package com.globomantics.plugins

import com.example.services.Mapper
import com.example.services.ServiceProvider
import com.globomantics.datamodel.BookDbModel
import com.globomantics.datamodel.CategoryDbModel
import com.globomantics.datamodel.DatabaseContext
import com.globomantics.datamodel.UserDbModel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.DriverManager
import kotlin.reflect.typeOf
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
                val context = buildServiceProvider().getService<DatabaseContext>()
                val books = context.getEntities<BookDbModel>(null)
                call.respond(books.map { Mapper.map<BookDbModel, Book>(it) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val context = buildServiceProvider().getService<DatabaseContext>()
                    val books = context.getEntities<BookDbModel>(Pair(BookDbModel::id, id))
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
                    val context = buildServiceProvider().getService<DatabaseContext>()
                    context.updateEntities<BookDbModel>(Pair(BookDbModel::id, bookId)) {
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
                val context = buildServiceProvider().getService<DatabaseContext>()
                context.addEntity(Mapper.map<Book, BookDbModel>(book))
            }
        }
        route("/user") {
            get {
                val context = buildServiceProvider().getService<DatabaseContext>()
                val users = context.getEntities<UserDbModel>(null)
                call.respond(users.map { Mapper.map<UserDbModel, User>(it) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val context = buildServiceProvider().getService<DatabaseContext>()
                    val users = context.getEntities<UserDbModel>(Pair(UserDbModel::id, id))
                    call.respond(users.map { Mapper.map<UserDbModel, User>(it) }.toTypedArray())
                }
            }
            post {
                val user = call.receive<User>()
                val context = buildServiceProvider().getService<DatabaseContext>()
                context.addEntity(Mapper.map<User, UserDbModel>(user))
            }
        }
        route("/category") {
            get {
                val context = buildServiceProvider().getService<DatabaseContext>()
                val categories = context.getEntities<CategoryDbModel>(null)
                call.respond(categories.map { Mapper.map<CategoryDbModel, Category>(it) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val context = buildServiceProvider().getService<DatabaseContext>()
                    val categories = context.getEntities<CategoryDbModel>(Pair(CategoryDbModel::id, id))
                    call.respond(categories.map { Mapper.map<CategoryDbModel, Category>(it) }.toTypedArray())
                }
            }
            post {
                val category = call.receive<Category>()
                val context = buildServiceProvider().getService<DatabaseContext>()
                context.addEntity(Mapper.map<Category, CategoryDbModel>(category))
            }
        }
    }
}
