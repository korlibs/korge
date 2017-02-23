package com.soywiz.korge.ext.spriter

import com.brashmonkey.spriter.Loader
import com.brashmonkey.spriter.Timeline
import com.soywiz.korge.view.View

class KorgeDrawer(loader: Loader<View>) : com.brashmonkey.spriter.Drawer<View>(loader) {
	override fun setColor(r: Float, g: Float, b: Float, a: Float) {
		TODO()
		//renderer.setColor(r, g, b, a);
	}

	override fun rectangle(x: Float, y: Float, width: Float, height: Float) {
		TODO()
		//renderer.rect(x, y, width, height);
	}

	override fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
		TODO()
		//renderer.line(x1, y1, x2, y2);
	}

	override fun circle(x: Float, y: Float, radius: Float) {
		TODO()
		//renderer.circle(x, y, radius);
	}

	override fun draw(obj: Timeline.Key.Object) {
		TODO()
		//Sprite sprite = loader.get(obj.ref);
		//float newPivotX = (sprite.getWidth() * obj.pivot.x);
		//float newX = obj.position.x - newPivotX;
		//float newPivotY = (sprite.getHeight() * obj.pivot.y);
		//float newY = obj.position.y - newPivotY;
//
		//sprite.setX(newX);
		//sprite.setY(newY);
//
		//sprite.setOrigin(newPivotX, newPivotY);
		//sprite.setRotation(obj.angle);
//
		//sprite.setColor(1f, 1f, 1f, obj.alpha);
		//sprite.setScale(object.scale.x, obj.scale.y);
		//sprite.draw(batch);
	}
}