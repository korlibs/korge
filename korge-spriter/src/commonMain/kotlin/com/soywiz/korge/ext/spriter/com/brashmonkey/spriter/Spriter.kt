package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.kmem.*
import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.reflect.*

/**
 * A utility class for managing multiple [Loader] and [Player] instances.
 * @author Trixt0r
 */

object Spriter {

	private var loaderDependencies = arrayOfNulls<Any>(1)
	private var drawerDependencies = arrayOfNulls<Any>(1)
	private var loaderTypes = arrayOfNulls<KClass<*>>(1)
	private var drawerTypes = arrayOfNulls<KClass<*>>(1)

	init {
		loaderTypes[0] = Data::class
		drawerTypes[0] = Loader::class
	}

	private var loaderClass: KClass<out Loader<*>>? = null

	private val loadedData = HashMap<String, Data>()
	private val players = ArrayList<Player>()
	private val loaders = ArrayList<Loader<*>>()
	private var drawer: Drawer<*>? = null
	private val entityToLoader = HashMap<Entity, Loader<*>>()
	private var initialized = false

	/**
	 * Sets the dependencies for implemented [Loader].
	 * @param loaderDependencies the dependencies a loader has to get
	 */
	fun setLoaderDependencies(vararg loaderDependencies: Any) {
		if (loaderDependencies == null) return
		Spriter.loaderDependencies = arrayOfNulls<Any>(loaderDependencies.size + 1)
		arraycopy(
			(loaderDependencies as Array<Any>),
			0,
			Spriter.loaderDependencies as Array<Any>,
			1,
			loaderDependencies.size
		)
		loaderTypes = arrayOfNulls<KClass<*>>(loaderDependencies.size + 1)
		loaderTypes[0] = Data::class
		for (i in loaderDependencies.indices)
			loaderTypes[i + 1] = loaderDependencies[i]::class
	}

	/**
	 * Sets the dependencies for implemented [Drawer].
	 * @param drawerDependencies the dependencies a drawer has to get
	 */
	fun setDrawerDependencies(vararg drawerDependencies: Any?) {
		if (drawerDependencies == null) return
		Spriter.drawerDependencies = arrayOfNulls<Any>(drawerDependencies.size + 1)
		Spriter.drawerDependencies[0] = null
		arraycopy(
			(drawerDependencies as Array<Any>),
			0,
			Spriter.drawerDependencies as Array<Any>,
			1,
			drawerDependencies.size
		)
		drawerTypes = arrayOfNulls<KClass<*>>(drawerDependencies.size + 1)
		drawerTypes[0] = Loader::class
		for (i in drawerDependencies.indices)
			if (drawerDependencies[i] != null)
				drawerTypes[i + 1] = drawerDependencies[i]!!::class
	}

	/**
	 * Initializes this class with the implemented [Loader] class and [Drawer] class.
	 * Before calling this method make sure that you have set all necessary dependecies with [.setDrawerDependencies] and [.setLoaderDependencies].
	 * A drawer is created with this method.
	 * @param loaderClass the loader class
	 * *
	 * @param drawerClass the drawer class
	 */
	fun init(loaderClass: KClass<out Loader<*>>, drawerClass: KClass<out Drawer<*>>) {
		TODO()
		//Spriter.loaderClass = loaderClass
		//try {
		//	drawer = drawerClass.getDeclaredConstructor(*drawerTypes).newInstance(*drawerDependencies)
		//} catch (e: Exception) {
		//	e.printStackTrace()
		//}
//
		//initialized = drawer != null
	}

	/**
	 * Loads a SCML file with the given path.
	 * @param scmlFile the path to the SCML file
	 */
	suspend fun load(scmlFile: String) {
		load(LocalVfs(scmlFile))
	}

	/**
	 * Loads the given SCML file.
	 * @param scmlFile the scml file
	 */
	suspend fun load(scmlFile: VfsFile) {
		try {
			load(scmlFile.readAsSyncStream(), scmlFile.path.replace("\\\\".toRegex(), "/"))
		} catch (e: FileNotFoundException) {
			e.printStackTrace()
		}

	}

	/**
	 * Loads the given SCML stream pointing to a file saved at the given path.
	 * @param stream the SCML stream
	 * *
	 * @param scmlFile the path to the SCML file
	 */
	suspend fun load(stream: SyncStream, scmlFile: String) {
		TODO()
		//val reader = SCMLReader(stream)
		//val data = reader.data
		//loadedData.put(scmlFile, data)
		//loaderDependencies[0] = data
		//try {
		//	val loader = loaderClass!!.getDeclaredConstructor(*loaderTypes).newInstance(*loaderDependencies)
		//	loader.load(LocalVfs(scmlFile))
		//	loaders.add(loader)
		//	for (entity in data.entities) entityToLoader.put(entity, loader)
		//} catch (e: Exception) {
		//	e.printStackTrace()
		//}

	}

	/**
	 * Creates a new [Player] instance based on the given SCML file with the given entity index and the given class extending a [Player]
	 * @param scmlFile name of the SCML file
	 * *
	 * @param entityIndex the index of the entity
	 * *
	 * @param playerFactory the class extending a [Player] class, e.g. [PlayerTweener].
	 * *
	 * @return a [Player] instance managed by this class
	 * *
	 * @throws SpriterException if the given SCML file was not loaded yet
	 */
	@JvmOverloads
	fun newPlayer(scmlFile: String, entityIndex: Int, playerFactory: (Entity) -> Player = { Player(it) }): Player? {
		if (!loadedData.containsKey(scmlFile)) throw SpriterException("You have to load \"$scmlFile\" before using it!")
		try {
			val player = playerFactory(loadedData[scmlFile]!!.getEntity(entityIndex))
			players.add(player)
			return player
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return null
	}

	/**
	 * Creates a new [Player] instance based on the given SCML file with the given entity name
	 * @param scmlFile name of the SCML file
	 * *
	 * @param entityName name of the entity
	 * *
	 * @return a [Player] instance managed by this class
	 * *
	 * @throws SpriterException if the given SCML file was not loaded yet
	 */
	fun newPlayer(scmlFile: String, entityName: String): Player? {
		if (!loadedData.containsKey(scmlFile)) throw SpriterException("You have to load \"$scmlFile\" before using it!")
		return newPlayer(scmlFile, loadedData[scmlFile]!!.getEntityIndex(entityName))
	}

	/**
	 * Returns a loader for the given SCML filename.
	 * @param scmlFile the name of the SCML file
	 * *
	 * @return the loader for the given SCML filename
	 * *
	 * @throws NullPointerException if the SCML file was not loaded yet
	 */
	fun getLoader(scmlFile: String): Loader<*>? {
		return entityToLoader[getData(scmlFile).getEntity(0)]
	}

	/**
	 * Updates each created player by this class and immediately draws it.
	 * This method should only be called if you want to update and render on the same thread.
	 * @throws SpriterException if [.init] was not called before
	 */
	fun updateAndDraw() {
		if (!initialized) throw SpriterException("Call init() before updating!")
		for (i in players.indices) {
			players[i].update()
			drawer!!.loader = entityToLoader[players[i].getEntity()]!!
			drawer!!.draw(players[i])
		}
	}

	/**
	 * Updates each created player by this class.
	 * @throws SpriterException if [.init] was not called before
	 */
	fun update() {
		if (!initialized) throw SpriterException("Call init() before updating!")
		for (i in players.indices) {
			players[i].update()
		}
	}

	/**
	 * Draws each created player by this class.
	 * @throws SpriterException if [.init] was not called before
	 */
	fun draw() {
		if (!initialized) throw SpriterException("Call init() before drawing!")
		for (i in players.indices) {
			drawer!!.loader = entityToLoader[players[i].getEntity()]!!
			drawer!!.draw(players[i])
		}
	}

	/**
	 * Returns the drawer instance this class is using.
	 * @return the drawer which draws all players
	 */
	fun drawer(): Drawer<*> {
		return drawer!!
	}

	/**
	 * Returns the data for the given SCML filename.
	 * @param fileName the name of the SCML file
	 * *
	 * @return the data for the given SCML filename or null if not loaed yet
	 */
	fun getData(fileName: String): Data {
		return loadedData[fileName]!!
	}

	/**
	 * The number of players this class is managing.
	 * @return number of players
	 */
	fun players(): Int {
		return players.size
	}

	/**
	 * Clears all previous created players, Spriter datas, disposes all loaders, deletes the drawer and resets all internal lists.
	 * After this methods was called [.init] has to be called again so that everything works again.
	 */
	fun dispose() {
		drawer = null
		drawerDependencies = arrayOfNulls<Any>(1)
		drawerTypes = arrayOfNulls<KClass<*>>(1)
		drawerTypes[0] = Loader::class

		entityToLoader.clear()

		for (i in loaders.indices) {
			loaders[i].dispose()
		}
		loaders.clear()
		loadedData.clear()
		loaderClass = null
		loaderTypes = arrayOfNulls<KClass<*>>(1)
		loaderTypes[0] = Data::class
		loaderDependencies = arrayOfNulls<Any>(1)

		players.clear()

		initialized = false
	}

}
/**
 * Creates a new [Player] instance based on the given SCML file with the given entity index
 * @param scmlFile name of the SCML file
 * *
 * @param entityIndex the index of the entity
 * *
 * @return a [Player] instance managed by this class
 * *
 * @throws SpriterException if the given SCML file was not loaded yet
 */
