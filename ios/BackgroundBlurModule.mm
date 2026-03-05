#import "BackgroundBlurModule.h"
#import "ProcessorProvider.h"
#import "BackgroundBlurProcessor.h"

#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTUtils.h>
#endif

@implementation BackgroundBlurModule

RCT_EXPORT_MODULE(BackgroundBlur)

RCT_EXPORT_METHOD(initialize) {
    if (@available(iOS 15.0, *)) {
        if ([ProcessorProvider getProcessor:@"backgroundBlur"] != nil) {
            return;
        }

        BackgroundBlurProcessor *processor = [[BackgroundBlurProcessor alloc] initWithBlurRadius:12.0];
        if (!processor) {
            return;
        }

        [ProcessorProvider addProcessor:processor forName:@"backgroundBlur"];
    }
}

RCT_EXPORT_METHOD(deinitialize) {
    if ([ProcessorProvider getProcessor:@"backgroundBlur"] == nil) {
        return;
    }

    [ProcessorProvider removeProcessor:@"backgroundBlur"];
}

RCT_EXPORT_METHOD(setBlurRadius:(double)radius) {
    BackgroundBlurProcessor *processor = (BackgroundBlurProcessor *)[ProcessorProvider getProcessor:@"backgroundBlur"];
    if (processor) {
        [processor setBlurRadius:(float)radius];
    }
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(isAvailable) {
    if (@available(iOS 15.0, *)) {
        return @YES;
    }
    return @NO;
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeBackgroundBlurSpecJSI>(params);
}
#endif

@end
