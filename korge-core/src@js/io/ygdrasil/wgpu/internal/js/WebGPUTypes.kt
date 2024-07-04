// From wgpu4k. Eventually it will be used directly
// https:github.com/wgpu4k/wgpu4k/blob/main/wgpu4k/src/jsMain/kotlin/io.ygdrasil.wgpu/internal.js/webgpu_types.kt
@file:Suppress("UNUSED_PARAMETER", "UnsafeCastFromDynamic", "unused", "INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package io.ygdrasil.wgpu.internal.js

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.js.Promise


external interface GPUOrigin2DDictStrict : GPUOrigin2DDict

fun GPUOrigin2DDictStrict(
    x: GPUIntegerCoordinate,
    y: GPUIntegerCoordinate,
): GPUOrigin2DDictStrict = js("({x: x, y: y})")

external interface GPUExtent3DDictStrict : GPUExtent3DDict

fun GPUExtent3DDictStrict(
    width: GPUIntegerCoordinate,
    height: GPUIntegerCoordinate? = undefined,
    depthOrArrayLayers: GPUIntegerCoordinate? = undefined,
): GPUExtent3DDictStrict = js("({width: width, height: height, depthOrArrayLayers: depthOrArrayLayers})")


external interface GPUBindGroupDescriptor : GPUObjectDescriptorBase {
    var layout: GPUBindGroupLayout
    var entries: Array<GPUBindGroupEntry>
}

fun GPUBindGroupDescriptor(
    layout: GPUBindGroupLayout,
    entries: Array<GPUBindGroupEntry>,
): GPUBindGroupDescriptor = js("({layout: layout, entries: entries})")


external interface GPUBindGroupEntry {
    var binding: GPUIndex32
    var resource: dynamic /* GPUSampler | GPUTextureView | GPUBufferBinding | GPUExternalTexture */
}

fun GPUBindGroupEntry(
    binding: GPUIndex32,
    resource: dynamic /* GPUSampler | GPUTextureView | GPUBufferBinding | GPUExternalTexture */,
): GPUBindGroupEntry = js("({binding: binding, resource: resource})")


external interface GPUBindGroupLayoutDescriptor : GPUObjectDescriptorBase {
    var entries: Iterable<GPUBindGroupLayoutEntry>
}

fun GPUBindGroupLayoutDescriptor(
    entries: Iterable<GPUBindGroupLayoutEntry>,
): GPUBindGroupLayoutDescriptor = js("({entries: entries})")


external interface GPUBindGroupLayoutEntry {
    var binding: GPUIndex32
    var visibility: GPUShaderStageFlags
    var buffer: GPUBufferBindingLayout?
    var sampler: GPUSamplerBindingLayout?
    var texture: GPUTextureBindingLayout?
    var storageTexture: GPUStorageTextureBindingLayout?
    var externalTexture: GPUExternalTextureBindingLayout?
}

fun GPUBindGroupLayoutEntry(
    binding: GPUIndex32,
    visibility: GPUShaderStageFlags,
    buffer: GPUBufferBindingLayout? = undefined,
    sampler: GPUSamplerBindingLayout? = undefined,
    texture: GPUTextureBindingLayout? = undefined,
    storageTexture: GPUStorageTextureBindingLayout? = undefined,
    externalTexture: GPUExternalTextureBindingLayout? = undefined,
): GPUBindGroupLayoutEntry = js("({binding: binding, visibility: visibility, buffer: buffer, sampler: sampler, texture: texture, storageTexture: storageTexture, externalTexture: externalTexture})")

inline class GPUBlendOperation(val str: String) {
    companion object {
        inline val ADD get() = GPUBlendOperation("add")
        inline val SUBTRACT get() = GPUBlendOperation("subtract")
        inline val REVERSE_SUBTRACT get() = GPUBlendOperation("reverse-subtract")
        inline val MIN get() = GPUBlendOperation("min")
        inline val MAX get() = GPUBlendOperation("max")
    }
}

inline class GPUBlendFactor(val str: String) {
    companion object {
        inline val ZERO get() = GPUBlendFactor("zero")
        inline val ONE get() = GPUBlendFactor("one")
        inline val SRC get() = GPUBlendFactor("src")
        inline val ONE_MINUS_SRC get() = GPUBlendFactor("one-minus-src")
        inline val SRC_ALPHA get() = GPUBlendFactor("src-alpha")
        inline val ONE_MINUS_SRC_ALPHA get() = GPUBlendFactor("one-minus-src-alpha")
        inline val DST get() = GPUBlendFactor("dst")
        inline val ONE_MINUS_DST get() = GPUBlendFactor("one-minus-dst")
        inline val DST_ALPHA get() = GPUBlendFactor("dst-alpha")
        inline val ONE_MINUS_DST_ALPHA get() = GPUBlendFactor("one-minus-dst-alpha")
        inline val SRC_ALPHA_SATURATED get() = GPUBlendFactor("src-alpha-saturated")
        inline val CONSTANT get() = GPUBlendFactor("constant")
        inline val ONE_MINUS_CONSTANT get() = GPUBlendFactor("one-minus-constant")
    }
}

external interface GPUBlendComponent {
    var operation: GPUBlendOperation?
    var srcFactor: GPUBlendFactor?
    var dstFactor: GPUBlendFactor?
}

fun GPUBlendComponent(
    operation: GPUBlendOperation? = undefined,
    srcFactor: GPUBlendFactor? = undefined,
    dstFactor: GPUBlendFactor? = undefined,
): GPUBlendComponent = js("({operation: operation, srcFactor: srcFactor, dstFactor: dstFactor})")


external interface GPUBlendState {
    var color: GPUBlendComponent
    var alpha: GPUBlendComponent
}

fun GPUBlendState(
    color: GPUBlendComponent,
    alpha: GPUBlendComponent,
): GPUBlendState = js("({color: color, alpha: alpha})")


external interface GPUBufferBinding {
    var buffer: GPUBuffer
    var offset: GPUSize64?
    var size: GPUSize64?
}

fun GPUBufferBinding(
    buffer: GPUBuffer,
    offset: GPUSize64? = undefined,
    size: GPUSize64? = undefined,
): GPUBufferBinding = js("({buffer: buffer, offset: offset, size: size})")


external interface GPUBufferBindingLayout {
    var type: String? /* "uniform" | "storage" | "read-only-storage" */
    var hasDynamicOffset: Boolean?
    var minBindingSize: GPUSize64?
}

fun GPUBufferBindingLayout(
    type: String? = undefined,
    hasDynamicOffset: Boolean? = undefined,
    minBindingSize: GPUSize64? = undefined,
): GPUBufferBindingLayout = js("({type: type, hasDynamicOffset: hasDynamicOffset, minBindingSize: minBindingSize})")


external interface GPUBufferDescriptor : GPUObjectDescriptorBase {
    var size: GPUSize64
    var usage: GPUBufferUsageFlags
    var mappedAtCreation: Boolean?
}

fun GPUBufferDescriptor(
    size: GPUSize64,
    usage: GPUBufferUsageFlags,
    mappedAtCreation: Boolean? = undefined,
    label: String? = undefined,
): GPUBufferDescriptor = js("({size: size, usage: usage, mappedAtCreation: mappedAtCreation, label: label})")

inline class GPUTextureFormat(val str: String) {
    companion object {
        inline val R8UNORM get() = GPUTextureFormat("r8unorm")
        inline val R8SNORM get() = GPUTextureFormat("r8snorm")
        inline val R8UINT get() = GPUTextureFormat("r8uint")
        inline val R8SINT get() = GPUTextureFormat("r8sint")
        inline val R16UINT get() = GPUTextureFormat("r16uint")
        inline val R16SINT get() = GPUTextureFormat("r16sint")
        inline val R16FLOAT get() = GPUTextureFormat("r16float")
        inline val RG8UNORM get() = GPUTextureFormat("rg8unorm")
        inline val RG8SNORM get() = GPUTextureFormat("rg8snorm")
        inline val RG8UINT get() = GPUTextureFormat("rg8uint")
        inline val RG8SINT get() = GPUTextureFormat("rg8sint")
        inline val R32UINT get() = GPUTextureFormat("r32uint")
        inline val R32SINT get() = GPUTextureFormat("r32sint")
        inline val R32FLOAT get() = GPUTextureFormat("r32float")
        inline val RG16UINT get() = GPUTextureFormat("rg16uint")
        inline val RG16SINT get() = GPUTextureFormat("rg16sint")
        inline val RG16FLOAT get() = GPUTextureFormat("rg16float")
        inline val RGBA8UNORM get() = GPUTextureFormat("rgba8unorm")
        inline val RGBA8UNORM_SRGB get() = GPUTextureFormat("rgba8unorm-srgb")
        inline val RGBA8SNORM get() = GPUTextureFormat("rgba8snorm")
        inline val RGBA8UINT get() = GPUTextureFormat("rgba8uint")
        inline val RGBA8SINT get() = GPUTextureFormat("rgba8sint")
        inline val BGRA8UNORM get() = GPUTextureFormat("bgra8unorm")
        inline val BGRA8UNORM_SRGB get() = GPUTextureFormat("bgra8unorm-srgb")
        inline val RGB9E5UFLOAT get() = GPUTextureFormat("rgb9e5ufloat")
        inline val RGB10A2UINT get() = GPUTextureFormat("rgb10a2uint")
        inline val RGB10A2UNORM get() = GPUTextureFormat("rgb10a2unorm")
        inline val RG11B10UFLOAT get() = GPUTextureFormat("rg11b10ufloat")
        inline val RG32UINT get() = GPUTextureFormat("rg32uint")
        inline val RG32SINT get() = GPUTextureFormat("rg32sint")
        inline val RG32FLOAT get() = GPUTextureFormat("rg32float")
        inline val RGBA16UINT get() = GPUTextureFormat("rgba16uint")
        inline val RGBA16SINT get() = GPUTextureFormat("rgba16sint")
        inline val RGBA16FLOAT get() = GPUTextureFormat("rgba16float")
        inline val RGBA32UINT get() = GPUTextureFormat("rgba32uint")
        inline val RGBA32SINT get() = GPUTextureFormat("rgba32sint")
        inline val RGBA32FLOAT get() = GPUTextureFormat("rgba32float")
        inline val STENCIL8 get() = GPUTextureFormat("stencil8")
        inline val DEPTH16UNORM get() = GPUTextureFormat("depth16unorm")
        inline val DEPTH24PLUS get() = GPUTextureFormat("depth24plus")
        inline val DEPTH24PLUS_STENCIL8 get() = GPUTextureFormat("depth24plus-stencil8")
        inline val DEPTH32FLOAT get() = GPUTextureFormat("depth32float")
        inline val DEPTH32FLOAT_STENCIL8 get() = GPUTextureFormat("depth32float-stencil8")
        inline val BC1_RGBA_UNORM get() = GPUTextureFormat("bc1-rgba-unorm")
        inline val BC1_RGBA_UNORM_SRGB get() = GPUTextureFormat("bc1-rgba-unorm-srgb")
        inline val BC2_RGBA_UNORM get() = GPUTextureFormat("bc2-rgba-unorm")
        inline val BC2_RGBA_UNORM_SRGB get() = GPUTextureFormat("bc2-rgba-unorm-srgb")
        inline val BC3_RGBA_UNORM get() = GPUTextureFormat("bc3-rgba-unorm")
        inline val BC3_RGBA_UNORM_SRGB get() = GPUTextureFormat("bc3-rgba-unorm-srgb")
        inline val BC4_R_UNORM get() = GPUTextureFormat("bc4-r-unorm")
        inline val BC4_R_SNORM get() = GPUTextureFormat("bc4-r-snorm")
        inline val BC5_RG_UNORM get() = GPUTextureFormat("bc5-rg-unorm")
        inline val BC5_RG_SNORM get() = GPUTextureFormat("bc5-rg-snorm")
        inline val BC6H_RGB_UFLOAT get() = GPUTextureFormat("bc6h-rgb-ufloat")
        inline val BC6H_RGB_FLOAT get() = GPUTextureFormat("bc6h-rgb-float")
        inline val BC7_RGBA_UNORM get() = GPUTextureFormat("bc7-rgba-unorm")
        inline val BC7_RGBA_UNORM_SRGB get() = GPUTextureFormat("bc7-rgba-unorm-srgb")
        inline val ETC2_RGB8UNORM get() = GPUTextureFormat("etc2-rgb8unorm")
        inline val ETC2_RGB8UNORM_SRGB get() = GPUTextureFormat("etc2-rgb8unorm-srgb")
        inline val ETC2_RGB8A1UNORM get() = GPUTextureFormat("etc2-rgb8a1unorm")
        inline val ETC2_RGB8A1UNORM_SRGB get() = GPUTextureFormat("etc2-rgb8a1unorm-srgb")
        inline val ETC2_RGBA8UNORM get() = GPUTextureFormat("etc2-rgba8unorm")
        inline val ETC2_RGBA8UNORM_SRGB get() = GPUTextureFormat("etc2-rgba8unorm-srgb")
        inline val EAC_R11UNORM get() = GPUTextureFormat("eac-r11unorm")
        inline val EAC_R11SNORM get() = GPUTextureFormat("eac-r11snorm")
        inline val EAC_RG11UNORM get() = GPUTextureFormat("eac-rg11unorm")
        inline val EAC_RG11SNORM get() = GPUTextureFormat("eac-rg11snorm")
        inline val ASTC_4X4_UNORM get() = GPUTextureFormat("astc-4x4-unorm")
        inline val ASTC_4X4_UNORM_SRGB get() = GPUTextureFormat("astc-4x4-unorm-srgb")
        inline val ASTC_5X4_UNORM get() = GPUTextureFormat("astc-5x4-unorm")
        inline val ASTC_5X4_UNORM_SRGB get() = GPUTextureFormat("astc-5x4-unorm-srgb")
        inline val ASTC_5X5_UNORM get() = GPUTextureFormat("astc-5x5-unorm")
        inline val ASTC_5X5_UNORM_SRGB get() = GPUTextureFormat("astc-5x5-unorm-srgb")
        inline val ASTC_6X5_UNORM get() = GPUTextureFormat("astc-6x5-unorm")
        inline val ASTC_6X5_UNORM_SRGB get() = GPUTextureFormat("astc-6x5-unorm-srgb")
        inline val ASTC_6X6_UNORM get() = GPUTextureFormat("astc-6x6-unorm")
        inline val ASTC_6X6_UNORM_SRGB get() = GPUTextureFormat("astc-6x6-unorm-srgb")
        inline val ASTC_8X5_UNORM get() = GPUTextureFormat("astc-8x5-unorm")
        inline val ASTC_8X5_UNORM_SRGB get() = GPUTextureFormat("astc-8x5-unorm-srgb")
        inline val ASTC_8X6_UNORM get() = GPUTextureFormat("astc-8x6-unorm")
        inline val ASTC_8X6_UNORM_SRGB get() = GPUTextureFormat("astc-8x6-unorm-srgb")
        inline val ASTC_8X8_UNORM get() = GPUTextureFormat("astc-8x8-unorm")
        inline val ASTC_8X8_UNORM_SRGB get() = GPUTextureFormat("astc-8x8-unorm-srgb")
        inline val ASTC_10X5_UNORM get() = GPUTextureFormat("astc-10x5-unorm")
        inline val ASTC_10X5_UNORM_SRGB get() = GPUTextureFormat("astc-10x5-unorm-srgb")
        inline val ASTC_10X6_UNORM get() = GPUTextureFormat("astc-10x6-unorm")
        inline val ASTC_10X6_UNORM_SRGB get() = GPUTextureFormat("astc-10x6-unorm-srgb")
        inline val ASTC_10X8_UNORM get() = GPUTextureFormat("astc-10x8-unorm")
        inline val ASTC_10X8_UNORM_SRGB get() = GPUTextureFormat("astc-10x8-unorm-srgb")
        inline val ASTC_10X10_UNORM get() = GPUTextureFormat("astc-10x10-unorm")
        inline val ASTC_10X10_UNORM_SRGB get() = GPUTextureFormat("astc-10x10-unorm-srgb")
        inline val ASTC_12X10_UNORM get() = GPUTextureFormat("astc-12x10-unorm")
        inline val ASTC_12X10_UNORM_SRGB get() = GPUTextureFormat("astc-12x10-unorm-srgb")
        inline val ASTC_12X12_UNORM get() = GPUTextureFormat("astc-12x12-unorm")
        inline val ASTC_12X12_UNORM_SRGB get() = GPUTextureFormat("astc-12x12-unorm-srgb")
    }
}

inline class GPUAlphaMode(val str: String) {
    companion object {
        inline val OPAQUE get() = GPUAlphaMode("opaque")
        inline val PREMULTIPLIED get() = GPUAlphaMode("premultiplied")
    }
}

external interface GPUCanvasConfiguration {
    var device: GPUDevice
    var format: GPUTextureFormat
    var usage: GPUTextureUsageFlags?
    var viewFormats: Array<GPUTextureFormat?>?
    var colorSpace: Any?
    var alphaMode: GPUAlphaMode?
}

fun GPUCanvasConfiguration(
    device: GPUDevice,
    format: GPUTextureFormat,
    usage: GPUTextureUsageFlags? = undefined,
    viewFormats: Array<GPUTextureFormat?>? = undefined,
    colorSpace: Any? = undefined,
    alphaMode: GPUAlphaMode? = undefined,
): GPUCanvasConfiguration = js("({device: device, format: format, usage: usage, viewFormats: viewFormats, colorSpace: colorSpace, alphaMode: alphaMode})")


external interface GPUColorDict {
    var r: Number
    var g: Number
    var b: Number
    var a: Number
}

fun GPUColorDict(
    r: Number,
    g: Number,
    b: Number,
    a: Number,
): GPUColorDict = js("({r: r, g: g, b: b, a: a})")


external interface GPUColorTargetState {
    var format: GPUTextureFormat
    var blend: GPUBlendState?
    var writeMask: GPUColorWriteFlags?
}

fun GPUColorTargetState(
    format: GPUTextureFormat,
    blend: GPUBlendState? = undefined,
    writeMask: GPUColorWriteFlags? = undefined,
): GPUColorTargetState = js("({format: format, blend: blend, writeMask: writeMask})")

typealias GPUCommandBufferDescriptor = GPUObjectDescriptorBase

typealias GPUCommandEncoderDescriptor = GPUObjectDescriptorBase


external interface GPUComputePassDescriptor : GPUObjectDescriptorBase {
    var timestampWrites: GPUComputePassTimestampWrites?
}

fun GPUComputePassDescriptor(
    timestampWrites: GPUComputePassTimestampWrites? = undefined,
    label: String? = undefined,
): GPUComputePassDescriptor = js("({timestampWrites: timestampWrites, label: label})")


external interface GPUComputePassTimestampWrites {
    var querySet: GPUQuerySet
    var beginningOfPassWriteIndex: GPUSize32?
    var endOfPassWriteIndex: GPUSize32?
}

fun GPUComputePassTimestampWrites(
    querySet: GPUQuerySet,
    beginningOfPassWriteIndex: GPUSize32? = undefined,
    endOfPassWriteIndex: GPUSize32? = undefined,
): GPUComputePassTimestampWrites = js("({querySet: querySet, beginningOfPassWriteIndex: beginningOfPassWriteIndex, endOfPassWriteIndex: endOfPassWriteIndex})")


external interface GPUComputePipelineDescriptor : GPUPipelineDescriptorBase {
    var compute: GPUProgrammableStage
}

fun GPUComputePipelineDescriptor(
    compute: GPUProgrammableStage,
    layout: dynamic /* GPUPipelineLayout | "auto" */,
    label: String? = undefined,
): GPUComputePipelineDescriptor = js("({compute: compute, layout: layout, label: label})")

inline class GPUCompare(val str: String) {
    companion object {
        inline val NEVER get() = GPUCompare("never")
        inline val LESS get() = GPUCompare("less")
        inline val EQUAL get() = GPUCompare("equal")
        inline val LESS_EQUAL get() = GPUCompare("less-equal")
        inline val GREATER get() = GPUCompare("greater")
        inline val NOT_EQUAL get() = GPUCompare("not-equal")
        inline val GREATER_EQUAL get() = GPUCompare("greater-equal")
        inline val ALWAYS get() = GPUCompare("always")
    }
}

external interface GPUDepthStencilState {
    var format: GPUTextureFormat
    var depthWriteEnabled: Boolean?
    var depthCompare: GPUCompare?
    var stencilFront: GPUStencilFaceState?
    var stencilBack: GPUStencilFaceState?
    var stencilReadMask: GPUStencilValue?
    var stencilWriteMask: GPUStencilValue?
    var depthBias: GPUDepthBias?
    var depthBiasSlopeScale: Float?
    var depthBiasClamp: Float?
}

fun GPUDepthStencilState(
    format: GPUTextureFormat,
    depthWriteEnabled: Boolean? = undefined,
    depthCompare: GPUCompare? = undefined,
    stencilFront: GPUStencilFaceState? = undefined,
    stencilBack: GPUStencilFaceState? = undefined,
    stencilReadMask: GPUStencilValue? = undefined,
    stencilWriteMask: GPUStencilValue? = undefined,
    depthBias: GPUDepthBias? = undefined,
    depthBiasSlopeScale: Float? = undefined,
    depthBiasClamp: Float? = undefined,
): GPUDepthStencilState = js("({format: format, depthWriteEnabled: depthWriteEnabled, depthCompare: depthCompare, stencilFront: stencilFront, stencilBack: stencilBack, stencilReadMask: stencilReadMask, stencilWriteMask: stencilWriteMask, depthBias: depthBias, depthBiasSlopeScale: depthBiasSlopeScale, depthBiasClamp: depthBiasClamp})")


external interface GPUDeviceDescriptor : GPUObjectDescriptorBase {
    var requiredFeatures: Array<String? /* "depth-clip-control" | "depth32float-stencil8" | "texture-compression-bc" | "texture-compression-etc2" | "texture-compression-astc" | "timestamp-query" | "indirect-first-instance" | "shader-f16" | "rg11b10ufloat-renderable" | "bgra8unorm-storage" | "float32-filterable" */>?
    var requiredLimits: Record<String, GPUSize64>?
    var defaultQueue: GPUQueueDescriptor?
}

fun GPUDeviceDescriptor(
    requiredFeatures: Array<String? /* "depth-clip-control" | "depth32float-stencil8" | "texture-compression-bc" | "texture-compression-etc2" | "texture-compression-astc" | "timestamp-query" | "indirect-first-instance" | "shader-f16" | "rg11b10ufloat-renderable" | "bgra8unorm-storage" | "float32-filterable" */>? = undefined,
    requiredLimits: Record<String, GPUSize64>? = undefined,
    defaultQueue: GPUQueueDescriptor? = undefined,
    label: String? = undefined,
): GPUDeviceDescriptor = js("({requiredFeatures: requiredFeatures, requiredLimits: requiredLimits, defaultQueue: defaultQueue, label: label})")


external interface GPUExtent3DDict {
    var width: GPUIntegerCoordinate
    var height: GPUIntegerCoordinate?
    var depthOrArrayLayers: GPUIntegerCoordinate?
}

fun GPUExtent3DDict(
    width: GPUIntegerCoordinate,
    height: GPUIntegerCoordinate? = undefined,
    depthOrArrayLayers: GPUIntegerCoordinate? = undefined,
): GPUExtent3DDict = js("({width: width, height: height, depthOrArrayLayers: depthOrArrayLayers})")

external interface GPUExternalTextureBindingLayout


external interface GPUExternalTextureDescriptor : GPUObjectDescriptorBase {
    var source: dynamic /* HTMLVideoElement | VideoFrame */
    var colorSpace: Any?
}

fun GPUExternalTextureDescriptor(
    source: dynamic /* HTMLVideoElement | VideoFrame */,
    colorSpace: Any? = undefined,
    label: String? = undefined,
): GPUExternalTextureDescriptor = js("({source: source, colorSpace: colorSpace, label: label})")


external interface GPUFragmentState : GPUProgrammableStage {
    var targets: Array<GPUColorTargetState?>
}

fun GPUFragmentState(
    module: GPUShaderModule,
    entryPoint: String? = undefined,
    constants: Map<String, GPUPipelineConstantValue>? = undefined,
    targets: Array<GPUColorTargetState?>,
): GPUFragmentState = js("({module: module, entryPoint: entryPoint, constants: constants, targets: targets})")


external interface GPUImageCopyBuffer : GPUImageDataLayout {
    var buffer: GPUBuffer
}

fun GPUImageCopyBuffer(
    buffer: GPUBuffer,
    offset: GPUSize64? = undefined,
    bytesPerRow: GPUSize32? = undefined,
    rowsPerImage: GPUSize32? = undefined,
): GPUImageCopyBuffer = js("({buffer: buffer, offset: offset, bytesPerRow: bytesPerRow, rowsPerImage: rowsPerImage})")


external interface GPUImageCopyExternalImage {
    var source: Any
    /* ImageBitmap | ImageData | HTMLImageElement | HTMLVideoElement | VideoFrame | HTMLCanvasElement | OffscreenCanvas */
    var origin: GPUOrigin2DDictStrict /* Iterable<GPUIntegerCoordinate>? | GPUOrigin2DDictStrict? */
    var flipY: Boolean
}

fun GPUImageCopyExternalImage(
    source: Any,
    origin: GPUOrigin2DDictStrict,
    flipY: Boolean,
): GPUImageCopyExternalImage = js("({source: source, origin: origin, flipY: flipY})")


external interface GPUImageCopyTexture {
    var texture: GPUTexture
    var mipLevel: GPUIntegerCoordinate?
    var origin: Array<GPUIntegerCoordinate>?
    var aspect: String?
}

fun GPUImageCopyTexture(
    texture: GPUTexture,
    mipLevel: GPUIntegerCoordinate? = undefined,
    origin: Array<GPUIntegerCoordinate>? = undefined,
    aspect: String? = undefined,
): GPUImageCopyTexture = js("({texture: texture, mipLevel: mipLevel, origin: origin, aspect: aspect})")


external interface GPUImageCopyTextureTagged : GPUImageCopyTexture {
    var colorSpace: String
    var premultipliedAlpha: Boolean
}

fun GPUImageCopyTextureTagged(
    texture: GPUTexture,
    mipLevel: GPUIntegerCoordinate? = undefined,
    origin: Array<GPUIntegerCoordinate>? = undefined,
    aspect: String? = undefined,
    colorSpace: String,
    premultipliedAlpha: Boolean,
): GPUImageCopyTextureTagged = js("({texture: texture, mipLevel: mipLevel, origin: origin, aspect: aspect, colorSpace: colorSpace, premultipliedAlpha: premultipliedAlpha})")


external interface GPUImageDataLayout {
    var offset: GPUSize64?
    var bytesPerRow: GPUSize32?
    var rowsPerImage: GPUSize32?
}

fun GPUImageDataLayout(
    offset: GPUSize64? = undefined,
    bytesPerRow: GPUSize32? = undefined,
    rowsPerImage: GPUSize32? = undefined,
): GPUImageDataLayout = js("({offset: offset, bytesPerRow: bytesPerRow, rowsPerImage: rowsPerImage})")


external interface GPUMultisampleState {
    var count: GPUSize32?
    @Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")
    var mask: GPUSampleMask?
    var alphaToCoverageEnabled: Boolean?
}

fun GPUMultisampleState(
    count: GPUSize32? = undefined,
    mask: GPUSampleMask? = undefined,
    alphaToCoverageEnabled: Boolean? = undefined,
): GPUMultisampleState = js("({count: count, mask: mask, alphaToCoverageEnabled: alphaToCoverageEnabled})")


external interface GPUObjectDescriptorBase {
    var label: String?
}

fun GPUObjectDescriptorBase(
    label: String? = undefined,
): GPUObjectDescriptorBase = js("({label: label})")


external interface GPUOrigin2DDict {
    var x: GPUIntegerCoordinate
    var y: GPUIntegerCoordinate
}

fun GPUOrigin2DDict(
    x: GPUIntegerCoordinate,
    y: GPUIntegerCoordinate,
): GPUOrigin2DDict = js("({x: x, y: y})")


external interface GPUOrigin3DDict {
    var x: GPUIntegerCoordinate?
    var y: GPUIntegerCoordinate?
    var z: GPUIntegerCoordinate?
}

fun GPUOrigin3DDict(
    x: GPUIntegerCoordinate? = undefined,
    y: GPUIntegerCoordinate? = undefined,
    z: GPUIntegerCoordinate? = undefined,
): GPUOrigin3DDict = js("({x: x, y: y, z: z})")


external interface GPUPipelineDescriptorBase : GPUObjectDescriptorBase {
    var layout: dynamic /* GPUPipelineLayout | "auto" */
}

fun GPUPipelineDescriptorBase(
    layout: dynamic /* GPUPipelineLayout | "auto" */,
    label: String? = undefined,
): GPUPipelineDescriptorBase = js("({layout: layout, label: label})")


external interface GPUPipelineErrorInit {
    var reason: String /* "validation" | "internal" */
}

fun GPUPipelineErrorInit(
    reason: String,
): GPUPipelineErrorInit = js("({reason: reason})")


external interface GPUPipelineLayoutDescriptor : GPUObjectDescriptorBase {
    var bindGroupLayouts: Array<GPUBindGroupLayout>
}

fun GPUPipelineLayoutDescriptor(
    bindGroupLayouts: Array<GPUBindGroupLayout>,
): GPUPipelineLayoutDescriptor = js("({bindGroupLayouts: bindGroupLayouts})")


external interface GPUPrimitiveState {
    var topology: String? /* "point-list" | "line-list" | "line-strip" | "triangle-list" | "triangle-strip" */
    var stripIndexFormat: String? /* "uint16" | "uint32" */
    var frontFace: String? /* "ccw" | "cw" */
    var cullMode: String? /* "none" | "front" | "back" */
    var unclippedDepth: Boolean?
}

fun GPUPrimitiveState(
    topology: String? = undefined,
    stripIndexFormat: String? = undefined,
    frontFace: String? = undefined,
    cullMode: String? = undefined,
    unclippedDepth: Boolean? = undefined,
): GPUPrimitiveState = js("({topology: topology, stripIndexFormat: stripIndexFormat, frontFace: frontFace, cullMode: cullMode, unclippedDepth: unclippedDepth})")


external interface GPUProgrammableStage {
    var module: GPUShaderModule
    var entryPoint: String?
    var constants: Map<String, GPUPipelineConstantValue>?
}

fun GPUProgrammableStage(
    module: GPUShaderModule,
    entryPoint: String? = undefined,
    constants: Map<String, GPUPipelineConstantValue>? = undefined,
): GPUProgrammableStage = js("({module: module, entryPoint: entryPoint, constants: constants})")


external interface GPUQuerySetDescriptor : GPUObjectDescriptorBase {
    var type: String /* "occlusion" | "timestamp" */
    var count: GPUSize32
}

fun GPUQuerySetDescriptor(
    type: String,
    count: GPUSize32,
): GPUQuerySetDescriptor = js("({type: type, count: count})")

typealias GPUQueueDescriptor = GPUObjectDescriptorBase

typealias GPURenderBundleDescriptor = GPUObjectDescriptorBase


external interface GPURenderBundleEncoderDescriptor : GPURenderPassLayout {
    var depthReadOnly: Boolean?
    var stencilReadOnly: Boolean?
}


external interface GPURenderPassColorAttachment {
    var view: GPUTextureView
    var depthSlice: GPUIntegerCoordinate?
    var resolveTarget: GPUTextureView?
    var clearValue: Array<Number>? /* Iterable<Number>? | GPUColorDict? */
    var loadOp: GPULoadOP
    var storeOp: GPUStoreOP
}

fun GPURenderPassColorAttachment(
    view: GPUTextureView,
    depthSlice: GPUIntegerCoordinate? = undefined,
    resolveTarget: GPUTextureView? = undefined,
    clearValue: Array<Number>? = undefined,
    loadOp: GPULoadOP,
    storeOp: GPUStoreOP,
): GPURenderPassColorAttachment = js("({view: view, depthSlice: depthSlice, resolveTarget: resolveTarget, clearValue: clearValue, loadOp: loadOp, storeOp: storeOp})")

inline class GPULoadOP(val str: String) {
    companion object {
        inline val LOAD get() = GPULoadOP("load")
        inline val CLEAR get() = GPULoadOP("clear")
    }
}

inline class GPUStoreOP(val str: String) {
    companion object {
        inline val STORE get() = GPUStoreOP("store")
        inline val DISCARD get() = GPUStoreOP("discard")
    }
}

external interface GPURenderPassDepthStencilAttachment {
    var view: GPUTextureView
    var depthClearValue: Number?
    var depthLoadOp: GPULoadOP?
    var depthStoreOp: GPUStoreOP?
    var depthReadOnly: Boolean?
    var stencilClearValue: GPUStencilValue?
    var stencilLoadOp: GPULoadOP?
    var stencilStoreOp: GPUStoreOP?
    var stencilReadOnly: Boolean?
}

fun GPURenderPassDepthStencilAttachment(
    view: GPUTextureView,
    depthClearValue: Number? = undefined,
    depthLoadOp: GPULoadOP? = undefined,
    depthStoreOp: GPUStoreOP? = undefined,
    depthReadOnly: Boolean? = undefined,
    stencilClearValue: GPUStencilValue? = undefined,
    stencilLoadOp: GPULoadOP? = undefined,
    stencilStoreOp: GPUStoreOP? = undefined,
    stencilReadOnly: Boolean? = undefined,
): GPURenderPassDepthStencilAttachment = js("({view: view, depthClearValue: depthClearValue, depthLoadOp: depthLoadOp, depthStoreOp: depthStoreOp, depthReadOnly: depthReadOnly, stencilClearValue: stencilClearValue, stencilLoadOp: stencilLoadOp, stencilStoreOp: stencilStoreOp, stencilReadOnly: stencilReadOnly})")

external interface GPURenderPassDescriptor : GPUObjectDescriptorBase {
    var colorAttachments: Array<GPURenderPassColorAttachment>
    var depthStencilAttachment: GPURenderPassDepthStencilAttachment?
    var occlusionQuerySet: GPUQuerySet?
    var timestampWrites: GPURenderPassTimestampWrites?
    var maxDrawCount: GPUSize64?
}

fun GPURenderPassDescriptor(
    colorAttachments: Array<GPURenderPassColorAttachment>,
    depthStencilAttachment: GPURenderPassDepthStencilAttachment? = undefined,
    occlusionQuerySet: GPUQuerySet? = undefined,
    timestampWrites: GPURenderPassTimestampWrites? = undefined,
    maxDrawCount: GPUSize64? = undefined,
): GPURenderPassDescriptor = js("({colorAttachments: colorAttachments, depthStencilAttachment: depthStencilAttachment, occlusionQuerySet: occlusionQuerySet, timestampWrites: timestampWrites, maxDrawCount: maxDrawCount})")


external interface GPURenderPassLayout : GPUObjectDescriptorBase {
    var colorFormats: Iterable<GPUTextureFormat>
    var depthStencilFormat: GPUTextureFormat?
    var sampleCount: GPUSize32?
}

fun GPURenderPassLayout(
    colorFormats: Iterable<GPUTextureFormat>,
    depthStencilFormat: GPUTextureFormat? = undefined,
    sampleCount: GPUSize32? = undefined,
    label: String? = undefined,
): GPURenderPassLayout = js("({colorFormats: colorFormats, depthStencilFormat: depthStencilFormat, sampleCount: sampleCount, label: label})")

external interface GPURenderPassTimestampWrites {
    var querySet: GPUQuerySet
    var beginningOfPassWriteIndex: GPUSize32?
    var endOfPassWriteIndex: GPUSize32?
}

fun GPURenderPassTimestampWrites(
    querySet: GPUQuerySet,
    beginningOfPassWriteIndex: GPUSize32? = undefined,
    endOfPassWriteIndex: GPUSize32? = undefined,
): GPURenderPassTimestampWrites = js("({querySet: querySet, beginningOfPassWriteIndex: beginningOfPassWriteIndex, endOfPassWriteIndex: endOfPassWriteIndex})")

external interface GPURenderPipelineDescriptor : GPUPipelineDescriptorBase {
    var vertex: GPUVertexState
    var primitive: GPUPrimitiveState?
    var depthStencil: GPUDepthStencilState?
    var multisample: GPUMultisampleState?
    var fragment: GPUFragmentState?
}

fun GPURenderPipelineDescriptor(
    vertex: GPUVertexState,
    primitive: GPUPrimitiveState? = undefined,
    depthStencil: GPUDepthStencilState? = undefined,
    multisample: GPUMultisampleState? = undefined,
    fragment: GPUFragmentState? = undefined,
    layout: dynamic = undefined,
    label: String? = undefined,
): GPURenderPipelineDescriptor = js("({vertex: vertex, primitive: primitive, depthStencil: depthStencil, multisample: multisample, fragment: fragment, layout: layout, label: label})")

inline class GPUPowerPreference(val str: String) {
    companion object {
        inline val LOW_POWER get() = GPUPowerPreference("low-power")
        inline val HIGH_PERFORMANCE get() = GPUPowerPreference("high-performance")
    }
}

external interface GPURequestAdapterOptions {
    var powerPreference: GPUPowerPreference?
    var forceFallbackAdapter: Boolean?
}

fun GPURequestAdapterOptions(
    powerPreference: GPUPowerPreference? = undefined,
    forceFallbackAdapter: Boolean? = undefined,
): GPURequestAdapterOptions = js("({powerPreference: powerPreference, forceFallbackAdapter: forceFallbackAdapter})")


external interface GPUSamplerBindingLayout {
    var type: String? /* "filtering" | "non-filtering" | "comparison" */
}

inline class GPUAddressMode(val str: String) {
    companion object {
        inline val CLAMP_TO_EDGE get() = GPUAddressMode("clamp-to-edge")
        inline val REPEAT get() = GPUAddressMode("repeat")
        inline val MIRROR_REPEAT get() = GPUAddressMode("mirror-repeat")
    }
}

inline class GPUFilterMode(val str: String) {
    companion object {
        inline val NEAREST get() = GPUFilterMode("nearest")
        inline val LINEAR get() = GPUFilterMode("linear")
    }
}

external interface GPUSamplerDescriptor : GPUObjectDescriptorBase {
    var addressModeU: GPUAddressMode?
    var addressModeV: GPUAddressMode?
    var addressModeW: GPUAddressMode?
    var magFilter: GPUFilterMode?
    var minFilter: GPUFilterMode?
    var mipmapFilter: GPUFilterMode?
    var lodMinClamp: Number?
    var lodMaxClamp: Number?
    var compare: GPUCompare?
    var maxAnisotropy: Number?
}

fun GPUSamplerDescriptor(
    addressModeU: GPUAddressMode? = undefined,
    addressModeV: GPUAddressMode? = undefined,
    addressModeW: GPUAddressMode? = undefined,
    magFilter: GPUFilterMode? = undefined,
    minFilter: GPUFilterMode? = undefined,
    mipmapFilter: GPUFilterMode? = undefined,
    lodMinClamp: Number? = undefined,
    lodMaxClamp: Number? = undefined,
    compare: GPUCompare? = undefined,
    maxAnisotropy: Number? = undefined,
    label: String? = undefined,
): GPUSamplerDescriptor = js("({addressModeU: addressModeU, addressModeV: addressModeV, addressModeW: addressModeW, magFilter: magFilter, minFilter: minFilter, mipmapFilter: mipmapFilter, lodMinClamp: lodMinClamp, lodMaxClamp: lodMaxClamp, compare: compare, maxAnisotropy: maxAnisotropy, label: label})")

external interface GPUShaderModuleCompilationHint {
    var entryPoint: String
    var layout: dynamic /* GPUPipelineLayout? | "auto" */
}

fun GPUShaderModuleCompilationHint(
    entryPoint: String,
    layout: dynamic = undefined,
): GPUShaderModuleCompilationHint = js("({entryPoint: entryPoint, layout: layout})")

external interface GPUShaderModuleDescriptor : GPUObjectDescriptorBase {
    var code: String
    var sourceMap: Any?
    var compilationHints: Array<GPUShaderModuleCompilationHint>?
}

fun GPUShaderModuleDescriptor(
    //language=WGSL
    code: String,
    sourceMap: Any? = undefined,
    compilationHints: Array<GPUShaderModuleCompilationHint>? = undefined,
    label: String? = undefined,
): GPUShaderModuleDescriptor = js("({ code: code, sourceMap: sourceMap, compilationHints: compilationHints, label: label })")


external interface GPUStencilFaceState {
    var compare: String? /* "never" | "less" | "equal" | "less-equal" | "greater" | "not-equal" | "greater-equal" | "always" */
    var failOp: String? /* "keep" | "zero" | "replace" | "invert" | "increment-clamp" | "decrement-clamp" | "increment-wrap" | "decrement-wrap" */
    var depthFailOp: String? /* "keep" | "zero" | "replace" | "invert" | "increment-clamp" | "decrement-clamp" | "increment-wrap" | "decrement-wrap" */
    var passOp: String? /* "keep" | "zero" | "replace" | "invert" | "increment-clamp" | "decrement-clamp" | "increment-wrap" | "decrement-wrap" */
}

fun GPUStencilFaceState(
    compare: String? = undefined,
    failOp: String? = undefined,
    depthFailOp: String? = undefined,
    passOp: String? = undefined,
): GPUStencilFaceState = js("({compare: compare, failOp: failOp, depthFailOp: depthFailOp, passOp: passOp})")

inline class GPUViewDimension(val str: String) {
    companion object {
        inline val _1D get() = GPUViewDimension("1d")
        inline val _2D get() = GPUViewDimension("2d")
        inline val _2D_ARRAY get() = GPUViewDimension("2d-array")
        inline val CUBE get() = GPUViewDimension("cube")
        inline val CUBE_ARRAY get() = GPUViewDimension("cube-array")
        inline val _3D get() = GPUViewDimension("3d")
    }
}

inline class GPUAccess(val str: String) {
    companion object {
        inline val READ_ONLY get() = GPUAccess("read-only")
        inline val WRITE_ONLY get() = GPUAccess("write-only")
        inline val READ_WRITE get() = GPUAccess("read-write")
    }
}

external interface GPUStorageTextureBindingLayout {
    var access: GPUAccess?
    var format: GPUTextureFormat
    var viewDimension: GPUViewDimension?
}

fun GPUStorageTextureBindingLayout(
    access: GPUAccess? = undefined,
    format: GPUTextureFormat,
    viewDimension: GPUViewDimension,
): GPUStorageTextureBindingLayout = js("({access: access, format: format, viewDimension: viewDimension})")


external interface GPUTextureBindingLayout {
    var sampleType: String? /* "float" | "unfilterable-float" | "depth" | "sint" | "uint" */
    var viewDimension: GPUViewDimension?
    var multisampled: Boolean?
}

fun GPUTextureBindingLayout(
    sampleType: String? = undefined,
    viewDimension: GPUViewDimension? = undefined,
    multisampled: Boolean? = undefined,
): GPUTextureBindingLayout = js("({sampleType: sampleType, viewDimension: viewDimension, multisampled: multisampled})")


external interface GPUTextureDescriptor : GPUObjectDescriptorBase {
    var size: dynamic /* Iterable<GPUIntegerCoordinate> | GPUExtent3DDictStrict */
    var mipLevelCount: GPUIntegerCoordinate?
    var sampleCount: GPUSize32?
    var dimension: String? /* "1d" | "2d" | "3d" */
    var format: GPUTextureFormat
    var usage: GPUTextureUsageFlags
    var viewFormats: Array<GPUTextureFormat>?
}

fun GPUTextureDescriptor(
    size: dynamic,
    mipLevelCount: GPUIntegerCoordinate? = undefined,
    sampleCount: GPUSize32? = undefined,
    dimension: String? = undefined,
    format: GPUTextureFormat,
    usage: GPUTextureUsageFlags,
    viewFormats: Array<GPUTextureFormat>? = undefined,
    label: String? = undefined,
): GPUTextureDescriptor = js("({size: size, mipLevelCount: mipLevelCount, sampleCount: sampleCount, dimension: dimension, format: format, usage: usage, viewFormats: viewFormats, label: label})")


external interface GPUTextureViewDescriptor : GPUObjectDescriptorBase {
    var format: GPUTextureFormat?
    var dimension: GPUViewDimension?
    var aspect: String? /* "all" | "stencil-only" | "depth-only" */
    var baseMipLevel: GPUIntegerCoordinate?
    var mipLevelCount: GPUIntegerCoordinate?
    var baseArrayLayer: GPUIntegerCoordinate?
    var arrayLayerCount: GPUIntegerCoordinate?
}

fun GPUTextureViewDescriptor(
    format: GPUTextureFormat? = undefined,
    dimension: GPUViewDimension? = undefined,
    aspect: String? = undefined,
    baseMipLevel: GPUIntegerCoordinate? = undefined,
    mipLevelCount: GPUIntegerCoordinate? = undefined,
    baseArrayLayer: GPUIntegerCoordinate? = undefined,
    arrayLayerCount: GPUIntegerCoordinate? = undefined,
    label: String? = undefined,
): GPUTextureViewDescriptor = js("({format: format, dimension: dimension, aspect: aspect, baseMipLevel: baseMipLevel, mipLevelCount: mipLevelCount, baseArrayLayer: baseArrayLayer, arrayLayerCount: arrayLayerCount, label: label})")

external interface GPUUncapturedErrorEventInit : EventInit {
    var error: GPUError
}

fun GPUUncapturedErrorEventInit(
    error: GPUError,
    bubbles: Boolean? = undefined,
    cancelable: Boolean? = undefined,
    composed: Boolean? = undefined,
): GPUUncapturedErrorEventInit = js("({error: error, bubbles: bubbles, cancelable: cancelable, composed: composed})")


external interface GPUVertexAttribute {
    var format: GPUTextureFormat
    var offset: GPUSize64
    var shaderLocation: GPUIndex32
}

fun GPUVertexAttribute(
    format: GPUTextureFormat,
    offset: GPUSize64,
    shaderLocation: GPUIndex32,
): GPUVertexAttribute = js("({format: format, offset: offset, shaderLocation: shaderLocation})")


external interface GPUVertexBufferLayout {
    var arrayStride: GPUSize64
    var stepMode: String? /* "vertex" | "instance" */

    var attributes: Array<GPUVertexAttribute>
}

fun GPUVertexBufferLayout(
    arrayStride: GPUSize64,
    stepMode: String? = undefined,
    attributes: Array<GPUVertexAttribute>,
): GPUVertexBufferLayout = js("({arrayStride: arrayStride, stepMode: stepMode, attributes: attributes})")


external interface GPUVertexState : GPUProgrammableStage {
    var buffers: Array<GPUVertexBufferLayout?>?
}

fun GPUVertexState(
    module: GPUShaderModule,
    entryPoint: String? = undefined,
    constants: Map<String, GPUPipelineConstantValue>? = undefined,
    buffers: Array<GPUVertexBufferLayout?>? = undefined
): GPUVertexState = js("({ module : module, entryPoint : entryPoint, constants : constants, buffers : buffers })")

external interface GPUBindingCommandsMixin {
    fun setBindGroup(
        index: GPUIndex32,
        bindGroup: GPUBindGroup?,
        dynamicOffsets: Array<GPUBufferDynamicOffset> = definedExternally
    )

    fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?)
    fun setBindGroup(
        index: GPUIndex32,
        bindGroup: GPUBindGroup?,
        dynamicOffsetsData: Uint32Array,
        dynamicOffsetsDataStart: GPUSize64,
        dynamicOffsetsDataLength: GPUSize32
    )
}

external interface GPUCommandsMixin

external interface GPUDebugCommandsMixin {
    fun pushDebugGroup(groupLabel: String)
    fun popDebugGroup()
    fun insertDebugMarker(markerLabel: String)
}

external interface GPUObjectBase {
    var label: String
}

external interface GPUPipelineBase {
    fun getBindGroupLayout(index: Number): GPUBindGroupLayout
}

external interface GPURenderCommandsMixin {
    fun setPipeline(pipeline: GPURenderPipeline)
    fun setIndexBuffer(buffer: GPUBuffer, indexFormat: String /* "uint16" | "uint32" */, offset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally)
    fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally)
    fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32 = definedExternally, firstVertex: GPUSize32 = definedExternally, firstInstance: GPUSize32 = definedExternally)
    fun drawIndexed(indexCount: GPUSize32, instanceCount: GPUSize32 = definedExternally, firstIndex: GPUSize32 = definedExternally, baseVertex: GPUSignedOffset32 = definedExternally, firstInstance: GPUSize32 = definedExternally)
    fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
    fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
}

external interface NavigatorGPU {
    var gpu: GPU
}

external class GPU {
    var __brand: String /* "GPU" */
    fun requestAdapter(options: GPURequestAdapterOptions = definedExternally): Promise<GPUAdapter?>
    fun getPreferredCanvasFormat(): GPUTextureFormat
    var wgslLanguageFeatures: WGSLLanguageFeatures

    companion object {
        var prototype: GPU
    }
}


external class GPUAdapter {
    var __brand: String /* "GPUAdapter" */
    var features: GPUSupportedFeatures
    var limits: GPUSupportedLimits
    var isFallbackAdapter: Boolean
    fun requestDevice(descriptor: GPUDeviceDescriptor = definedExternally): Promise<GPUDevice>
    fun requestAdapterInfo(): Promise<GPUAdapterInfo>

    companion object {
        var prototype: GPUAdapter
    }
}


external class GPUAdapterInfo {
    var __brand: String /* "GPUAdapterInfo" */
    var vendor: String
    var architecture: String
    var device: String
    var description: String

    companion object {
        var prototype: GPUAdapterInfo
    }
}


external class GPUBindGroup : GPUObjectBase {
    var __brand: String /* "GPUBindGroup" */
    override var label: String

    companion object {
        var prototype: GPUBindGroup
    }
}


external class GPUBindGroupLayout : GPUObjectBase {
    var __brand: String /* "GPUBindGroupLayout" */
    override var label: String

    companion object {
        var prototype: GPUBindGroupLayout
    }
}


external class GPUBuffer : GPUObjectBase {
    var __brand: String /* "GPUBuffer" */
    override var label: String
    var size: GPUSize64Out
    var usage: GPUFlagsConstant
    var mapState: String /* "unmapped" | "pending" | "mapped" */
    fun mapAsync(mode: GPUMapModeFlags, offset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally): Promise<Nothing?>
    fun getMappedRange(offset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally): ArrayBuffer
    fun unmap()
    fun destroy()

    companion object {
        var prototype: GPUBuffer
    }
}


external class GPUCanvasContext {
    var __brand: String /* "GPUCanvasContext" */
    var canvas: dynamic /* HTMLCanvasElement | OffscreenCanvas */

    fun configure(configuration: GPUCanvasConfiguration)
    fun unconfigure()
    fun getCurrentTexture(): GPUTexture

    companion object {
        var prototype: GPUCanvasContext
    }
}


external class GPUCommandBuffer : GPUObjectBase {
    var __brand: String /* "GPUCommandBuffer" */
    override var label: String

    companion object {
        var prototype: GPUCommandBuffer
    }
}


external class GPUCommandEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin {
    var __brand: String /* "GPUCommandEncoder" */
    override var label: String
    fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder
    fun beginComputePass(descriptor: GPUComputePassDescriptor? = definedExternally): GPUComputePassEncoder
    fun copyBufferToBuffer(source: GPUBuffer, sourceOffset: GPUSize64, destination: GPUBuffer, destinationOffset: GPUSize64, size: GPUSize64)
    fun copyBufferToTexture(
        source: GPUImageCopyBuffer,
        destination: GPUImageCopyTexture,
        copySize: Array<GPUIntegerCoordinate>
    )
    fun copyBufferToTexture(source: GPUImageCopyBuffer, destination: GPUImageCopyTexture, copySize: GPUExtent3DDictStrict)
    fun copyTextureToBuffer(source: GPUImageCopyTexture, destination: GPUImageCopyBuffer, copySize: Iterable<GPUIntegerCoordinate>)
    fun copyTextureToBuffer(source: GPUImageCopyTexture, destination: GPUImageCopyBuffer, copySize: GPUExtent3DDictStrict)
    fun copyTextureToTexture(
        source: GPUImageCopyTexture,
        destination: GPUImageCopyTexture,
        copySize: Array<GPUIntegerCoordinate>
    )
    fun copyTextureToTexture(source: GPUImageCopyTexture, destination: GPUImageCopyTexture, copySize: GPUExtent3DDictStrict)
    fun clearBuffer(buffer: GPUBuffer, offset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally)
    fun resolveQuerySet(querySet: GPUQuerySet, firstQuery: GPUSize32, queryCount: GPUSize32, destination: GPUBuffer, destinationOffset: GPUSize64)
    fun finish(descriptor: GPUCommandBufferDescriptor = definedExternally): GPUCommandBuffer

    companion object {
        var prototype: GPUCommandEncoder
    }

    override fun pushDebugGroup(groupLabel: String)
    override fun popDebugGroup()
    override fun insertDebugMarker(markerLabel: String)
}

external class GPUCompilationInfo {
    var __brand: String /* "GPUCompilationInfo" */
    var messages: Array<GPUCompilationMessage>

    companion object {
        var prototype: GPUCompilationInfo
    }
}


external class GPUCompilationMessage {
    var __brand: String /* "GPUCompilationMessage" */
    var message: String
    var type: String /* "error" | "warning" | "info" */
    var lineNum: Number
    var linePos: Number
    var offset: Number
    var length: Number

    companion object {
        var prototype: GPUCompilationMessage
    }
}


external class GPUComputePassEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin {
    var __brand: String /* "GPUComputePassEncoder" */
    override var label: String
    fun setPipeline(pipeline: GPUComputePipeline)
    fun dispatchWorkgroups(
        workgroupCountX: GPUSize32,
        workgroupCountY: GPUSize32 = definedExternally,
        workgroupCountZ: GPUSize32 = definedExternally
    )

    fun dispatchWorkgroupsIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
    fun end()

    companion object {
        var prototype: GPUComputePassEncoder
    }

    override fun pushDebugGroup(groupLabel: String)
    override fun popDebugGroup()
    override fun insertDebugMarker(markerLabel: String)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsets: Array<GPUBufferDynamicOffset>)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: Uint32Array, dynamicOffsetsDataStart: GPUSize64, dynamicOffsetsDataLength: GPUSize32)
}


external class GPUComputePipeline : GPUObjectBase, GPUPipelineBase {
    var __brand: String /* "GPUComputePipeline" */
    override var label: String
    override fun getBindGroupLayout(index: Number): GPUBindGroupLayout

    companion object {
        var prototype: GPUComputePipeline
    }
}


open external class GPUDevice : EventTarget, GPUObjectBase {

    override var label: String
    var __brand: String /* "GPUDevice" */
    var features: GPUSupportedFeatures
    var limits: GPUSupportedLimits
    var queue: GPUQueue
    fun destroy()
    fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer
    fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture
    fun createSampler(descriptor: GPUSamplerDescriptor = definedExternally): GPUSampler
    fun importExternalTexture(descriptor: GPUExternalTextureDescriptor): GPUExternalTexture
    fun createBindGroupLayout(descriptor: GPUBindGroupLayoutDescriptor): GPUBindGroupLayout
    fun createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): GPUPipelineLayout
    fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup
    fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule
    fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): GPUComputePipeline
    fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): GPURenderPipeline
    fun createComputePipelineAsync(descriptor: GPUComputePipelineDescriptor): Promise<GPUComputePipeline>
    fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Promise<GPURenderPipeline>
    fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor = definedExternally): GPUCommandEncoder
    fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): GPURenderBundleEncoder
    fun createQuerySet(descriptor: GPUQuerySetDescriptor): GPUQuerySet
    var lost: Promise<GPUDeviceLostInfo>
    fun pushErrorScope(filter: String /* "validation" | "out-of-memory" | "internal" */)
    fun popErrorScope(): Promise<GPUError?>
    var onuncapturederror: ((self: GPUDevice, ev: GPUUncapturedErrorEvent) -> Any)?

    companion object {
        var prototype: GPUDevice
    }

}


external class GPUDeviceLostInfo {
    var __brand: String /* "GPUDeviceLostInfo" */
    var reason: String /* "unknown" | "destroyed" */
    var message: String

    companion object {
        var prototype: GPUDeviceLostInfo
    }
}


open external class GPUError {
    var message: String

    companion object {
        var prototype: GPUError
    }
}


external class GPUExternalTexture : GPUObjectBase {
    var __brand: String /* "GPUExternalTexture" */
    override var label: String

    companion object {
        var prototype: GPUExternalTexture
    }
}


external class GPUInternalError : GPUError {
    var __brand: String /* "GPUInternalError" */

    companion object {
        var prototype: GPUInternalError
    }
}


external class GPUOutOfMemoryError : GPUError {
    var __brand: String /* "GPUOutOfMemoryError" */

    companion object {
        var prototype: GPUOutOfMemoryError
    }
}


open external class GPUPipelineError : DOMException {
    var __brand: String /* "GPUPipelineError" */
    var reason: String /* "validation" | "internal" */

    companion object {
        var prototype: GPUPipelineError
    }
}


external class GPUPipelineLayout : GPUObjectBase {
    var __brand: String /* "GPUPipelineLayout" */
    override var label: String

    companion object {
        var prototype: GPUPipelineLayout
    }
}


external class GPUQuerySet : GPUObjectBase {
    var __brand: String /* "GPUQuerySet" */
    override var label: String
    fun destroy()
    var type: String /* "occlusion" | "timestamp" */
    var count: GPUSize32Out

    companion object {
        var prototype: GPUQuerySet
    }
}


external class GPUQueue : GPUObjectBase {
    var __brand: String /* "GPUQueue" */
    override var label: String
    fun submit(commandBuffers: Array<GPUCommandBuffer>)
    fun onSubmittedWorkDone(): Promise<Nothing?>
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBufferView, dataOffset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally)
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBufferView)
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBufferView, dataOffset: GPUSize64 = definedExternally)
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBuffer, dataOffset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally)
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBuffer)
    /*fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBuffer, dataOffset: GPUSize64 = definedExternally)
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: SharedArrayBuffer, dataOffset: GPUSize64 = definedExternally, size: GPUSize64 = definedExternally)
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: SharedArrayBuffer)
    fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: SharedArrayBuffer, dataOffset: GPUSize64 = definedExternally)*/
    fun writeTexture(destination: GPUImageCopyTexture, data: ArrayBufferView, dataLayout: GPUImageDataLayout, size: Iterable<GPUIntegerCoordinate>)
    fun writeTexture(destination: GPUImageCopyTexture, data: ArrayBufferView, dataLayout: GPUImageDataLayout, size: GPUExtent3DDictStrict)
    fun writeTexture(destination: GPUImageCopyTexture, data: ArrayBuffer, dataLayout: GPUImageDataLayout, size: Iterable<GPUIntegerCoordinate>)
    fun writeTexture(destination: GPUImageCopyTexture, data: ArrayBuffer, dataLayout: GPUImageDataLayout, size: GPUExtent3DDictStrict)

    /*fun writeTexture(destination: GPUImageCopyTexture, data: SharedArrayBuffer, dataLayout: GPUImageDataLayout, size: Iterable<GPUIntegerCoordinate>)
    fun writeTexture(destination: GPUImageCopyTexture, data: SharedArrayBuffer, dataLayout: GPUImageDataLayout, size: GPUExtent3DDictStrict)*/
    fun copyExternalImageToTexture(
        source: GPUImageCopyExternalImage,
        destination: GPUImageCopyTextureTagged,
        copySize: Array<GPUIntegerCoordinate>
    )
    fun copyExternalImageToTexture(source: GPUImageCopyExternalImage, destination: GPUImageCopyTextureTagged, copySize: GPUExtent3DDictStrict)

    companion object {
        var prototype: GPUQueue
    }
}


external class GPURenderBundle : GPUObjectBase {
    var __brand: String /* "GPURenderBundle" */
    override var label: String

    companion object {
        var prototype: GPURenderBundle
    }
}


external class GPURenderBundleEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin, GPURenderCommandsMixin {
    var __brand: String /* "GPURenderBundleEncoder" */
    override var label: String
    fun finish(descriptor: GPURenderBundleDescriptor = definedExternally): GPURenderBundle

    companion object {
        var prototype: GPURenderBundleEncoder
    }

    override fun pushDebugGroup(groupLabel: String)
    override fun popDebugGroup()
    override fun insertDebugMarker(markerLabel: String)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsets: Array<GPUBufferDynamicOffset>)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: Uint32Array, dynamicOffsetsDataStart: GPUSize64, dynamicOffsetsDataLength: GPUSize32)
    override fun setPipeline(pipeline: GPURenderPipeline)
    override fun setIndexBuffer(buffer: GPUBuffer, indexFormat: String, offset: GPUSize64, size: GPUSize64)
    override fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64, size: GPUSize64)
    override fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32, firstVertex: GPUSize32, firstInstance: GPUSize32)
    override fun drawIndexed(indexCount: GPUSize32, instanceCount: GPUSize32, firstIndex: GPUSize32, baseVertex: GPUSignedOffset32, firstInstance: GPUSize32)
    override fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
    override fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
}


external class GPURenderPassEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin, GPURenderCommandsMixin {
    var __brand: String /* "GPURenderPassEncoder" */
    override var label: String
    fun setViewport(x: Number, y: Number, width: Number, height: Number, minDepth: Number, maxDepth: Number)
    fun setScissorRect(x: GPUIntegerCoordinate, y: GPUIntegerCoordinate, width: GPUIntegerCoordinate, height: GPUIntegerCoordinate)
    fun setBlendConstant(color: Iterable<Number>)
    fun setBlendConstant(color: GPUColorDict)
    fun setStencilReference(reference: GPUStencilValue)
    fun beginOcclusionQuery(queryIndex: GPUSize32)
    fun endOcclusionQuery()
    fun executeBundles(bundles: Iterable<GPURenderBundle>)
    fun end()

    companion object {
        var prototype: GPURenderPassEncoder
    }

    override fun pushDebugGroup(groupLabel: String)
    override fun popDebugGroup()
    override fun insertDebugMarker(markerLabel: String)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsets: Array<GPUBufferDynamicOffset>)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?)
    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: Uint32Array, dynamicOffsetsDataStart: GPUSize64, dynamicOffsetsDataLength: GPUSize32)
    override fun setPipeline(pipeline: GPURenderPipeline)
    override fun setIndexBuffer(buffer: GPUBuffer, indexFormat: String, offset: GPUSize64, size: GPUSize64)
    override fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64, size: GPUSize64)
    override fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32, firstVertex: GPUSize32, firstInstance: GPUSize32)
    override fun drawIndexed(indexCount: GPUSize32, instanceCount: GPUSize32, firstIndex: GPUSize32, baseVertex: GPUSignedOffset32, firstInstance: GPUSize32)
    override fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
    override fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
}


external class GPURenderPipeline : GPUObjectBase, GPUPipelineBase {
    var __brand: String /* "GPURenderPipeline" */
    override var label: String
    override fun getBindGroupLayout(index: Number): GPUBindGroupLayout

    companion object {
        var prototype: GPURenderPipeline
    }
}


external class GPUSampler : GPUObjectBase {
    var __brand: String /* "GPUSampler" */
    override var label: String

    companion object {
        var prototype: GPUSampler
    }
}


external class GPUShaderModule : GPUObjectBase {
    var __brand: String /* "GPUShaderModule" */
    fun getCompilationInfo(): Promise<GPUCompilationInfo>
    override var label: String

    companion object {
        var prototype: GPUShaderModule
    }
}

typealias GPUSupportedFeatures = ReadonlySet<String>


external class GPUSupportedLimits {
    var __brand: String /* "GPUSupportedLimits" */
    var maxTextureDimension1D: Number
    var maxTextureDimension2D: Number
    var maxTextureDimension3D: Number
    var maxTextureArrayLayers: Number
    var maxBindGroups: Number
    var maxBindGroupsPlusVertexBuffers: Number
    var maxBindingsPerBindGroup: Number
    var maxDynamicUniformBuffersPerPipelineLayout: Number
    var maxDynamicStorageBuffersPerPipelineLayout: Number
    var maxSampledTexturesPerShaderStage: Number
    var maxSamplersPerShaderStage: Number
    var maxStorageBuffersPerShaderStage: Number
    var maxStorageTexturesPerShaderStage: Number
    var maxUniformBuffersPerShaderStage: Number
    var maxUniformBufferBindingSize: Number
    var maxStorageBufferBindingSize: Number
    var minUniformBufferOffsetAlignment: Number
    var minStorageBufferOffsetAlignment: Number
    var maxVertexBuffers: Number
    var maxBufferSize: Number
    var maxVertexAttributes: Number
    var maxVertexBufferArrayStride: Number
    var maxInterStageShaderComponents: Number
    var maxInterStageShaderVariables: Number
    var maxColorAttachments: Number
    var maxColorAttachmentBytesPerSample: Number
    var maxComputeWorkgroupStorageSize: Number
    var maxComputeInvocationsPerWorkgroup: Number
    var maxComputeWorkgroupSizeX: Number
    var maxComputeWorkgroupSizeY: Number
    var maxComputeWorkgroupSizeZ: Number
    var maxComputeWorkgroupsPerDimension: Number

    companion object {
        var prototype: GPUSupportedLimits
    }
}


external class GPUTexture : GPUObjectBase {
    var __brand: String /* "GPUTexture" */
    override var label: String
    fun createView(descriptor: GPUTextureViewDescriptor = definedExternally): GPUTextureView
    fun destroy()
    var width: GPUIntegerCoordinateOut
    var height: GPUIntegerCoordinateOut
    var depthOrArrayLayers: GPUIntegerCoordinateOut
    var mipLevelCount: GPUIntegerCoordinateOut
    var sampleCount: GPUSize32Out
    var dimension: String /* "1d" | "2d" | "3d" */
    var format: GPUTextureFormat
    var usage: GPUFlagsConstant

    companion object {
        var prototype: GPUTexture
    }
}


external class GPUTextureView : GPUObjectBase {
    var __brand: String /* "GPUTextureView" */
    override var label: String

    companion object {
        var prototype: GPUTextureView
    }
}


open external class GPUUncapturedErrorEvent : Event {
    var __brand: String /* "GPUUncapturedErrorEvent" */
    var error: GPUError

    companion object {
        var prototype: GPUUncapturedErrorEvent
    }
}


external class GPUValidationError : GPUError {
    var __brand: String /* "GPUValidationError" */

    companion object {
        var prototype: GPUValidationError
    }
}

typealias WGSLLanguageFeatures = ReadonlySet<String>

external interface WorkerNavigator : NavigatorGPU


external class GPUBufferUsage {
    var __brand: String /* "GPUBufferUsage" */
    var MAP_READ: GPUFlagsConstant
    var MAP_WRITE: GPUFlagsConstant
    var COPY_SRC: GPUFlagsConstant
    var COPY_DST: GPUFlagsConstant
    var INDEX: GPUFlagsConstant
    var VERTEX: GPUFlagsConstant
    var UNIFORM: GPUFlagsConstant
    var STORAGE: GPUFlagsConstant
    var INDIRECT: GPUFlagsConstant
    var QUERY_RESOLVE: GPUFlagsConstant

    companion object {
        var prototype: GPUBufferUsage
        var MAP_READ: GPUFlagsConstant
        var MAP_WRITE: GPUFlagsConstant
        var COPY_SRC: GPUFlagsConstant
        var COPY_DST: GPUFlagsConstant
        var INDEX: GPUFlagsConstant
        var VERTEX: GPUFlagsConstant
        var UNIFORM: GPUFlagsConstant
        var STORAGE: GPUFlagsConstant
        var INDIRECT: GPUFlagsConstant
        var QUERY_RESOLVE: GPUFlagsConstant
    }
}


external class GPUColorWrite {
    var __brand: String /* "GPUColorWrite" */
    var RED: GPUFlagsConstant
    var GREEN: GPUFlagsConstant
    var BLUE: GPUFlagsConstant
    var ALPHA: GPUFlagsConstant
    var ALL: GPUFlagsConstant

    companion object {
        var prototype: GPUColorWrite
        var RED: GPUFlagsConstant
        var GREEN: GPUFlagsConstant
        var BLUE: GPUFlagsConstant
        var ALPHA: GPUFlagsConstant
        var ALL: GPUFlagsConstant
    }
}


external class GPUMapMode {
    var __brand: String /* "GPUMapMode" */
    var READ: GPUFlagsConstant
    var WRITE: GPUFlagsConstant

    companion object {
        var prototype: GPUMapMode
        var READ: GPUFlagsConstant
        var WRITE: GPUFlagsConstant
    }
}


external class GPUShaderStage {
    var __brand: String /* "GPUShaderStage" */
    var VERTEX: GPUFlagsConstant
    var FRAGMENT: GPUFlagsConstant
    var COMPUTE: GPUFlagsConstant

    companion object {
        var prototype: GPUShaderStage
        var VERTEX: GPUFlagsConstant
        var FRAGMENT: GPUFlagsConstant
        var COMPUTE: GPUFlagsConstant
    }
}


external class GPUTextureUsage {
    var __brand: String /* "GPUTextureUsage" */
    var COPY_SRC: GPUFlagsConstant
    var COPY_DST: GPUFlagsConstant
    var TEXTURE_BINDING: GPUFlagsConstant
    var STORAGE_BINDING: GPUFlagsConstant
    var RENDER_ATTACHMENT: GPUFlagsConstant

    companion object {
        var prototype: GPUTextureUsage
        var COPY_SRC: GPUFlagsConstant
        var COPY_DST: GPUFlagsConstant
        var TEXTURE_BINDING: GPUFlagsConstant
        var STORAGE_BINDING: GPUFlagsConstant
        var RENDER_ATTACHMENT: GPUFlagsConstant
    }
}


// EXTRA


typealias Record<K, T> = Any
external interface DOMException

external interface ReadonlyMap<K, V> {
    fun entries(): IterableIterator<dynamic /* JsTuple<K, V> */>
    fun keys(): IterableIterator<K>
    fun values(): IterableIterator<V>
    fun forEach(callbackfn: (value: V, key: K, map: ReadonlyMap<K, V>) -> Unit, thisArg: Any = definedExternally)
    fun get(key: K): V?
    fun has(key: K): Boolean
    var size: Number
}

external interface ReadonlySet<T> {
    fun entries(): IterableIterator<dynamic /* JsTuple<T, T> */>
    fun keys(): IterableIterator<T>
    fun values(): IterableIterator<T>
    fun forEach(callbackfn: (value: T, value2: T, set: ReadonlySet<T>) -> Unit, thisArg: Any = definedExternally)
    fun has(value: T): Boolean
    var size: Number
}

external interface IteratorYieldResult<TYield> {
    var done: Boolean?

    var value: TYield
}

external interface IteratorReturnResult<TReturn> {
    var done: Boolean
    var value: TReturn
}

external interface Iterator<T, TReturn, TNext> {
    fun next(vararg args: Any /* JsTuple<> | JsTuple<TNext> */): dynamic /* IteratorYieldResult<T> | IteratorReturnResult<TReturn> */
    val `return`: ((value: TReturn) -> dynamic)?
    val `throw`: ((e: Any) -> dynamic)?
}

typealias Iterator__1<T> = Iterator<T, Any, Nothing?>

external interface Iterable<T>

external interface IterableIterator<T> : Iterator__1<T>

external interface PromiseConstructor {
    var prototype: Promise<Any>
    fun all(values: Any /* JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?> | JsTuple<Any?, Any?> */): Promise<dynamic /* JsTuple<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> | JsTuple<T1, T2, T3, T4, T5, T6, T7, T8, T9> | JsTuple<T1, T2, T3, T4, T5, T6, T7, T8> | JsTuple<T1, T2, T3, T4, T5, T6, T7> | JsTuple<T1, T2, T3, T4, T5, T6> | JsTuple<T1, T2, T3, T4, T5> | JsTuple<T1, T2, T3, T4> | JsTuple<T1, T2, T3> | JsTuple<T1, T2> */>
    fun <T> all(values: Array<Any? /* T | PromiseLike<T> */>): Promise<Array<T>>
    fun <T> race(values: Array<T>): Promise<Any>
    fun <T> reject(reason: Any = definedExternally): Promise<T>
    fun <T> resolve(value: T): Promise<T>
    //fun <T> resolve(value: PromiseLike<T>): Promise<T>
    fun resolve(): Promise<Unit>
    fun <T> all(values: Iterable<Any? /* T | PromiseLike<T> */>): Promise<Array<T>>
    fun <T> race(values: Iterable<T>): Promise<Any>
    fun <T> race(values: Iterable<Any? /* T | PromiseLike<T> */>): Promise<T>
}

typealias GPUBufferDynamicOffset = Number

typealias GPUBufferUsageFlags = Int

typealias GPUColorWriteFlags = Number

typealias GPUDepthBias = Int

//typealias GPUFlagsConstant = Number
typealias GPUFlagsConstant = Int

typealias GPUIndex32 = Int

typealias GPUIntegerCoordinate = Int

typealias GPUIntegerCoordinates = Pair<GPUIntegerCoordinate, GPUIntegerCoordinate>

typealias GPUIntegerCoordinateOut = Int

typealias GPUMapModeFlags = Number

typealias GPUPipelineConstantValue = Number

typealias GPUSampleMask = UInt

typealias GPUShaderStageFlags = Number

typealias GPUSignedOffset32 = Number

typealias GPUSize32 = Int

typealias GPUSize32Out = Int

typealias GPUSize64 = Number

typealias GPUSize64Out = Number

typealias GPUStencilValue = Number

typealias GPUTextureUsageFlags = Int

@JsExport
data class Size3D(
    var width: Int,
    var height: Int = 1,
    var depthOrArrayLayers: Int = 1
) {
    fun toArray() = arrayOf(width, height, depthOrArrayLayers)
}

data class Origin3D(
    var x: GPUIntegerCoordinate = 0,
    var y: GPUIntegerCoordinate = 0,
    var z: GPUIntegerCoordinate = 0
) {
    fun toArray() = arrayOf(x, y, z)
}
