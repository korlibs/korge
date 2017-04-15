package com.brashmonkey.spriter

/**
 * A player tweener is responsible for tweening to [Player] instances.
 * Such a
 * @author Trixt0r
 */
/**
 * Creates a player tweener which will tween the given two players.
 * @param player1 the first player
 * *
 * @param player2 the second player
 */
class PlayerTweener(var player1: Player, var player2: Player) : Player(player1.getEntity()) {

	private var anim: TweenedAnimation? = null
	/**
	 * Returns the first set player.
	 * @return the first player
	 */
	var firstPlayer: Player = player1
		private set
	/**
	 * Returns the second set player.
	 * @return the second player
	 */
	var secondPlayer: Player = player2
		private set
	/**
	 * Indicates whether to update the [Player] instances this instance is holding.
	 * If this variable is set to `false`, you will have to call [Player.update] on your own.
	 */
	var updatePlayers = true

	/**
	 * The name of root bone to start the tweening at.
	 * Set it to null to tween the whole hierarchy.
	 */
	var baseBoneName: String? = null

	init {
		this.setPlayers(player1, player2)
	}

	/**
	 * Creates a player tweener based on the entity.
	 * The players to tween will be created by this instance.
	 * @param entity the entity the players will animate
	 */
	constructor(entity: Entity) : this(Player(entity), Player(entity)) {
	}

	/**
	 * Tweens the current set players.
	 * This method will update the set players if [.updatePlayers] is `true`.
	 * @throws SpriterException if no bone with [.baseBoneName] exists
	 */
	override fun update() {
		if (updatePlayers) {
			firstPlayer!!.update()
			secondPlayer!!.update()
		}
		anim!!.setAnimations(firstPlayer!!.animation, secondPlayer!!.animation)
		super.update()
		if (baseBoneName != null) {
			val index = if (anim!!.onFirstMainLine()) firstPlayer!!.getBoneIndex(baseBoneName) else secondPlayer!!.getBoneIndex(baseBoneName)
			if (index == -1) throw SpriterException("A bone with name \"$baseBoneName\" does no exist!")
			anim!!.base = anim!!.currentKey.getBoneRef(index)
			super.update()
		}
	}

	/**
	 * Sets the players for this tweener.
	 * Both players have to hold the same [Entity]
	 * @param player1 the first player
	 * *
	 * @param player2 the second player
	 */
	fun setPlayers(player1: Player, player2: Player) {
		if (player1.entity !== player2.entity)
			throw SpriterException("player1 and player2 have to hold the same entity!")
		this.firstPlayer = player1
		this.secondPlayer = player2
		if (player1.entity === entity) return
		this.anim = TweenedAnimation(player1.getEntity())
		anim!!.setAnimations(player1.animation, player2.animation)
		super.setEntity(player1.getEntity())
		super.setAnimation(anim)
	}

	/**
	 * Returns the interpolation weight.
	 * @return the interpolation weight between 0.0f  and 1.0f
	 */
	/**
	 * Sets the interpolation weight of this tweener.
	 * @param weight  the interpolation weight between 0.0f  and 1.0f
	 */
	var weight: Float
		get() = this.anim!!.weight
		set(weight) {
			this.anim!!.weight = weight
		}

	/**
	 * Sets the base animation of this tweener by the given animation index.
	 * Has only an effect if [.baseBoneName] is not `null`.
	 * @param index the index of the base animation
	 */
	fun setBaseAnimation(index: Int) {
		this.baseAnimation = entity.getAnimation(index)
	}

	/**
	 * Sets the base animation of this tweener by the given name.
	 * Has only an effect if [.baseBoneName] is not `null`.
	 * @param name the name of the base animation
	 */
	fun setBaseAnimation(name: String) {
		this.baseAnimation = entity.getAnimation(name)!!
	}

	/**
	 * Returns the base animation if this tweener.
	 * @return the base animation
	 */
	/**
	 * Sets the base animation of this tweener.
	 * Has only an effect if [.baseBoneName] is not `null`.
	 * @param anim the base animation
	 */
	var baseAnimation: Animation
		get() = this.anim!!.baseAnimation!!
		set(anim) {
			this.anim!!.baseAnimation = anim
		}

	/**
	 * Not supported by this class.
	 */
	override fun setAnimation(anim: Animation) {
	}

	/**
	 * Not supported by this class.
	 */
	override fun setEntity(entity: Entity) {
	}
}
