package samples.fleks.systems

import com.github.quillraven.fleks.ComponentListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korim.format.ImageAnimation
import samples.fleks.components.*
import samples.fleks.components.Sprite
import samples.fleks.assets.Assets

/**
 * This System takes care of displaying sprites (image-animation objects) on the screen. It takes the image configuration from
 * [Sprite] component to setup graphics from Assets and create an ImageAnimationView object for displaying in the Container.
 *
 */
class SpriteSystem : IteratingSystem(
    allOfComponents = arrayOf(Sprite::class, Position::class),
    interval = EachFrame
) {

    private val positions = Inject.componentMapper<Position>()
    private val sprites = Inject.componentMapper<Sprite>()

    override fun onTickEntity(entity: Entity) {

        val sprite = sprites[entity]
        val pos = positions[entity]
        // sync view position
        sprite.imageAnimView.x = pos.x
        sprite.imageAnimView.y = pos.y
    }

    class SpriteListener : ComponentListener<Sprite> {

        private val world = Inject.dependency<World>()
        private val layerContainer = Inject.dependency<Container>("layer0")
        private val assets = Inject.dependency<Assets>()

        override fun onComponentAdded(entity: Entity, component: Sprite) {
            // Set animation object
            val asset = assets.getImage(component.imageData)
            component.imageAnimView.animation = asset.animationsByName.getOrElse(component.animation) { asset.defaultAnimation }
            component.imageAnimView.onPlayFinished = {
                // when animation finished playing trigger destruction of entity
                // TODO handle destruction with "Destruct" component
                world.remove(entity)
            }
            component.imageAnimView.addTo(layerContainer)
            // Set play status
            component.imageAnimView.direction = when {
                component.forwardDirection && !component.loop -> ImageAnimation.Direction.ONCE_FORWARD
                !component.forwardDirection && component.loop -> ImageAnimation.Direction.REVERSE
                !component.forwardDirection && !component.loop -> ImageAnimation.Direction.ONCE_REVERSE
                else -> ImageAnimation.Direction.FORWARD
            }
            if (component.isPlaying) { component.imageAnimView.play() }
        }

        override fun onComponentRemoved(entity: Entity, component: Sprite) {
            component.imageAnimView.removeFromParent()
        }
    }
}
