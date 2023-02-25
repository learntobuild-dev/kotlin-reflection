package com.globomantics.datamodel

@TableName("USER")
class UserDbModel(): DbEntity() {
    constructor(name: String) : this() {
        this.name = name
    }

    @PrimaryKey
    @ColumnName("ID")
    var id: Int = 0

    @ColumnName("NAME")
    var name: String = ""
}