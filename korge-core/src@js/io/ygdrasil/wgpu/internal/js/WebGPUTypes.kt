// From wgpu4k. Eventually it will be used directly
// https:github.com/wgpu4k/wgpu4k/blob/main/wgpu4k/src/jsMain/kotlin/io.ygdrasil.wgpu/internal.js/webgpu_types.kt
package io.ygdrasil.wgpu.internal.js

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.js.Promise

external interface GPUOrigin2DDictStrict : GPUOrigin2DDict

external interface GPUExtent3DDictStrict : GPUExtent3DDict

external interface GPUBindGroupDescriptor : GPUObjectDescriptorBase {
    var layout: GPUBindGroupLayout
    var entries: Array<GPUBindGroupEntry>
}

external interface GPUBindGroupEntry {
    var binding: GPUIndex32
    var resource: dynamic /* GPUSampler | GPUTextureView | GPUBufferBinding | GPUExternalTexture */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUBindGroupLayoutDescriptor : GPUObjectDescriptorBase {
    var entries: Iterable<GPUBindGroupLayoutEntry>
}

external interface GPUBindGroupLayoutEntry {
    var binding: GPUIndex32
    var visibility: GPUShaderStageFlags
    var buffer: GPUBufferBindingLayout?
        get() = definedExternally
        set(value) = definedExternally
    var sampler: GPUSamplerBindingLayout?
        get() = definedExternally
        set(value) = definedExternally
    var texture: GPUTextureBindingLayout?
        get() = definedExternally
        set(value) = definedExternally
    var storageTexture: GPUStorageTextureBindingLayout?
        get() = definedExternally
        set(value) = definedExternally
    var externalTexture: GPUExternalTextureBindingLayout?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUBlendComponent {
    var operation: String? /* "add" | "subtract" | "reverse-subtract" | "min" | "max" */
        get() = definedExternally
        set(value) = definedExternally
    var srcFactor: String? /* "zero" | "one" | "src" | "one-minus-src" | "src-alpha" | "one-minus-src-alpha" | "dst" | "one-minus-dst" | "dst-alpha" | "one-minus-dst-alpha" | "src-alpha-saturated" | "constant" | "one-minus-constant" */
        get() = definedExternally
        set(value) = definedExternally
    var dstFactor: String? /* "zero" | "one" | "src" | "one-minus-src" | "src-alpha" | "one-minus-src-alpha" | "dst" | "one-minus-dst" | "dst-alpha" | "one-minus-dst-alpha" | "src-alpha-saturated" | "constant" | "one-minus-constant" */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUBlendState {
    var color: GPUBlendComponent
    var alpha: GPUBlendComponent
}

external interface GPUBufferBinding {
    var buffer: GPUBuffer
    var offset: GPUSize64?
        get() = definedExternally
        set(value) = definedExternally
    var size: GPUSize64?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUBufferBindingLayout {
    var type: String? /* "uniform" | "storage" | "read-only-storage" */
        get() = definedExternally
        set(value) = definedExternally
    var hasDynamicOffset: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var minBindingSize: GPUSize64?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUBufferDescriptor : GPUObjectDescriptorBase {
    var size: GPUSize64
    var usage: GPUBufferUsageFlags
    var mappedAtCreation: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUCanvasConfiguration {
    var device: GPUDevice
    var format: String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
    var usage: GPUTextureUsageFlags?
        get() = definedExternally
        set(value) = definedExternally
    var viewFormats: Array<String? /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */>?
        get() = definedExternally
        set(value) = definedExternally
    var colorSpace: Any?
        get() = definedExternally
        set(value) = definedExternally
    var alphaMode: String? /* "opaque" | "premultiplied" */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUColorDict {
    var r: Number
    var g: Number
    var b: Number
    var a: Number
}

external interface GPUColorTargetState {
    var format: String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
    var blend: GPUBlendState?
        get() = definedExternally
        set(value) = definedExternally
    var writeMask: GPUColorWriteFlags?
        get() = definedExternally
        set(value) = definedExternally
}

typealias GPUCommandBufferDescriptor = GPUObjectDescriptorBase

typealias GPUCommandEncoderDescriptor = GPUObjectDescriptorBase

external interface GPUComputePassDescriptor : GPUObjectDescriptorBase {
    var timestampWrites: GPUComputePassTimestampWrites?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUComputePassTimestampWrites {
    var querySet: GPUQuerySet
    var beginningOfPassWriteIndex: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
    var endOfPassWriteIndex: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUComputePipelineDescriptor : GPUPipelineDescriptorBase {
    var compute: GPUProgrammableStage
}

external interface GPUDepthStencilState {
    var format: String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
    var depthWriteEnabled: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var depthCompare: String? /* "never" | "less" | "equal" | "less-equal" | "greater" | "not-equal" | "greater-equal" | "always" */
        get() = definedExternally
        set(value) = definedExternally
    var stencilFront: GPUStencilFaceState?
        get() = definedExternally
        set(value) = definedExternally
    var stencilBack: GPUStencilFaceState?
        get() = definedExternally
        set(value) = definedExternally
    var stencilReadMask: GPUStencilValue?
        get() = definedExternally
        set(value) = definedExternally
    var stencilWriteMask: GPUStencilValue?
        get() = definedExternally
        set(value) = definedExternally
    var depthBias: GPUDepthBias?
        get() = definedExternally
        set(value) = definedExternally
    var depthBiasSlopeScale: Float?
        get() = definedExternally
        set(value) = definedExternally
    var depthBiasClamp: Float?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUDeviceDescriptor : GPUObjectDescriptorBase {
    var requiredFeatures: Iterable<String? /* "depth-clip-control" | "depth32float-stencil8" | "texture-compression-bc" | "texture-compression-etc2" | "texture-compression-astc" | "timestamp-query" | "indirect-first-instance" | "shader-f16" | "rg11b10ufloat-renderable" | "bgra8unorm-storage" | "float32-filterable" */>?
        get() = definedExternally
        set(value) = definedExternally
    var requiredLimits: Record<String, GPUSize64>?
        get() = definedExternally
        set(value) = definedExternally
    var defaultQueue: GPUQueueDescriptor?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUExtent3DDict {
    var width: GPUIntegerCoordinate
    var height: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var depthOrArrayLayers: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUExternalTextureBindingLayout

external interface GPUExternalTextureDescriptor : GPUObjectDescriptorBase {
    var source: dynamic /* HTMLVideoElement | VideoFrame */
        get() = definedExternally
        set(value) = definedExternally
    var colorSpace: Any?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUFragmentState : GPUProgrammableStage {
    var targets: Array<GPUColorTargetState?>
}

external interface GPUImageCopyBuffer : GPUImageDataLayout {
    var buffer: GPUBuffer
}

external interface GPUImageCopyExternalImage {
    var source: Any

    /* ImageBitmap | ImageData | HTMLImageElement | HTMLVideoElement | VideoFrame | HTMLCanvasElement | OffscreenCanvas */
    var origin: GPUOrigin2DDictStrict /* Iterable<GPUIntegerCoordinate>? | GPUOrigin2DDictStrict? */
    var flipY: Boolean
}

external interface GPUImageCopyTexture {
    var texture: GPUTexture
    var mipLevel: GPUIntegerCoordinate
    var origin: Array<GPUIntegerCoordinate>
    var aspect: String
}

external interface GPUImageCopyTextureTagged : GPUImageCopyTexture {
    var colorSpace: String
    var premultipliedAlpha: Boolean
}

external interface GPUImageDataLayout {
    var offset: GPUSize64?
        get() = definedExternally
        set(value) = definedExternally
    var bytesPerRow: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
    var rowsPerImage: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUMultisampleState {
    var count: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
    @Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")
    var mask: GPUSampleMask?
        get() = definedExternally
        set(value) = definedExternally
    var alphaToCoverageEnabled: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUObjectDescriptorBase {
    var label: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUOrigin2DDict {
    var x: GPUIntegerCoordinate
    var y: GPUIntegerCoordinate
}

external interface GPUOrigin3DDict {
    var x: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var y: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var z: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUPipelineDescriptorBase : GPUObjectDescriptorBase {
    var layout: dynamic /* GPUPipelineLayout | "auto" */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUPipelineErrorInit {
    var reason: String /* "validation" | "internal" */
}

external interface GPUPipelineLayoutDescriptor : GPUObjectDescriptorBase {
    var bindGroupLayouts: Array<GPUBindGroupLayout>
}

external interface GPUPrimitiveState {
    var topology: String? /* "point-list" | "line-list" | "line-strip" | "triangle-list" | "triangle-strip" */
        get() = definedExternally
        set(value) = definedExternally
    var stripIndexFormat: String? /* "uint16" | "uint32" */
        get() = definedExternally
        set(value) = definedExternally
    var frontFace: String? /* "ccw" | "cw" */
        get() = definedExternally
        set(value) = definedExternally
    var cullMode: String? /* "none" | "front" | "back" */
        get() = definedExternally
        set(value) = definedExternally
    var unclippedDepth: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUProgrammableStage {
    var module: GPUShaderModule
    var entryPoint: String?
        get() = definedExternally
        set(value) = definedExternally
    var constants: Map<String, GPUPipelineConstantValue>?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUQuerySetDescriptor : GPUObjectDescriptorBase {
    var type: String /* "occlusion" | "timestamp" */
    var count: GPUSize32
}

typealias GPUQueueDescriptor = GPUObjectDescriptorBase

typealias GPURenderBundleDescriptor = GPUObjectDescriptorBase

external interface GPURenderBundleEncoderDescriptor : GPURenderPassLayout {
    var depthReadOnly: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var stencilReadOnly: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPURenderPassColorAttachment {
    var view: GPUTextureView
    var depthSlice: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var resolveTarget: GPUTextureView?
        get() = definedExternally
        set(value) = definedExternally
    var clearValue: Array<Number>? /* Iterable<Number>? | GPUColorDict? */
        get() = definedExternally
        set(value) = definedExternally
    var loadOp: String /* "load" | "clear" */
    var storeOp: String /* "store" | "discard" */
}

external interface GPURenderPassDepthStencilAttachment {
    var view: GPUTextureView
    var depthClearValue: Number?
        get() = definedExternally
        set(value) = definedExternally
    var depthLoadOp: String? /* "load" | "clear" */
        get() = definedExternally
        set(value) = definedExternally
    var depthStoreOp: String? /* "store" | "discard" */
        get() = definedExternally
        set(value) = definedExternally
    var depthReadOnly: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var stencilClearValue: GPUStencilValue?
        get() = definedExternally
        set(value) = definedExternally
    var stencilLoadOp: String? /* "load" | "clear" */
        get() = definedExternally
        set(value) = definedExternally
    var stencilStoreOp: String? /* "store" | "discard" */
        get() = definedExternally
        set(value) = definedExternally
    var stencilReadOnly: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPURenderPassDescriptor : GPUObjectDescriptorBase {
    var colorAttachments: Array<GPURenderPassColorAttachment>
    var depthStencilAttachment: GPURenderPassDepthStencilAttachment?
        get() = definedExternally
        set(value) = definedExternally
    var occlusionQuerySet: GPUQuerySet?
        get() = definedExternally
        set(value) = definedExternally
    var timestampWrites: GPURenderPassTimestampWrites?
        get() = definedExternally
        set(value) = definedExternally
    var maxDrawCount: GPUSize64?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPURenderPassLayout : GPUObjectDescriptorBase {
    var colorFormats: Iterable<String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */>
    var depthStencilFormat: String? /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
        get() = definedExternally
        set(value) = definedExternally
    var sampleCount: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPURenderPassTimestampWrites {
    var querySet: GPUQuerySet
    var beginningOfPassWriteIndex: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
    var endOfPassWriteIndex: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPURenderPipelineDescriptor : GPUPipelineDescriptorBase {
    var vertex: GPUVertexState
    var primitive: GPUPrimitiveState?
        get() = definedExternally
        set(value) = definedExternally
    var depthStencil: GPUDepthStencilState?
        get() = definedExternally
        set(value) = definedExternally
    var multisample: GPUMultisampleState?
        get() = definedExternally
        set(value) = definedExternally
    var fragment: GPUFragmentState?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPURequestAdapterOptions {
    var powerPreference: String? /* "low-power" | "high-performance" */
        get() = definedExternally
        set(value) = definedExternally
    var forceFallbackAdapter: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUSamplerBindingLayout {
    var type: String? /* "filtering" | "non-filtering" | "comparison" */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUSamplerDescriptor : GPUObjectDescriptorBase {
    var addressModeU: String? /* "clamp-to-edge" | "repeat" | "mirror-repeat" */
        get() = definedExternally
        set(value) = definedExternally
    var addressModeV: String? /* "clamp-to-edge" | "repeat" | "mirror-repeat" */
        get() = definedExternally
        set(value) = definedExternally
    var addressModeW: String? /* "clamp-to-edge" | "repeat" | "mirror-repeat" */
        get() = definedExternally
        set(value) = definedExternally
    var magFilter: String? /* "nearest" | "linear" */
        get() = definedExternally
        set(value) = definedExternally
    var minFilter: String? /* "nearest" | "linear" */
        get() = definedExternally
        set(value) = definedExternally
    var mipmapFilter: String? /* "nearest" | "linear" */
        get() = definedExternally
        set(value) = definedExternally
    var lodMinClamp: Number?
        get() = definedExternally
        set(value) = definedExternally
    var lodMaxClamp: Number?
        get() = definedExternally
        set(value) = definedExternally
    var compare: String? /* "never" | "less" | "equal" | "less-equal" | "greater" | "not-equal" | "greater-equal" | "always" */
        get() = definedExternally
        set(value) = definedExternally
    var maxAnisotropy: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUShaderModuleCompilationHint {
    var entryPoint: String
    var layout: dynamic /* GPUPipelineLayout? | "auto" */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUShaderModuleDescriptor : GPUObjectDescriptorBase {
    var code: String
    var sourceMap: Any?
        get() = definedExternally
        set(value) = definedExternally
    var compilationHints: Array<GPUShaderModuleCompilationHint>?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUStencilFaceState {
    var compare: String? /* "never" | "less" | "equal" | "less-equal" | "greater" | "not-equal" | "greater-equal" | "always" */
        get() = definedExternally
        set(value) = definedExternally
    var failOp: String? /* "keep" | "zero" | "replace" | "invert" | "increment-clamp" | "decrement-clamp" | "increment-wrap" | "decrement-wrap" */
        get() = definedExternally
        set(value) = definedExternally
    var depthFailOp: String? /* "keep" | "zero" | "replace" | "invert" | "increment-clamp" | "decrement-clamp" | "increment-wrap" | "decrement-wrap" */
        get() = definedExternally
        set(value) = definedExternally
    var passOp: String? /* "keep" | "zero" | "replace" | "invert" | "increment-clamp" | "decrement-clamp" | "increment-wrap" | "decrement-wrap" */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUStorageTextureBindingLayout {
    var access: String? /* "write-only" | "read-only" | "read-write" */
        get() = definedExternally
        set(value) = definedExternally
    var format: String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
    var viewDimension: String? /* "1d" | "2d" | "2d-array" | "cube" | "cube-array" | "3d" */
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUTextureBindingLayout {
    var sampleType: String? /* "float" | "unfilterable-float" | "depth" | "sint" | "uint" */
        get() = definedExternally
        set(value) = definedExternally
    var viewDimension: String? /* "1d" | "2d" | "2d-array" | "cube" | "cube-array" | "3d" */
        get() = definedExternally
        set(value) = definedExternally
    var multisampled: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUTextureDescriptor : GPUObjectDescriptorBase {
    var size: dynamic /* Iterable<GPUIntegerCoordinate> | GPUExtent3DDictStrict */
        get() = definedExternally
        set(value) = definedExternally
    var mipLevelCount: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var sampleCount: GPUSize32?
        get() = definedExternally
        set(value) = definedExternally
    var dimension: String? /* "1d" | "2d" | "3d" */
        get() = definedExternally
        set(value) = definedExternally
    var format: String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
    var usage: GPUTextureUsageFlags
    abstract var viewFormats: Array<String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */>
}

external interface GPUTextureViewDescriptor : GPUObjectDescriptorBase {
    var format: String? /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
        get() = definedExternally
        set(value) = definedExternally
    var dimension: String? /* "1d" | "2d" | "2d-array" | "cube" | "cube-array" | "3d" */
        get() = definedExternally
        set(value) = definedExternally
    var aspect: String? /* "all" | "stencil-only" | "depth-only" */
        get() = definedExternally
        set(value) = definedExternally
    var baseMipLevel: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var mipLevelCount: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var baseArrayLayer: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
    var arrayLayerCount: GPUIntegerCoordinate?
        get() = definedExternally
        set(value) = definedExternally
}

external interface GPUUncapturedErrorEventInit : EventInit {
    var error: GPUError
}

external interface GPUVertexAttribute {
    var format: String

    /* "uint8x2" | "uint8x4" | "sint8x2" | "sint8x4" | "unorm8x2" | "unorm8x4" | "snorm8x2" | "snorm8x4" | "uint16x2" | "uint16x4" | "sint16x2" | "sint16x4" | "unorm16x2" | "unorm16x4" | "snorm16x2" | "snorm16x4" | "float16x2" | "float16x4" | "float32" | "float32x2" | "float32x3" | "float32x4" | "uint32" | "uint32x2" | "uint32x3" | "uint32x4" | "sint32" | "sint32x2" | "sint32x3" | "sint32x4" | "unorm10-10-10-2" */
    var offset: GPUSize64
    var shaderLocation: GPUIndex32
}

external interface GPUVertexBufferLayout {
    var arrayStride: GPUSize64
    var stepMode: String? /* "vertex" | "instance" */
        get() = definedExternally
        set(value) = definedExternally
    var attributes: Array<GPUVertexAttribute>
}

external interface GPUVertexState : GPUProgrammableStage {
    var buffers: Array<GPUVertexBufferLayout?>?
        get() = definedExternally
        set(value) = definedExternally
}

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
    fun getPreferredCanvasFormat(): String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
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
        get() = definedExternally
        set(value) = definedExternally
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
    var format: String /* "r8unorm" | "r8snorm" | "r8uint" | "r8sint" | "r16uint" | "r16sint" | "r16float" | "rg8unorm" | "rg8snorm" | "rg8uint" | "rg8sint" | "r32uint" | "r32sint" | "r32float" | "rg16uint" | "rg16sint" | "rg16float" | "rgba8unorm" | "rgba8unorm-srgb" | "rgba8snorm" | "rgba8uint" | "rgba8sint" | "bgra8unorm" | "bgra8unorm-srgb" | "rgb9e5ufloat" | "rgb10a2uint" | "rgb10a2unorm" | "rg11b10ufloat" | "rg32uint" | "rg32sint" | "rg32float" | "rgba16uint" | "rgba16sint" | "rgba16float" | "rgba32uint" | "rgba32sint" | "rgba32float" | "stencil8" | "depth16unorm" | "depth24plus" | "depth24plus-stencil8" | "depth32float" | "depth32float-stencil8" | "bc1-rgba-unorm" | "bc1-rgba-unorm-srgb" | "bc2-rgba-unorm" | "bc2-rgba-unorm-srgb" | "bc3-rgba-unorm" | "bc3-rgba-unorm-srgb" | "bc4-r-unorm" | "bc4-r-snorm" | "bc5-rg-unorm" | "bc5-rg-snorm" | "bc6h-rgb-ufloat" | "bc6h-rgb-float" | "bc7-rgba-unorm" | "bc7-rgba-unorm-srgb" | "etc2-rgb8unorm" | "etc2-rgb8unorm-srgb" | "etc2-rgb8a1unorm" | "etc2-rgb8a1unorm-srgb" | "etc2-rgba8unorm" | "etc2-rgba8unorm-srgb" | "eac-r11unorm" | "eac-r11snorm" | "eac-rg11unorm" | "eac-rg11snorm" | "astc-4x4-unorm" | "astc-4x4-unorm-srgb" | "astc-5x4-unorm" | "astc-5x4-unorm-srgb" | "astc-5x5-unorm" | "astc-5x5-unorm-srgb" | "astc-6x5-unorm" | "astc-6x5-unorm-srgb" | "astc-6x6-unorm" | "astc-6x6-unorm-srgb" | "astc-8x5-unorm" | "astc-8x5-unorm-srgb" | "astc-8x6-unorm" | "astc-8x6-unorm-srgb" | "astc-8x8-unorm" | "astc-8x8-unorm-srgb" | "astc-10x5-unorm" | "astc-10x5-unorm-srgb" | "astc-10x6-unorm" | "astc-10x6-unorm-srgb" | "astc-10x8-unorm" | "astc-10x8-unorm-srgb" | "astc-10x10-unorm" | "astc-10x10-unorm-srgb" | "astc-12x10-unorm" | "astc-12x10-unorm-srgb" | "astc-12x12-unorm" | "astc-12x12-unorm-srgb" */
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
        get() = definedExternally
        set(value) = definedExternally
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

typealias GPUSize64 = Long

typealias GPUSize64Out = Long

typealias GPUStencilValue = Long

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
