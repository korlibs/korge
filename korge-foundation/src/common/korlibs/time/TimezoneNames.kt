package korlibs.time

import korlibs.time.internal.Serializable

data class TimezoneNames(val timeZones: List<Timezone>) : Serializable {
    val namesToOffsetsList by lazy { timeZones.groupBy { it.abbr } }
    val namesToOffsets by lazy { namesToOffsetsList.map { it.key to it.value.first().offset.time }.toMap() }
    constructor(vararg tz: Timezone) : this(tz.toList())

	operator fun plus(other: TimezoneNames): TimezoneNames = TimezoneNames(this.timeZones + other.timeZones)
    operator fun get(name: String): Timezone? = namesToOffsetsList[name.uppercase().trim()]?.first()

    /** Some abbreviations collides, so we can get a list of Timezones based on abbreviation */
    fun getAll(name: String): List<Timezone> = namesToOffsetsList[name.uppercase().trim()] ?: emptyList()

	companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        val DEFAULT = TimezoneNames(
			Timezone.PDT,
			Timezone.PST,
			Timezone.GMT,
			Timezone.UTC,
            Timezone.CET,
            Timezone.CEST,
		)
	}
}
