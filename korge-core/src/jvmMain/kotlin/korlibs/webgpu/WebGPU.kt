package korlibs.webgpu

import korlibs.ffi.*

typealias float = Float
typealias size_t = Int
typealias int16_t = Short
typealias uint16_t = Short
typealias int32_t = Int
typealias uint32_t = Int
typealias WGPUBool = Boolean
typealias uint64_t = Long

typealias VoidPointer = FFIPointer
typealias IntPointer = FFIPointer

typealias WGPUChainedStruct = FFIPointer
typealias WGPUChainedStructOut = FFIPointer

typealias WGPUDevice = FFIPointer
typealias WGPUInstance = FFIPointer
typealias WGPUAdapter = FFIPointer
typealias WGPUBindGroup = FFIPointer
typealias WGPUPipelineLayout = FFIPointer
typealias WGPUTexture = FFIPointer
typealias WGPUTextureView = FFIPointer
typealias WGPUSurface = FFIPointer
typealias WGPUShaderModule = FFIPointer
typealias WGPUSampler = FFIPointer
typealias WGPURenderPipeline = FFIPointer
typealias WGPUBindGroupLayout = FFIPointer
typealias WGPURenderPassEncoder = FFIPointer
typealias WGPURenderBundleEncoder = FFIPointer
typealias WGPUBuffer = FFIPointer
typealias WGPURenderBundle = FFIPointer
typealias WGPUComputePassEncoder = FFIPointer
typealias WGPUComputePipeline = FFIPointer
typealias WGPUCommandEncoder = FFIPointer
typealias WGPUQuerySet = FFIPointer
typealias WGPUCommandBuffer = FFIPointer
typealias WGPUQueue = FFIPointer
typealias WGPUProc = FFIPointer

typealias WGPUErrorCallback = FFIPointer
typealias WGPUShaderModuleGetCompilationInfoCallback = FFIPointer
typealias WGPUQueueOnSubmittedWorkDoneCallback = FFIPointer
typealias WGPUDeviceLostCallback = FFIPointer
typealias WGPUAdapterRequestDeviceCallback = FFIPointer
typealias WGPUBufferMapAsyncCallback = FFIPointer
typealias WGPUDeviceCreateComputePipelineAsyncCallback = FFIPointer
typealias WGPUDeviceCreateRenderPipelineAsyncCallback = FFIPointer
typealias WGPUInstanceRequestAdapterCallback = FFIPointer

//inline class WGPUAdapterType(val value: Int)
//inline class WGPUAddressMode(val value: Int)
//inline class WGPUBackendType(val value: Int)
//inline class WGPUBlendFactor(val value: Int)
//inline class WGPUBlendOperation(val value: Int)
//inline class WGPUBufferBindingType(val value: Int)
//inline class WGPUBufferMapAsyncStatus(val value: Int)
//inline class WGPUBufferMapState(val value: Int)
//inline class WGPUCompareFunction(val value: Int)
//inline class WGPUCompilationInfoRequestStatus(val value: Int)
//inline class WGPUCompilationMessageType(val value: Int)
//inline class WGPUCompositeAlphaMode(val value: Int)
//inline class WGPUCreatePipelineAsyncStatus(val value: Int)
//inline class WGPUCullMode(val value: Int)
//inline class WGPUDeviceLostReason(val value: Int)
//inline class WGPUErrorFilter(val value: Int)
//inline class WGPUErrorType(val value: Int)
//inline class WGPUFeatureName(val value: Int)
//inline class WGPUFilterMode(val value: Int)
//inline class WGPUFrontFace(val value: Int)
//inline class WGPUIndexFormat(val value: Int)
//inline class WGPULoadOp(val value: Int)
//inline class WGPUMipmapFilterMode(val value: Int)
//inline class WGPUPowerPreference(val value: Int)
//inline class WGPUPresentMode(val value: Int)
//inline class WGPUPrimitiveTopology(val value: Int)
//inline class WGPUQueryType(val value: Int)
//inline class WGPUQueueWorkDoneStatus(val value: Int)
//inline class WGPURequestAdapterStatus(val value: Int)
//inline class WGPURequestDeviceStatus(val value: Int)
//inline class WGPUSType(val value: Int)
//inline class WGPUSamplerBindingType(val value: Int)
//inline class WGPUStencilOperation(val value: Int)
//inline class WGPUStorageTextureAccess(val value: Int)
//inline class WGPUStoreOp(val value: Int)
//inline class WGPUSurfaceGetCurrentTextureStatus(val value: Int)
//inline class WGPUTextureAspect(val value: Int)
//inline class WGPUTextureDimension(val value: Int)
//inline class WGPUTextureFormat(val value: Int)
//inline class WGPUTextureSampleType(val value: Int)
//inline class WGPUTextureViewDimension(val value: Int)
//inline class WGPUVertexFormat(val value: Int)
//inline class WGPUVertexStepMode(val value: Int)
//inline class WGPUWGSLFeatureName(val value: Int)
//inline class WGPUBufferUsage(val value: Int)
//inline class WGPUColorWriteMask(val value: Int)
//inline class WGPUMapMode(val value: Int)
//inline class WGPUShaderStage(val value: Int)
//inline class WGPUTextureUsage(val value: Int)

typealias WGPUAdapterType =  Int
typealias WGPUAddressMode =  Int
typealias WGPUBackendType =  Int
typealias WGPUBlendFactor =  Int
typealias WGPUBlendOperation =  Int
typealias WGPUBufferBindingType =  Int
typealias WGPUBufferMapAsyncStatus =  Int
typealias WGPUBufferMapState =  Int
typealias WGPUCompareFunction =  Int
typealias WGPUCompilationInfoRequestStatus =  Int
typealias WGPUCompilationMessageType =  Int
typealias WGPUCompositeAlphaMode =  Int
typealias WGPUCreatePipelineAsyncStatus =  Int
typealias WGPUCullMode =  Int
typealias WGPUDeviceLostReason =  Int
typealias WGPUErrorFilter =  Int
typealias WGPUErrorType =  Int
typealias WGPUFeatureName =  Int
typealias WGPUFilterMode =  Int
typealias WGPUFrontFace =  Int
typealias WGPUIndexFormat =  Int
typealias WGPULoadOp =  Int
typealias WGPUMipmapFilterMode =  Int
typealias WGPUPowerPreference =  Int
typealias WGPUPresentMode =  Int
typealias WGPUPrimitiveTopology =  Int
typealias WGPUQueryType =  Int
typealias WGPUQueueWorkDoneStatus =  Int
typealias WGPURequestAdapterStatus =  Int
typealias WGPURequestDeviceStatus =  Int
typealias WGPUSType =  Int
typealias WGPUSamplerBindingType =  Int
typealias WGPUStencilOperation =  Int
typealias WGPUStorageTextureAccess =  Int
typealias WGPUStoreOp =  Int
typealias WGPUSurfaceGetCurrentTextureStatus =  Int
typealias WGPUTextureAspect =  Int
typealias WGPUTextureDimension =  Int
typealias WGPUTextureFormat =  Int
typealias WGPUTextureSampleType =  Int
typealias WGPUTextureViewDimension =  Int
typealias WGPUVertexFormat =  Int
typealias WGPUVertexStepMode =  Int
typealias WGPUWGSLFeatureName =  Int
typealias WGPUBufferUsage =  Int
typealias WGPUColorWriteMask =  Int
typealias WGPUMapMode =  Int
typealias WGPUShaderStage =  Int
typealias WGPUTextureUsage =  Int

typealias WGPUBufferUsageFlags = WGPUBufferUsage
typealias WGPUTextureUsageFlags = WGPUTextureUsage
typealias WGPUMapModeFlags = WGPUMapMode


// https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.h
object WebGPU {
    // Constants
    const val WGPUAdapterType_DiscreteGPU = 0x00000000
    const val WGPUAdapterType_IntegratedGPU = 0x00000001
    const val WGPUAdapterType_CPU = 0x00000002
    const val WGPUAdapterType_Unknown = 0x00000003
    const val WGPUAdapterType_Force32 = 0x7FFFFFFF

    const val WGPUAddressMode_Repeat = 0x00000000
    const val WGPUAddressMode_MirrorRepeat = 0x00000001
    const val WGPUAddressMode_ClampToEdge = 0x00000002
    const val WGPUAddressMode_Force32 = 0x7FFFFFFF

    const val WGPUBackendType_Undefined = 0x00000000
    const val WGPUBackendType_Null = 0x00000001
    const val WGPUBackendType_WebGPU = 0x00000002
    const val WGPUBackendType_D3D11 = 0x00000003
    const val WGPUBackendType_D3D12 = 0x00000004
    const val WGPUBackendType_Metal = 0x00000005
    const val WGPUBackendType_Vulkan = 0x00000006
    const val WGPUBackendType_OpenGL = 0x00000007
    const val WGPUBackendType_OpenGLES = 0x00000008
    const val WGPUBackendType_Force32 = 0x7FFFFFFF

    const val WGPUBlendFactor_Zero = 0x00000000
    const val WGPUBlendFactor_One = 0x00000001
    const val WGPUBlendFactor_Src = 0x00000002
    const val WGPUBlendFactor_OneMinusSrc = 0x00000003
    const val WGPUBlendFactor_SrcAlpha = 0x00000004
    const val WGPUBlendFactor_OneMinusSrcAlpha = 0x00000005
    const val WGPUBlendFactor_Dst = 0x00000006
    const val WGPUBlendFactor_OneMinusDst = 0x00000007
    const val WGPUBlendFactor_DstAlpha = 0x00000008
    const val WGPUBlendFactor_OneMinusDstAlpha = 0x00000009
    const val WGPUBlendFactor_SrcAlphaSaturated = 0x0000000A
    const val WGPUBlendFactor_Constant = 0x0000000B
    const val WGPUBlendFactor_OneMinusConstant = 0x0000000C
    const val WGPUBlendFactor_Force32 = 0x7FFFFFFF

    const val WGPUBlendOperation_Add = 0x00000000
    const val WGPUBlendOperation_Subtract = 0x00000001
    const val WGPUBlendOperation_ReverseSubtract = 0x00000002
    const val WGPUBlendOperation_Min = 0x00000003
    const val WGPUBlendOperation_Max = 0x00000004
    const val WGPUBlendOperation_Force32 = 0x7FFFFFFF

    const val WGPUBufferBindingType_Undefined = 0x00000000
    const val WGPUBufferBindingType_Uniform = 0x00000001
    const val WGPUBufferBindingType_Storage = 0x00000002
    const val WGPUBufferBindingType_ReadOnlyStorage = 0x00000003
    const val WGPUBufferBindingType_Force32 = 0x7FFFFFFF

    const val WGPUBufferMapAsyncStatus_Success = 0x00000000
    const val WGPUBufferMapAsyncStatus_ValidationError = 0x00000001
    const val WGPUBufferMapAsyncStatus_Unknown = 0x00000002
    const val WGPUBufferMapAsyncStatus_DeviceLost = 0x00000003
    const val WGPUBufferMapAsyncStatus_DestroyedBeforeCallback = 0x00000004
    const val WGPUBufferMapAsyncStatus_UnmappedBeforeCallback = 0x00000005
    const val WGPUBufferMapAsyncStatus_MappingAlreadyPending = 0x00000006
    const val WGPUBufferMapAsyncStatus_OffsetOutOfRange = 0x00000007
    const val WGPUBufferMapAsyncStatus_SizeOutOfRange = 0x00000008
    const val WGPUBufferMapAsyncStatus_Force32 = 0x7FFFFFFF

    const val WGPUBufferMapState_Unmapped = 0x00000000
    const val WGPUBufferMapState_Pending = 0x00000001
    const val WGPUBufferMapState_Mapped = 0x00000002
    const val WGPUBufferMapState_Force32 = 0x7FFFFFFF

    const val WGPUCompareFunction_Undefined = 0x00000000
    const val WGPUCompareFunction_Never = 0x00000001
    const val WGPUCompareFunction_Less = 0x00000002
    const val WGPUCompareFunction_LessEqual = 0x00000003
    const val WGPUCompareFunction_Greater = 0x00000004
    const val WGPUCompareFunction_GreaterEqual = 0x00000005
    const val WGPUCompareFunction_Equal = 0x00000006
    const val WGPUCompareFunction_NotEqual = 0x00000007
    const val WGPUCompareFunction_Always = 0x00000008
    const val WGPUCompareFunction_Force32 = 0x7FFFFFFF

    const val WGPUCompilationInfoRequestStatus_Success = 0x00000000
    const val WGPUCompilationInfoRequestStatus_Error = 0x00000001
    const val WGPUCompilationInfoRequestStatus_DeviceLost = 0x00000002
    const val WGPUCompilationInfoRequestStatus_Unknown = 0x00000003
    const val WGPUCompilationInfoRequestStatus_Force32 = 0x7FFFFFFF

    const val WGPUCompilationMessageType_Error = 0x00000000
    const val WGPUCompilationMessageType_Warning = 0x00000001
    const val WGPUCompilationMessageType_Info = 0x00000002
    const val WGPUCompilationMessageType_Force32 = 0x7FFFFFFF

    const val WGPUCompositeAlphaMode_Auto = 0x00000000
    const val WGPUCompositeAlphaMode_Opaque = 0x00000001
    const val WGPUCompositeAlphaMode_Premultiplied = 0x00000002
    const val WGPUCompositeAlphaMode_Unpremultiplied = 0x00000003
    const val WGPUCompositeAlphaMode_Inherit = 0x00000004
    const val WGPUCompositeAlphaMode_Force32 = 0x7FFFFFFF

    const val WGPUCreatePipelineAsyncStatus_Success = 0x00000000
    const val WGPUCreatePipelineAsyncStatus_ValidationError = 0x00000001
    const val WGPUCreatePipelineAsyncStatus_InternalError = 0x00000002
    const val WGPUCreatePipelineAsyncStatus_DeviceLost = 0x00000003
    const val WGPUCreatePipelineAsyncStatus_DeviceDestroyed = 0x00000004
    const val WGPUCreatePipelineAsyncStatus_Unknown = 0x00000005
    const val WGPUCreatePipelineAsyncStatus_Force32 = 0x7FFFFFFF

    const val WGPUCullMode_None = 0x00000000
    const val WGPUCullMode_Front = 0x00000001
    const val WGPUCullMode_Back = 0x00000002
    const val WGPUCullMode_Force32 = 0x7FFFFFFF

    const val WGPUDeviceLostReason_Unknown = 0x00000001
    const val WGPUDeviceLostReason_Destroyed = 0x00000002
    const val WGPUDeviceLostReason_Force32 = 0x7FFFFFFF

    const val WGPUErrorFilter_Validation = 0x00000000
    const val WGPUErrorFilter_OutOfMemory = 0x00000001
    const val WGPUErrorFilter_Internal = 0x00000002
    const val WGPUErrorFilter_Force32 = 0x7FFFFFFF

    const val WGPUErrorType_NoError = 0x00000000
    const val WGPUErrorType_Validation = 0x00000001
    const val WGPUErrorType_OutOfMemory = 0x00000002
    const val WGPUErrorType_Internal = 0x00000003
    const val WGPUErrorType_Unknown = 0x00000004
    const val WGPUErrorType_DeviceLost = 0x00000005
    const val WGPUErrorType_Force32 = 0x7FFFFFFF

    const val WGPUFeatureName_Undefined = 0x00000000
    const val WGPUFeatureName_DepthClipControl = 0x00000001
    const val WGPUFeatureName_Depth32FloatStencil8 = 0x00000002
    const val WGPUFeatureName_TimestampQuery = 0x00000003
    const val WGPUFeatureName_TextureCompressionBC = 0x00000004
    const val WGPUFeatureName_TextureCompressionETC2 = 0x00000005
    const val WGPUFeatureName_TextureCompressionASTC = 0x00000006
    const val WGPUFeatureName_IndirectFirstInstance = 0x00000007
    const val WGPUFeatureName_ShaderF16 = 0x00000008
    const val WGPUFeatureName_RG11B10UfloatRenderable = 0x00000009
    const val WGPUFeatureName_BGRA8UnormStorage = 0x0000000A
    const val WGPUFeatureName_Float32Filterable = 0x0000000B
    const val WGPUFeatureName_Force32 = 0x7FFFFFFF

    const val WGPUFilterMode_Nearest = 0x00000000
    const val WGPUFilterMode_Linear = 0x00000001
    const val WGPUFilterMode_Force32 = 0x7FFFFFFF

    const val WGPUFrontFace_CCW = 0x00000000
    const val WGPUFrontFace_CW = 0x00000001
    const val WGPUFrontFace_Force32 = 0x7FFFFFFF

    const val WGPUIndexFormat_Undefined = 0x00000000
    const val WGPUIndexFormat_Uint16 = 0x00000001
    const val WGPUIndexFormat_Uint32 = 0x00000002
    const val WGPUIndexFormat_Force32 = 0x7FFFFFFF

    const val WGPULoadOp_Undefined = 0x00000000
    const val WGPULoadOp_Clear = 0x00000001
    const val WGPULoadOp_Load = 0x00000002
    const val WGPULoadOp_Force32 = 0x7FFFFFFF

    const val WGPUMipmapFilterMode_Nearest = 0x00000000
    const val WGPUMipmapFilterMode_Linear = 0x00000001
    const val WGPUMipmapFilterMode_Force32 = 0x7FFFFFFF

    const val WGPUPowerPreference_Undefined = 0x00000000
    const val WGPUPowerPreference_LowPower = 0x00000001
    const val WGPUPowerPreference_HighPerformance = 0x00000002
    const val WGPUPowerPreference_Force32 = 0x7FFFFFFF

    const val WGPUPresentMode_Fifo = 0x00000000
    const val WGPUPresentMode_FifoRelaxed = 0x00000001
    const val WGPUPresentMode_Immediate = 0x00000002
    const val WGPUPresentMode_Mailbox = 0x00000003
    const val WGPUPresentMode_Force32 = 0x7FFFFFFF

    const val WGPUPrimitiveTopology_PointList = 0x00000000
    const val WGPUPrimitiveTopology_LineList = 0x00000001
    const val WGPUPrimitiveTopology_LineStrip = 0x00000002
    const val WGPUPrimitiveTopology_TriangleList = 0x00000003
    const val WGPUPrimitiveTopology_TriangleStrip = 0x00000004
    const val WGPUPrimitiveTopology_Force32 = 0x7FFFFFFF

    const val WGPUQueryType_Occlusion = 0x00000000
    const val WGPUQueryType_Timestamp = 0x00000001
    const val WGPUQueryType_Force32 = 0x7FFFFFFF

    const val WGPUQueueWorkDoneStatus_Success = 0x00000000
    const val WGPUQueueWorkDoneStatus_Error = 0x00000001
    const val WGPUQueueWorkDoneStatus_Unknown = 0x00000002
    const val WGPUQueueWorkDoneStatus_DeviceLost = 0x00000003
    const val WGPUQueueWorkDoneStatus_Force32 = 0x7FFFFFFF

    const val WGPURequestAdapterStatus_Success = 0x00000000
    const val WGPURequestAdapterStatus_Unavailable = 0x00000001
    const val WGPURequestAdapterStatus_Error = 0x00000002
    const val WGPURequestAdapterStatus_Unknown = 0x00000003
    const val WGPURequestAdapterStatus_Force32 = 0x7FFFFFFF

    const val WGPURequestDeviceStatus_Success = 0x00000000
    const val WGPURequestDeviceStatus_Error = 0x00000001
    const val WGPURequestDeviceStatus_Unknown = 0x00000002
    const val WGPURequestDeviceStatus_Force32 = 0x7FFFFFFF

    const val WGPUSType_Invalid = 0x00000000
    const val WGPUSType_SurfaceDescriptorFromMetalLayer = 0x00000001
    const val WGPUSType_SurfaceDescriptorFromWindowsHWND = 0x00000002
    const val WGPUSType_SurfaceDescriptorFromXlibWindow = 0x00000003
    const val WGPUSType_SurfaceDescriptorFromCanvasHTMLSelector = 0x00000004
    const val WGPUSType_ShaderModuleSPIRVDescriptor = 0x00000005
    const val WGPUSType_ShaderModuleWGSLDescriptor = 0x00000006
    const val WGPUSType_PrimitiveDepthClipControl = 0x00000007
    const val WGPUSType_SurfaceDescriptorFromWaylandSurface = 0x00000008
    const val WGPUSType_SurfaceDescriptorFromAndroidNativeWindow = 0x00000009
    const val WGPUSType_SurfaceDescriptorFromXcbWindow = 0x0000000A
    const val WGPUSType_RenderPassDescriptorMaxDrawCount = 0x0000000F
    const val WGPUSType_Force32 = 0x7FFFFFFF

    const val WGPUSamplerBindingType_Undefined = 0x00000000
    const val WGPUSamplerBindingType_Filtering = 0x00000001
    const val WGPUSamplerBindingType_NonFiltering = 0x00000002
    const val WGPUSamplerBindingType_Comparison = 0x00000003
    const val WGPUSamplerBindingType_Force32 = 0x7FFFFFFF

    const val WGPUStencilOperation_Keep = 0x00000000
    const val WGPUStencilOperation_Zero = 0x00000001
    const val WGPUStencilOperation_Replace = 0x00000002
    const val WGPUStencilOperation_Invert = 0x00000003
    const val WGPUStencilOperation_IncrementClamp = 0x00000004
    const val WGPUStencilOperation_DecrementClamp = 0x00000005
    const val WGPUStencilOperation_IncrementWrap = 0x00000006
    const val WGPUStencilOperation_DecrementWrap = 0x00000007
    const val WGPUStencilOperation_Force32 = 0x7FFFFFFF

    const val WGPUStorageTextureAccess_Undefined = 0x00000000
    const val WGPUStorageTextureAccess_WriteOnly = 0x00000001
    const val WGPUStorageTextureAccess_ReadOnly = 0x00000002
    const val WGPUStorageTextureAccess_ReadWrite = 0x00000003
    const val WGPUStorageTextureAccess_Force32 = 0x7FFFFFFF

    const val WGPUStoreOp_Undefined = 0x00000000
    const val WGPUStoreOp_Store = 0x00000001
    const val WGPUStoreOp_Discard = 0x00000002
    const val WGPUStoreOp_Force32 = 0x7FFFFFFF

    const val WGPUSurfaceGetCurrentTextureStatus_Success = 0x00000000
    const val WGPUSurfaceGetCurrentTextureStatus_Timeout = 0x00000001
    const val WGPUSurfaceGetCurrentTextureStatus_Outdated = 0x00000002
    const val WGPUSurfaceGetCurrentTextureStatus_Lost = 0x00000003
    const val WGPUSurfaceGetCurrentTextureStatus_OutOfMemory = 0x00000004
    const val WGPUSurfaceGetCurrentTextureStatus_DeviceLost = 0x00000005
    const val WGPUSurfaceGetCurrentTextureStatus_Force32 = 0x7FFFFFFF

    const val WGPUTextureAspect_All = 0x00000000
    const val WGPUTextureAspect_StencilOnly = 0x00000001
    const val WGPUTextureAspect_DepthOnly = 0x00000002
    const val WGPUTextureAspect_Force32 = 0x7FFFFFFF

    const val WGPUTextureDimension_1D = 0x00000000
    const val WGPUTextureDimension_2D = 0x00000001
    const val WGPUTextureDimension_3D = 0x00000002
    const val WGPUTextureDimension_Force32 = 0x7FFFFFFF

    const val WGPUTextureFormat_Undefined = 0x00000000
    const val WGPUTextureFormat_R8Unorm = 0x00000001
    const val WGPUTextureFormat_R8Snorm = 0x00000002
    const val WGPUTextureFormat_R8Uint = 0x00000003
    const val WGPUTextureFormat_R8Sint = 0x00000004
    const val WGPUTextureFormat_R16Uint = 0x00000005
    const val WGPUTextureFormat_R16Sint = 0x00000006
    const val WGPUTextureFormat_R16Float = 0x00000007
    const val WGPUTextureFormat_RG8Unorm = 0x00000008
    const val WGPUTextureFormat_RG8Snorm = 0x00000009
    const val WGPUTextureFormat_RG8Uint = 0x0000000A
    const val WGPUTextureFormat_RG8Sint = 0x0000000B
    const val WGPUTextureFormat_R32Float = 0x0000000C
    const val WGPUTextureFormat_R32Uint = 0x0000000D
    const val WGPUTextureFormat_R32Sint = 0x0000000E
    const val WGPUTextureFormat_RG16Uint = 0x0000000F
    const val WGPUTextureFormat_RG16Sint = 0x00000010
    const val WGPUTextureFormat_RG16Float = 0x00000011
    const val WGPUTextureFormat_RGBA8Unorm = 0x00000012
    const val WGPUTextureFormat_RGBA8UnormSrgb = 0x00000013
    const val WGPUTextureFormat_RGBA8Snorm = 0x00000014
    const val WGPUTextureFormat_RGBA8Uint = 0x00000015
    const val WGPUTextureFormat_RGBA8Sint = 0x00000016
    const val WGPUTextureFormat_BGRA8Unorm = 0x00000017
    const val WGPUTextureFormat_BGRA8UnormSrgb = 0x00000018
    const val WGPUTextureFormat_RGB10A2Uint = 0x00000019
    const val WGPUTextureFormat_RGB10A2Unorm = 0x0000001A
    const val WGPUTextureFormat_RG11B10Ufloat = 0x0000001B
    const val WGPUTextureFormat_RGB9E5Ufloat = 0x0000001C
    const val WGPUTextureFormat_RG32Float = 0x0000001D
    const val WGPUTextureFormat_RG32Uint = 0x0000001E
    const val WGPUTextureFormat_RG32Sint = 0x0000001F
    const val WGPUTextureFormat_RGBA16Uint = 0x00000020
    const val WGPUTextureFormat_RGBA16Sint = 0x00000021
    const val WGPUTextureFormat_RGBA16Float = 0x00000022
    const val WGPUTextureFormat_RGBA32Float = 0x00000023
    const val WGPUTextureFormat_RGBA32Uint = 0x00000024
    const val WGPUTextureFormat_RGBA32Sint = 0x00000025
    const val WGPUTextureFormat_Stencil8 = 0x00000026
    const val WGPUTextureFormat_Depth16Unorm = 0x00000027
    const val WGPUTextureFormat_Depth24Plus = 0x00000028
    const val WGPUTextureFormat_Depth24PlusStencil8 = 0x00000029
    const val WGPUTextureFormat_Depth32Float = 0x0000002A
    const val WGPUTextureFormat_Depth32FloatStencil8 = 0x0000002B
    const val WGPUTextureFormat_BC1RGBAUnorm = 0x0000002C
    const val WGPUTextureFormat_BC1RGBAUnormSrgb = 0x0000002D
    const val WGPUTextureFormat_BC2RGBAUnorm = 0x0000002E
    const val WGPUTextureFormat_BC2RGBAUnormSrgb = 0x0000002F
    const val WGPUTextureFormat_BC3RGBAUnorm = 0x00000030
    const val WGPUTextureFormat_BC3RGBAUnormSrgb = 0x00000031
    const val WGPUTextureFormat_BC4RUnorm = 0x00000032
    const val WGPUTextureFormat_BC4RSnorm = 0x00000033
    const val WGPUTextureFormat_BC5RGUnorm = 0x00000034
    const val WGPUTextureFormat_BC5RGSnorm = 0x00000035
    const val WGPUTextureFormat_BC6HRGBUfloat = 0x00000036
    const val WGPUTextureFormat_BC6HRGBFloat = 0x00000037
    const val WGPUTextureFormat_BC7RGBAUnorm = 0x00000038
    const val WGPUTextureFormat_BC7RGBAUnormSrgb = 0x00000039
    const val WGPUTextureFormat_ETC2RGB8Unorm = 0x0000003A
    const val WGPUTextureFormat_ETC2RGB8UnormSrgb = 0x0000003B
    const val WGPUTextureFormat_ETC2RGB8A1Unorm = 0x0000003C
    const val WGPUTextureFormat_ETC2RGB8A1UnormSrgb = 0x0000003D
    const val WGPUTextureFormat_ETC2RGBA8Unorm = 0x0000003E
    const val WGPUTextureFormat_ETC2RGBA8UnormSrgb = 0x0000003F
    const val WGPUTextureFormat_EACR11Unorm = 0x00000040
    const val WGPUTextureFormat_EACR11Snorm = 0x00000041
    const val WGPUTextureFormat_EACRG11Unorm = 0x00000042
    const val WGPUTextureFormat_EACRG11Snorm = 0x00000043
    const val WGPUTextureFormat_ASTC4x4Unorm = 0x00000044
    const val WGPUTextureFormat_ASTC4x4UnormSrgb = 0x00000045
    const val WGPUTextureFormat_ASTC5x4Unorm = 0x00000046
    const val WGPUTextureFormat_ASTC5x4UnormSrgb = 0x00000047
    const val WGPUTextureFormat_ASTC5x5Unorm = 0x00000048
    const val WGPUTextureFormat_ASTC5x5UnormSrgb = 0x00000049
    const val WGPUTextureFormat_ASTC6x5Unorm = 0x0000004A
    const val WGPUTextureFormat_ASTC6x5UnormSrgb = 0x0000004B
    const val WGPUTextureFormat_ASTC6x6Unorm = 0x0000004C
    const val WGPUTextureFormat_ASTC6x6UnormSrgb = 0x0000004D
    const val WGPUTextureFormat_ASTC8x5Unorm = 0x0000004E
    const val WGPUTextureFormat_ASTC8x5UnormSrgb = 0x0000004F
    const val WGPUTextureFormat_ASTC8x6Unorm = 0x00000050
    const val WGPUTextureFormat_ASTC8x6UnormSrgb = 0x00000051
    const val WGPUTextureFormat_ASTC8x8Unorm = 0x00000052
    const val WGPUTextureFormat_ASTC8x8UnormSrgb = 0x00000053
    const val WGPUTextureFormat_ASTC10x5Unorm = 0x00000054
    const val WGPUTextureFormat_ASTC10x5UnormSrgb = 0x00000055
    const val WGPUTextureFormat_ASTC10x6Unorm = 0x00000056
    const val WGPUTextureFormat_ASTC10x6UnormSrgb = 0x00000057
    const val WGPUTextureFormat_ASTC10x8Unorm = 0x00000058
    const val WGPUTextureFormat_ASTC10x8UnormSrgb = 0x00000059
    const val WGPUTextureFormat_ASTC10x10Unorm = 0x0000005A
    const val WGPUTextureFormat_ASTC10x10UnormSrgb = 0x0000005B
    const val WGPUTextureFormat_ASTC12x10Unorm = 0x0000005C
    const val WGPUTextureFormat_ASTC12x10UnormSrgb = 0x0000005D
    const val WGPUTextureFormat_ASTC12x12Unorm = 0x0000005E
    const val WGPUTextureFormat_ASTC12x12UnormSrgb = 0x0000005F
    const val WGPUTextureFormat_Force32 = 0x7FFFFFFF

    const val WGPUTextureSampleType_Undefined = 0x00000000
    const val WGPUTextureSampleType_Float = 0x00000001
    const val WGPUTextureSampleType_UnfilterableFloat = 0x00000002
    const val WGPUTextureSampleType_Depth = 0x00000003
    const val WGPUTextureSampleType_Sint = 0x00000004
    const val WGPUTextureSampleType_Uint = 0x00000005
    const val WGPUTextureSampleType_Force32 = 0x7FFFFFFF

    const val WGPUTextureViewDimension_Undefined = 0x00000000
    const val WGPUTextureViewDimension_1D = 0x00000001
    const val WGPUTextureViewDimension_2D = 0x00000002
    const val WGPUTextureViewDimension_2DArray = 0x00000003
    const val WGPUTextureViewDimension_Cube = 0x00000004
    const val WGPUTextureViewDimension_CubeArray = 0x00000005
    const val WGPUTextureViewDimension_3D = 0x00000006
    const val WGPUTextureViewDimension_Force32 = 0x7FFFFFFF

    const val WGPUVertexFormat_Undefined = 0x00000000
    const val WGPUVertexFormat_Uint8x2 = 0x00000001
    const val WGPUVertexFormat_Uint8x4 = 0x00000002
    const val WGPUVertexFormat_Sint8x2 = 0x00000003
    const val WGPUVertexFormat_Sint8x4 = 0x00000004
    const val WGPUVertexFormat_Unorm8x2 = 0x00000005
    const val WGPUVertexFormat_Unorm8x4 = 0x00000006
    const val WGPUVertexFormat_Snorm8x2 = 0x00000007
    const val WGPUVertexFormat_Snorm8x4 = 0x00000008
    const val WGPUVertexFormat_Uint16x2 = 0x00000009
    const val WGPUVertexFormat_Uint16x4 = 0x0000000A
    const val WGPUVertexFormat_Sint16x2 = 0x0000000B
    const val WGPUVertexFormat_Sint16x4 = 0x0000000C
    const val WGPUVertexFormat_Unorm16x2 = 0x0000000D
    const val WGPUVertexFormat_Unorm16x4 = 0x0000000E
    const val WGPUVertexFormat_Snorm16x2 = 0x0000000F
    const val WGPUVertexFormat_Snorm16x4 = 0x00000010
    const val WGPUVertexFormat_Float16x2 = 0x00000011
    const val WGPUVertexFormat_Float16x4 = 0x00000012
    const val WGPUVertexFormat_Float32 = 0x00000013
    const val WGPUVertexFormat_Float32x2 = 0x00000014
    const val WGPUVertexFormat_Float32x3 = 0x00000015
    const val WGPUVertexFormat_Float32x4 = 0x00000016
    const val WGPUVertexFormat_Uint32 = 0x00000017
    const val WGPUVertexFormat_Uint32x2 = 0x00000018
    const val WGPUVertexFormat_Uint32x3 = 0x00000019
    const val WGPUVertexFormat_Uint32x4 = 0x0000001A
    const val WGPUVertexFormat_Sint32 = 0x0000001B
    const val WGPUVertexFormat_Sint32x2 = 0x0000001C
    const val WGPUVertexFormat_Sint32x3 = 0x0000001D
    const val WGPUVertexFormat_Sint32x4 = 0x0000001E
    const val WGPUVertexFormat_Force32 = 0x7FFFFFFF

    const val WGPUVertexStepMode_Vertex = 0x00000000
    const val WGPUVertexStepMode_Instance = 0x00000001
    const val WGPUVertexStepMode_VertexBufferNotUsed = 0x00000002
    const val WGPUVertexStepMode_Force32 = 0x7FFFFFFF

    const val WGPUWGSLFeatureName_Undefined = 0x00000000
    const val WGPUWGSLFeatureName_ReadonlyAndReadwriteStorageTextures = 0x00000001
    const val WGPUWGSLFeatureName_Packed4x8IntegerDotProduct = 0x00000002
    const val WGPUWGSLFeatureName_UnrestrictedPointerParameters = 0x00000003
    const val WGPUWGSLFeatureName_PointerCompositeAccess = 0x00000004
    const val WGPUWGSLFeatureName_Force32 = 0x7FFFFFFF

    const val WGPUBufferUsage_None = 0x00000000
    const val WGPUBufferUsage_MapRead = 0x00000001
    const val WGPUBufferUsage_MapWrite = 0x00000002
    const val WGPUBufferUsage_CopySrc = 0x00000004
    const val WGPUBufferUsage_CopyDst = 0x00000008
    const val WGPUBufferUsage_Index = 0x00000010
    const val WGPUBufferUsage_Vertex = 0x00000020
    const val WGPUBufferUsage_Uniform = 0x00000040
    const val WGPUBufferUsage_Storage = 0x00000080
    const val WGPUBufferUsage_Indirect = 0x00000100
    const val WGPUBufferUsage_QueryResolve = 0x00000200
    const val WGPUBufferUsage_Force32 = 0x7FFFFFFF

    const val WGPUColorWriteMask_None = 0x00000000
    const val WGPUColorWriteMask_Red = 0x00000001
    const val WGPUColorWriteMask_Green = 0x00000002
    const val WGPUColorWriteMask_Blue = 0x00000004
    const val WGPUColorWriteMask_Alpha = 0x00000008
    const val WGPUColorWriteMask_All = WGPUColorWriteMask_None or WGPUColorWriteMask_Red or WGPUColorWriteMask_Green or WGPUColorWriteMask_Blue or WGPUColorWriteMask_Alpha
    const val WGPUColorWriteMask_Force32 = 0x7FFFFFFF

    const val WGPUMapMode_None = 0x00000000
    const val WGPUMapMode_Read = 0x00000001
    const val WGPUMapMode_Write = 0x00000002
    const val WGPUMapMode_Force32 = 0x7FFFFFFF

    const val WGPUShaderStage_None = 0x00000000
    const val WGPUShaderStage_Vertex = 0x00000001
    const val WGPUShaderStage_Fragment = 0x00000002
    const val WGPUShaderStage_Compute = 0x00000004
    const val WGPUShaderStage_Force32 = 0x7FFFFFFF

    const val WGPUTextureUsage_None = 0x00000000
    const val WGPUTextureUsage_CopySrc = 0x00000001
    const val WGPUTextureUsage_CopyDst = 0x00000002
    const val WGPUTextureUsage_TextureBinding = 0x00000004
    const val WGPUTextureUsage_StorageBinding = 0x00000008
    const val WGPUTextureUsage_RenderAttachment = 0x00000010
    const val WGPUTextureUsage_Force32 = 0x7FFFFFFF

    // Functions
    external fun wgpuCreateInstance(descriptor: WGPUInstanceDescriptor): WGPUInstance
    external fun wgpuGetProcAddress(device: WGPUDevice, procName: String): WGPUProc

    external fun wgpuAdapterEnumerateFeatures(adapter: WGPUAdapter, features: WGPUFeatureName): size_t
    external fun wgpuAdapterGetInfo(adapter: WGPUAdapter, info: WGPUAdapterInfo)
    external fun wgpuAdapterGetLimits(adapter: WGPUAdapter, limits: WGPUSupportedLimits): WGPUBool
    external fun wgpuAdapterHasFeature(adapter: WGPUAdapter, feature: WGPUFeatureName): WGPUBool
    external fun wgpuAdapterRequestDevice(adapter: WGPUAdapter, descriptor: WGPUDeviceDescriptor, callback: WGPUAdapterRequestDeviceCallback, userdata: FFIPointer?)
    external fun wgpuAdapterReference(adapter: WGPUAdapter)
    external fun wgpuAdapterRelease(adapter: WGPUAdapter)

    external fun wgpuAdapterInfoFreeMembers(adapterInfo: WGPUAdapterInfo)
    external fun wgpuBindGroupSetLabel(bindGroup: WGPUBindGroup, label: String)
    
    external fun wgpuBindGroupReference(bindGroup: WGPUBindGroup)
    external fun wgpuBindGroupRelease(bindGroup: WGPUBindGroup)

    external fun wgpuBindGroupLayoutSetLabel(bindGroupLayout: WGPUBindGroupLayout, label: String)
    external fun wgpuBindGroupLayoutReference(bindGroupLayout: WGPUBindGroupLayout)
    external fun wgpuBindGroupLayoutRelease(bindGroupLayout: WGPUBindGroupLayout)
    
    external fun wgpuBufferDestroy(buffer: WGPUBuffer)
    external fun wgpuBufferGetConstMappedRange(buffer: WGPUBuffer, offset: size_t, size: size_t): FFIPointer
    external fun wgpuBufferGetMapState(buffer: WGPUBuffer): WGPUBufferMapState
    external fun wgpuBufferGetMappedRange(buffer: WGPUBuffer, offset: size_t, size: size_t): FFIPointer
    external fun wgpuBufferGetSize(buffer: WGPUBuffer): uint64_t
    external fun wgpuBufferGetUsage(buffer: WGPUBuffer): WGPUBufferUsageFlags
    external fun wgpuBufferMapAsync(buffer: WGPUBuffer, mode: WGPUMapModeFlags, offset: size_t, size: size_t, callback: WGPUBufferMapAsyncCallback, userdata: FFIPointer??)
    external fun wgpuBufferSetLabel(buffer: WGPUBuffer, label: String)
    external fun wgpuBufferUnmap(buffer: WGPUBuffer)
    external fun wgpuBufferReference(buffer: WGPUBuffer)
    external fun wgpuBufferRelease(buffer: WGPUBuffer)

    external fun wgpuCommandBufferSetLabel(commandBuffer: WGPUCommandBuffer, label: String)
    external fun wgpuCommandBufferReference(commandBuffer: WGPUCommandBuffer)
    external fun wgpuCommandBufferRelease(commandBuffer: WGPUCommandBuffer)

    external fun wgpuCommandEncoderBeginComputePass(commandEncoder: WGPUCommandEncoder, descriptor: WGPUComputePassDescriptor): WGPUComputePassEncoder
    external fun wgpuCommandEncoderBeginRenderPass(commandEncoder: WGPUCommandEncoder, descriptor: WGPURenderPassDescriptor): WGPURenderPassEncoder
    external fun wgpuCommandEncoderClearBuffer(commandEncoder: WGPUCommandEncoder, buffer: WGPUBuffer, offset: uint64_t, size: uint64_t)
    external fun wgpuCommandEncoderCopyBufferToBuffer(commandEncoder: WGPUCommandEncoder, source: WGPUBuffer, sourceOffset: uint64_t, destination: WGPUBuffer, destinationOffset: uint64_t, size: uint64_t)
    external fun wgpuCommandEncoderCopyBufferToTexture(commandEncoder: WGPUCommandEncoder, source: WGPUImageCopyBuffer, destination: WGPUImageCopyTexture, copySize: WGPUExtent3D)
    external fun wgpuCommandEncoderCopyTextureToBuffer(commandEncoder: WGPUCommandEncoder, source: WGPUImageCopyTexture, destination: WGPUImageCopyBuffer, copySize: WGPUExtent3D)
    external fun wgpuCommandEncoderCopyTextureToTexture(commandEncoder: WGPUCommandEncoder, source: WGPUImageCopyTexture, destination: WGPUImageCopyTexture, copySize: WGPUExtent3D)
    external fun wgpuCommandEncoderFinish(commandEncoder: WGPUCommandEncoder, descriptor: WGPUCommandBufferDescriptor?): WGPUCommandBuffer
    external fun wgpuCommandEncoderInsertDebugMarker(commandEncoder: WGPUCommandEncoder, markerLabel: String)
    external fun wgpuCommandEncoderPopDebugGroup(commandEncoder: WGPUCommandEncoder)
    external fun wgpuCommandEncoderPushDebugGroup(commandEncoder: WGPUCommandEncoder, groupLabel: String)
    external fun wgpuCommandEncoderResolveQuerySet(commandEncoder: WGPUCommandEncoder, querySet: WGPUQuerySet, firstQuery: uint32_t, queryCount: uint32_t, destination: WGPUBuffer, destinationOffset: uint64_t)
    external fun wgpuCommandEncoderSetLabel(commandEncoder: WGPUCommandEncoder, label: String)
    external fun wgpuCommandEncoderWriteTimestamp(commandEncoder: WGPUCommandEncoder, querySet: WGPUQuerySet, queryIndex: uint32_t)
    external fun wgpuCommandEncoderReference(commandEncoder: WGPUCommandEncoder)
    external fun wgpuCommandEncoderRelease(commandEncoder: WGPUCommandEncoder)

    external fun wgpuComputePassEncoderDispatchWorkgroups(computePassEncoder: WGPUComputePassEncoder, workgroupCountX: uint32_t, workgroupCountY: uint32_t, workgroupCountZ: uint32_t)
    external fun wgpuComputePassEncoderDispatchWorkgroupsIndirect(computePassEncoder: WGPUComputePassEncoder, indirectBuffer: WGPUBuffer, indirectOffset: uint64_t)
    external fun wgpuComputePassEncoderEnd(computePassEncoder: WGPUComputePassEncoder)
    external fun wgpuComputePassEncoderInsertDebugMarker(computePassEncoder: WGPUComputePassEncoder, markerLabel: String)
    external fun wgpuComputePassEncoderPopDebugGroup(computePassEncoder: WGPUComputePassEncoder)
    external fun wgpuComputePassEncoderPushDebugGroup(computePassEncoder: WGPUComputePassEncoder, groupLabel: String)
    external fun wgpuComputePassEncoderSetBindGroup(computePassEncoder: WGPUComputePassEncoder, groupIndex: uint32_t, group: WGPUBindGroup?, dynamicOffsetCount: size_t, dynamicOffsets: IntArray)
    external fun wgpuComputePassEncoderSetLabel(computePassEncoder: WGPUComputePassEncoder, label: String)
    external fun wgpuComputePassEncoderSetPipeline(computePassEncoder: WGPUComputePassEncoder, pipeline: WGPUComputePipeline)
    external fun wgpuComputePassEncoderReference(computePassEncoder: WGPUComputePassEncoder)
    external fun wgpuComputePassEncoderRelease(computePassEncoder: WGPUComputePassEncoder)

    external fun wgpuComputePipelineGetBindGroupLayout(computePipeline: WGPUComputePipeline, groupIndex: uint32_t): WGPUBindGroupLayout
    external fun wgpuComputePipelineSetLabel(computePipeline: WGPUComputePipeline, label: String)
    external fun wgpuComputePipelineReference(computePipeline: WGPUComputePipeline)
    external fun wgpuComputePipelineRelease(computePipeline: WGPUComputePipeline)

    external fun wgpuDeviceCreateBindGroup(device: WGPUDevice, descriptor: WGPUBindGroupDescriptor): WGPUBindGroup
    external fun wgpuDeviceCreateBindGroupLayout(device: WGPUDevice, descriptor: WGPUBindGroupLayoutDescriptor): WGPUBindGroupLayout
    external fun wgpuDeviceCreateBuffer(device: WGPUDevice, descriptor: WGPUBufferDescriptor): WGPUBuffer
    external fun wgpuDeviceCreateCommandEncoder(device: WGPUDevice, descriptor: WGPUCommandEncoderDescriptor?): WGPUCommandEncoder
    external fun wgpuDeviceCreateComputePipeline(device: WGPUDevice, descriptor: WGPUComputePipelineDescriptor): WGPUComputePipeline
    external fun wgpuDeviceCreateComputePipelineAsync(device: WGPUDevice, descriptor: WGPUComputePipelineDescriptor, callback: WGPUDeviceCreateComputePipelineAsyncCallback, userdata: FFIPointer?)
    external fun wgpuDeviceCreatePipelineLayout(device: WGPUDevice, descriptor: WGPUPipelineLayoutDescriptor): WGPUPipelineLayout
    external fun wgpuDeviceCreateQuerySet(device: WGPUDevice, descriptor: WGPUQuerySetDescriptor): WGPUQuerySet
    external fun wgpuDeviceCreateRenderBundleEncoder(device: WGPUDevice, descriptor: WGPURenderBundleEncoderDescriptor): WGPURenderBundleEncoder
    external fun wgpuDeviceCreateRenderPipeline(device: WGPUDevice, descriptor: WGPURenderPipelineDescriptor): WGPURenderPipeline
    external fun wgpuDeviceCreateRenderPipelineAsync(device: WGPUDevice, descriptor: WGPURenderPipelineDescriptor, callback: WGPUDeviceCreateRenderPipelineAsyncCallback, userdata: FFIPointer?)
    external fun wgpuDeviceCreateSampler(device: WGPUDevice, descriptor: WGPUSamplerDescriptor?): WGPUSampler
    external fun wgpuDeviceCreateShaderModule(device: WGPUDevice, descriptor: WGPUShaderModuleDescriptor): WGPUShaderModule
    external fun wgpuDeviceCreateTexture(device: WGPUDevice, descriptor: WGPUTextureDescriptor): WGPUTexture
    external fun wgpuDeviceDestroy(device: WGPUDevice)
    external fun wgpuDeviceEnumerateFeatures(device: WGPUDevice, features: WGPUFeatureName): size_t
    external fun wgpuDeviceGetLimits(device: WGPUDevice, limits: WGPUSupportedLimits): WGPUBool
    external fun wgpuDeviceGetQueue(device: WGPUDevice): WGPUQueue
    external fun wgpuDeviceHasFeature(device: WGPUDevice, feature: WGPUFeatureName): WGPUBool
    external fun wgpuDevicePopErrorScope(device: WGPUDevice, callback: WGPUErrorCallback, userdata: FFIPointer?)
    external fun wgpuDevicePushErrorScope(device: WGPUDevice, filter: WGPUErrorFilter)
    external fun wgpuDeviceSetLabel(device: WGPUDevice, label: String)
    external fun wgpuDeviceReference(device: WGPUDevice)
    external fun wgpuDeviceRelease(device: WGPUDevice)

    external fun wgpuInstanceCreateSurface(instance: WGPUInstance, descriptor: WGPUSurfaceDescriptor): WGPUSurface
    external fun wgpuInstanceHasWGSLLanguageFeature(instance: WGPUInstance, feature: WGPUWGSLFeatureName): WGPUBool
    external fun wgpuInstanceProcessEvents(instance: WGPUInstance)
    external fun wgpuInstanceRequestAdapter(instance: WGPUInstance, options: WGPURequestAdapterOptions?, callback: WGPUInstanceRequestAdapterCallback, userdata: FFIPointer?)
    external fun wgpuInstanceReference(instance: WGPUInstance)
    external fun wgpuInstanceRelease(instance: WGPUInstance)

    external fun wgpuPipelineLayoutSetLabel(pipelineLayout: WGPUPipelineLayout, label: String)
    external fun wgpuPipelineLayoutReference(pipelineLayout: WGPUPipelineLayout)
    external fun wgpuPipelineLayoutRelease(pipelineLayout: WGPUPipelineLayout)

    external fun wgpuQuerySetDestroy(querySet: WGPUQuerySet)
    external fun wgpuQuerySetGetCount(querySet: WGPUQuerySet): uint32_t
    external fun wgpuQuerySetGetType(querySet: WGPUQuerySet): WGPUQueryType
    external fun wgpuQuerySetSetLabel(querySet: WGPUQuerySet, label: String)
    external fun wgpuQuerySetReference(querySet: WGPUQuerySet)
    external fun wgpuQuerySetRelease(querySet: WGPUQuerySet)

    external fun wgpuQueueOnSubmittedWorkDone(queue: WGPUQueue, callback: WGPUQueueOnSubmittedWorkDoneCallback, userdata: FFIPointer?)
    external fun wgpuQueueSetLabel(queue: WGPUQueue, label: String)
    external fun wgpuQueueSubmit(queue: WGPUQueue, commandCount: size_t, commands: WGPUCommandBuffer)
    external fun wgpuQueueWriteBuffer(queue: WGPUQueue, buffer: WGPUBuffer, bufferOffset: uint64_t, data: FFIPointer, size: size_t)
    external fun wgpuQueueWriteTexture(queue: WGPUQueue, destination: WGPUImageCopyTexture, data: FFIPointer, dataSize: size_t, dataLayout: WGPUTextureDataLayout, writeSize: WGPUExtent3D)
    external fun wgpuQueueReference(queue: WGPUQueue)
    external fun wgpuQueueRelease(queue: WGPUQueue)

    external fun wgpuRenderBundleSetLabel(renderBundle: WGPURenderBundle, label: String)
    external fun wgpuRenderBundleReference(renderBundle: WGPURenderBundle)
    external fun wgpuRenderBundleRelease(renderBundle: WGPURenderBundle)

    external fun wgpuRenderBundleEncoderDraw(renderBundleEncoder: WGPURenderBundleEncoder, vertexCount: uint32_t, instanceCount: uint32_t, firstVertex: uint32_t, firstInstance: uint32_t)
    external fun wgpuRenderBundleEncoderDrawIndexed(renderBundleEncoder: WGPURenderBundleEncoder, indexCount: uint32_t, instanceCount: uint32_t, firstIndex: uint32_t, baseVertex: int32_t,  firstInstance: uint32_t)
    external fun wgpuRenderBundleEncoderDrawIndexedIndirect(renderBundleEncoder: WGPURenderBundleEncoder, indirectBuffer: WGPUBuffer, indirectOffset: uint64_t)
    external fun wgpuRenderBundleEncoderDrawIndirect(renderBundleEncoder: WGPURenderBundleEncoder, indirectBuffer: WGPUBuffer, indirectOffset: uint64_t)
    external fun wgpuRenderBundleEncoderFinish(renderBundleEncoder: WGPURenderBundleEncoder, descriptor: WGPURenderBundleDescriptor?): WGPURenderBundle
    external fun wgpuRenderBundleEncoderInsertDebugMarker(renderBundleEncoder: WGPURenderBundleEncoder, markerLabel: String)
    external fun wgpuRenderBundleEncoderPopDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder)
    external fun wgpuRenderBundleEncoderPushDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder, groupLabel: String)
    external fun wgpuRenderBundleEncoderSetBindGroup(renderBundleEncoder: WGPURenderBundleEncoder, groupIndex: uint32_t, group: WGPUBindGroup?, dynamicOffsetCount: size_t, dynamicOffsets: IntArray)
    external fun wgpuRenderBundleEncoderSetIndexBuffer(renderBundleEncoder: WGPURenderBundleEncoder, buffer: WGPUBuffer, format: WGPUIndexFormat, offset: uint64_t, size: uint64_t)
    external fun wgpuRenderBundleEncoderSetLabel(renderBundleEncoder: WGPURenderBundleEncoder, label: String)
    external fun wgpuRenderBundleEncoderSetPipeline(renderBundleEncoder: WGPURenderBundleEncoder, pipeline: WGPURenderPipeline)
    external fun wgpuRenderBundleEncoderSetVertexBuffer(renderBundleEncoder: WGPURenderBundleEncoder, slot: uint32_t, buffer: WGPUBuffer?, offset: uint64_t, size: uint64_t)
    external fun wgpuRenderBundleEncoderReference(renderBundleEncoder: WGPURenderBundleEncoder)
    external fun wgpuRenderBundleEncoderRelease(renderBundleEncoder: WGPURenderBundleEncoder)

    external fun wgpuRenderPassEncoderBeginOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder, queryIndex: uint32_t)
    external fun wgpuRenderPassEncoderDraw(renderPassEncoder: WGPURenderPassEncoder, vertexCount: uint32_t, instanceCount: uint32_t, firstVertex: uint32_t, firstInstance: uint32_t)
    external fun wgpuRenderPassEncoderDrawIndexed(renderPassEncoder: WGPURenderPassEncoder, indexCount: uint32_t, instanceCount: uint32_t, firstIndex: uint32_t, baseVertex: int32_t, firstInstance: uint32_t)
    external fun wgpuRenderPassEncoderDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder, indirectBuffer: WGPUBuffer, indirectOffset: uint64_t)
    external fun wgpuRenderPassEncoderDrawIndirect(renderPassEncoder: WGPURenderPassEncoder, indirectBuffer: WGPUBuffer, indirectOffset: uint64_t)
    external fun wgpuRenderPassEncoderEnd(renderPassEncoder: WGPURenderPassEncoder)
    external fun wgpuRenderPassEncoderEndOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder)
    external fun wgpuRenderPassEncoderExecuteBundles(renderPassEncoder: WGPURenderPassEncoder, bundleCount: size_t, bundles: WGPURenderBundle)
    external fun wgpuRenderPassEncoderInsertDebugMarker(renderPassEncoder: WGPURenderPassEncoder, markerLabel: String)
    external fun wgpuRenderPassEncoderPopDebugGroup(renderPassEncoder: WGPURenderPassEncoder)
    external fun wgpuRenderPassEncoderPushDebugGroup(renderPassEncoder: WGPURenderPassEncoder, groupLabel: String)
    external fun wgpuRenderPassEncoderSetBindGroup(renderPassEncoder: WGPURenderPassEncoder, groupIndex: uint32_t, group: WGPUBindGroup?, dynamicOffsetCount: size_t, dynamicOffsets: IntArray)
    external fun wgpuRenderPassEncoderSetBlendConstant(renderPassEncoder: WGPURenderPassEncoder, color: WGPUColor)
    external fun wgpuRenderPassEncoderSetIndexBuffer(renderPassEncoder: WGPURenderPassEncoder, buffer: WGPUBuffer, format: WGPUIndexFormat, offset: uint64_t, size: uint64_t)
    external fun wgpuRenderPassEncoderSetLabel(renderPassEncoder: WGPURenderPassEncoder, label: String)
    external fun wgpuRenderPassEncoderSetPipeline(renderPassEncoder: WGPURenderPassEncoder, pipeline: WGPURenderPipeline)
    external fun wgpuRenderPassEncoderSetScissorRect(renderPassEncoder: WGPURenderPassEncoder, x: uint32_t, y: uint32_t, width: uint32_t, height: uint32_t)
    external fun wgpuRenderPassEncoderSetStencilReference(renderPassEncoder: WGPURenderPassEncoder, reference: uint32_t)
    external fun wgpuRenderPassEncoderSetVertexBuffer(renderPassEncoder: WGPURenderPassEncoder, slot: uint32_t, buffer: WGPUBuffer?, offset: uint64_t, size: uint64_t)
    external fun wgpuRenderPassEncoderSetViewport(renderPassEncoder: WGPURenderPassEncoder, x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float)
    external fun wgpuRenderPassEncoderReference(renderPassEncoder: WGPURenderPassEncoder)
    external fun wgpuRenderPassEncoderRelease(renderPassEncoder: WGPURenderPassEncoder)

    external fun wgpuRenderPipelineGetBindGroupLayout(renderPipeline: WGPURenderPipeline, groupIndex: uint32_t): WGPUBindGroupLayout
    external fun wgpuRenderPipelineSetLabel(renderPipeline: WGPURenderPipeline, label: String)
    external fun wgpuRenderPipelineReference(renderPipeline: WGPURenderPipeline)
    external fun wgpuRenderPipelineRelease(renderPipeline: WGPURenderPipeline)

    external fun wgpuSamplerSetLabel(sampler: WGPUSampler, label: String)
    external fun wgpuSamplerReference(sampler: WGPUSampler)
    external fun wgpuSamplerRelease(sampler: WGPUSampler)

    external fun wgpuShaderModuleGetCompilationInfo(shaderModule: WGPUShaderModule, callback: WGPUShaderModuleGetCompilationInfoCallback, userdata: FFIPointer?)
    external fun wgpuShaderModuleSetLabel(shaderModule: WGPUShaderModule, label: String)
    external fun wgpuShaderModuleReference(shaderModule: WGPUShaderModule)
    external fun wgpuShaderModuleRelease(shaderModule: WGPUShaderModule)

    external fun wgpuSurfaceConfigure(surface: WGPUSurface, config: WGPUSurfaceConfiguration)
    external fun wgpuSurfaceGetCapabilities(surface: WGPUSurface, adapter: WGPUAdapter, capabilities: WGPUSurfaceCapabilities)
    external fun wgpuSurfaceGetCurrentTexture(surface: WGPUSurface, surfaceTexture: WGPUSurfaceTexture)
    external fun wgpuSurfacePresent(surface: WGPUSurface)
    external fun wgpuSurfaceSetLabel(surface: WGPUSurface, label: String)
    external fun wgpuSurfaceUnconfigure(surface: WGPUSurface)
    external fun wgpuSurfaceReference(surface: WGPUSurface)
    external fun wgpuSurfaceRelease(surface: WGPUSurface)

    external fun wgpuSurfaceCapabilitiesFreeMembers(surfaceCapabilities: WGPUSurfaceCapabilities)

    external fun wgpuTextureCreateView(texture: WGPUTexture, descriptor: WGPUTextureViewDescriptor): WGPUTextureView
    external fun wgpuTextureDestroy(texture: WGPUTexture)
    external fun wgpuTextureGetDepthOrArrayLayers(texture: WGPUTexture): uint32_t
    external fun wgpuTextureGetDimension(texture: WGPUTexture): WGPUTextureDimension
    external fun wgpuTextureGetFormat(texture: WGPUTexture): WGPUTextureFormat
    external fun wgpuTextureGetHeight(texture: WGPUTexture): uint32_t
    external fun wgpuTextureGetMipLevelCount(texture: WGPUTexture): uint32_t
    external fun wgpuTextureGetSampleCount(texture: WGPUTexture): uint32_t
    external fun wgpuTextureGetUsage(texture: WGPUTexture): WGPUTextureUsageFlags
    external fun wgpuTextureGetWidth(texture: WGPUTexture): uint32_t
    external fun wgpuTextureSetLabel(texture: WGPUTexture, label: String)
    external fun wgpuTextureReference(texture: WGPUTexture)
    external fun wgpuTextureRelease(texture: WGPUTexture)

    external fun wgpuTextureViewSetLabel(textureView: WGPUTextureView, label: String)
    external fun wgpuTextureViewReference(textureView: WGPUTextureView)
    external fun wgpuTextureViewRelease(textureView: WGPUTextureView)
}

/*
inline class WGPUAdapterType(val value: Int) {
    companion object {
        val DiscreteGPU = WGPUAdapterType(0x00000000)
        val IntegratedGPU = WGPUAdapterType(0x00000001)
        val CPU = WGPUAdapterType(0x00000002)
        val Unknown = WGPUAdapterType(0x00000003)
        val Force32 = WGPUAdapterType(0x7FFFFFFF)
    }
}
*/
