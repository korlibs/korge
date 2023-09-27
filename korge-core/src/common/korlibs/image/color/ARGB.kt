package korlibs.image.color

object ARGB : ColorFormat by ColorFormat.Mixin(
    32,
	bOffset = 24, bSize = 8,
	gOffset = 16, gSize = 8,
	rOffset = 8, rSize = 8,
	aOffset = 0, aSize = 8
)
