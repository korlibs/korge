//fun main(args: Array<String>) = EventLoop {
//val panel = SVGPanel()

//var svgUniverse = SVGCache.getSVGUniverse()
//
//svgUniverse.loadSVG(File("c:/temp/shape1.svg").toURI().toURL())
//val diagram = svgUniverse.getDiagram(File("c:/temp/shape1.svg").toURI())
//val ni = NativeImage(1024, 1024)
//val image = (ni as AwtNativeImage).awtImage
//diagram.render(image.createGraphics())
//
//File("c:/temp/file2.png").toVfs().writeBitmap(ni.toBmp32())

//panel.antiAlias = true
//panel.svgURI = File("c:/temp/shape1.svg").toURI()
//panel.svgURI
//SVGRoot().render()

/*
val panel = JLabel()
val ni = NativeImage(1024, 1024)

val ctx = ni.getContext2d()
ctx.scale(4.0, 4.0)
ctx.fillStyle = ColorPaint(Colors.RED)
ctx.beginPath()
ctx.moveTo(0, 0)
ctx.lineTo(0, 109)
ctx.lineTo(242, 0)
ctx.lineTo(0, 0)
ctx.fill()

ctx.fillStyle = ColorPaint(Colors.BLUE)
ctx.beginPath()
ctx.moveTo(242, 0)
ctx.lineTo(242, 109)
ctx.lineTo(0, 109)
ctx.fill()

panel.icon = ImageIcon((ni as AwtNativeImage).awtImage)

File("c:/temp/file.png").toVfs().writeBitmap(ni.toBmp32())

// <polygon style="fill:red;" points="0 0 0 109 242 0 0 0"/><polyline style="fill:blue;" points="242 0 242 109 0 109"/>

val frame = JFrame()
frame.add(panel)
frame.pack()
frame.isVisible = true
*/
//val frame = JFrame()
//frame.add(panel)
//frame.pack()
//frame.isVisible = true
//}
