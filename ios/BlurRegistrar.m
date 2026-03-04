#import "ProcessorProvider.h"
#import "BackgroundBlurProcessor.h"

@interface BlurRegistrar : NSObject
@end

@implementation BlurRegistrar

+ (void)load {
    if (@available(iOS 15.0, *)) {
        BackgroundBlurProcessor *blurProcessor = [[BackgroundBlurProcessor alloc]
            initWithBlurRadius:12.0];

        if (blurProcessor) {
            [ProcessorProvider addProcessor:blurProcessor forName:@"backgroundBlur"];
        }
    }
}

@end
