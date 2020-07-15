package com.soywiz.korio.file.std

import com.soywiz.kds.iterators.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.flow.*

suspend fun IsoVfs(file: VfsFile): VfsFile =
	ISO.openVfs(file.open(VfsOpenMode.READ), closeStream = true)

suspend fun IsoVfs(s: AsyncStream): VfsFile = ISO.openVfs(s, closeStream = false)
suspend fun AsyncStream.openAsIso() = IsoVfs(this)
suspend fun VfsFile.openAsIso() = IsoVfs(this)

suspend fun <R> AsyncStream.openAsIso(callback: suspend (VfsFile) -> R): R = openAsIso().useVfs(callback)
suspend fun <R> VfsFile.openAsIso(callback: suspend (VfsFile) -> R): R = openAsIso().useVfs(callback)

class IsoVfs(val iso: ISO.IsoFile, val closeStream: Boolean) : VfsV2() {
	val vfs = this
	val isoFile = iso

	override suspend fun close() {
		if (closeStream) {
			iso.reader.close()
		}
	}

	fun getVfsStat(file: ISO.IsoFile): VfsStat =
		createExistsStat(
			file.fullname,
			isDirectory = file.isDirectory,
			size = file.size,
			inode = file.record.extent.toLong(),
			extraInfo = intArrayOf(file.record.extent, 0, 0, 0, 0, 0),
			id = file.fullname
		)

	override suspend fun stat(path: String): VfsStat = try {
		getVfsStat(isoFile.get(path))
	} catch (e: Throwable) {
		createNonExistsStat(path)
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream = isoFile[path].open2(mode)

	override suspend fun listFlow(path: String) = flow<VfsFile> {
		val file = isoFile[path]
		file.children.fastForEach { c ->
			//yield(getVfsStat(c))
			emit(vfs[c.fullname])
		}
	}

	override fun toString(): String = "IsoVfs($iso)"
}

object ISO {
	val CHARSET = ASCII
	//val CHARSET = UTF8

	const val SECTOR_SIZE = 0x800L

	suspend fun read(s: AsyncStream): IsoFile = IsoReader(s).read()

	suspend fun openVfs(s: AsyncStream, closeStream: Boolean): VfsFile = IsoVfs(read(s), closeStream).root

	class IsoReader(val s: AsyncStream) : AsyncCloseable {
		override suspend fun close() {
			s.close()
		}

		suspend fun getSector(sector: Int, size: Int): AsyncStream =
			s.sliceWithSize(sector.toLong() * SECTOR_SIZE, size.toLong())

		suspend fun getSectorMemory(sector: Int, size: Int = SECTOR_SIZE.toInt()) =
			getSector(sector, size).readAvailable().openSync()

		suspend fun read(): IsoFile {
			val primary = PrimaryVolumeDescriptor(getSectorMemory(0x10, SECTOR_SIZE.toInt()))
			var udfFileSystem = false

			// http://wiki.osdev.org/UDF
			for (n in 0 until 0x10) {
				val s = getSectorMemory(0x11 + n, SECTOR_SIZE.toInt())
				val vdh = VolumeDescriptorHeader(s.clone())
				//println(vdh.id)
				when (vdh.id) {
					"CD001" -> Unit
					"BEA01" -> Unit
					"NSR02" -> udfFileSystem = true
					"NSR03" -> udfFileSystem = true
					"BOOT2" -> Unit
					"TEA01" -> Unit
				}
				//if (vdh.type == VolumeDescriptorHeader.TypeEnum.VolumePartitionSetTerminator) break
			}

			//println(udfFileSystem)

			if (udfFileSystem) {
				val udfs = getSectorMemory(0x100)
				val avd = UdfAnchorVolumeDescriptorPointer(udfs)

				val mvd = getSectorMemory(avd.mainVolumeDescriptorSequenceExtent.location)

				val pv = UdfPrimaryVolumeDescriptor(mvd)

				if (pv.descriptorTag.tagId != UdfDescriptorTag.TagId.PRIMARY_VOLUME_DESCRIPTOR) {
					invalidOp("Expected UDF primary volume descriptor")
				}

				//println(pv)
				//println(avd)
				//println(avd)
			}

			val root = IsoFile(this@IsoReader, primary.rootDirectoryRecord, null)
			readDirectoryRecords(
				root,
				getSectorMemory(primary.rootDirectoryRecord.extent, primary.rootDirectoryRecord.size)
			)
			return root
		}

		suspend fun readDirectoryRecords(parent: IsoFile, sector: SyncStream): Unit {
			while (!sector.eof) {
				val dr = DirectoryRecord(sector)
				if (dr == null) {
					sector.skipToAlign(SECTOR_SIZE.toInt())
					continue
				}
				if (dr.name == "" || dr.name == "\u0001") continue
				val file = IsoFile(this@IsoReader, dr, parent)

				if (dr.isDirectory) readDirectoryRecords(file, getSectorMemory(dr.extent, dr.size))
			}
		}
	}

	class IsoFile(val reader: IsoReader, val record: DirectoryRecord, val parent: IsoFile?)  {
		val name: String get() = record.name
		val normalizedName = name.normalizeName()
		val isDirectory: Boolean get() = record.isDirectory
		val fullname: String = if (parent == null) record.name else "${parent.fullname}/${record.name}".trimStart('/')
		val children = arrayListOf<IsoFile>()
		val childrenByName = LinkedHashMap<String, IsoFile>()
		val size: Long = record.size.toLong()

		init {
			parent?.children?.add(this)
			parent?.childrenByName?.put(normalizedName, this)
		}

		fun dump() {
			println("$fullname: $record")
			children.fastForEach { c ->
				c.dump()
			}
		}

		private fun String.normalizeName() = this.toLowerCase()

		suspend fun open2(mode: VfsOpenMode) = reader.getSector(record.extent, record.size)
		operator fun get(name: String): IsoFile {
			var current = this
			name.split("/").fastForEach { part ->
				when (part) {
					"" -> Unit
					"." -> Unit
					".." -> current = current.parent!!
					// @TODO: kotlin-js bug? It doesn't seems to like this somehow.
					//else -> current = current.children.firstOrNull { it.name.toUpperCase() == part.toUpperCase() } ?: throw kotlin.IllegalStateException("Can't find part $part for accessing path $name children: ${current.children}")
					else -> current = current.childrenByName[part.normalizeName()] ?: throw kotlin.IllegalStateException("Can't find part $part for accessing path $name children: ${current.children}")
				}
			}
			return current
		}

		override fun toString(): String {
			return "IsoFile(fullname='$fullname', size=$size)"
		}
	}

	fun SyncStream.readLongArrayLE(count: Int): LongArray = (0 until count).map { readS64LE() }.toLongArray()

	fun SyncStream.readU32_leBE(): Int {
		val le = readS32LE()
		readS32BE()
		return le
	}

	fun SyncStream.readTextWithLength(): String {
		val len = readU8()
		return readStringz(len, CHARSET)
	}

	fun SyncStream.readU16_leBE(): Int {
		val le = readS16LE()
		readS16BE()
		return le
	}

	data class UdfDescriptorTag(
		val tagId: TagId,
		val descVersion: Int,
		val tagChecksum: Int,
		val reserved: Int,
		val tagSerialNumber: Int,
		val descriptorCRC: Int,
		val descriptorCRCLength: Int,
		val tagLocation: Int
	) {
		data class TagId(val id: Int) {
			companion object {
				val PRIMARY_VOLUME_DESCRIPTOR = TagId(0x0001)
				val ANCHOR_VOLUME_DESCRIPTOR_POINTER = TagId(0x0002)
				val VOLUME_DESCRIPTOR_POINTER = TagId(0x0003)
				val IMPLEMENTATION_USE_VOLUME_DESCRIPTOR = TagId(0x0004)
				val PARTITION_DESCRIPTOR = TagId(0x0005)
				val LOGICAL_VOLUME_DESCRIPTOR = TagId(0x0006)
				val UNALLOCATED_SPACE_DESCRIPTOR = TagId(0x0007)
				val TERMINATING_DESCRIPTOR = TagId(0x0008)
				val LOGICAL_VOLUME_INTEGRITY_DESCRIPTOR = TagId(0x0009)
				val FILE_SET_DESCRIPTOR = TagId(0x0100)
				val FILE_IDENTIFIER_DESCRIPTOR = TagId(0x0101)
				val ALLOCATION_EXTENT_DESCRIPTOR = TagId(0x0102)
				val INDIRECT_ENTRY = TagId(0x0103)
				val TERMINAL_ENTRY = TagId(0x0104)
				val FILE_ENTRY = TagId(0x0105)
				val EXTENDED_ATTRIBUTE_HEADER_DESCRIPTOR = TagId(0x0106)
				val UNALLOCATED_SPACE_ENTRY = TagId(0x0107)
				val SPACE_BITMAP_DESCRIPTOR = TagId(0x0108)
				val PARTITION_INTEGRITY_ENTRY = TagId(0x0109)
				val EXTENDED_FILE_ENTRY = TagId(0x010a)
			}
		}

		constructor(s: SyncStream) : this(
			tagId = TagId(s.readU16LE()),
			descVersion = s.readU16LE(),
			tagChecksum = s.readU8(),
			reserved = s.readU8(),
			tagSerialNumber = s.readU16LE(),
			descriptorCRC = s.readU16LE(),
			descriptorCRCLength = s.readU16LE(),
			tagLocation = s.readS32LE()
		)
	}

	data class UdfExtent(
		val length: Int,
		val location: Int
	) {
		constructor(s: SyncStream) : this(
			length = s.readS32LE(),
			location = s.readS32LE()
		)
	}

	data class UdfAnchorVolumeDescriptorPointer(
		val descriptorTag: UdfDescriptorTag,
		val mainVolumeDescriptorSequenceExtent: UdfExtent,
		val reserveVolumeDescriptorSequenceExtent: UdfExtent
	) {
		constructor(s: SyncStream) : this(
			descriptorTag = UdfDescriptorTag(s),
			mainVolumeDescriptorSequenceExtent = UdfExtent(s),
			reserveVolumeDescriptorSequenceExtent = UdfExtent(s)
		)
	}

	data class UdfCharspec(
		val characterSetType: Int,
		val characterSetInfo: String
	) {
		constructor(s: SyncStream) : this(
			characterSetType = s.readU8(),
			characterSetInfo = s.readStringz(63, CHARSET)
		)
	}

	data class UdfEntityId(
		val flags: Int,
		val identifier: String,
		val identifierSuffix: String
	) {
		constructor(s: SyncStream) : this(
			flags = s.readU8(),
			identifier = s.readStringz(23, CHARSET),
			identifierSuffix = s.readStringz(8, CHARSET)
		)
	}

	data class UdfTimestamp(
		val typeAndTimezone: Int,
		val year: Int,
		val month: Int,
		val day: Int,
		val hour: Int,
		val minute: Int,
		val second: Int,
		val centiseconds: Int,
		val hundredsofMicroseconds: Int,
		val microseconds: Int
	) {
		constructor(s: SyncStream) : this(
			typeAndTimezone = s.readS16LE(),
			year = s.readS16LE(),
			month = s.readU8(),
			day = s.readU8(),
			hour = s.readU8(),
			minute = s.readU8(),
			second = s.readU8(),
			centiseconds = s.readU8(),
			hundredsofMicroseconds = s.readU8(),
			microseconds = s.readU8()
		)
	}

	data class UdfPrimaryVolumeDescriptor(
		val descriptorTag: UdfDescriptorTag,
		val volumeDescriptorSequenceNumber: Int,
		val primaryVolumeDescriptorNumber: Int,
		val volumeId: String,
		val volumeSequenceNumber: Int,
		val maximumVolumeSequenceNumber: Int,
		val interchangeLevel: Int,
		val maximumInterchangeLevel: Int,
		val characterSetList: Int,
		val maximumCharacterSetList: Int,
		val volumeSetIdentifier: String,
		val descriptorCharacterSet: UdfCharspec,
		val explanatoryCharacterSet: UdfCharspec,
		val volumeAbstract: UdfExtent,
		val volumeCopyrightNotice: UdfExtent,
		val applicationIdentifier: UdfEntityId,
		val recordingDateandTime: UdfTimestamp,
		val implementationIdentifier: UdfEntityId,
		val implementationUse: ByteArray,
		val predecessorVolumeDescriptorSequenceLocation: Int,
		val flags: Int
	) {
		constructor(s: SyncStream) : this(
			descriptorTag = UdfDescriptorTag(s),
			volumeDescriptorSequenceNumber = s.readS32LE(),
			primaryVolumeDescriptorNumber = s.readS32LE(),
			volumeId = s.readUdfDString(32),
			volumeSequenceNumber = s.readU16LE(),
			maximumVolumeSequenceNumber = s.readU16LE(),
			interchangeLevel = s.readU16LE(),
			maximumInterchangeLevel = s.readU16LE(),
			characterSetList = s.readS32LE(),
			maximumCharacterSetList = s.readS32LE(),
			volumeSetIdentifier = s.readUdfDString(128),
			descriptorCharacterSet = UdfCharspec(s),
			explanatoryCharacterSet = UdfCharspec(s),
			volumeAbstract = UdfExtent(s),
			volumeCopyrightNotice = UdfExtent(s),
			applicationIdentifier = UdfEntityId(s),
			recordingDateandTime = UdfTimestamp(s),
			implementationIdentifier = UdfEntityId(s),
			implementationUse = s.readBytesExact(64),
			predecessorVolumeDescriptorSequenceLocation = s.readS32LE(),
			flags = s.readU16LE()
		)
	}

	data class PrimaryVolumeDescriptor(
		val volumeDescriptorHeader: VolumeDescriptorHeader,
		val pad1: Int,
		val systemId: String,
		val volumeId: String,
		val pad2: Long,
		val volumeSpaceSize: Int,
		val pad3: LongArray,
		val volumeSetSize: Int,
		val volumeSequenceNumber: Int,
		val logicalBlockSize: Int,
		val pathTableSize: Int,
		val typeLPathTable: Int,
		val optType1PathTable: Int,
		val typeMPathTable: Int,
		val optTypeMPathTable: Int,
		val rootDirectoryRecord: DirectoryRecord,
		val volumeSetId: String,
		val publisherId: String,
		val preparerId: String,
		val applicationId: String,
		val copyrightFileId: String,
		val abstractFileId: String,
		val bibliographicFileId: String,
		val creationDate: IsoDate,
		val modificationDate: IsoDate,
		val expirationDate: IsoDate,
		val effectiveDate: IsoDate,
		val fileStructureVersion: Int,
		val pad5: Int,
		val applicationData: ByteArray,
		val pad6: ByteArray
		//fixed byte Pad6_[653];
	) {
		constructor(s: SyncStream) : this(
			volumeDescriptorHeader = VolumeDescriptorHeader(s),
			pad1 = s.readU8(),
			systemId = s.readStringz(0x20, CHARSET),
			volumeId = s.readStringz(0x20, CHARSET),
			pad2 = s.readS64LE(),
			volumeSpaceSize = s.readU32_leBE(),
			pad3 = s.readLongArrayLE(4),
			volumeSetSize = s.readU16_leBE(),
			volumeSequenceNumber = s.readU16_leBE(),
			logicalBlockSize = s.readU16_leBE(),
			pathTableSize = s.readU32_leBE(),
			typeLPathTable = s.readS32LE(),
			optType1PathTable = s.readS32LE(),
			typeMPathTable = s.readS32LE(),
			optTypeMPathTable = s.readS32LE(),
			rootDirectoryRecord = DirectoryRecord(s)!!,
			volumeSetId = s.readStringz(0x80, CHARSET),
			publisherId = s.readStringz(0x80, CHARSET),
			preparerId = s.readStringz(0x80, CHARSET),
			applicationId = s.readStringz(0x80, CHARSET),
			copyrightFileId = s.readStringz(37, CHARSET),
			abstractFileId = s.readStringz(37, CHARSET),
			bibliographicFileId = s.readStringz(37, CHARSET),
			creationDate = IsoDate(s),
			modificationDate = IsoDate(s),
			expirationDate = IsoDate(s),
			effectiveDate = IsoDate(s),
			fileStructureVersion = s.readU8(),
			pad5 = s.readU8(),
			applicationData = s.readBytes(0x200),
			pad6 = s.readBytes(653)
		) {
			//println(this)
		}
	}

	data class VolumeDescriptorHeader(
		val type: TypeEnum,
		val id: String,
		val version: Int
	) {
		data class TypeEnum(val id: Int) {
			companion object {
				val BootRecord = TypeEnum(0x00)
				val VolumePartitionSetTerminator = TypeEnum(0xFF)
				val PrimaryVolumeDescriptor = TypeEnum(0x01)
				val SupplementaryVolumeDescriptor = TypeEnum(0x02)
				val VolumePartitionDescriptor = TypeEnum(0x03)

				//val BY_ID = values().associateBy { it.id }
			}
		}

		constructor(s: SyncStream) : this(
			type = TypeEnum(s.readU8()),
			id = s.readStringz(5, CHARSET),
			version = s.readU8()
		)
	}

	data class IsoDate(val data: String) {
		constructor(s: SyncStream) : this(data = s.readString(17, ASCII))

		val year = data.substring(0, 4).toIntOrNull() ?: 0
		val month = data.substring(4, 6).toIntOrNull() ?: 0
		val day = data.substring(6, 8).toIntOrNull() ?: 0
		val hour = data.substring(8, 10).toIntOrNull() ?: 0
		val minute = data.substring(10, 12).toIntOrNull() ?: 0
		val second = data.substring(12, 14).toIntOrNull() ?: 0
		val hsecond = data.substring(14, 16).toIntOrNull() ?: 0
		//val offset = data.substring(16).toInt()

		override fun toString(): String =
			"IsoDate(%04d-%02d-%02d %02d:%02d:%02d.%d)".format(year, month, day, hour, minute, second, hsecond)
	}

	data class DateStruct(
		val year: Int,
		val month: Int,
		val day: Int,
		val hour: Int,
		val minute: Int,
		val second: Int,
		val offset: Int
	) {
		constructor(s: SyncStream) : this(
			year = s.readU8(),
			month = s.readU8(),
			day = s.readU8(),
			hour = s.readU8(),
			minute = s.readU8(),
			second = s.readU8(),
			offset = s.readU8()
		)

		val fullYear = 1900 + year
	}

	data class DirectoryRecord(
		val length: Int,
		val extendedAttributeLength: Int,
		val extent: Int,
		val size: Int,
		val date: DateStruct,
		val flags: Int,
		val fileUnitSize: Int,
		val interleave: Int,
		val volumeSequenceNumber: Int,
		val rawName: String
	) {
		val name = rawName.substringBefore(';')
		val offset: Long = extent.toLong() * SECTOR_SIZE
		val isDirectory = (flags and 2) != 0

		companion object {
			operator fun invoke(_s: SyncStream): DirectoryRecord? {
				val length = _s.readU8()
				if (length <= 0) {
					return null
				} else {
					val s = _s.readStream((length - 1).toLong())

					val dr = DirectoryRecord(
						length = length,
						extendedAttributeLength = s.readU8(),
						extent = s.readU32_leBE(),
						size = s.readU32_leBE(),
						date = DateStruct(s),
						flags = s.readU8(),
						fileUnitSize = s.readU8(),
						interleave = s.readU8(),
						volumeSequenceNumber = s.readU16_leBE(),
						rawName = s.readTextWithLength()
					)

					//println("DR: $dr, ${s.available}")

					return dr
				}
			}
		}
	}
}

private fun SyncStream.readUdfDString(bytes: Int): String {
	val ss = readStream(bytes)
	val count = ss.readU16LE() / 2
	//println("readUdfDString($bytes, $count)")
	return ss.readUtf16LE(count)
}

private fun SyncStream.readUtf16LE(count: Int): String {
	var s = ""
	for (n in 0 until count) {
		s += readS16LE().toChar()
		//println("S($count): $s")
	}
	return s
}
