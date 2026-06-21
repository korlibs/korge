package korlibs.korge.service.vibration

import korlibs.datastructure.*
import korlibs.korge.view.*

val Views.vibration by extraPropertyThis { NativeVibration(this.coroutineContext) }
