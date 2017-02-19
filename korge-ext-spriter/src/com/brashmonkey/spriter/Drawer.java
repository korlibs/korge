package com.brashmonkey.spriter;

import java.util.Iterator;

import com.brashmonkey.spriter.Entity.CharacterMap;
import com.brashmonkey.spriter.Entity.ObjectInfo;
import com.brashmonkey.spriter.Entity.ObjectType;
import com.brashmonkey.spriter.Timeline.Key.Bone;
import com.brashmonkey.spriter.Timeline.Key.Object;

/**
 * A Drawer is responsible for drawing a {@link Player}.
 * Since this library is meant to be as generic as possible this class has to be abstract, because it cannot be assumed how to draw a resource.
 * Anyone who wants to draw a {@link Player} has to know how to draw a resource. A resource can be e.g. a sprite, a texture or a texture region.
 * To draw a {@link Player} call {@link #draw(Player)}. This method relies on {@link #draw(Object)}, which has to be implemented with the chosen backend.
 * To debug draw a {@link Player} call {@link #drawBones(Player)}, {@link #drawBoxes(Player)} and {@link #drawPoints(Player)},
 * which rely on {@link #rectangle(float, float, float, float)}, {@link #circle(float, float, float)}, {@link #line(float, float, float, float)} and {@link #setColor(float, float, float, float)}.
 * @author Trixt0r
 *
 * @param <R> The backend specific resource. In general such a resource is called "sprite", "texture" or "image".
 */
public abstract class Drawer<R> {
	
	/**
	 * The radius of a point for debug drawing purposes.
	 */
	public float pointRadius = 5f;
	protected Loader<R> loader;
	
	/**
	 * Creates a new drawer based on the given loader.
	 * @param loader the loader containing resources
	 */
	public Drawer(Loader<R> loader){
		this.loader = loader;
	}
	
	/**
	 * Sets the loader of this drawer.
	 * @param loader the loader containing resources
	 * @throws SpriterException if the loader is <code>null</code>
	 */
	public void setLoader(Loader<R> loader){
		if(loader == null) throw new SpriterException("The loader instance can not be null!");
		this.loader = loader;
	}
	
	/**
	 * Draws the bones of the given player composed of lines.
	 * @param player the player to draw
	 */
	public void drawBones(Player player){
		this.setColor(1, 0, 0, 1);
		Iterator<Bone> it = player.boneIterator();
		while(it.hasNext()){
			Timeline.Key.Bone bone = it.next();
			Timeline.Key key = player.getKeyFor(bone);
			if(!key.active) continue;
			ObjectInfo info = player.getObjectInfoFor(bone);
			Dimension size = info.size;
			drawBone(bone, size);
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
	 * @param size the size of the bone
	 */
	public void drawBone(Bone bone, Dimension size){
		float halfHeight = size.height/2;
		float xx = bone.position.x+(float)Math.cos(Math.toRadians(bone.angle))*size.height;
		float yy = bone.position.y+(float)Math.sin(Math.toRadians(bone.angle))*size.height;
		float x2 = (float)Math.cos(Math.toRadians(bone.angle+90))*halfHeight*bone.scale.y;
		float y2 = (float)Math.sin(Math.toRadians(bone.angle+90))*halfHeight*bone.scale.y;
		
		float targetX = bone.position.x+(float)Math.cos(Math.toRadians(bone.angle))*size.width*bone.scale.x,
				targetY = bone.position.y+(float)Math.sin(Math.toRadians(bone.angle))*size.width*bone.scale.x;
		float upperPointX = xx+x2, upperPointY = yy+y2;
		this.line(bone.position.x, bone.position.y, upperPointX, upperPointY);
		this.line(upperPointX, upperPointY, targetX, targetY);

		float lowerPointX = xx-x2, lowerPointY = yy-y2;
		this.line(bone.position.x, bone.position.y, lowerPointX, lowerPointY);
		this.line(lowerPointX, lowerPointY, targetX, targetY);
		this.line(bone.position.x, bone.position.y, targetX, targetY);
	}
	
	/**
	 * Draws the boxes of the player.
	 * @param player the player to draw the boxes from
	 */
	public void drawBoxes(Player player){
		this.setColor(0f, 1f, 0f, 1f);
		this.drawBoneBoxes(player);
		this.drawObjectBoxes(player);
		this.drawPoints(player);
	}
	
	/**
	 * Draws the boxes of all bones of the given player.
	 * @param player the player to draw the bone boxes of
	 */
	public void drawBoneBoxes(Player player){
		drawBoneBoxes(player, player.boneIterator());
	}
	
	/**
	 * Draws the boxes of all bones of the given player based on the given iterator.
	 * @param player the player to draw the bone boxes of
	 * @param it the iterator iterating over the bones to draw
	 */
	public void drawBoneBoxes(Player player, Iterator<Bone> it){
		while(it.hasNext()){
			Bone bone = it.next();
			this.drawBox(player.getBox(bone));
		}
	}
	
	/**
	 * Draws the boxes of the player objects, i.e. sprites and objects.
	 * @param player the player to draw the object boxes of
	 */
	public void drawObjectBoxes(Player player){
		drawObjectBoxes(player, player.objectIterator());
	}
	
	/**
	 * Draws the boxes of sprites and boxes of the given player based on the given iterator.
	 * @param player player the player to draw the object boxes of
	 * @param it the iterator iterating over the object to draw
	 */
	public void drawObjectBoxes(Player player, Iterator<Object> it){
		while(it.hasNext()){
			Object bone = it.next();
			this.drawBox(player.getBox(bone));
		}
	}
	
	/**
	 * Draws all points of the given player.
	 * @param player the player to draw the points of.
	 */
	public void drawPoints(Player player){
		drawPoints(player, player.objectIterator());
	}
	
	/**
	 * Draws the points of the given player based on the given iterator.
	 * @param player player the player to draw the points of
	 * @param it the iterator iterating over the points to draw
	 */
	public void drawPoints(Player player, Iterator<Object> it){
		while(it.hasNext()){
			Object point = it.next();
			if(player.getObjectInfoFor(point).type == ObjectType.Point){
				float x = point.position.x+(float)(Math.cos(Math.toRadians(point.angle))*pointRadius);
				float y = point.position.y+(float)(Math.sin(Math.toRadians(point.angle))*pointRadius);
				circle(point.position.x, point.position.y, pointRadius);
				line(point.position.x, point.position.y, x,y);
			}
		}
	}
	
	/**
	 * Draws the given player with its current character map.
	 * @param player the player to draw
	 */
	public void draw(Player player){
		this.draw(player, player.characterMaps);
	}
	
	/**
	 * Draws the given player with the given character map. 
	 * @param player the player to draw
	 * @param map the character map to draw
	 */
	public void draw(Player player, CharacterMap[] maps){
		this.draw(player.objectIterator(), maps);
	}
	
	/**
	 * Draws the objects the given iterator is providing with the given character map. 
	 * @param it the iterator iterating over the objects to draw
	 * @param map the character map to draw
	 */
	public void draw(Iterator<Timeline.Key.Object> it, CharacterMap[] maps){
		while(it.hasNext()){
			Timeline.Key.Object object = it.next();
			if(object.ref.hasFile()){
				if(maps != null){
					for(CharacterMap map: maps)
						if(map != null)
							object.ref.set(map.get(object.ref));
				}
				this.draw(object);
			}
		}
	}
	
	/**
	 * Draws the given box composed of lines.
	 * @param box the box to draw
	 */
	public void drawBox(Box box){
		this.line(box.points[0].x, box.points[0].y, box.points[1].x, box.points[1].y);
		this.line(box.points[1].x, box.points[1].y, box.points[3].x, box.points[3].y);
		this.line(box.points[3].x, box.points[3].y, box.points[2].x, box.points[2].y);
		this.line(box.points[2].x, box.points[2].y, box.points[0].x, box.points[0].y);
	}
	
	public void drawRectangle(Rectangle rect){
		this.rectangle(rect.left, rect.bottom, rect.size.width, rect.size.height);
	}
	
	/**
	 * Sets the color for drawing lines, rectangles and circles.
	 * @param r the red value between 0.0 - 1.0
	 * @param g the green value between 0.0 - 1.0
	 * @param b the blue value between 0.0 - 1.0
	 * @param a the alpha value between 0.0 - 1.0
	 */
	public abstract void setColor(float r, float g, float b, float a);
	
	/**
	 * Draws a line from (x1, y1) to (x2, y2).
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public abstract void line(float x1, float y1, float x2, float y2);
	
	/**
	 * Draws a rectangle with origin at (x, y) and the given size.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param width the width of the size
	 * @param height the height of the size
	 */
	public abstract void rectangle(float x, float y, float width, float height);
	
	/**
	 * Draws a circle at (x, y) with the given radius.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param radius the radius of the circle
	 */
	public abstract void circle(float x, float y, float radius);
	
	/**
	 * Draws the given object with its current resource.
	 * @param object the object to draw.
	 */
	public abstract void draw(Timeline.Key.Object object);
}
