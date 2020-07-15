package com.soywiz.korge.gradle.targets.apple

import com.soywiz.korge.gradle.GameCategory
import com.soywiz.korge.gradle.KorgeExtension

object InfoPlistBuilder {
	fun GameCategory?.toUTI(): String {
		return "public.app-category." + when (this) {
			null -> "games"
			GameCategory.ACTION -> "action-games"
			GameCategory.ADVENTURE -> "adventure-games"
			GameCategory.ARCADE -> "arcade-games"
			GameCategory.BOARD -> "board-games"
			GameCategory.CARD -> "card-games"
			GameCategory.CASINO -> "casino-games"
			GameCategory.DICE -> "dice-games"
			GameCategory.EDUCATIONAL -> "educational-games"
			GameCategory.FAMILY -> "family-games"
			GameCategory.KIDS -> "kids-games"
			GameCategory.MUSIC -> "music-games"
			GameCategory.PUZZLE -> "puzzle-games"
			GameCategory.RACING -> "racing-games"
			GameCategory.ROLE_PLAYING -> "role-playing-games"
			GameCategory.SIMULATION -> "simulation-games"
			GameCategory.SPORTS -> "sports-games"
			GameCategory.STRATEGY -> "strategy-games"
			GameCategory.TRIVIA -> "trivia-games"
			GameCategory.WORD -> "word-games"
		}
	}

	// https://developer.apple.com/library/archive/documentation/General/Reference/InfoPlistKeyReference/Articles/CoreFoundationKeys.html
	// https://developer.apple.com/library/archive/documentation/General/Reference/InfoPlistKeyReference/Articles/LaunchServicesKeys.html
	fun build(ext: KorgeExtension): String = buildString {
		appendln("""<?xml version="1.0" encoding="UTF-8"?>""")
		appendln("""<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">""")
		appendln("""<plist version="1.0"><dict>""")
		run {
			//appendln("  <key>BuildMachineOSBuild</key><string>16G1510</string>")
			//appendln("  <key>LSUIElement</key><string>1</string>")
			//appendln("  <key>DTSDKBuild</key><string>14D125</string>")
			//appendln("  <key>DTSDKName</key><string>macosx10.1010.10</string>")
			//appendln("  <key>DTXcode</key><string>0833</string>")
			//appendln("  <key>DTXcodeBuild</key><string>8E3004b</string>")
			//appendln("  <key>NSMainNibFile</key><string>MainMenu</string>")
			//appendln("  <key>NSPrincipalClass</key><string>MyApplication</string>")
			//appendln("  <key>NSSupportsAutomaticGraphicsSwitching</key><true/>")

			appendln("  <key>CFBundleDisplayName</key><string>${ext.name}</string>")
			appendln("  <key>CFBundleExecutable</key><string>${ext.exeBaseName}</string>")
			appendln("  <key>CFBundleIconFile</key><string>${ext.exeBaseName}.icns</string>")
			appendln("  <key>CFBundleIdentifier</key><string>${ext.id}</string>")
			appendln("  <key>CFBundleName</key><string>${ext.name}</string>")
			appendln("  <key>CFBundleGetInfoString</key><string>${ext.description}</string>")
			appendln("  <key>NSHumanReadableCopyright</key><string>${ext.copyright}</string>")
			appendln("  <key>CFBundleVersion</key><string>${ext.version}</string>")
			appendln("  <key>CFBundleShortVersionString</key><string>${ext.version}</string>")
			appendln("  <key>LSApplicationCategoryType</key><string>${ext.gameCategory.toUTI()}</string>")

			appendln("  <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>")
			appendln("  <key>CFBundlePackageType</key><string>APPL</string>")
			appendln("  <key>CFBundleSignature</key><string>????</string>")
			appendln("  <key>LSMinimumSystemVersion</key><string>10.9.0</string>")
			appendln("  <key>NSHighResolutionCapable</key><true/>")
		}
		appendln("""</dict></plist>""")
	}
}
