#if !TARGET_OS_TV

#import "BackgroundBlurCIProcessor.h"
#import <CoreImage/CoreImage.h>
#import <CoreVideo/CoreVideo.h>

@implementation BackgroundBlurCIProcessor {
    CIContext *_context;

    float _blurRadius;

    VNSequenceRequestHandler *_sequenceHandler;
    VNGeneratePersonSegmentationRequest *_segmentationRequest;

    CVPixelBufferPoolRef _outputPool;
    size_t _poolWidth;
    size_t _poolHeight;

    RTCVideoFrame *_lastProcessedFrame;
    BOOL _isProcessing;
    BOOL _ready;
}

- (instancetype)initWithBlurRadius:(float)blurRadius {
    self = [super init];
    if (!self) return nil;

    _context = [CIContext contextWithOptions:@{
        kCIContextUseSoftwareRenderer: @NO,
    }];
    if (!_context) return nil;

    _blurRadius = blurRadius;
    _isProcessing = NO;
    _ready = NO;

    [self warmUp];

    return self;
}

- (void)warmUp {
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        if (@available(iOS 15.0, *)) {
            self->_segmentationRequest = [[VNGeneratePersonSegmentationRequest alloc] init];
            self->_segmentationRequest.qualityLevel = VNGeneratePersonSegmentationRequestQualityLevelBalanced;
            self->_segmentationRequest.outputPixelFormat = kCVPixelFormatType_OneComponent8;
            self->_sequenceHandler = [[VNSequenceRequestHandler alloc] init];

            [self warmUpVisionModel];
            [self warmUpCIFilters];
        }

        self->_ready = YES;
    });
}

- (void)warmUpVisionModel API_AVAILABLE(ios(15.0)) {
    size_t warmUpWidth = 64;
    size_t warmUpHeight = 64;
    NSDictionary *attrs = @{
        (id)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange),
        (id)kCVPixelBufferWidthKey: @(warmUpWidth),
        (id)kCVPixelBufferHeightKey: @(warmUpHeight),
        (id)kCVPixelBufferIOSurfacePropertiesKey: @{},
    };

    CVPixelBufferRef dummyBuffer = NULL;
    CVReturn result = CVPixelBufferCreate(
        kCFAllocatorDefault, warmUpWidth, warmUpHeight,
        kCVPixelFormatType_420YpCbCr8BiPlanarFullRange,
        (__bridge CFDictionaryRef)attrs, &dummyBuffer);

    if (result != kCVReturnSuccess || !dummyBuffer) return;

    CVPixelBufferLockBaseAddress(dummyBuffer, 0);
    for (size_t plane = 0; plane < CVPixelBufferGetPlaneCount(dummyBuffer); plane++) {
        void *base = CVPixelBufferGetBaseAddressOfPlane(dummyBuffer, plane);
        size_t h = CVPixelBufferGetHeightOfPlane(dummyBuffer, plane);
        size_t bytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(dummyBuffer, plane);
        memset(base, 0, h * bytesPerRow);
    }
    CVPixelBufferUnlockBaseAddress(dummyBuffer, 0);

    NSError *error = nil;
    [_sequenceHandler performRequests:@[_segmentationRequest]
                      onCVPixelBuffer:dummyBuffer
                          orientation:kCGImagePropertyOrientationUp
                                error:&error];

    CVPixelBufferRelease(dummyBuffer);
}

- (void)warmUpCIFilters API_AVAILABLE(ios(15.0)) {
    CIImage *dummy = [[CIImage imageWithColor:[CIColor colorWithRed:0 green:0 blue:0]]
                      imageByCroppingToRect:CGRectMake(0, 0, 64, 64)];

    CIFilter *blurFilter = [CIFilter filterWithName:@"CIGaussianBlur"];
    [blurFilter setValue:[dummy imageByClampingToExtent] forKey:kCIInputImageKey];
    [blurFilter setValue:@(_blurRadius) forKey:kCIInputRadiusKey];
    CIImage *blurred = [blurFilter.outputImage imageByCroppingToRect:dummy.extent];

    CIFilter *blendFilter = [CIFilter filterWithName:@"CIBlendWithMask"];
    [blendFilter setValue:dummy forKey:kCIInputImageKey];
    [blendFilter setValue:blurred forKey:kCIInputBackgroundImageKey];
    [blendFilter setValue:dummy forKey:kCIInputMaskImageKey];
    CIImage *result = blendFilter.outputImage;

    NSDictionary *attrs = @{
        (id)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32BGRA),
        (id)kCVPixelBufferWidthKey: @(64),
        (id)kCVPixelBufferHeightKey: @(64),
        (id)kCVPixelBufferIOSurfacePropertiesKey: @{},
        (id)kCVPixelBufferMetalCompatibilityKey: @YES,
    };

    CVPixelBufferRef warmupBuffer = NULL;
    CVPixelBufferCreate(kCFAllocatorDefault, 64, 64, kCVPixelFormatType_32BGRA,
                        (__bridge CFDictionaryRef)attrs, &warmupBuffer);
    if (warmupBuffer) {
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        [_context render:result toCVPixelBuffer:warmupBuffer bounds:dummy.extent colorSpace:colorSpace];
        CGColorSpaceRelease(colorSpace);
        CVPixelBufferRelease(warmupBuffer);
    }
}

#pragma mark - VideoFrameProcessorDelegate

- (RTCVideoFrame *)capturer:(RTCVideoCapturer *)capturer didCaptureVideoFrame:(RTCVideoFrame *)frame {
    if (@available(iOS 15.0, *)) {} else {
        return frame;
    }

    if (!_ready) return frame;
    if (_isProcessing) return _lastProcessedFrame ?: frame;

    _isProcessing = YES;
    RTCVideoFrame *result = [self processFrame:frame];
    _lastProcessedFrame = result;
    _isProcessing = NO;
    return result;
}

#pragma mark - Core Pipeline

- (RTCVideoFrame *)processFrame:(RTCVideoFrame *)frame API_AVAILABLE(ios(15.0)) {
    if (![frame.buffer isKindOfClass:[RTCCVPixelBuffer class]]) return frame;

    CVPixelBufferRef inputPixelBuffer = ((RTCCVPixelBuffer *)frame.buffer).pixelBuffer;
    size_t width = CVPixelBufferGetWidth(inputPixelBuffer);
    size_t height = CVPixelBufferGetHeight(inputPixelBuffer);
    OSType pixelFormat = CVPixelBufferGetPixelFormatType(inputPixelBuffer);

    BOOL isNV12 = (pixelFormat == kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange ||
                   pixelFormat == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange);
    if (!isNV12) return frame;

    NSError *error = nil;
    [_sequenceHandler performRequests:@[_segmentationRequest]
                      onCVPixelBuffer:inputPixelBuffer
                          orientation:kCGImagePropertyOrientationUp
                                error:&error];
    if (error) {
        NSLog(@"[BackgroundBlurCIProcessor] Segmentation error: %@", error);
        return frame;
    }

    VNPixelBufferObservation *observation = _segmentationRequest.results.firstObject;
    if (!observation) return frame;
    CVPixelBufferRef maskPixelBuffer = observation.pixelBuffer;

    [self ensurePoolForWidth:width height:height];

    CVPixelBufferRef outputBuffer = NULL;
    if (CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, _outputPool, &outputBuffer) != kCVReturnSuccess) {
        return frame;
    }

    CIImage *original = [CIImage imageWithCVPixelBuffer:inputPixelBuffer];
    CIImage *mask = [CIImage imageWithCVPixelBuffer:maskPixelBuffer];

    CGFloat scaleX = (CGFloat)width / (CGFloat)CVPixelBufferGetWidth(maskPixelBuffer);
    CGFloat scaleY = (CGFloat)height / (CGFloat)CVPixelBufferGetHeight(maskPixelBuffer);
    mask = [mask imageByApplyingTransform:CGAffineTransformMakeScale(scaleX, scaleY)];

    CIFilter *blurFilter = [CIFilter filterWithName:@"CIGaussianBlur"];
    [blurFilter setValue:[original imageByClampingToExtent] forKey:kCIInputImageKey];
    [blurFilter setValue:@(_blurRadius) forKey:kCIInputRadiusKey];
    CIImage *blurred = [blurFilter.outputImage imageByCroppingToRect:original.extent];

    CIFilter *blendFilter = [CIFilter filterWithName:@"CIBlendWithMask"];
    [blendFilter setValue:original forKey:kCIInputImageKey];
    [blendFilter setValue:blurred forKey:kCIInputBackgroundImageKey];
    [blendFilter setValue:mask forKey:kCIInputMaskImageKey];
    CIImage *composited = blendFilter.outputImage;

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    [_context render:composited toCVPixelBuffer:outputBuffer bounds:original.extent colorSpace:colorSpace];
    CGColorSpaceRelease(colorSpace);

    RTCCVPixelBuffer *rtcBuffer = [[RTCCVPixelBuffer alloc] initWithPixelBuffer:outputBuffer];
    RTCVideoFrame *outputFrame = [[RTCVideoFrame alloc] initWithBuffer:rtcBuffer
                                                              rotation:frame.rotation
                                                           timeStampNs:frame.timeStampNs];
    CVPixelBufferRelease(outputBuffer);
    return outputFrame;
}

#pragma mark - Helpers

- (void)ensurePoolForWidth:(size_t)width height:(size_t)height {
    if (_poolWidth == width && _poolHeight == height && _outputPool) return;

    if (_outputPool) {
        CVPixelBufferPoolRelease(_outputPool);
        _outputPool = NULL;
    }

    _poolWidth = width;
    _poolHeight = height;

    NSDictionary *attrs = @{
        (id)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32BGRA),
        (id)kCVPixelBufferWidthKey: @(width),
        (id)kCVPixelBufferHeightKey: @(height),
        (id)kCVPixelBufferIOSurfacePropertiesKey: @{},
        (id)kCVPixelBufferMetalCompatibilityKey: @YES,
    };
    CVPixelBufferPoolCreate(kCFAllocatorDefault, NULL, (__bridge CFDictionaryRef)attrs, &_outputPool);
}

#pragma mark - Configuration

- (void)setBlurRadius:(float)blurRadius {
    _blurRadius = blurRadius;
}

#pragma mark - Dealloc

- (void)dealloc {
    if (_outputPool) CVPixelBufferPoolRelease(_outputPool);
}

@end

#endif
