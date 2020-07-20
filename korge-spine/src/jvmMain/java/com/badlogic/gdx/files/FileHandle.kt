package com.badlogic.gdx.files;

import kotlin.NotImplementedError;

import java.io.Reader;

public class FileHandle {
    public String nameWithoutExtension() {
        throw new NotImplementedError();
    }

    public byte[] read(int i) {
        return new byte[0];
    }

    public Reader reader(String charset) {
        throw new NotImplementedError();
    }

    public String pathWithoutExtension() {
        return null;
    }

    public String extension() {
        return null;
    }
}
