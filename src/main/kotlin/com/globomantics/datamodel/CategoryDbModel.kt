package com.globomantics.datamodel

@TableName("CATEGORY")
class CategoryDbModel {
    constructor(id: Int, name: String) {
        this.id = id
        this.name = name
    }

    @PrimaryKey
    @ColumnName("ID")
    var id: Int = 0

    @ColumnName("NAME")
    var name: String = ""
}