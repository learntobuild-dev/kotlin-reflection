package com.example.datamodel

@TableName("CATEGORY")
class CategoryDbModel {
    constructor(name: String) {
        this.name = name
    }

    @PrimaryKey
    @ColumnName("ID")
    var id: Int = 0

    @ColumnName("NAME")
    var name: String = ""
}