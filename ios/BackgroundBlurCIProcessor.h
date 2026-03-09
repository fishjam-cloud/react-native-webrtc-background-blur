#if !TARGET_OS_TV

#import <Foundation/Foundation.h>
#import <Vision/Vision.h>
#import <WebRTC/RTCCVPixelBuffer.h>
#import <WebRTC/RTCVideoFrame.h>

#import "VideoFrameProcessor.h"

API_AVAILABLE(ios(15.0))
@interface BackgroundBlurCIProcessor : NSObject <VideoFrameProcessorDelegate>

- (instancetype)initWithBlurRadius:(float)blurRadius;
- (void)setBlurRadius:(float)blurRadius;

@end

#endif
