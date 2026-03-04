#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#import <MetalPerformanceShaders/MetalPerformanceShaders.h>
#import <Vision/Vision.h>
#import <WebRTC/RTCCVPixelBuffer.h>
#import <WebRTC/RTCVideoFrame.h>

#import "VideoFrameProcessor.h"

API_AVAILABLE(ios(15.0))
@interface BackgroundBlurProcessor : NSObject <VideoFrameProcessorDelegate>

- (instancetype)initWithBlurRadius:(float)blurRadius;
- (void)setBlurRadius:(float)blurRadius;

@end
