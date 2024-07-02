package korlibs.korge

import korlibs.event.*
import korlibs.graphics.*
import korlibs.io.stream.*
import korlibs.korge.ipc.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.memory.*
import korlibs.render.awt.*

class IPCViewsCompleter : ViewsCompleter {
    override fun completeViews(views: Views) {
        val korgeIPC = KorgeIPCInfo.DEFAULT_PATH_OR_NULL
        if (korgeIPC != null) {
            println("KorgeIPC: $korgeIPC : ${KorgeIPCInfo.KORGE_IPC_prop} : ${KorgeIPCInfo.KORGE_IPC_env} : isServer = true")
            val ipc = KorgeIPC(korgeIPC, isServer = true)

            val viewsNodeId = ViewsNodeId(views)

            views.onEvent(GenericEvent.Type.GAME_TO_PROJECTOR) { e ->
                ipc.writeEvent(IPCPacket(IPCPacket.EVENT_GAME_TO_PROJECTOR, IPCPacket.genericEventGen(e.kind, e.data)))
            }

            views.onBeforeRender {
                while (true) {
                    val e = ipc.tryReadEvent() ?: break

                    //println("PROCESSING_PACKET: $e")
                    //if (e.timestamp < System.currentTimeMillis() - 100) continue
                    //if (e.timestamp < System.currentTimeMillis() - 100 && e.type != IPCOldEvent.RESIZE && e.type != IPCOldEvent.BRING_BACK && e.type != IPCOldEvent.BRING_FRONT) continue // @TODO: BRING_BACK/BRING_FRONT

                    when (e.type) {
                        IPCPacket.KEY_DOWN, IPCPacket.KEY_UP, IPCPacket.KEY_TYPE -> {
                            val keyCode = e.buffer.getInt()
                            val char = e.buffer.getInt()

                            views.gameWindow.dispatchKeyEvent(
                                type = when (e.type) {
                                    IPCPacket.KEY_DOWN -> KeyEvent.Type.DOWN
                                    IPCPacket.KEY_UP -> KeyEvent.Type.UP
                                    IPCPacket.KEY_TYPE -> KeyEvent.Type.TYPE
                                    else -> KeyEvent.Type.DOWN
                                },
                                id = 0,
                                key = awtKeyCodeToKey(keyCode),
                                character = char.toChar(),
                                keyCode = keyCode,
                                str = null,
                            )
                        }
                        IPCPacket.EVENT_PROJECTOR_TO_GAME -> {
                            val (type, data) = IPCPacket.genericEventParse(e.data)
                            views.dispatch(GenericEvent(GenericEvent.Type.PROJECTOR_TO_GAME, type, data))
                        }
                        IPCPacket.MOUSE_MOVE, IPCPacket.MOUSE_DOWN, IPCPacket.MOUSE_UP, IPCPacket.MOUSE_CLICK, IPCPacket.MOUSE_SCROLL -> {
                            val x = e.buffer.getInt()
                            val y = e.buffer.getInt()
                            val button = e.buffer.getInt()
                            val scrollX = e.buffer.getFloat()
                            val scrollY = e.buffer.getFloat()
                            val scrollZ = e.buffer.getFloat()

                            views.gameWindow.dispatchMouseEvent(
                                id = 0,
                                type = when (e.type) {
                                    IPCPacket.MOUSE_CLICK -> MouseEvent.Type.CLICK
                                    IPCPacket.MOUSE_MOVE -> MouseEvent.Type.MOVE
                                    IPCPacket.MOUSE_SCROLL -> MouseEvent.Type.SCROLL
                                    IPCPacket.MOUSE_DOWN -> MouseEvent.Type.DOWN
                                    IPCPacket.MOUSE_UP -> MouseEvent.Type.UP
                                    else -> MouseEvent.Type.DOWN
                                }, x = x, y = y,
                                button = MouseButton[button],
                                scrollDeltaX = scrollX, scrollDeltaY = scrollY, scrollDeltaZ = scrollZ,
                            )
                            //println(e)
                        }

                        IPCPacket.RESIZE -> {
                            val width = e.buffer.getInt()
                            val height = e.buffer.getInt()
                            val pixelScale = e.buffer.getFloat()

                            val awtGameWindow = (views.gameWindow as? AwtGameWindow?)
                            if (awtGameWindow != null) {
                                awtGameWindow.frame.setSize(width, height)
                            } else {
                                (views.gameWindow as? AwtOffscreenGameWindow)?.devicePixelRatio = pixelScale.toDouble()
                                views.gameWindow.setSize(width, height)
                                //views.gameWindow.dispatch(ReshapeEvent(width = width, height = height, setPos = false))
                                //views.resized(width, height)
                            }
                            //
                        }

                        IPCPacket.BRING_BACK, IPCPacket.BRING_FRONT -> {
                            val awtGameWindow = (views.gameWindow as? AwtGameWindow?)
                            if (awtGameWindow != null) {
                                if (e.type == IPCPacket.BRING_BACK) {
                                    awtGameWindow.frame.toBack()
                                } else {
                                    awtGameWindow.frame.toFront()
                                }
                            }
                        }

                        IPCPacket.REQUEST_NODE_CHILDREN -> {
                            val req = e.parseJson<IPCNodeChildrenRequest>()
                            val reqNodeId = req.nodeId
                            val container = viewsNodeId.findById(reqNodeId) as? Container?
                            val nodeId = viewsNodeId.getId(container)
                            val parentNodeId = viewsNodeId.getId(container?.parent)

                            e.socket.writePacket(IPCPacket.fromJson(IPCNodeChildrenResponse.ID, IPCNodeChildrenResponse(nodeId, parentNodeId, container?.children?.map {
                                IPCNodeInfo(
                                    viewsNodeId.getId(it),
                                    isContainer = it is Container,
                                    it::class.qualifiedName ?: "View",
                                    it.name ?: ""
                                )
                            })))
                        }
                        IPCPacket.REQUEST_NODE_PROPS -> {
                            val req = e.parseJson<IPCNodePropsRequest>()
                            val reqNodeId = req.nodeId
                            val view = viewsNodeId.findById(reqNodeId)
                            val nodeId = viewsNodeId.getId(view)
                            val parentNodeId = viewsNodeId.getId(view?.parent)
                            val info = ViewPropsInfo[view]
                            val groups = info.groups
                            val groupsByFqname = groups.associateBy { it.clazz.qualifiedName ?: "" }

                            e.socket.writePacket(IPCPacket.fromJson(IPCNodePropsResponse.ID, IPCNodePropsResponse(nodeId, parentNodeId,groupsByFqname.mapValues {
                                if (view == null) {
                                    emptyList()
                                } else {
                                    it.value.actionsAndProps.map {
                                        IPCPropInfo(
                                            it.kname, it.name, it.ktype.toString(),
                                            if (it.ktype == Unit::class) null else kotlin.runCatching { it.get(view).toString() }.getOrNull()
                                        )
                                    }
                                }
                            })))
                        }

                        IPCPacket.REQUEST_NODE_SET_PROP -> {
                            val req = e.parseJson<IPCPacketPropSetRequest>()
                            val reqNodeId = req.nodeId
                            val view = viewsNodeId.findById(reqNodeId)
                            val nodeId = viewsNodeId.getId(view)
                            val info = ViewPropsInfo[view]
                            val basePWithProperty = info.allPropsAndActionsByKName[req.callId]
                            if (view != null) {
                                basePWithProperty?.set(view, req.value)
                            }
                            e.socket.writePacket(IPCPacket.fromJson(IPCPacketPropSetResponse.ID, IPCPacketPropSetResponse(nodeId, req.callId, if (view == null) null else basePWithProperty?.get(view)?.toString())))
                        }

                        else -> {
                            println("Unhandled event: $e")
                        }
                    }
                }
            }

            var fbMem = Buffer(0, direct = true)

            views.onAfterRender {
                val fb = it.currentFrameBufferOrMain
                val nbytes = fb.width * fb.height * 4
                if (fbMem.size < nbytes) {
                    fbMem = Buffer(nbytes, direct = true)
                }
                it.ag.readToMemory(fb.base, fb.info, 0, 0, fb.width, fb.height, fbMem, AGReadKind.COLOR)
                //val bmp = it.ag.readColor(it.currentFrameBuffer)
                //channel.trySend(bmp)
                ipc.setFrame(IPCFrame(System.currentTimeMillis().toInt(), fb.width, fb.height, IntArray(0), fbMem.sliceWithSize(0, nbytes).nioIntBuffer))
                //println("SENT_FRAME")
            }
        }
    }
}
