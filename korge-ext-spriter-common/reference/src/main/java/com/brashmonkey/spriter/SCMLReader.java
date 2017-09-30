package com.brashmonkey.spriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.brashmonkey.spriter.Entity.*;
import com.brashmonkey.spriter.Mainline.Key.*;
import com.brashmonkey.spriter.XmlReader.*;

/**
 * This class parses a SCML file and creates a {@link Data} instance.
 * If you want to keep track of what is going on during the build process of the objects parsed from the SCML file,
 * you could extend this class and override the load*() methods for pre or post processing.
 * This could be e.g. useful for a loading screen which responds to the current building or parsing state.
 * @author Trixt0r
 */
public class SCMLReader {
	
	protected Data data;
	
	/**
	 * Creates a new SCML reader and will parse all objects in the given stream.
	 * @param stream the stream
	 */
	public SCMLReader(InputStream stream){
		this.data = this.load(stream);
	}
	
	/**
	 * Creates a new SCML reader and will parse the given xml string.
	 * @param xml the xml string
	 */
	public SCMLReader(String xml){
		this.data = this.load(xml);
	}
	
	/**
	 * Parses the SCML object save in the given xml string and returns the build data object.
	 * @param xml the xml string
	 * @return the built data
	 */
	protected Data load(String xml){
		XmlReader reader = new XmlReader();
		return load(reader.parse(xml));
	}
	
	/**
	 * Parses the SCML objects saved in the given stream and returns the built data object.
	 * @param stream the stream from the SCML file 
	 * @return the built data
	 */
	protected Data load(InputStream stream){
		try {
			XmlReader reader = new XmlReader();
			return load(reader.parse(stream));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Reads the data from the given root element, i.e. the spriter_data node.
	 * @param root
	 * @return
	 */
	protected Data load(Element root) {
		ArrayList<Element> folders = root.getChildrenByName("folder");
		ArrayList<Element> entities = root.getChildrenByName("entity");
		data = new Data(root.get("scml_version"), root.get("generator"), root.get("generator_version"),
						Data.PixelMode.get(root.getInt("pixel_mode", 0)),
						folders.size(),	entities.size());
		loadFolders(folders);
		loadEntities(entities);
		return data;
	}

	/**
	 * Iterates through the given folders and adds them to the current {@link Data} object.
	 * @param folders a list of folders to load
	 */
	protected void loadFolders(ArrayList<Element> folders){
		for(int i = 0; i < folders.size(); i++){
			Element repo = folders.get(i);
			ArrayList<Element> files = repo.getChildrenByName("file");
			Folder folder = new Folder(repo.getInt("id"), repo.get("name", "no_name_"+i), files.size());
			loadFiles(files, folder);
			data.addFolder(folder);
		}
	}
	
	/**
	 * Iterates through the given files and adds them to the given {@link Folder} object.
	 * @param files a list of files to load
	 * @param folder the folder containing the files
	 */
	protected void loadFiles(ArrayList<Element> files, Folder folder){
		for(int j = 0; j < files.size(); j++){
			Element f = files.get(j);
			File file = new File(f.getInt("id"), f.get("name"),
					new Dimension(f.getInt("width", 0), f.getInt("height", 0)),
					new Point(f.getFloat("pivot_x", 0f), f.getFloat("pivot_y", 1f)));
			
			folder.addFile(file);
		}
	}

	/**
	 * Iterates through the given entities and adds them to the current {@link Data} object.
	 * @param entities a list of entities to load
	 */
	protected void loadEntities(ArrayList<Element> entities){
		for(int i = 0; i < entities.size(); i++){
			Element e = entities.get(i);
			ArrayList<Element> infos = e.getChildrenByName("obj_info");
			ArrayList<Element> charMaps = e.getChildrenByName("character_map");
			ArrayList<Element> animations = e.getChildrenByName("animation");
			Entity entity = new Entity(e.getInt("id"), e.get("name"),
					animations.size(), charMaps.size(), infos.size());
			data.addEntity(entity);
			loadObjectInfos(infos, entity);
			loadCharacterMaps(charMaps, entity);
			loadAnimations(animations, entity);
		}
	}
	
	/**
	 * Iterates through the given object infos and adds them to the given {@link Entity} object.
	 * @param infos a list of infos to load
	 * @param entity the entity containing the infos
	 */
	protected void loadObjectInfos(ArrayList<Element> infos, Entity entity){
		for(int i = 0; i< infos.size(); i++){
			Element info = infos.get(i);
			ObjectInfo objInfo = new ObjectInfo(info.get("name","info"+i),
									ObjectType.getObjectInfoFor(info.get("type","")),
									new Dimension(info.getFloat("w", 0), info.getFloat("h", 0)));
			entity.addInfo(objInfo);
			Element frames = info.getChildByName("frames");
			if(frames == null) continue;
			ArrayList<Element> frameIndices = frames.getChildrenByName("i");
			for (int i1 = 0; i1 < frameIndices.size(); i1++) {
				Element index = frameIndices.get(i1);
				int folder = index.getInt("folder", 0);
				int file = index.getInt("file", 0);
				objInfo.frames.add(new FileReference(folder, file));
			}
		}
	}
	
	/**
	 * Iterates through the given character maps and adds them to the given {@link Entity} object.
	 * @param maps a list of character maps to load
	 * @param entity the entity containing the character maps
	 */
	protected void loadCharacterMaps(ArrayList<Element> maps, Entity entity){
		for(int i = 0; i< maps.size(); i++){
			Element map = maps.get(i);
			CharacterMap charMap = new CharacterMap(map.getInt("id"), map.getAttribute("name", "charMap"+i));
			entity.addCharacterMap(charMap);
			ArrayList<Element> mappings = map.getChildrenByName("map");
			for (int i1 = 0; i1 < mappings.size(); i1++) {
				Element mapping = mappings.get(i1);
				int folder = mapping.getInt("folder");
				int file = mapping.getInt("file");
				charMap.put(new FileReference(folder, file),
						new FileReference(mapping.getInt("target_folder", folder), mapping.getInt("target_file", file)));
			}
		}
	}
	
	/**
	 * Iterates through the given animations and adds them to the given {@link Entity} object.
	 * @param animations a list of animations to load
	 * @param entity the entity containing the animations maps
	 */
	protected void loadAnimations(ArrayList<Element> animations, Entity entity){
		for(int i = 0; i < animations.size(); i++){
			Element a = animations.get(i);
			ArrayList<Element> timelines = a.getChildrenByName("timeline");
			Element mainline = a.getChildByName("mainline");
			ArrayList<Element> mainlineKeys = mainline.getChildrenByName("key");
			Animation animation = new Animation(new Mainline(mainlineKeys.size()),
									  a.getInt("id"), a.get("name"), a.getInt("length"), 
									  a.getBoolean("looping", true),timelines.size());
			entity.addAnimation(animation);
			loadMainlineKeys(mainlineKeys, animation.mainline);
			loadTimelines(timelines, animation, entity);
			animation.prepare();
		}
	}
	
	/**
	 * Iterates through the given mainline keys and adds them to the given {@link Mainline} object.
	 * @param keys a list of mainline keys
	 * @param main the mainline
	 */
	protected void loadMainlineKeys(ArrayList<Element> keys, Mainline main){
		for(int i = 0; i < main.keys.length; i++){
			Element k = keys.get(i);
			ArrayList<Element> objectRefs = k.getChildrenByName("object_ref");
			ArrayList<Element> boneRefs = k.getChildrenByName("bone_ref");
			Curve curve = new Curve();
			curve.setType(Curve.getType(k.get("curve_type","linear")));
			curve.constraints.set(k.getFloat("c1", 0f),k.getFloat("c2", 0f),k.getFloat("c3", 0f),k.getFloat("c4", 0f));
			Mainline.Key key = new Mainline.Key(k.getInt("id"), k.getInt("time", 0), curve,
					boneRefs.size(), objectRefs.size());
			main.addKey(key);
			loadRefs(objectRefs, boneRefs, key);
		}
	}
	
	/**
	 * Iterates through the given bone and object references and adds them to the given {@link Mainline.Key} object.
	 * @param objectRefs a list of object references
	 * @param boneRefs a list if bone references
	 * @param key the mainline key
	 */
	protected void loadRefs(ArrayList<Element> objectRefs, ArrayList<Element> boneRefs, Mainline.Key key){
		for (int i = 0; i < boneRefs.size(); i++) {
			Element e = boneRefs.get(i);
			BoneRef boneRef = new BoneRef(e.getInt("id"), e.getInt("timeline"),
					e.getInt("key"), key.getBoneRef(e.getInt("parent", -1)));
			key.addBoneRef(boneRef);
		}

		for (int i = 0; i < objectRefs.size(); i++) {
			Element o = objectRefs.get(i);
			ObjectRef objectRef = new ObjectRef(o.getInt("id"), o.getInt("timeline"),
					o.getInt("key"), key.getBoneRef(o.getInt("parent", -1)), o.getInt("z_index", 0));
			key.addObjectRef(objectRef);
		}
		Arrays.sort(key.objectRefs);
	}
	
	/**
	 * Iterates through the given timelines and adds them to the given {@link Animation} object.
	 * @param timelines a list of timelines
	 * @param animation the animation containing the timelines
	 * @param entity entity for assigning the timeline an object info
	 */
	protected void loadTimelines(ArrayList<Element> timelines, Animation animation, Entity entity){
		for(int i = 0; i< timelines.size(); i++){
			Element t = timelines.get(i);
			ArrayList<Element> keys = timelines.get(i).getChildrenByName("key");
			String name = t.get("name");
			ObjectType type = ObjectType.getObjectInfoFor(t.get("object_type", "sprite"));
			ObjectInfo info = entity.getInfo(name);
			if(info == null) info = new ObjectInfo(name, type, new Dimension(0,0));
			Timeline timeline = new Timeline(t.getInt("id"), name, info, keys.size());
			animation.addTimeline(timeline);
			loadTimelineKeys(keys, timeline);
		}
	}
	
	/**
	 * Iterates through the given timeline keys and adds them to the given {@link Timeline} object.
	 * @param keys a list if timeline keys
	 * @param timeline the timeline containing the keys
	 */
	protected void loadTimelineKeys(ArrayList<Element> keys, Timeline timeline){
		for(int i = 0; i< keys.size(); i++){
			Element k = keys.get(i);
			Curve curve = new Curve();
			curve.setType(Curve.getType(k.get("curve_type", "linear")));
			curve.constraints.set(k.getFloat("c1", 0f),k.getFloat("c2", 0f),k.getFloat("c3", 0f),k.getFloat("c4", 0f));
			Timeline.Key key = new Timeline.Key(k.getInt("id"), k.getInt("time", 0), k.getInt("spin", 1), curve);
			Element obj = k.getChildByName("bone");
			if(obj == null) obj = k.getChildByName("object");
			
			Point position = new Point(obj.getFloat("x", 0f), obj.getFloat("y", 0f));
			Point scale = new Point(obj.getFloat("scale_x", 1f), obj.getFloat("scale_y", 1f));
			Point pivot = new Point(obj.getFloat("pivot_x", 0f), obj.getFloat("pivot_y", (timeline.objectInfo.type == ObjectType.Bone)? .5f:1f));
			float angle = obj.getFloat("angle", 0f), alpha = 1f;
			int folder = -1, file = -1;
			if(obj.getName().equals("object")){
				if(timeline.objectInfo.type == ObjectType.Sprite){
					alpha = obj.getFloat("a", 1f);
					folder = obj.getInt("folder", -1);
					file = obj.getInt("file", -1);
					File f = data.getFolder(folder).getFile(file);
					pivot = new Point(obj.getFloat("pivot_x", f.pivot.x), obj.getFloat("pivot_y", f.pivot.y));
					timeline.objectInfo.size.set(f.size);
				}
			}
			Timeline.Key.Object object;
			if(obj.getName().equals("bone")) object = new Timeline.Key.Object(position, scale, pivot, angle, alpha, new FileReference(folder, file));
			else object = new Timeline.Key.Object(position, scale, pivot, angle, alpha, new FileReference(folder, file));
			key.setObject(object);
			timeline.addKey(key);
		}
	}
	
	/**
	 * Returns the loaded SCML data.
	 * @return the SCML data.
	 */
	public Data getData(){
		return data;
	}
	
}

