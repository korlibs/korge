package korlibs.korge.input

import korlibs.event.*
import korlibs.io.lang.*
import korlibs.korge.view.*

/**
 * Handles a [DropFileEvent]. The event happens when a drag&drop file
 * over the window happens.
 *
 * Events are emitted with the following types:
 *
 * * [DropFileEvent.Type.START] - When a drag with a file enters the window (still not drop). This is a good time to highlight that the action will perform a file drop
 * * [DropFileEvent.Type.DROP] - When the drop is effectively performed. [DropFileEvent.files] should be set here.
 * * [DropFileEvent.Type.END] - When the drag is cancelled or have been executed already.
 */
fun View.onDropFile(handler: (DropFileEvent) -> Unit): AutoCloseable =
    onEvents(*DropFileEvent.Type.ALL) { handler(it) }
