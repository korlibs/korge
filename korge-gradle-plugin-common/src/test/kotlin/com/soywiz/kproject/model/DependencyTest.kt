package com.soywiz.kproject.model

import com.soywiz.kproject.util.*
import kotlin.test.*

class DependencyTest {
    @Test
    fun testGit() {
        assertEquals(
            GitDependency("adder", GitRepository("https://github.com/korlibs/kproject.git"), "/modules/adder", ref = "54f73b01cea9cb2e8368176ac45f2fca948e57db"),
            Dependency.parseString("git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db")
        )
    }

    @Test
    fun testGitNewNew() {
        assertEquals(
            GitDependency("korge-parallax", GitRepository("https://github.com/korlibs/korge-parallax.git"), "/korge-parallax", ref = "dacd7f4c430c48349565295394f723b05841c54a"),
            Dependency.parseString("https://github.com/korlibs/korge-parallax/tree/dacd7f4c430c48349565295394f723b05841c54a/korge-parallax")
        )
    }

    @Test
    fun testMaven() {
        assertEquals(
            MavenDependency(group="org.jetbrains.kotlinx", name="kotlinx-coroutines-core", version= Version("1.6.4"), target="common"),
            Dependency.parseString("maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        )

        assertEquals(
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",
            MavenDependency(group="org.jetbrains.kotlinx", name="kotlinx-coroutines-core", version=Version("1.6.4"), target="common").coordinates
        )
    }

    @Test
    fun testMavenNew() {
        assertEquals(
            MavenDependency(group="org.jetbrains.kotlinx", name="kotlinx-coroutines-core", version=Version("1.6.4"), target="common"),
            Dependency.parseString("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"),
        )
        assertEquals(
            MavenDependency(group="org.jetbrains.kotlinx", name="kotlinx-coroutines-core", version=Version("1.6.4"), target="jvm"),
            Dependency.parseString("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4::jvm"),
        )
        assertEquals(
            MavenDependency(group="com.soywiz.korlibs.korge2", name="korge", version=Version(""), target="common"),
            Dependency.parseString("com.soywiz.korlibs.korge2:korge"),
        )

    }

    @Test
    fun testGitNew() {
        assertEquals(
            GitDependency(name="korge-tiled", repo=GitRepository("git@github.com:korlibs/korge-ext.git"), path="/korge-tiled", ref="0.0.1", commit="734d96ccc18733064ef9fbda8ac359585011112d", hash=null),
            Dependency.parseString("git@github.com:korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d")
        )
        assertEquals(
            GitDependency(name="korge-tiled", repo=GitRepository("https://github.com/korlibs/korge-ext.git"), path="/korge-tiled", ref="0.0.1", commit="734d96ccc18733064ef9fbda8ac359585011112d", hash=null),
            Dependency.parseString("https://github.com/korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d")
        )
        assertEquals(
            GitDependency(name="korge-ext", repo=GitRepository("git@github.com:korlibs/korge-ext.git"), path="/", ref="0.0.1", commit="734d96ccc18733064ef9fbda8ac359585011112d", hash=null),
            Dependency.parseString("git@github.com:korlibs/korge-ext.git/#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d")
        )
        assertEquals(
            GitDependency(name="korge-ext", repo=GitRepository("git@github.com:korlibs/korge-ext.git"), path="/", ref="0.0.1", commit="734d96ccc18733064ef9fbda8ac359585011112d", hash=null),
            Dependency.parseString("git@github.com:korlibs/korge-ext.git#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d")
        )
        assertEquals(
            GitDependency(name="korge-ext", repo=GitRepository("https://github.com/korlibs/korge-ext.git"), path="/", ref="0.0.1", commit="734d96ccc18733064ef9fbda8ac359585011112d", hash=null),
            Dependency.parseString("https://github.com/korlibs/korge-ext.git#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d")
        )
    }

    @Test
    fun testPath() {
        val root = MemoryFiles().root
        assertEquals(
            FileRefDependency(root["korge-tiled/kproject.yml"]),
            Dependency.parseString("../korge-tiled", root["hello/kproject.yml"])
        )
    }

    @Test
    fun testCommitCount() {
        assertEquals(
            30,
            (Dependency.parseString("git@github.com:korlibs/korge-ext.git#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d") as GitDependency).commitCount
        )
    }
}
