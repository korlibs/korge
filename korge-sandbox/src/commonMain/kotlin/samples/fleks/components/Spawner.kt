package samples.fleks.components

/**
 * This component makes an entity a spawner. That means the entity will spawn new entities as configured below.
 */
data class Spawner(
    // config
    var numberOfObjects: Int = 1,  // The spawner will generate this number of object when triggered (by interval)
    var interval: Int = 0,  // 0 - disabled, 1 - every frame, 2 - every second frame, 3 - every third frame,...
    var timeVariation: Int = 0,  // 0 - no variation, 1 - one frame variation, 2 - two frames variation, ...
    // Spawner details for spawned objects (spawned objects do also spawn objects itself)
    var spawnerNumberOfObjects: Int = 0,  // 0 - Disable spawning feature for spawned object
    var spawnerInterval: Int = 0,
    var spawnerTimeVariation: Int = 0,
    var spawnerPositionX: Double = 0.0,  // Position of spawned object relative to spawner position
    var spawnerPositionY: Double = 0.0,
    var spawnerPositionVariationX: Double = 0.0,
    var spawnerPositionVariationY: Double = 0.0,
    var spawnerPositionAccelerationX: Double = 0.0,
    var spawnerPositionAccelerationY: Double = 0.0,
    var spawnerPositionAccelerationVariation: Double = 0.0,
    var spawnerSpriteImageData: String = "",  // "" - Disable sprite graphic for spawned object
    var spawnerSpriteAnimation: String = "",
    var spawnerSpriteIsPlaying: Boolean = false,
    var spawnerSpriteForwardDirection: Boolean = true,
    var spawnerSpriteLoop: Boolean = false,
    // Position details for spawned objects
    var positionX: Double = 0.0,  // Position of spawned object relative to spawner position
    var positionY: Double = 0.0,
    var positionVariationX: Double = 0.0,
    var positionVariationY: Double = 0.0,
    var positionAccelerationX: Double = 0.0,
    var positionAccelerationY: Double = 0.0,
    var positionAccelerationVariation: Double = 0.0,
    // Sprite animation details for spawned objects
    var spriteImageData: String = "",  // "" - Disable sprite graphic for spawned object
    var spriteAnimation: String = "",
    var spriteIsPlaying: Boolean = false,
    var spriteForwardDirection: Boolean = true,
    var spriteLoop: Boolean = false,
    // Destruct info for spawned objects
    var destruct: Boolean = false,  // true - spawned object gets a destruct component, false - no destruct component spawned

    // internal state
    var nextSpawnIn: Int = 0
)
