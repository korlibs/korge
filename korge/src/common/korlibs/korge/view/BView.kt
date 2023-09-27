package korlibs.korge.view

import korlibs.datastructure.iterators.fastForEach

interface BView {
    val bview: View
    val bviewAll: List<View>
}

inline fun BView.bviewFastForEach(block: (view: View) -> Unit) {
    bviewAll.fastForEach { block(it) }
}
