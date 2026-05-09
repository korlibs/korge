package korlibs.korge.gradle.targets.apple

import korlibs.korge.gradle.*

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
		appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">""")
        appendLine("""<plist version="1.0"><dict>""")
		run {
			//appendLine("  <key>BuildMachineOSBuild</key><string>16G1510</string>")
			//appendLine("  <key>LSUIElement</key><string>1</string>")
			//appendLine("  <key>DTSDKBuild</key><string>14D125</string>")
			//appendLine("  <key>DTSDKName</key><string>macosx10.1010.10</string>")
			//appendLine("  <key>DTXcode</key><string>0833</string>")
			//appendLine("  <key>DTXcodeBuild</key><string>8E3004b</string>")
			//appendLine("  <key>NSMainNibFile</key><string>MainMenu</string>")
			//appendLine("  <key>NSPrincipalClass</key><string>MyApplication</string>")
			//appendLine("  <key>NSSupportsAutomaticGraphicsSwitching</key><true/>")

			appendLine("  <key>CFBundleDisplayName</key><string>${ext.name}</string>")
			appendLine("  <key>CFBundleExecutable</key><string>${ext.exeBaseName}</string>")
			appendLine("  <key>CFBundleIconFile</key><string>${ext.exeBaseName}.icns</string>")
			appendLine("  <key>CFBundleIdentifier</key><string>${ext.id}</string>")
			appendLine("  <key>CFBundleName</key><string>${ext.name}</string>")
			appendLine("  <key>CFBundleGetInfoString</key><string>${ext.description}</string>")
			appendLine("  <key>NSHumanReadableCopyright</key><string>${ext.copyright}</string>")
			appendLine("  <key>CFBundleVersion</key><string>${ext.version}</string>")
			appendLine("  <key>CFBundleShortVersionString</key><string>${ext.version}</string>")
			appendLine("  <key>LSApplicationCategoryType</key><string>${ext.gameCategory.toUTI()}</string>")

			appendLine("  <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>")
			appendLine("  <key>CFBundlePackageType</key><string>APPL</string>")
			appendLine("  <key>CFBundleSignature</key><string>????</string>")
			appendLine("  <key>LSMinimumSystemVersion</key><string>10.9.0</string>")
			appendLine("  <key>NSHighResolutionCapable</key><true/>")
            appendLine("  <key>LSRequiresNativeExecution</key><true/>")
		}
        appendLine("""</dict></plist>""")
	}
}
