package com.globomantics.services

import com.globomantics.datamodel.ColumnName
import com.globomantics.datamodel.DatabaseContext
import com.globomantics.datamodel.PrimaryKey
import com.globomantics.datamodel.TableName
import java.sql.Connection
import java.sql.DriverManager
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

class Database {
    companion object {
        fun getUserCount(): Int? {
            ensureCreated(typeOf<DatabaseContext>())
            getConnection().use {
                it.createStatement().use { stmt ->
                    val sql = "SELECT COUNT(*) FROM USER"
                    val queryResult = stmt.executeQuery(sql)
                    while (queryResult.next()) {
                        return queryResult.getInt(1)
                    }
                }
            }
            return null
        }

        fun getCategoryCount(): Int? {
            ensureCreated(typeOf<DatabaseContext>())
            getConnection().use {
                it.createStatement().use { stmt ->
                    val sql = "SELECT COUNT(*) FROM CATEGORY"
                    val queryResult = stmt.executeQuery(sql)
                    while (queryResult.next()) {
                        return queryResult.getInt(1)
                    }
                }
            }
            return null
        }

        fun getBookCount(): Int? {
            ensureCreated(typeOf<DatabaseContext>())
            getConnection().use {
                it.createStatement().use { stmt ->
                    val sql = "SELECT COUNT(*) FROM BOOK"
                    val queryResult = stmt.executeQuery(sql)
                    while (queryResult.next()) {
                        return queryResult.getInt(1)
                    }
                }
            }
            return null
        }

        fun ensureCreated(context: KType) {
            val contextClass = context.classifier as KClass<*>
            val connection = getConnection()
            val tables = getTables(connection)
            for (property in contextClass.declaredMemberProperties) {
                val propertyType = property.returnType
                val propertyClass = propertyType.classifier as KClass<*>
                val tableName =
                    propertyClass
                        .findAnnotation<TableName>()
                        ?.tableName
                        ?: propertyClass.simpleName
                if (!tables.contains(tableName)) {
                    connection.createStatement().use {
                        it.execute(buildCreateTable(propertyType))
                    }
                }
            }
        }

        private fun getTables(conn: Connection): Array<String> {
            val stmt = conn.createStatement()
            val sql = "select name from sqlite_schema where type = 'table'"
            val queryResult = stmt.executeQuery(sql)
            val result: MutableList<String> = mutableListOf()
            while (queryResult.next()) {
                result.add(queryResult.getString("name"))
            }
            stmt.close()
            return result.toTypedArray()
        }

        private fun buildCreateTable(type: KType): String {
            val tableClass = type.classifier as KClass<*>
            val statement = StringBuilder()
            val annotatedTableName =
                tableClass.findAnnotation<TableName>()?.tableName
            val actualTable =
                annotatedTableName
                    ?: tableClass.simpleName
                    ?: throw Exception("Could not obtain table name")
            statement.append("CREATE TABLE $actualTable (")
            val properties = tableClass.declaredMemberProperties
            for (propertyIndex in properties.indices) {
                val property = properties.elementAt(propertyIndex)
                val annotatedColumName = property.findAnnotation<ColumnName>()?.columnName
                val actualColumName = annotatedColumName ?: property.name
                val sqlTypeName = toSQLTypeName(property.returnType)
                val primaryKey = if (property.hasAnnotation<PrimaryKey>()) "PRIMARY KEY" else ""
                val nullity = if (property.returnType.isMarkedNullable) "NULL" else "NOT NULL"
                statement.append("$actualColumName $sqlTypeName $primaryKey $nullity")
                if (propertyIndex < properties.size - 1) {
                    statement.append(", ")
                }
            }
            statement.append(")")
            return statement.toString()
        }

        private fun toSQLTypeName(kotlinType: KType): String {
            val actualType =
                if (kotlinType.isMarkedNullable)
                    kotlinType.withNullability(false)
                else
                    kotlinType
            if (actualType == typeOf<Int>()) {
                return "INT"
            } else {
                if (actualType == typeOf<String>()) {
                    return "TEXT"
                } else {
                    throw Exception("Unsupported type $actualType")
                }
            }
        }

        fun getConnection(): Connection {
            Class.forName("org.sqlite.JDBC")
            return DriverManager.getConnection("jdbc:sqlite:BookStore.db")
        }
    }
}