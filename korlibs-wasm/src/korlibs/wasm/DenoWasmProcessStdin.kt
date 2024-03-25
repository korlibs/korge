package korlibs.wasm

/*
import korlibs.datastructure.lock.*
import korlibs.ffi.*
import korlibs.io.file.sync.*
import korlibs.io.lang.*
import korlibs.io.serialization.json.*
import korlibs.io.stream.*
import korlibs.memory.*

private val PATHS: List<String> by lazy { (Environment["PATH"] ?: "").split(Environment.PATH_SEPARATOR) }

@OptIn(SyncIOAPI::class)
object DenoWasmProcessStdin {
    val temp = Environment.TEMP
    val file = SyncIOFile("$temp/korge.wasm.js")
        .also { it.writeText(DenoWasmServerStdinCode) }

    val commands: List<String> by lazy {
        ExecutableResolver.findInPaths("deno", PATHS)?.let { return@lazy listOf(it, "run", "-A", "--unstable", file.absolutePath) }
        ExecutableResolver.findInPaths("node", PATHS)?.let { return@lazy listOf(it, file.absolutePath) }
        emptyList()
    }
    val process = SyncIO.exec(commands, mapOf())
    val io = DenoWasmIO(process.stdin, process.stdout, Closeable { }, debug = Environment["DEBUG"] == "true")
    var lastId = 1

    fun open(wasmModuleBytes: ByteArray): DenoWASM {
        return DenoWASM(io, lastId++, wasmModuleBytes)
    }

    fun close() {
        process.destroy()
        process.close()
    }
}

class DenoWASM(val io: DenoWasmIO, val streamId: Int, val wasmModuleBytes: ByteArray) : Closeable {
    init {
        io.loadWASM(streamId, wasmModuleBytes)
    }
    //fun open(bytes: ByteArray) { io.loadWASM(streamId, bytes) }

    fun allocAndWrite(bytes: ByteArray): Int = io.allocAndWrite(streamId, bytes)
    fun free(vararg ptrs: Int) { io.free(streamId, *ptrs) }

    fun stackSave(): Int = io.stackSave(streamId)
    fun stackRestore(ptr: Int) = io.stackRestore(streamId, ptr)
    fun stackAlloc(size: Int): Int = io.stackAlloc(streamId, size)
    fun stackAllocAndWrite(bytes: ByteArray): Int = io.stackAllocAndWrite(streamId, bytes)

    fun writeBytes(ptr: Int, bytes: ByteArray) { io.writeBytes(streamId, ptr, bytes) }
    fun readBytes(ptr: Int, len: Int): ByteArray = io.readBytes(streamId, ptr, len)

    fun executeFunction(name: String, vararg params: Any?): Any? = io.executeFunction(streamId, name, *params)
    fun executeFunctionIndirect(address: Int, vararg params: Any?): Any? = io.executeFunctionIndirect(streamId, address, *params)

    override fun close() { io.unloadWASM(streamId) }
}

class DenoWasmIO(
    val output: SyncOutputStream,
    val input: SyncInputStream,
    val _close: Closeable? = null,
    val debug: Boolean = true,
) {
    fun close() {
        _close?.close()
    }

    fun loadWASM(streamId: Int, bytes: ByteArray) {
        writeReadMessage(1000, streamId, bytes)
        if (debug) println("CMD:loadWASM")
    }

    fun unloadWASM(streamId: Int) {
        writeReadMessage(1001, streamId, byteArrayOf())
        if (debug) println("CMD:unloadWASM")
    }

    fun writeBytes(streamId: Int, ptr: Int, bytes: ByteArray) {
        val payload = ByteArray(4 + bytes.size)
        payload.set32LE(0, ptr)
        payload.setBytes(4, bytes)
        writeReadMessage(1010, streamId, payload)
        if (debug) println("CMD:writeBytes:ptr=$ptr, bytes=${bytes.size}")
    }

    fun allocAndWrite(streamId: Int, bytes: ByteArray): Int {
        return writeReadMessage(1011, streamId, bytes).getS32LE(0)
    }

    fun free(streamId: Int, vararg ptrs: Int) {
        val payload = ByteArray(ptrs.size * 4)
        for (n in 0 until ptrs.size) {
            payload.set32LE(n * 4, ptrs[n])
        }
        writeReadMessage(1012, streamId, payload)
        if (debug) println("CMD:free:ptrs=${ptrs.toList()}")
    }

    fun stackSave(streamId: Int): Int {
        return writeReadMessage(1013, streamId, ByteArray(0)).getS32LE(0)
    }

    fun stackRestore(streamId: Int, ptr: Int) {
        val payload = ByteArray(4)
        payload.set32LE(0, ptr)
        writeReadMessage(1014, streamId, payload)
    }

    fun stackAlloc(streamId: Int, size: Int): Int {
        val payload = ByteArray(4)
        payload.set32LE(0, size)
        return writeReadMessage(1015, streamId, payload).getS32LE(0)
    }

    fun stackAllocAndWrite(streamId: Int, bytes: ByteArray): Int {
        return writeReadMessage(1016, streamId, bytes).getS32LE(0)
    }

    fun readBytes(streamId: Int, ptr: Int, len: Int): ByteArray {
        val payload = ByteArray(8)
        payload.set32LE(0, ptr)
        payload.set32LE(4, len)
        if (debug) println("CMD:readBytes:ptr=$ptr,len=$len")
        return writeReadMessage(1020, streamId, payload)
    }

    fun executeFunction(streamId: Int, name: String, vararg params: Any?): Any? {
        val resultBytes = writeReadMessage(
            1030,
            streamId,
            Json.stringify(mapOf("func" to name, "params" to params.toList())).encodeToByteArray()
        )
        if (debug) println("CMD:executeFunction:name=$name, params=${params.toList()}")
        //return if (resultBytes.isEmpty()) null else Json.parse(resultBytes.decodeToString())
        return if (resultBytes.isEmpty()) Unit else Json.parse(resultBytes.decodeToString()).also {
            if (debug) println(" -> CMD:executeFunction:result=$it")
        }
    }

    fun executeFunctionIndirect(streamId: Int, address: Int, vararg params: Any?): Any? {
        val resultBytes = writeReadMessage(
            1030,
            streamId,
            Json.stringify(mapOf("indirect" to address, "params" to params.toList())).encodeToByteArray()
        )
        val data = resultBytes.decodeToString()
        if (debug) println("CMD:executeFunctionIndirect:address=$address, params=${params.toList()}")
        return Json.parse(data)
    }

    private val lock = Lock()

    private fun writeReadMessage(type: Int, streamId: Int, payload: ByteArray): ByteArray = lock {
        _writeMessage(type, streamId, payload)
        return _readMessage(type)
    }

    private fun _writeMessage(type: Int, streamId: Int, payload: ByteArray) {
        val buffer = ByteArray(12)
        buffer.set32LE(0, type)
        buffer.set32LE(4, streamId)
        buffer.set32LE(8, payload.size)
        //println("writeMessage: type=$type, payload=${payload.size}")
        //socket.channel.write(arrayOf(buffer, ByteBuffer.wrap(payload)))
        output.write(buffer)
        output.write(payload)
        output.flush()
    }

    private fun _readMessage(type: Int): ByteArray {
        //println(" -- readMessage type=$type")
        val sizeBuffer = input.readBytesExact(4)
        val len = sizeBuffer.getS32LE(0)
        //println(" -- readMessage: len=$len")
        val out = input.readBytesExact(len)
        //println(" --> ${out.size}")
        return out
    }

    private fun SyncInputStream.readBytesExact(size: Int): ByteArray {
        return readBytesExact(ByteArray(size))
    }

    private fun SyncInputStream.readBytesExact(b: ByteArray): ByteArray {
        var offset = 0
        var remaining = b.size
        //println("readBytesExact[$remaining]")
        while (remaining > 0) {
            //println("   - READ $offset, $remaining")
            val len = read(b, offset, remaining)
            //println("      -> $len")
            if (len < 0) {
                error("InputStream error: len=$len")
            }
            if (len <= 0) break
            offset += len
            remaining -= len
        }
        check(remaining == 0) { "remaining == 0 : $remaining" }
        //println("REMAINING: $remaining : ${b.toList()}")
        return b
    }
}

private val DenoWasmServerStdinCode = /* language: javascript **/"""
// ISOMORPHIC: works on Deno and node
const isDeno = (typeof Deno) !== "undefined";
const debugEnv = isDeno ? Deno.env.get("DEBUG") : process.env
const debug = debugEnv == "true";
//const debug = true
const fs = (isDeno) ? null : require('fs');

if (debug) console.error('STARTED!', isDeno);

function readSyncExact(reader, size) {
  if (size >= 32 * 1024 * 1024) throw new Error(`too big to read ${'$'}{size}`);
  var out = new Uint8Array(size);
  var pos = 0;
if (debug) console.error('try_read', size)
  while (pos < size) {
    const chunk = out.subarray(pos)
    var read = isDeno
      ? reader.readSync(chunk)
      : fs.readSync(reader, chunk);
    if (debug) console.error('read', read)
    if (read == null || read <= 0) throw Error("Broken pipe");
    pos += read;
  }
  return out;
}

function writeSyncExact(writer, data) {
  var pos = 0;
  while (pos < data.length) {
    const chunk = data.subarray(pos)
    var written = isDeno
      ? writer.writeSync(chunk)
      : fs.writeSync(writer, chunk);
    if (written <= 0) throw Error("Broken pipe");
    pos += written;
  }
  return pos;
}

function readPacket(reader) {
  const [kind, streamId, len] = new Int32Array(
    (readSyncExact(reader, 12)).buffer,
  );
  const payload = readSyncExact(reader, len);
  if (debug) {
    console.error("   packet: ", "id:", streamId, "kind:", kind, "len:", len);
  }
  return { kind, streamId, payload };
}

function writePacket(writer, payload) {
  const data = new Uint8Array(4 + payload.length);
  const dview = new DataView(data.buffer);
  dview.setInt32(0, payload.length, true);
  data.set(payload, 4);
  const written = writeSyncExact(writer, data);
  if (written != data.length) {
    throw new Error(
      `Not written as many bytes as expected! ${'$'}{written}, but expected ${'$'}{data.length}`,
    );
  }
}

class WasmModule {
  constructor(
    exports,
    u8,
  ) {
    this.exports = exports
    this.u8 = u8
  }
}

const wasmModules = new Map();

function readAndProcessPacket(
  reader,
  writer,
) {
  const { kind, streamId, payload } = readPacket(reader);
  const wasmModule = wasmModules.get(streamId);
  // write bytes
  switch (kind) {
    case 1000: { // Load WASM module
      const wasmModule = new WebAssembly.Module(payload);
      if (debug) console.error("!!CMD: Loaded WASM... ", wasmModule);
      const wasmInstance = new WebAssembly.Instance(wasmModule, {
        "wasi_snapshot_preview1": {
          proc_exit: () => console.error(arguments),
          fd_close: () => console.error(arguments),
          fd_write: () => console.error(arguments),
          fd_seek: () => console.error(arguments),
        },
      });
      wasmModules.set(
        streamId,
        new WasmModule(
          wasmInstance.exports,
          new Uint8Array((wasmInstance.exports.memory).buffer),
        ),
      );
      writePacket(writer, new Uint8Array(0));
      break;
    }
    case 1001: { // Unload WASM module and close connection
      if (debug) console.error("!!CMD: Unload WASM...");
      wasmModules.delete(streamId);
      writePacket(writer, new Uint8Array(0));
      break;
    }
    case 1010: { // writeBytes
      const [offset] = new Int32Array(payload.buffer, 0, 1);
      const data = new Uint8Array(payload, 4);
      if (debug) console.error("!!CMD: Write Bytes...", offset, data.length);
      wasmModule?.u8?.set(data, offset);
      writePacket(writer, new Uint8Array(0));
      break;
    }
    case 1011: { // allocAndWrite
      let offset = 0;
      if (wasmModule) {
        offset = wasmModule.exports.malloc(payload.length);
        if (debug) {
          console.error("!!CMD: allocAndWrite...", offset, payload.length);
        }
        wasmModule.u8?.set(payload, offset);
      }
      writePacket(writer, new Uint8Array(new Int32Array([offset]).buffer));
      break;
    }
    case 1012: { // free
      if (wasmModule) {
        const ptrs = new Int32Array(payload.buffer);
        for (const ptr of ptrs) {
          if (ptr != 0) wasmModule.exports.free(ptr);
        }
        if (debug) console.error("!!CMD: free...", ptrs);
      }
      writePacket(writer, new Uint8Array(0));
      break;
    }
    case 1013: { // stackSave
      const result = wasmModule.exports.stackSave();
      if (debug) console.error("!!CMD: stackSave...", "->", result);
      writePacket(writer, new Uint8Array(new Int32Array([result]).buffer));
      break;
    }
    case 1014: { // stackRestore
      const [ptr] = new Int32Array(payload.buffer, 0, 1);
      wasmModule.exports.stackRestore(ptr);
      if (debug) console.error("!!CMD: stackRestore...", ptr);
      writePacket(writer, new Uint8Array(0));
      break;
    }
    case 1015: { // stackAlloc
      const [size] = new Int32Array(payload.buffer, 0, 1);
      const result = wasmModule.exports.stackAlloc(size);
      if (debug) console.error("!!CMD: stackAlloc...", size, "->", result);
      writePacket(writer, new Uint8Array(new Int32Array([result]).buffer));
      break;
    }
    case 1016: { // stackAllocWrite
      const ptr = wasmModule.exports.stackAlloc(payload.length);
      wasmModule?.u8?.set(payload, ptr);
      if (debug) console.error("!!CMD: stackAllocWrite...", payload.length, "->", ptr);
      writePacket(writer, new Uint8Array(new Int32Array([ptr]).buffer));
      break;
    }
    case 1020: { // readBytes
      const info = new Int32Array(payload.buffer);
      const offset = info[0];
      const len = info[1];
      if (debug) console.error("!!CMD: Read Bytes...", offset, len, payload);
      writePacket(
        writer,
        wasmModule
          ? new Uint8Array(wasmModule.u8.buffer, offset, len)
          : new Uint8Array(len),
      );
      break;
    }
    case 1030: { // executeFunction
      const json = JSON.parse(new TextDecoder().decode(payload));
      if (debug) console.error("!!CMD: executeFunction...", json);
      let result = null;
      try {
        if (json.indirect !== undefined) {
            result = wasmModule.exports.__indirect_function_table.get(json.indirect)(...json.params);
        } else {
            result = wasmModule.exports[json.func](...json.params);
        }
      } catch (e) {
        console.error(e);
        console.error(e.stack);
      }
      const resultPayload = new TextEncoder().encode(JSON.stringify(result));
      if (debug) console.error("    -->", result, resultPayload);
      writePacket(writer, resultPayload);
      break;
    }
    default:
      console.error(`!!ERROR: Invalid message: ${'$'}{kind}`);
  }
}

function unhandledrejection(event) {
  console.error(`UNHANDLED PROMISE REJECTION: ${'$'}{event.reason}`);
  console.error(event.reason.stack);
  event.preventDefault();
}

if (isDeno) {
  addEventListener("unhandledrejection", unhandledrejection);
} else {
  process.on("unhandledrejection", unhandledrejection);
}

let running = true;

while (running) {
  if (isDeno) {
    readAndProcessPacket(Deno.stdin, Deno.stdout);
  } else {
    // I think (pure conjecture) that process.stdin.fd is a getter that, when referenced, will put stdin in non-blocking mode (which is causing the error). When you use the file descriptor directly, you work around that.
    // https://stackoverflow.com/questions/40362369/stdin-read-fails-on-some-input
    //readAndProcessPacket(process.stdin.fd, process.stdout.fd);
    readAndProcessPacket(0, 1);
  }
}
""".trimIndent()
*/
