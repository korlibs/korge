---
layout: default
title: KMiniOrm
fa-icon: fa-database
priority: 100
---

KMiniOrm is a ORM (only JVM at the moment).
It supports MongoDB, In-Memory for testing and JDBC sources like PosgreSQL, MySQL, SQLite and H2.

It has been designed to be a pleasure to use,
for querying, inserting and updating,
and to maintain your structure with as little manual migrations as possible.

It has scalability on mind, trying to avoid joins and providing a basic interface that can be implemented
on most databases. While still providing raw Query support on SQL-based implementations.

<https://github.com/korlibs/kminiorm/>{:target="_blank",:rel="noopener"}

{% include stars.html project="kminiorm" %}

{% include toc_include.md max_level="3" %}

## Small sample

```kotlin
data class MyTable(
    @DbPrimary val key: String,
    @DbIndex val value: Long
) : DbBaseModel

val sqliteFile = File("sample.sq3")
val db = JdbcDb(
    "jdbc:sqlite:${sqliteFile.absoluteFile.toURI()}",
    debugSQL = System.getenv("DEBUG_SQL") == "true",
    dialect = SqliteDialect,
    async = true
)

val table = db.table<MyTable>()
table.insert(
    MyTable("hello", 10L),
    MyTable("world", 20L),
    MyTable("this", 30L),
    MyTable("is", 40L),
    MyTable("a", 50L),
    MyTable("test", 60L),
    onConflict = DbOnConflict.IGNORE
)

table.where { it::value ge 20L }.limit(10).collect {
    println(it)
}
```

## Defining Tables

You can use normal Kotlin properties for defining columns.
And can use `@DbName` on classes and properties to overwrite
its internal name representation.

When a column is added or removed to/from your model,
or an index annotation added, the table structure is updated.
The table creation/migration happens when you request
the table with `val table = db.table<ModelClass>()`. This
'table' instance acts as a model repository. 

### Single-column indices

You can use `@DbPrimary`, `@DbUnique` and `@DbIndex` annotations
to annotate single properties.

```kotlin
data class MyTable(
    @DbPrimary val key: String,
    @DbIndex val value: Long
) : DbBaseModel
```

### Multi-column indices

When using the index annotations with a repeated name string,
it creates a compound index with those columns together.

```kotlin
data class MyTable(
    @DbUnique("a_b") val a: String,
    @DbUnique("a_b") val b: String
) : DbBaseModel
```

## TODO

This document is under construction.
