package com.soywiz.korgw


const val KeyPress = 2
const val KeyRelease = 3
const val ButtonPress = 4
const val ButtonRelease = 5
const val MotionNotify = 6
const val EnterNotify = 7
const val LeaveNotify = 8
const val FocusIn = 9
const val FocusOut = 10
const val KeymapNotify = 11
const val Expose = 12
const val GraphicsExpose = 13
const val NoExpose = 14
const val VisibilityNotify = 15
const val CreateNotify = 16
const val DestroyNotify = 17
const val UnmapNotify = 18
const val MapNotify = 19
const val MapRequest = 20
const val ReparentNotify = 21
const val ConfigureNotify = 22
const val ConfigureRequest = 23
const val GravityNotify = 24
const val ResizeRequest = 25
const val CirculateNotify = 26
const val CirculateRequest = 27
const val PropertyNotify = 28
const val SelectionClear = 29
const val SelectionRequest = 30
const val SelectionNotify = 31
const val ColormapNotify = 32
const val ClientMessage = 33
const val MappingNotify = 34
const val GenericEvent = 35
const val LASTEvent = 36

///

const val GLX_USE_GL = 1
const val GLX_BUFFER_SIZE = 2
const val GLX_LEVEL = 3
const val GLX_RGBA = 4
const val GLX_DOUBLEBUFFER = 5
const val GLX_STEREO = 6
const val GLX_AUX_BUFFERS = 7
const val GLX_RED_SIZE = 8
const val GLX_GREEN_SIZE = 9
const val GLX_BLUE_SIZE = 10
const val GLX_ALPHA_SIZE = 11
const val GLX_DEPTH_SIZE = 12
const val GLX_STENCIL_SIZE = 13
const val GLX_ACCUM_RED_SIZE = 14
const val GLX_ACCUM_GREEN_SIZE = 15
const val GLX_ACCUM_BLUE_SIZE = 16
const val GLX_ACCUM_ALPHA_SIZE = 17

////

const val PropModeReplace = 0
const val PropertyNewValue = 0
const val PropertyDelete = 1


////

const val KeyPressMask = (1 shl 0)
const val KeyReleaseMask = (1 shl 1)
const val Button3MotionMask = (1 shl 10)
const val Button4MotionMask = (1 shl 11)
const val Button5MotionMask = (1 shl 12)
const val ButtonMotionMask = (1 shl 13)
const val KeymapStateMask = (1 shl 14)
const val ExposureMask = (1 shl 15)
const val VisibilityChangeMask = (1 shl 16)
const val StructureNotifyMask = (1 shl 17)
const val ResizeRedirectMask = (1 shl 18)
const val SubstructureNotifyMask = (1 shl 19)
const val ButtonPressMask = (1 shl 2)
const val SubstructureRedirectMask = (1 shl 20)
const val FocusChangeMask = (1 shl 21)
const val PropertyChangeMask = (1 shl 22)
const val ColormapChangeMask = (1 shl 23)
const val ButtonReleaseMask = (1 shl 3)
const val EnterWindowMask = (1 shl 4)
const val LeaveWindowMask = (1 shl 5)
const val PointerMotionMask = (1 shl 6)
const val PointerMotionHintMask = (1 shl 7)
const val Button1MotionMask = (1 shl 8)
const val Button2MotionMask = (1 shl 9)
