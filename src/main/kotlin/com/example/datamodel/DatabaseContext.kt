package com.example.datamodel

import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.typeOf

class DatabaseContext(
    val Book: BookDbModel,
    val Category: CategoryDbModel,
    val User: UserDbModel
) {
    companion object {
        inline fun <reified T : Any> getEntities(
            connection: Connection): Array<T> {
            val entities = getEntities(connection, typeOf<T>());
            return entities.map { it as T }.toTypedArray()
        }

        fun getEntities(
            connection: Connection,
            entityType: KType): Array<Any> {
            val entityClass = entityType.classifier as KClass<*>
            val annotatedTableName =
                entityClass.findAnnotation<TableName>()?.tableName
            val actualTableName =
                annotatedTableName
                    ?: entityClass.simpleName
                    ?: throw Exception("Could not obtain table name")
            var result = mutableListOf<Any>()
            connection.createStatement().use {
                val queryResult = it.executeQuery("SELECT * FROM $actualTableName")
                while (queryResult.next()) {
                    val entityInstance = entityClass.createInstance()
                    for (property in entityClass.declaredMemberProperties) {
                        val annotatedColumName = property.findAnnotation<ColumnName>()?.columnName
                        val actualColumnName = annotatedColumName ?: property.name
                        if (property is KMutableProperty<*>) {
                            val columnValue = queryResult.getObject(actualColumnName)
                            property.setter.call(entityInstance, columnValue)
                        }
                    }
                    result.add(entityInstance)
                }
            }
            return result.toTypedArray()
        }

        inline fun <reified T : Any> addEntity(
            connection: Connection,
            value: T) {
            val valueType = typeOf<T>()
            addEntity(connection, value, valueType)
        }

        fun addEntity(
            connection: Connection,
            value: Any,
            valueType: KType) {
            val tableClass = valueType.classifier as KClass<*>
            val annotatedTableName =
                tableClass.findAnnotation<TableName>()?.tableName
            val actualTableName =
                annotatedTableName
                    ?: tableClass.simpleName
                    ?: throw Exception("Could not obtain table name")
            val columnNames = mutableListOf<String>()
            val columnValues = mutableListOf<String>()
            for (property in tableClass.declaredMemberProperties) {
                val annotatedColumName = property.findAnnotation<ColumnName>()?.columnName
                val actualColumName = annotatedColumName ?: property.name
                columnNames.add("'$actualColumName'")
                if (property.hasAnnotation<PrimaryKey>()) {
                    //TODO We assume that the primary key is of type Int
                    if (property.returnType != typeOf<Int>()) {
                        throw Exception("Unsupported primary key type")
                    }
                    val nextId = (getCount(connection, actualTableName) ?: 0) + 1
                    columnValues.add(nextId.toString())
                } else {
                    val propertyValue = property.call(value)
                    if (propertyValue == null) {
                        if (property.returnType.isMarkedNullable) {
                            columnValues.add("NULL")
                        } else {
                            throw Exception("NULL property value for NOT NULL column")
                        }
                    } else {
                        val propertyType =
                            if (property.returnType.isMarkedNullable)
                                property.returnType.withNullability(false)
                            else
                                property.returnType
                        //TODO We assume that toString will produce valid SQL value
                        val value = propertyValue.toString()
                        if (propertyType == typeOf<Int>()) {
                            columnValues.add(value)
                        } else {
                            if (propertyType == typeOf<String>()) {
                                columnValues.add("'$value'")
                            } else {
                                throw Exception("Unsupported property type")
                            }
                        }
                    }
                }
            }
            val sqlStatement =
                "INSERT INTO $actualTableName " +
                "(${columnNames.joinToString(",")}) " +
                "VALUES (${columnValues.joinToString(",")})"
            connection.createStatement().use {
                it.execute(sqlStatement)
            }
        }

        private fun getCount(
            connection: Connection,
            tableName: String): Int? {
            connection.createStatement().use { stmt ->
                val sql = "SELECT COUNT(*) FROM $tableName"
                val queryResult = stmt.executeQuery(sql)
                while (queryResult.next()) {
                    return queryResult.getInt(1)
                }
            }
            return null
        }

        fun ensureCreated(
            connection: Connection) {
            val contextClass = DatabaseContext::class
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

        private fun getTables(
            connection: Connection): Array<String> {
            val stmt = connection.createStatement()
            val sql = "select name from sqlite_schema where type = 'table'"
            val queryResult = stmt.executeQuery(sql)
            val result: MutableList<String> = mutableListOf()
            while (queryResult.next()) {
                result.add(queryResult.getString("name"))
            }
            stmt.close()
            return result.toTypedArray()
        }

        private fun buildCreateTable(
            type: KType): String {
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

        private fun toSQLTypeName(
            kotlinType: KType): String {
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
    }
}