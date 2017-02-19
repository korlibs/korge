package com.brashmonkey.spriter;

import com.brashmonkey.spriter.Entity.ObjectInfo;

/**
 * Represents a box, which consists of four points: top-left, top-right, bottom-left and bottom-right.
 * A box is responsible for checking collisions and calculating a bounding box for a {@link Timeline.Key.Bone}.
 * @author Trixt0r
 *
 */
public class Box {
	public final Point[] points;
	private Rectangle rect;
	
	/**
	 * Creates a new box with no witdh and height.
	 */
	public Box(){
		this.points = new Point[4];
		//this.temp = new Point[4];
		for(int i = 0; i < 4; i++){
			this.points[i] = new Point(0,0);
			//this.temp[i] = new Point(0,0);
		}
		this.rect = new Rectangle(0,0,0,0);
	}
	
	/**
	 * Calculates its four points for the given bone or object with the given info.
	 * @param boneOrObject the bone or object
	 * @param info the info
	 * @throws NullPointerException if info or boneOrObject is <code>null</code>
	 */
	public void calcFor(Timeline.Key.Bone boneOrObject, ObjectInfo info){
		float width = info.size.width*boneOrObject.scale.x;
		float height = info.size.height*boneOrObject.scale.y;
	
		float pivotX = width*boneOrObject.pivot.x;
		float pivotY = height*boneOrObject.pivot.y;
		
		this.points[0].set(-pivotX,-pivotY);
		this.points[1].set(width-pivotX, -pivotY);
		this.points[2].set(-pivotX,height-pivotY);
		this.points[3].set(width-pivotX,height-pivotY);
		
		for(int i = 0; i < 4; i++)
			this.points[i].rotate(boneOrObject.angle);
		for(int i = 0; i < 4; i++)
			this.points[i].translate(boneOrObject.position);
	}
	
	/**
	 * Returns whether the given coordinates lie inside the box of the given bone or object.
	 * @param boneOrObject the bone or object
	 * @param info the object info of the given bone or object
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return <code>true</code> if the given point lies in the box
	 * @throws NullPointerException if info or boneOrObject is <code>null</code>
	 */
	public boolean collides(Timeline.Key.Bone boneOrObject, ObjectInfo info, float x, float y){
		float width = info.size.width*boneOrObject.scale.x;
		float height = info.size.height*boneOrObject.scale.y;
		
		float pivotX = width*boneOrObject.pivot.x;
		float pivotY = height*boneOrObject.pivot.y;
		
		Point point = new Point(x-boneOrObject.position.x,y-boneOrObject.position.y);
		point.rotate(-boneOrObject.angle);
		
		return point.x >= -pivotX && point.x <= width-pivotX && point.y >= -pivotY && point.y <= height-pivotY;
	}
	
	/**
	 * Returns whether this box is inside the given rectangle.
	 * @param rect the rectangle
	 * @return  <code>true</code> if one of the four points is inside the rectangle
	 */
	public boolean isInside(Rectangle rect){
		boolean inside = false;
		for(Point p: points)
			inside |= rect.isInside(p);
		return inside;
	}
	
	/**
	 * Returns a bounding box for this box.
	 * @return the bounding box
	 */
	public Rectangle getBoundingRect(){
		this.rect.set(points[0].x,points[0].y,points[0].x,points[0].y);
		this.rect.left = Math.min(Math.min(Math.min(Math.min(points[0].x, points[1].x),points[2].x),points[3].x), this.rect.left);
		this.rect.right = Math.max(Math.max(Math.max(Math.max(points[0].x, points[1].x),points[2].x),points[3].x), this.rect.right);
		this.rect.top = Math.max(Math.max(Math.max(Math.max(points[0].y, points[1].y),points[2].y),points[3].y), this.rect.top);
		this.rect.bottom = Math.min(Math.min(Math.min(Math.min(points[0].y, points[1].y),points[2].y),points[3].y), this.rect.bottom);
		return this.rect;
	}

}
