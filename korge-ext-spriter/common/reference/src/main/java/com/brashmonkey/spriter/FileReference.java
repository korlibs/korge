package com.brashmonkey.spriter;

/**
 * Represents a reference to a specific file.
 * A file reference consists of a folder and file index.
 * @author Trixt0r
 *
 */
public class FileReference {
	
	public int folder, file;
	
	public FileReference(int folder, int file){
		this.set(folder, file);
	}
	
	@Override
	public int hashCode(){
		return folder*10000+file;//We can have 10000 files per folder
	}
	
	@Override
	public boolean equals(Object ref){
		if(ref instanceof FileReference){
			return this.file == ((FileReference)ref).file && this.folder == ((FileReference)ref).folder;
		} else return false;
	}
	
	public void set(int folder, int file){
		this.folder = folder;
		this.file = file;
	}
	
	public void set(FileReference ref){
		this.set(ref.folder, ref.file);
	}
	
	public boolean hasFile(){
		return this.file != -1;
	}
	
	public boolean hasFolder(){
		return this.folder != -1;
	}
	
	public String toString(){
		return "[folder: "+folder+", file: "+file+"]";
	}

}
