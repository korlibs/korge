package com.soywiz.korge.services

import com.soywiz.korge.view.Views

actual fun CreateInAppPurchases(views: Views): InAppPurchases = object : InAppPurchases(views) { }
