package com.soywiz.kproject.model

import com.soywiz.kproject.internal.*

//enum class NewKProjectType { LIBRARY, EXECUTABLE }

data class NewKProjectModel(
    val file: FileRef,
    val name: String?,
    val src: Dependency? = null,
    val version: String? = null,
    val targets: List<String> = emptyList(),
    val plugins: List<KPPlugin> = emptyList(),
    val dependencies: List<Dependency> = emptyList(),
    val testDependencies: List<Dependency> = emptyList(),
    val versions: Map<String, String> = emptyMap(),
) {
    val allDependencies by lazy { dependencies + testDependencies }

    val targetsSet = targets.map { KProjectTarget[it] }.toSet()

    fun hasTarget(target: KProjectTarget): Boolean {
        if (targetsSet.isEmpty()) return true
        return target in targetsSet
    }

    companion object
}

fun NewKProjectModel.Companion.loadFile(file: FileRef): NewKProjectModel {
    return parseObject(Yaml.decode(file.readText()), file)
}

fun NewKProjectModel.Companion.parseObject(data: Any?, file: FileRef = MemoryFileRef("unknown.kproject.yml", byteArrayOf())): NewKProjectModel {
    val data = data.dyn

    //println("data[\"versions\"]=${data["versions"]}")

    val dataVersions = data["versions"]

    return NewKProjectModel(
        file = file,
        name = data["name"].toStringOrNull() ?: (if (file.name != "kproject.yml") file.name.removeSuffix(".kproject.yml") else file.parent().name),
        src = data["src"].orNull { Dependency.parseObject(it.value, file) },
        version = data["version"].orNull { it.str },
        targets = data["targets"].list.map { it.str },
        plugins = data["plugins"].list.map { KPPlugin.parseObject(it.value) },
        dependencies = data["dependencies"].list.map { Dependency.parseObject(it.value, file) },
        testDependencies = data["testDependencies"].list.map { Dependency.parseObject(it.value, file) },
        versions = when (dataVersions.value) {
            is List<*> -> dataVersions.list.associate {
                val entry = it.map.entries.first()
                entry.key.str to entry.value.str
            }
            else -> dataVersions.map.toList().associate { it.first.str to it.second.str }
        }
    )
}
