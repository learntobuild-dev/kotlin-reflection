package com.example.datamodel

@TableName("USER")
class UserDbModel {
    constructor(name: String) {
        this.name = name
    }

    @PrimaryKey
    @ColumnName("ID")
    var id: Int = 0

    @ColumnName("NAME")
    var name: String = ""
}