package korlibs.webgpu

import korlibs.ffi.*

/*
class WGPUAdapterInfo {
    var nextInChain: WGPUChainedStructOut
    var vendor: String
    var architecture: String
    var device: String
    var description: String
    var backendType: WGPUBackendType
    var adapterType: WGPUAdapterType
    var vendorID: uint32_t
    var deviceID: uint32_t
}

class WGPUBindGroupEntry {
    var nextInChain: WGPUChainedStruct
    var binding: uint32_t
    var buffer: WGPUBuffer?
    var offset: uint64_t
    var size: uint64_t
    var sampler: WGPUSampler?
    var textureView: WGPUTextureView?
}

class WGPUBlendComponent {
    lateinit var operation: WGPUBlendOperation
    lateinit var srcFactor: WGPUBlendFactor
    lateinit var dstFactor: WGPUBlendFactor
}

class WGPUBufferBindingLayout {
    var nextInChain: WGPUChainedStruct
    var type: WGPUBufferBindingType
    var hasDynamicOffset: WGPUBool
    var minBindingSize: uint64_t
}

class WGPUBufferDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var usage: WGPUBufferUsageFlags
    var size: uint64_t
    var mappedAtCreation: WGPUBool
}

class WGPUColor {
    var r: Double
    var g: Double
    var b: Double
    var a: Double
}

class WGPUCommandBufferDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
}

class WGPUCommandEncoderDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
}

class WGPUCompilationMessage {
    var nextInChain: WGPUChainedStruct
    var message: String?
    var type: WGPUCompilationMessageType
    var lineNum: uint64_t
    var linePos: uint64_t
    var offset: uint64_t
    var length: uint64_t
    var utf16LinePos: uint64_t
    var utf16Offset: uint64_t
    var utf16Length: uint64_t
}

class WGPUComputePassTimestampWrites {
    var querySet: WGPUQuerySet
    var beginningOfPassWriteIndex: uint32_t
    var endOfPassWriteIndex: uint32_t
}

class WGPUConstantEntry {
    var nextInChain: WGPUChainedStruct
    var key: String
    var value: Double
}

class WGPUExtent3D {
    var width: uint32_t
    var height: uint32_t
    var depthOrArrayLayers: uint32_t
}

class WGPUInstanceDescriptor {
    var nextInChain: WGPUChainedStruct
}

class WGPULimits {
    var maxTextureDimension1D: uint32_t
    var maxTextureDimension2D: uint32_t
    var maxTextureDimension3D: uint32_t
    var maxTextureArrayLayers: uint32_t
    var maxBindGroups: uint32_t
    var maxBindGroupsPlusVertexBuffers: uint32_t
    var maxBindingsPerBindGroup: uint32_t
    var maxDynamicUniformBuffersPerPipelineLayout: uint32_t
    var maxDynamicStorageBuffersPerPipelineLayout: uint32_t
    var maxSampledTexturesPerShaderStage: uint32_t
    var maxSamplersPerShaderStage: uint32_t
    var maxStorageBuffersPerShaderStage: uint32_t
    var maxStorageTexturesPerShaderStage: uint32_t
    var maxUniformBuffersPerShaderStage: uint32_t
    var maxUniformBufferBindingSize: uint64_t
    var maxStorageBufferBindingSize: uint64_t
    var minUniformBufferOffsetAlignment: uint32_t
    var minStorageBufferOffsetAlignment: uint32_t
    var maxVertexBuffers: uint32_t
    var maxBufferSize: uint64_t
    var maxVertexAttributes: uint32_t
    var maxVertexBufferArrayStride: uint32_t
    var maxInterStageShaderComponents: uint32_t
    var maxInterStageShaderVariables: uint32_t
    var maxColorAttachments: uint32_t
    var maxColorAttachmentBytesPerSample: uint32_t
    var maxComputeWorkgroupStorageSize: uint32_t
    var maxComputeInvocationsPerWorkgroup: uint32_t
    var maxComputeWorkgroupSizeX: uint32_t
    var maxComputeWorkgroupSizeY: uint32_t
    var maxComputeWorkgroupSizeZ: uint32_t
    var maxComputeWorkgroupsPerDimension: uint32_t
}

class WGPUMultisampleState {
    var nextInChain: WGPUChainedStruct
    var count: uint32_t
    var mask: uint32_t
    var alphaToCoverageEnabled: WGPUBool
}

class WGPUOrigin3D {
    var x: uint32_t = 0
    var y: uint32_t = 0
    var z: uint32_t = 0
}

class WGPUPipelineLayoutDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var bindGroupLayoutCount: size_t
    var bindGroupLayouts: WGPUBindGroupLayout
}

class WGPUPrimitiveDepthClipControl {
    var chain: WGPUChainedStruct
    var unclippedDepth: WGPUBool
}

class WGPUPrimitiveState {
    var nextInChain: WGPUChainedStruct
    var topology: WGPUPrimitiveTopology
    var stripIndexFormat: WGPUIndexFormat
    var frontFace: WGPUFrontFace
    var cullMode: WGPUCullMode
}

class WGPUQuerySetDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var type: WGPUQueryType
    var count: uint32_t
}

class WGPUQueueDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
}

class WGPURenderBundleDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
}

class WGPURenderBundleEncoderDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var colorFormatCount: size_t
    var colorFormats: FFITypedPointer<WGPUTextureFormat>
    var depthStencilFormat: WGPUTextureFormat
    var sampleCount: uint32_t
    var depthReadOnly: WGPUBool
    var stencilReadOnly: WGPUBool
}

class WGPURenderPassDepthStencilAttachment {
    var view: WGPUTextureView
    var depthLoadOp: WGPULoadOp
    var depthStoreOp: WGPUStoreOp
    var depthClearValue: Float
    var depthReadOnly: WGPUBool
    var stencilLoadOp: WGPULoadOp
    var stencilStoreOp: WGPUStoreOp
    var stencilClearValue: uint32_t
    var stencilReadOnly: WGPUBool
}

class WGPURenderPassDescriptorMaxDrawCount {
    var chain: WGPUChainedStruct
    var maxDrawCount: uint64_t
}

class WGPURenderPassTimestampWrites {
    var querySet: WGPUQuerySet
    var beginningOfPassWriteIndex: uint32_t
    var endOfPassWriteIndex: uint32_t
}

class WGPURequestAdapterOptions {
    var nextInChain: WGPUChainedStruct
    var compatibleSurface: WGPUSurface?
    var powerPreference: WGPUPowerPreference
    var backendType: WGPUBackendType
    var forceFallbackAdapter: WGPUBool
}

class WGPUSamplerBindingLayout {
    var nextInChain: WGPUChainedStruct
    var type: WGPUSamplerBindingType
}

class WGPUSamplerDescriptor : FFIStructure() {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var addressModeU: WGPUAddressMode
    var addressModeV: WGPUAddressMode
    var addressModeW: WGPUAddressMode
    var magFilter: WGPUFilterMode
    var minFilter: WGPUFilterMode
    var mipmapFilter: WGPUMipmapFilterMode
    var lodMinClamp: float
    var lodMaxClamp: float
    var compare: WGPUCompareFunction
    var maxAnisotropy: uint16_t
}

class WGPUShaderModuleCompilationHint {
    var nextInChain: WGPUChainedStruct
    var entryPoint: String
    var layout: WGPUPipelineLayout
}

class WGPUShaderModuleSPIRVDescriptor {
    var chain: WGPUChainedStruct
    var codeSize: uint32_t
    var code: IntArray
}

class WGPUShaderModuleWGSLDescriptor {
    var chain: WGPUChainedStruct
    var code: String
}

class WGPUStencilFaceState {
    var compare: WGPUCompareFunction
    var failOp: WGPUStencilOperation
    var depthFailOp: WGPUStencilOperation
    var passOp: WGPUStencilOperation
}

class WGPUStorageTextureBindingLayout {
    var nextInChain: WGPUChainedStruct
    var access: WGPUStorageTextureAccess
    var format: WGPUTextureFormat
    var viewDimension: WGPUTextureViewDimension
}

class WGPUSurfaceCapabilities {
    var nextInChain: WGPUChainedStructOut
    var usages: WGPUTextureUsageFlags
    var formatCount: size_t
    WGPUTextureFormat const * formats;
    var presentModeCount: size_t
    WGPUPresentMode const * presentModes;
    var alphaModeCount: size_t
    WGPUCompositeAlphaMode const * alphaModes;
}

class WGPUSurfaceConfiguration {
    var nextInChain: WGPUChainedStruct
    var device: WGPUDevice
    var format: WGPUTextureFormat
    var usage: WGPUTextureUsageFlags
    var viewFormatCount: size_t
    WGPUTextureFormat const * viewFormats;
    var alphaMode: WGPUCompositeAlphaMode
    var width: uint32_t
    var height: uint32_t
    var presentMode: WGPUPresentMode
}

class WGPUSurfaceDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
}

class WGPUSurfaceDescriptorFromAndroidNativeWindow {
    var chain: WGPUChainedStruct
    var window: FFIPointer?
}

class WGPUSurfaceDescriptorFromCanvasHTMLSelector {
    var chain: WGPUChainedStruct
    var selector: String
}

class WGPUSurfaceDescriptorFromMetalLayer {
    var chain: WGPUChainedStruct
    var layer: FFIPointer
}

class WGPUSurfaceDescriptorFromWaylandSurface {
    var chain: WGPUChainedStruct
    var display: FFIPointer
    var surface: FFIPointer
}

class WGPUSurfaceDescriptorFromWindowsHWND {
    chain: WGPUChainedStruct
    var hinstance: FFIPointer
    var hwnd: FFIPointer
}

class WGPUSurfaceDescriptorFromXcbWindow {
    var chain: WGPUChainedStruct
    var connection: VoidPointer
    var window: uint32_t
}

class WGPUSurfaceDescriptorFromXlibWindow {
    var chain: WGPUChainedStruct
    var display: VoidPointer
    var window: uint64_t
}

class WGPUSurfaceTexture {
    var texture: WGPUTexture
    var suboptimal: WGPUBool
    var status: WGPUSurfaceGetCurrentTextureStatus
}

class WGPUTextureBindingLayout {
    var nextInChain: WGPUChainedStruct
    var sampleType: WGPUTextureSampleType
    var viewDimension: WGPUTextureViewDimension
    var multisampled: WGPUBool
}

class WGPUTextureDataLayout {
    var nextInChain: WGPUChainedStruct
    var offset: uint64_t
    var bytesPerRow: uint32_t
    var rowsPerImage: uint32_t
}

class WGPUTextureViewDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var format: WGPUTextureFormat
    var dimension: WGPUTextureViewDimension
    var baseMipLevel: uint32_t
    var mipLevelCount: uint32_t
    var baseArrayLayer: uint32_t
    var arrayLayerCount: uint32_t
    var aspect: WGPUTextureAspect
}

class WGPUUncapturedErrorCallbackInfo {
    var nextInChain: WGPUChainedStruct
    var callback: WGPUErrorCallback
    var userdata: VoidPointer
}

class WGPUVertexAttribute {
    var format: WGPUVertexFormat
    var offset: uint64_t
    var shaderLocation: uint32_t
}

class WGPUBindGroupDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var layout: WGPUBindGroupLayout
    var entryCount: size_t
    var entries: FFITypedPointer<WGPUBindGroupEntry>
}

class WGPUBindGroupLayoutEntry {
    var nextInChain: WGPUChainedStruct
    var binding: uint32_t
    var visibility: WGPUShaderStageFlags
    var buffer: WGPUBufferBindingLayout
    var sampler: WGPUSamplerBindingLayout
    var texture: WGPUTextureBindingLayout
    var storageTexture: WGPUStorageTextureBindingLayout
}

class WGPUBlendState {
    var color: WGPUBlendComponent
    var alpha: WGPUBlendComponent
}

class WGPUCompilationInfo {
    var nextInChain: WGPUChainedStruct
    var messageCount: size_t
    var messages: FFITypedPointer<WGPUCompilationMessage>
}

class WGPUComputePassDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var timestampWrites: WGPUComputePassTimestampWrites?
}

class WGPUDepthStencilState {
    var nextInChain: WGPUChainedStruct
    var format: WGPUTextureFormat
    var depthWriteEnabled: WGPUBool
    var depthCompare: WGPUCompareFunction
    var stencilFront: WGPUStencilFaceState
    var stencilBack: WGPUStencilFaceState
    var stencilReadMask: uint32_t
    var stencilWriteMask: uint32_t
    var depthBias: int32_t
    var depthBiasSlopeScale: float
    var depthBiasClamp: float
}

class WGPUImageCopyBuffer {
    var nextInChain: WGPUChainedStruct
    var layout: WGPUTextureDataLayout
    var buffer: WGPUBuffer
}

class WGPUImageCopyTexture {
    var nextInChain: WGPUChainedStruct
    var texture: WGPUTexture
    var mipLevel: uint32_t
    var origin: WGPUOrigin3D
    var aspect: WGPUTextureAspect
}

class WGPUProgrammableStageDescriptor {
    var nextInChain: WGPUChainedStruct
    var module: WGPUShaderModule
    var entryPoint: String?
    var constantCount: size_t
    var constants: FFITypedPointer<WGPUConstantEntry>
}

class WGPURenderPassColorAttachment {
    var nextInChain: WGPUChainedStruct
    var view: WGPUTextureView?
    var depthSlice: uint32_t
    var resolveTarget: WGPUTextureView?
    var loadOp: WGPULoadOp
    var storeOp: WGPUStoreOp
    var clearValue: WGPUColor
}

class WGPURequiredLimits {
    var nextInChain: WGPUChainedStruct
    var limits: WGPULimits
}

class WGPUShaderModuleDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var hintCount: size_t
    var hints: FFITypedPointer<WGPUShaderModuleCompilationHint>
}

class WGPUSupportedLimits {
    var nextInChain: WGPUChainedStructOut
    var limits: WGPULimits
}

class WGPUTextureDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var usage: WGPUTextureUsageFlags
    var dimension: WGPUTextureDimension
    var size: WGPUExtent3D
    var format: WGPUTextureFormat
    var mipLevelCount: uint32_t
    var sampleCount: uint32_t
    var viewFormatCount: size_t
    var viewFormats: FFITypedPointer<WGPUTextureFormat>
}

class WGPUVertexBufferLayout {
    var arrayStride: uint64_t
    var stepMode: WGPUVertexStepMode
    var attributeCount: size_t
    var attributes: FFITypedPointer<WGPUVertexAttribute>
}

class WGPUBindGroupLayoutDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var entryCount: size_t
    var entries: FFITypedPointer<WGPUBindGroupLayoutEntry>
}

class WGPUColorTargetState {
    var nextInChain: WGPUChainedStruct
    var format: WGPUTextureFormat
    var blend: WGPUBlendState?
    var writeMask: WGPUColorWriteMaskFlags
}

class WGPUComputePipelineDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var layout: WGPUPipelineLayout?
    var compute: WGPUProgrammableStageDescriptor
}

class WGPUDeviceDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var requiredFeatureCount: size_t
    var requiredFeatures: FFITypedPointer<WGPUFeatureName>
    var requiredLimits: FFITypedPointer<WGPURequiredLimits>
    var defaultQueue: WGPUQueueDescriptor
    var deviceLostCallback: WGPUDeviceLostCallback
    var deviceLostUserdata: FFIPointer
    var uncapturedErrorCallbackInfo: WGPUUncapturedErrorCallbackInfo
}

class WGPURenderPassDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var colorAttachmentCount: size_t
    var colorAttachments: FFITypedPointer<WGPURenderPassColorAttachment>
    var depthStencilAttachment: WGPURenderPassDepthStencilAttachment?
    var occlusionQuerySet: WGPUQuerySet?
    var timestampWrites: WGPURenderPassTimestampWrites?
}

class WGPUVertexState {
    var nextInChain: WGPUChainedStruct
    var module: WGPUShaderModule
    var entryPoint: String?
    var constantCount: size_t
    var constants: FFITypedPointer<WGPUConstantEntry>
    var bufferCount: size_t
    var buffers: FFITypedPointer<WGPUVertexBufferLayout>
}

class WGPUFragmentState {
    var nextInChain: WGPUChainedStruct
    var module: WGPUShaderModule
    var entryPoint: String?
    var constantCount: size_t
    var constants: FFITypedPointer<WGPUConstantEntry>
    var targetCount: size_t
    var targets: WGPUColorTargetState
}

class WGPURenderPipelineDescriptor {
    var nextInChain: WGPUChainedStruct
    var label: String?
    var layout: WGPUPipelineLayout?
    var vertex: WGPUVertexState
    var primitive: WGPUPrimitiveState
    var depthStencil: WGPUDepthStencilState?
    var multisample: WGPUMultisampleState
    var fragment: WGPUFragmentState?
}
*/
