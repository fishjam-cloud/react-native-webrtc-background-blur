#import "BackgroundBlurModule.h"
#import "BlurRegistrar.h"
#import "ProcessorProvider.h"

#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTUtils.h>
#endif

@implementation BackgroundBlurModule

RCT_EXPORT_MODULE(BackgroundBlur)

RCT_EXPORT_METHOD(initialize) {
    if (@available(iOS 15.0, *)) {
        if ([BlurRegistrar sharedProcessor] != nil) {
            return;
        }

        BackgroundBlurProcessor *processor = [[BackgroundBlurProcessor alloc] initWithBlurRadius:12.0];
        if (!processor) {
            return;
        }

        [BlurRegistrar setSharedProcessor:processor];
        [ProcessorProvider addProcessor:processor forName:@"backgroundBlur"];
    }
}

RCT_EXPORT_METHOD(deinitialize) {
    if ([BlurRegistrar sharedProcessor] == nil) {
        return;
    }

    [ProcessorProvider removeProcessor:@"backgroundBlur"];
    [BlurRegistrar setSharedProcessor:nil];
}

RCT_EXPORT_METHOD(setBlurRadius:(double)radius) {
    BackgroundBlurProcessor *processor = [BlurRegistrar sharedProcessor];
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
