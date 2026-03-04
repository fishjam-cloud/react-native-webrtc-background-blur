#import "BlurRegistrar.h"
#import "ProcessorProvider.h"

static BackgroundBlurProcessor *_sharedProcessor = nil;

@implementation BlurRegistrar

+ (void)load {
    if (@available(iOS 15.0, *)) {
        BackgroundBlurProcessor *blurProcessor = [[BackgroundBlurProcessor alloc]
            initWithBlurRadius:12.0];

        if (blurProcessor) {
            _sharedProcessor = blurProcessor;
            [ProcessorProvider addProcessor:blurProcessor forName:@"backgroundBlur"];
        }
    }
}

+ (nullable BackgroundBlurProcessor *)sharedProcessor {
    return _sharedProcessor;
}

@end
