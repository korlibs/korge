package korlibs.image.tiles

enum class TileMapOrientation(val value: String) {
    ORTHOGONAL("orthogonal"),
    ISOMETRIC("isometric"),
    STAGGERED("staggered"),
    HEXAGONAL("hexagonal")
}

enum class TileMapRenderOrder(val value: String) {
    RIGHT_DOWN("right-down"),
    RIGHT_UP("right-up"),
    LEFT_DOWN("left-down"),
    LEFT_UP("left-up")
}

enum class TileMapStaggerAxis(val value: String) {
    X("x"), Y("y")
}

enum class TileMapStaggerIndex(val value: String) {
    EVEN("even"), ODD("odd")
}

enum class TileMapObjectAlignment(val value: String) {
    UNSPECIFIED("unspecified"),
    TOP_LEFT("topleft"),
    TOP("top"),
    TOP_RIGHT("topright"),
    LEFT("left"),
    CENTER("center"),
    RIGHT("right"),
    BOTTOM_LEFT("bottomleft"),
    BOTTOM("bottom"),
    BOTTOM_RIGHT("bottomright")
}
