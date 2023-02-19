package com.example.datamodel

import java.sql.Connection
import java.sql.DriverManager
import kotlin.reflect.*
import kotlin.reflect.full.*

class DatabaseContext {
    val Book: BookDbModel = BookDbModel()
    val Category: CategoryDbModel = CategoryDbModel()
    val User: UserDbModel = UserDbModel()

    val connection: Connection

    constructor() {
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:test1.db")
        ensureCreated(connection)
    }

    inline fun <reified T : Any> updateEntities(
        filter: Pair<KProperty<*>, Any>,
        crossinline update: (T) -> T) {
        updateEntities(connection, typeOf<T>(), filter) { update(it as T) }
    }

    inline fun <reified T : Any> getEntities(
        filter: Pair<KProperty<*>, Any>?): Array<T> {
        val entities = getEntities(connection, typeOf<T>(), filter)
        return entities.map { it as T }.toTypedArray()
    }

    inline fun <reified T : Any> addEntity(
        value: T) {
        val valueType = typeOf<T>()
        addEntity(connection, value, valueType)
    }

    companion object {
        fun updateEntities(
            connection: Connection,
            entityType: KType,
            filter: Pair<KProperty<*>, Any>,
            update: (Any) -> Any) {
            val entityClass = entityType.classifier as KClass<*>
            val annotatedTableName =
                entityClass.findAnnotation<TableName>()?.tableName
            val actualTableName =
                annotatedTableName
                    ?: entityClass.simpleName
                    ?: throw Exception("Could not obtain table name")
            val entities = getEntities(connection, entityType, filter)
            for (entity in entities) {
                val changes = mutableListOf<String>()
                val updatedEntity = update(entity)
                for (property in entityClass.declaredMemberProperties) {
                    if (property.hasAnnotation<PrimaryKey>()) {
                        continue
                    }
                    val oldValue = property.call(entity)
                    val newValue = property.call(updatedEntity)
                    if (oldValue != newValue) {
                        val annotatedColumnName =
                            property.findAnnotation<ColumnName>()?.columnName
                        val actualColumnName =
                            annotatedColumnName ?: property.name
                        val propertyType =
                            property.returnType.withNullability(false)
                        if (propertyType == typeOf<Int>()) {
                            changes.add("$actualColumnName=$newValue")
                        } else {
                            if (propertyType == typeOf<String>()) {
                                changes.add("$actualColumnName='$newValue'")
                            } else {
                                throw Exception("Unsupported property type")
                            }
                        }
                    }
                }
                if (changes.size > 0) {
                    val statement = StringBuilder()
                    statement.append("UPDATE $actualTableName SET ")
                    statement.append(changes.joinToString(","))
                    statement.append(" ${buildFilter(filter)}")
                    connection.createStatement().use { it.execute(statement.toString()) }
                }
            }
        }

        fun getEntities(
            connection: Connection,
            entityType: KType,
            filter: Pair<KProperty<*>, Any>?): Array<Any> {
            val entityClass = entityType.classifier as KClass<*>
            val annotatedTableName =
                entityClass.findAnnotation<TableName>()?.tableName
            val actualTableName =
                annotatedTableName
                    ?: entityClass.simpleName
                    ?: throw Exception("Could not obtain table name")
            val result = mutableListOf<Any>()
            connection.createStatement().use {
                val statement = StringBuilder()
                statement.append("SELECT * FROM $actualTableName ")
                if (filter != null) {
                    statement.append(buildFilter(filter))
                }
                val queryResult = it.executeQuery(statement.toString())
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

        private fun buildFilter(
            filter: Pair<KProperty<*>, Any>): String {
            val annotatedFilterColumnName =
                filter.first.findAnnotation<ColumnName>()?.columnName
            val actualFilterColumnName =
                annotatedFilterColumnName ?: filter.first.name
            val filterPropertyType =
                filter.first.returnType.withNullability(false)
            if (filterPropertyType == typeOf<Int>()) {
                return "WHERE $actualFilterColumnName=${filter.second}"
            } else {
                if (filterPropertyType == typeOf<String>()) {
                    return "WHERE $actualFilterColumnName='${filter.second}'"
                } else {
                    throw Exception("Unsupported filter property type")
                }
            }
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
                if (!propertyType.isSubtypeOf(typeOf<DbEntity>())) {
                    continue
                }
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