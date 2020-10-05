package com.soywiz.korge.services

import com.soywiz.korge.view.Views

abstract class InAppPurchases(val views: Views) {
}

expect fun CreateInAppPurchases(views: Views): InAppPurchases
