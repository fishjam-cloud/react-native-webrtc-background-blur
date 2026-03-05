#import "BlurRegistrar.h"

static BackgroundBlurProcessor *_sharedProcessor = nil;

@implementation BlurRegistrar

+ (nullable BackgroundBlurProcessor *)sharedProcessor {
    return _sharedProcessor;
}

+ (void)setSharedProcessor:(nullable BackgroundBlurProcessor *)processor {
    _sharedProcessor = processor;
}

@end
