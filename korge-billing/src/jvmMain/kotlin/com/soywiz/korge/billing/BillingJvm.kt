package com.soywiz.korge.billing

import com.soywiz.korge.view.Views

actual fun CreateInAppPurchases(views: Views): InAppPurchases = object : InAppPurchases(views) { }
