package com.badlogic.gdx.graphics.g2d;

public class TextureAtlas {
    public AtlasRegion findRegion(String path) {
        throw new RuntimeException();
    }

    static public class AtlasRegion extends TextureRegion {
        public float offsetX;
        public float offsetY;
        public float originalWidth;
        public float originalHeight;
        public boolean rotate;
        public float packedHeight;
        public float packedWidth;
        public int degrees;
    }
}
