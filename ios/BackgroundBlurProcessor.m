#if !TARGET_OS_TV

#import "BackgroundBlurProcessor.h"
#import <CoreVideo/CoreVideo.h>

@implementation BackgroundBlurProcessor {
    id<MTLDevice> _device;
    id<MTLCommandQueue> _commandQueue;
    CVMetalTextureCacheRef _textureCache;

    id<MTLComputePipelineState> _nv12ToBgraPipeline;
    id<MTLComputePipelineState> _compositePipeline;
    id<MTLSamplerState> _bilinearSampler;
    MPSImageGaussianBlur *_blurKernel;

    VNSequenceRequestHandler *_sequenceHandler;
    VNGeneratePersonSegmentationRequest *_segmentationRequest;

    CVPixelBufferPoolRef _bgraPoolA;
    CVPixelBufferPoolRef _bgraPoolB;
    CVPixelBufferPoolRef _outputPool;
    size_t _poolWidth;
    size_t _poolHeight;

    RTCVideoFrame *_lastProcessedFrame;
    BOOL _isProcessing;
}

#pragma mark - Init

- (instancetype)initWithBlurRadius:(float)blurRadius {
    self = [super init];
    if (!self) return nil;

    _device = MTLCreateSystemDefaultDevice();
    if (!_device) return nil;

    _commandQueue = [_device newCommandQueue];

    CVReturn status = CVMetalTextureCacheCreate(kCFAllocatorDefault, nil, _device, nil, &_textureCache);
    if (status != kCVReturnSuccess) return nil;

    NSError *error = nil;
    id<MTLLibrary> library = [_device newDefaultLibraryWithBundle:[NSBundle bundleForClass:[self class]] error:&error];
    if (!library) {
        NSLog(@"[BackgroundBlurProcessor] Failed to load Metal library: %@", error);
        return nil;
    }

    id<MTLFunction> nv12Func = [library newFunctionWithName:@"nv12ToBgra"];
    id<MTLFunction> compositeFunc = [library newFunctionWithName:@"compositeKernel"];
    if (!nv12Func || !compositeFunc) {
        NSLog(@"[BackgroundBlurProcessor] Failed to load Metal functions");
        return nil;
    }

    _nv12ToBgraPipeline = [_device newComputePipelineStateWithFunction:nv12Func error:&error];
    _compositePipeline = [_device newComputePipelineStateWithFunction:compositeFunc error:&error];
    if (!_nv12ToBgraPipeline || !_compositePipeline) {
        NSLog(@"[BackgroundBlurProcessor] Failed to create pipeline states: %@", error);
        return nil;
    }

    MTLSamplerDescriptor *samplerDesc = [[MTLSamplerDescriptor alloc] init];
    samplerDesc.minFilter = MTLSamplerMinMagFilterLinear;
    samplerDesc.magFilter = MTLSamplerMinMagFilterLinear;
    samplerDesc.sAddressMode = MTLSamplerAddressModeClampToEdge;
    samplerDesc.tAddressMode = MTLSamplerAddressModeClampToEdge;
    _bilinearSampler = [_device newSamplerStateWithDescriptor:samplerDesc];

    _blurKernel = [[MPSImageGaussianBlur alloc] initWithDevice:_device sigma:blurRadius];
    _blurKernel.edgeMode = MPSImageEdgeModeClamp;

    if (@available(iOS 15.0, *)) {
        _segmentationRequest = [[VNGeneratePersonSegmentationRequest alloc] init];
        _segmentationRequest.qualityLevel = VNGeneratePersonSegmentationRequestQualityLevelBalanced;
        _segmentationRequest.outputPixelFormat = kCVPixelFormatType_OneComponent8;
        _sequenceHandler = [[VNSequenceRequestHandler alloc] init];
    }

    _isProcessing = NO;

    return self;
}

#pragma mark - VideoFrameProcessorDelegate

- (RTCVideoFrame *)capturer:(RTCVideoCapturer *)capturer didCaptureVideoFrame:(RTCVideoFrame *)frame {
    if (@available(iOS 15.0, *)) {} else {
        return frame;
    }

    if (_isProcessing) {
        return _lastProcessedFrame ?: frame;
    }

    _isProcessing = YES;
    RTCVideoFrame *result = [self processFrame:frame];
    _lastProcessedFrame = result;
    _isProcessing = NO;
    return result;
}

#pragma mark - Core Pipeline

- (RTCVideoFrame *)processFrame:(RTCVideoFrame *)frame API_AVAILABLE(ios(15.0)) {
    if (![frame.buffer isKindOfClass:[RTCCVPixelBuffer class]]) {
        return frame;
    }

    CVPixelBufferRef inputPixelBuffer = ((RTCCVPixelBuffer *)frame.buffer).pixelBuffer;
    size_t width = CVPixelBufferGetWidth(inputPixelBuffer);
    size_t height = CVPixelBufferGetHeight(inputPixelBuffer);
    OSType pixelFormat = CVPixelBufferGetPixelFormatType(inputPixelBuffer);

    BOOL isNV12 = (pixelFormat == kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange ||
                   pixelFormat == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange);

    if (!isNV12) {
        return frame;
    }

    NSError *error = nil;
    [_sequenceHandler performRequests:@[_segmentationRequest]
                      onCVPixelBuffer:inputPixelBuffer
                          orientation:kCGImagePropertyOrientationUp
                                error:&error];
    if (error) {
        NSLog(@"[BackgroundBlurProcessor] Segmentation error: %@", error);
        return frame;
    }

    VNPixelBufferObservation *observation = _segmentationRequest.results.firstObject;
    if (!observation) {
        return frame;
    }
    CVPixelBufferRef maskPixelBuffer = observation.pixelBuffer;

    [self ensurePoolsForWidth:width height:height];

    CVPixelBufferRef bgraBufferA = NULL;
    CVPixelBufferRef bgraBufferB = NULL;
    CVPixelBufferRef outputBuffer = NULL;

    if (CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, _bgraPoolA, &bgraBufferA) != kCVReturnSuccess ||
        CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, _bgraPoolB, &bgraBufferB) != kCVReturnSuccess ||
        CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, _outputPool, &outputBuffer) != kCVReturnSuccess) {
        if (bgraBufferA) CVPixelBufferRelease(bgraBufferA);
        if (bgraBufferB) CVPixelBufferRelease(bgraBufferB);
        if (outputBuffer) CVPixelBufferRelease(outputBuffer);
        return frame;
    }

    id<MTLTexture> yTexture = [self textureFromPixelBuffer:inputPixelBuffer
                                               pixelFormat:MTLPixelFormatR8Unorm
                                                planeIndex:0
                                                     width:width
                                                    height:height];

    id<MTLTexture> cbcrTexture = [self textureFromPixelBuffer:inputPixelBuffer
                                                  pixelFormat:MTLPixelFormatRG8Unorm
                                                   planeIndex:1
                                                        width:width / 2
                                                       height:height / 2];

    id<MTLTexture> bgraTextureA = [self textureFromPixelBuffer:bgraBufferA
                                                   pixelFormat:MTLPixelFormatBGRA8Unorm
                                                    planeIndex:-1
                                                         width:width
                                                        height:height];

    id<MTLTexture> bgraTextureB = [self textureFromPixelBuffer:bgraBufferB
                                                   pixelFormat:MTLPixelFormatBGRA8Unorm
                                                    planeIndex:-1
                                                         width:width
                                                        height:height];

    id<MTLTexture> maskTexture = [self textureFromPixelBuffer:maskPixelBuffer
                                                  pixelFormat:MTLPixelFormatR8Unorm
                                                   planeIndex:-1
                                                        width:CVPixelBufferGetWidth(maskPixelBuffer)
                                                       height:CVPixelBufferGetHeight(maskPixelBuffer)];

    id<MTLTexture> outputTexture = [self textureFromPixelBuffer:outputBuffer
                                                    pixelFormat:MTLPixelFormatBGRA8Unorm
                                                     planeIndex:-1
                                                          width:width
                                                         height:height];

    if (!yTexture || !cbcrTexture || !bgraTextureA || !bgraTextureB || !maskTexture || !outputTexture) {
        CVPixelBufferRelease(bgraBufferA);
        CVPixelBufferRelease(bgraBufferB);
        CVPixelBufferRelease(outputBuffer);
        return frame;
    }

    id<MTLCommandBuffer> commandBuffer = [_commandQueue commandBuffer];

    MTLSize threadgroupSize = MTLSizeMake(16, 16, 1);
    MTLSize nv12GridSize = MTLSizeMake((width + 15) / 16, (height + 15) / 16, 1);

    id<MTLComputeCommandEncoder> nv12Encoder = [commandBuffer computeCommandEncoder];
    [nv12Encoder setComputePipelineState:_nv12ToBgraPipeline];
    [nv12Encoder setTexture:yTexture atIndex:0];
    [nv12Encoder setTexture:cbcrTexture atIndex:1];
    [nv12Encoder setTexture:bgraTextureA atIndex:2];
    [nv12Encoder dispatchThreadgroups:nv12GridSize threadsPerThreadgroup:threadgroupSize];
    [nv12Encoder endEncoding];

    [_blurKernel encodeToCommandBuffer:commandBuffer sourceTexture:bgraTextureA destinationTexture:bgraTextureB];

    MTLSize compositeGridSize = MTLSizeMake((width + 15) / 16, (height + 15) / 16, 1);

    id<MTLComputeCommandEncoder> compositeEncoder = [commandBuffer computeCommandEncoder];
    [compositeEncoder setComputePipelineState:_compositePipeline];
    [compositeEncoder setTexture:bgraTextureA atIndex:0];
    [compositeEncoder setTexture:bgraTextureB atIndex:1];
    [compositeEncoder setTexture:maskTexture atIndex:2];
    [compositeEncoder setTexture:outputTexture atIndex:3];
    [compositeEncoder setSamplerState:_bilinearSampler atIndex:0];
    [compositeEncoder dispatchThreadgroups:compositeGridSize threadsPerThreadgroup:threadgroupSize];
    [compositeEncoder endEncoding];

    [commandBuffer commit];
    [commandBuffer waitUntilCompleted];

    CVMetalTextureCacheFlush(_textureCache, 0);

    CVPixelBufferRelease(bgraBufferA);
    CVPixelBufferRelease(bgraBufferB);

    RTCCVPixelBuffer *rtcBuffer = [[RTCCVPixelBuffer alloc] initWithPixelBuffer:outputBuffer];
    RTCVideoFrame *outputFrame = [[RTCVideoFrame alloc] initWithBuffer:rtcBuffer
                                                              rotation:frame.rotation
                                                           timeStampNs:frame.timeStampNs];
    CVPixelBufferRelease(outputBuffer);
    return outputFrame;
}

#pragma mark - Helpers

- (void)ensurePoolsForWidth:(size_t)width height:(size_t)height {
    if (_poolWidth == width && _poolHeight == height && _bgraPoolA && _bgraPoolB && _outputPool) {
        return;
    }

    if (_bgraPoolA) { CVPixelBufferPoolRelease(_bgraPoolA); _bgraPoolA = NULL; }
    if (_bgraPoolB) { CVPixelBufferPoolRelease(_bgraPoolB); _bgraPoolB = NULL; }
    if (_outputPool) { CVPixelBufferPoolRelease(_outputPool); _outputPool = NULL; }

    _poolWidth = width;
    _poolHeight = height;

    NSDictionary *attrs = @{
        (id)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32BGRA),
        (id)kCVPixelBufferWidthKey: @(width),
        (id)kCVPixelBufferHeightKey: @(height),
        (id)kCVPixelBufferIOSurfacePropertiesKey: @{},
        (id)kCVPixelBufferMetalCompatibilityKey: @YES,
    };
    CFDictionaryRef cfAttrs = (__bridge CFDictionaryRef)attrs;

    CVPixelBufferPoolCreate(kCFAllocatorDefault, NULL, cfAttrs, &_bgraPoolA);
    CVPixelBufferPoolCreate(kCFAllocatorDefault, NULL, cfAttrs, &_bgraPoolB);
    CVPixelBufferPoolCreate(kCFAllocatorDefault, NULL, cfAttrs, &_outputPool);
}

- (id<MTLTexture>)textureFromPixelBuffer:(CVPixelBufferRef)pixelBuffer
                             pixelFormat:(MTLPixelFormat)pixelFormat
                              planeIndex:(int)planeIndex
                                   width:(size_t)width
                                  height:(size_t)height {
    CVMetalTextureRef cvTexture = NULL;
    CVReturn status;

    if (planeIndex >= 0) {
        status = CVMetalTextureCacheCreateTextureFromImage(
            kCFAllocatorDefault, _textureCache, pixelBuffer, nil,
            pixelFormat, width, height, planeIndex, &cvTexture);
    } else {
        status = CVMetalTextureCacheCreateTextureFromImage(
            kCFAllocatorDefault, _textureCache, pixelBuffer, nil,
            pixelFormat, width, height, 0, &cvTexture);
    }

    if (status != kCVReturnSuccess || !cvTexture) {
        return nil;
    }

    id<MTLTexture> texture = CVMetalTextureGetTexture(cvTexture);
    CFRelease(cvTexture);
    return texture;
}

#pragma mark - Dealloc

- (void)dealloc {
    if (_textureCache) {
        CVMetalTextureCacheFlush(_textureCache, 0);
        CFRelease(_textureCache);
    }
    if (_bgraPoolA) CVPixelBufferPoolRelease(_bgraPoolA);
    if (_bgraPoolB) CVPixelBufferPoolRelease(_bgraPoolB);
    if (_outputPool) CVPixelBufferPoolRelease(_outputPool);
}

@end

#endif
