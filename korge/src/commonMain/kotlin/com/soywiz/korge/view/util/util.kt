package com.soywiz.korge.view.util

import com.soywiz.korge.view.View


// Distributes an iterable of views evenly across a provided bounding width.
// The first view in the iterable will act as the ANCHOR VIEW which all other views will be
// offset from.
//
// If you've used Google Slides/Microsoft Excel before, this is similar to evenly distributing a
// bunch of selected shapes HORIZONTALLY.
//
// Unlike Slides, it does not use the right most view/shape, instead, you must provide the bounding
// width beforehand.
//
// If you provide an iterable with a single view, then nothing will happen.
fun distributeEvenlyHorizontally(views: Iterable<View>, boundingWidth: Double) {
    val originView = views.first()

    var remainingWidthToDistribute = boundingWidth
    views.forEach { remainingWidthToDistribute -= it.scaledWidth }

    val padding = remainingWidthToDistribute / (views.count() - 1)

    var offset = 0.0
    for (view in views) {
        view.x = originView.x + offset
        offset += view.scaledWidth + padding
    }
}

// Same as `distributeEvenlyHorizontally(views, boundingWidth)`, but calculates the `boundingWidth`
// beforehand by iterating through all the views and calculating the highest difference between
// the first view and the right of the other views.
//
// Note that the FIRST VIEW in the iterable will be the ORIGIN VIEW that other views will be
// offset from.
//
// Diagram:
// (L,0)                       (K,0)
//   │                           │
//   ▼                           ▼
//   ┌────────┐┌─────┐   ┌───────┐
//   │1st view││     │...│       │
//   └────────┘└─────┘   └───────┘
// boundingWidth = K - L
fun distributeEvenlyHorizontally(views: Iterable<View>) {
    val originView = views.first()
    val rightMostX = views.maxOf {
        it.x + it.scaledWidth
    }
    distributeEvenlyHorizontally(views, rightMostX - originView.x)
}

// Distributes an iterable of views evenly across a provided bounding height.
// The first view in the iterable will act as the ANCHOR VIEW which all other views will be
// offset from.
//
// If you've used Google Slides/Microsoft Excel before, this is similar to evenly distributing a
// bunch of selected shapes VERTICALLY.
//
// Unlike Slides, it does not use the bottom most view/shape, instead, you must provide the bounding
// height beforehand.
//
// If you provide an iterable with a single view, then nothing will happen.
fun distributeEvenlyVertically(views: Iterable<View>, boundingHeight: Double) {
    val originView = views.first()

    var remainingHeightToDistribute = boundingHeight
    views.forEach { remainingHeightToDistribute -= it.scaledHeight }

    val padding = remainingHeightToDistribute / (views.count() - 1)

    var offset = 0.0
    for (view in views) {
        view.y = originView.y + offset
        offset += view.scaledHeight + padding
    }
}

// Same as `distributeEvenlyVertically(views, boundingHeight)`, but calculates the `boundingHeight`
// beforehand by iterating through all the views and calculating the highest difference between
// the first view and the bottom of the other views.
//
// Note that the FIRST VIEW in the iterable will be the ORIGIN VIEW that other views will be
// offset from.
//
// Diagram:
// (0,L)──►┌────────┐
//         │1st view│
//         └────────┘
//         ┌────────┐
//         │        │
//         └────────┘
//           ...
//         ┌────────┐
//         │        │
// (0,K)──►└────────┘
// boundingHeight = K - L
fun distributeEvenlyVertically(views: Iterable<View>) {
    val originView = views.first()
    val bottomMostY = views.maxOf {
        it.y + it.scaledHeight
    }
    distributeEvenlyVertically(views, bottomMostY - originView.y)
}
