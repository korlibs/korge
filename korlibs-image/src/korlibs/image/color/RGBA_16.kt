package korlibs.image.color

object RGBA_4444 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	rOffset = 0, rSize = 4,
	gOffset = 4, gSize = 4,
	bOffset = 8, bSize = 4,
	aOffset = 12, aSize = 4
)

object RGBA_5551 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	rOffset = 0, rSize = 5,
	gOffset = 5, gSize = 5,
	bOffset = 10, bSize = 5,
	aOffset = 15, aSize = 1
)

object RGB_555 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	rOffset = 0, rSize = 5,
	gOffset = 5, gSize = 5,
	bOffset = 10, bSize = 5,
	aOffset = 15, aSize = 0
)

object RGB_565 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	rOffset = 0, rSize = 5,
	gOffset = 5, gSize = 6,
	bOffset = 11, bSize = 5,
	aOffset = 15, aSize = 0
)

/////////////////////////////////////////////////////////////////////////////

object BGRA_4444 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	bOffset = 0, bSize = 4,
	gOffset = 4, gSize = 4,
	rOffset = 8, rSize = 4,
	aOffset = 12, aSize = 4
)

object BGR_555 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	bOffset = 0, bSize = 5,
	gOffset = 5, gSize = 5,
	rOffset = 10, rSize = 5,
	aOffset = 15, aSize = 0
)

object BGR_565 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	bOffset = 0, bSize = 5,
	gOffset = 5, gSize = 6,
	rOffset = 11, rSize = 5,
	aOffset = 15, aSize = 0
)

object BGRA_5551 : ColorFormat16, ColorFormat by ColorFormat.Mixin(
    16,
	bOffset = 0, bSize = 5,
	gOffset = 5, gSize = 5,
	rOffset = 10, rSize = 5,
	aOffset = 15, aSize = 1
)
