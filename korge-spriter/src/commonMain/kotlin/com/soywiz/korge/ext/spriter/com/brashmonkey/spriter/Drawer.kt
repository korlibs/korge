package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Entity.*
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.*
import com.soywiz.korio.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * A Drawer is responsible for drawing a [Player].
 * Since this library is meant to be as generic as possible this class has to be abstract, because it cannot be assumed how to draw a resource.
 * Anyone who wants to draw a [Player] has to know how to draw a resource. A resource can be e.g. a sprite, a texture or a texture region.
 * To draw a [Player] call [.draw]. This method relies on [.draw], which has to be implemented with the chosen backend.
 * To debug draw a [Player] call [.drawBones], [.drawBoxes] and [.drawPoints],
 * which rely on [.rectangle], [.circle], [.line] and [.setColor].
 * @author Trixt0r
 * *
 * *
 * @param <R> The backend specific resource. In general such a resource is called "sprite", "texture" or "image".
</R> */
abstract class Drawer<R>
/**
 * Creates a new drawer based on the given loader.
 * @param loader the loader containing resources
 */
	(@JvmField var loader: Loader<*>) {

	/**
	 * The radius of a point for debug drawing purposes.
	 */
	var pointRadius = 5f

	/**
	 * Sets the loader of this drawer.
	 * @param loader the loader containing resources
	 * *
	 * @throws SpriterException if the loader is `null`
	 */
	fun setLoader(loader: Loader<R>?) {
		if (loader == null) throw SpriterException("The loader instance can not be null!")
		this.loader = loader
	}

	/**
	 * Draws the bones of the given player composed of lines.
	 * @param player the player to draw
	 */
	fun drawBones(player: Player) {
		this.setColor(1f, 0f, 0f, 1f)
		val it = player.boneIterator()
		while (it.hasNext()) {
			val bone = it.next()
			val key = player.getKeyFor(bone)
			if (!key.active) continue
			val info = player.getObjectInfoFor(bone)
			val size = info.size
			drawBone(bone, size)
		}
		/*for(Mainline.Key.BoneRef ref: player.getCurrentKey().boneRefs){
			Timeline.Key key = player.unmappedTweenedKeys[ref.timeline];
			Timeline.Key.Bone bone = key.object();
			if(player.animation.getTimeline(ref.timeline).objectInfo.type != ObjectType.Bone || !key.active) continue;
			ObjectInfo info = player.animation.getTimeline(ref.timeline).objectInfo;
			if(info == null) continue;
			Dimension size = info.size;
			drawBone(bone, size);
		}*/
	}

	/**
	 * Draws the given bone composed of lines with the given size.
	 * @param bone the bone to draw
	 * *
	 * @param size the size of the bone
	 */
	fun drawBone(bone: Bone, size: Dimension) {
		val halfHeight = size.height / 2
		val xx = bone.position.x + cos(Angle.toRadians(bone._angle.toDouble())).toFloat() * size.height
		val yy = bone.position.y + sin(Angle.toRadians(bone._angle.toDouble())).toFloat() * size.height
		val x2 = cos(Angle.toRadians((bone._angle + 90).toDouble())).toFloat() * halfHeight * bone.scale.y
		val y2 = sin(Angle.toRadians((bone._angle + 90).toDouble())).toFloat() * halfHeight * bone.scale.y

		val targetX =
			bone.position.x + cos(Angle.toRadians(bone._angle.toDouble())).toFloat() * size.width * bone.scale.x
		val targetY =
			bone.position.y + sin(Angle.toRadians(bone._angle.toDouble())).toFloat() * size.width * bone.scale.x
		val upperPointX = xx + x2
		val upperPointY = yy + y2
		this.line(bone.position.x, bone.position.y, upperPointX, upperPointY)
		this.line(upperPointX, upperPointY, targetX, targetY)

		val lowerPointX = xx - x2
		val lowerPointY = yy - y2
		this.line(bone.position.x, bone.position.y, lowerPointX, lowerPointY)
		this.line(lowerPointX, lowerPointY, targetX, targetY)
		this.line(bone.position.x, bone.position.y, targetX, targetY)
	}

	/**
	 * Draws the boxes of the player.
	 * @param player the player to draw the boxes from
	 */
	fun drawBoxes(player: Player) {
		this.setColor(0f, 1f, 0f, 1f)
		this.drawBoneBoxes(player)
		this.drawObjectBoxes(player)
		this.drawPoints(player)
	}

	/**
	 * Draws the boxes of all bones of the given player based on the given iterator.
	 * @param player the player to draw the bone boxes of
	 * *
	 * @param it the iterator iterating over the bones to draw
	 */
	@JvmOverloads
	fun drawBoneBoxes(player: Player, it: Iterator<Bone> = player.boneIterator()) {
		while (it.hasNext()) {
			val bone = it.next()
			this.drawBox(player.getBox(bone))
		}
	}

	/**
	 * Draws the boxes of sprites and boxes of the given player based on the given iterator.
	 * @param player player the player to draw the object boxes of
	 * *
	 * @param it the iterator iterating over the object to draw
	 */
	@JvmOverloads
	fun drawObjectBoxes(player: Player, it: Iterator<Object> = player.objectIterator()) {
		while (it.hasNext()) {
			val bone = it.next()
			this.drawBox(player.getBox(bone))
		}
	}

	/**
	 * Draws the points of the given player based on the given iterator.
	 * @param player player the player to draw the points of
	 * *
	 * @param it the iterator iterating over the points to draw
	 */
	@JvmOverloads
	fun drawPoints(player: Player, it: Iterator<Object> = player.objectIterator()) {
		while (it.hasNext()) {
			val point = it.next()
			if (player.getObjectInfoFor(point).type == ObjectType.Point) {
				val x = point.position.x + (cos(Angle.toRadians(point._angle.toDouble())) * pointRadius).toFloat()
				val y = point.position.y + (sin(Angle.toRadians(point._angle.toDouble())) * pointRadius).toFloat()
				circle(point.position.x, point.position.y, pointRadius)
				line(point.position.x, point.position.y, x, y)
			}
		}
	}

	/**
	 * Draws the given player with its current character map.
	 * @param player the player to draw
	 */
	fun draw(player: Player) {
		this.draw(player, player.characterMaps)
	}

	/**
	 * Draws the given player with the given character map.
	 * @param player the player to draw
	 * *
	 * @param map the character map to draw
	 */
	fun draw(player: Player, maps: Array<CharacterMap>) {
		this.draw(player.objectIterator(), maps)
	}

	/**
	 * Draws the objects the given iterator is providing with the given character map.
	 * @param it the iterator iterating over the objects to draw
	 * *
	 * @param map the character map to draw
	 */
	fun draw(it: Iterator<Object>, maps: Array<CharacterMap>?) {
		while (it.hasNext()) {
			val `object` = it.next()
			if (`object`.ref.hasFile()) {
				if (maps != null) {
					for (map in maps)
						if (map != null)
							`object`.ref.set(map.get(`object`.ref)!!)
				}
				this.draw(`object`)
			}
		}
	}

	/**
	 * Draws the given box composed of lines.
	 * @param box the box to draw
	 */
	fun drawBox(box: Box) {
		this.line(box.points[0].x, box.points[0].y, box.points[1].x, box.points[1].y)
		this.line(box.points[1].x, box.points[1].y, box.points[3].x, box.points[3].y)
		this.line(box.points[3].x, box.points[3].y, box.points[2].x, box.points[2].y)
		this.line(box.points[2].x, box.points[2].y, box.points[0].x, box.points[0].y)
	}

	fun drawRectangle(rect: Rectangle) {
		this.rectangle(rect.left, rect.bottom, rect.size.width, rect.size.height)
	}

	/**
	 * Sets the color for drawing lines, rectangles and circles.
	 * @param r the red value between 0.0 - 1.0
	 * *
	 * @param g the green value between 0.0 - 1.0
	 * *
	 * @param b the blue value between 0.0 - 1.0
	 * *
	 * @param a the alpha value between 0.0 - 1.0
	 */
	abstract fun setColor(r: Float, g: Float, b: Float, a: Float)

	/**
	 * Draws a line from (x1, y1) to (x2, y2).
	 * @param x1
	 * *
	 * @param y1
	 * *
	 * @param x2
	 * *
	 * @param y2
	 */
	abstract fun line(x1: Float, y1: Float, x2: Float, y2: Float)

	/**
	 * Draws a rectangle with origin at (x, y) and the given size.
	 * @param x the x coordinate
	 * *
	 * @param y the y coordinate
	 * *
	 * @param width the width of the size
	 * *
	 * @param height the height of the size
	 */
	abstract fun rectangle(x: Float, y: Float, width: Float, height: Float)

	/**
	 * Draws a circle at (x, y) with the given radius.
	 * @param x the x coordinate
	 * *
	 * @param y the y coordinate
	 * *
	 * @param radius the radius of the circle
	 */
	abstract fun circle(x: Float, y: Float, radius: Float)

	/**
	 * Draws the given object with its current resource.
	 * @param object the object to draw.
	 */
	abstract fun draw(`object`: Object)
}
/**
 * Draws the boxes of all bones of the given player.
 * @param player the player to draw the bone boxes of
 */
/**
 * Draws the boxes of the player objects, i.e. sprites and objects.
 * @param player the player to draw the object boxes of
 */
/**
 * Draws all points of the given player.
 * @param player the player to draw the points of.
 */
