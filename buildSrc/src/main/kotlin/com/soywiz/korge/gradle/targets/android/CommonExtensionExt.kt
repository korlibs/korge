package com.soywiz.korge.gradle.targets.android

import com.android.build.api.dsl.*
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.DefaultConfig
import com.android.build.gradle.internal.dsl.*
import com.soywiz.korge.gradle.*
import org.gradle.api.*
import kotlin.collections.*

// this tries to resolve inconsistency between versions (one using this receiver and others using it)
fun <DefaultConfigT : DefaultConfig> CommonExtension<*, *, DefaultConfigT, *>.defaultConfigThis(action: DefaultConfigT.() -> Unit) = defaultConfig(action)
fun <BuildTypeT : BuildType> CommonExtension<*, BuildTypeT, *, *>.buildTypesThis(action: NamedDomainObjectContainer<BuildTypeT>.() -> Unit) = buildTypes(action)
fun CommonExtension<*, *, *, *>.packagingOptionsThis(action: com.android.build.api.dsl.PackagingOptions.() -> Unit) = packagingOptions(action)
fun CommonExtension<*, *, *, *>.signingConfigsThis(action: NamedDomainObjectContainer<out ApkSigningConfig>.() -> Unit) = signingConfigs(action)
fun CommonExtension<*, *, *, *>.sourceSetsThis(action: NamedDomainObjectContainer<out AndroidSourceSet>.() -> Unit) = sourceSets(action)

