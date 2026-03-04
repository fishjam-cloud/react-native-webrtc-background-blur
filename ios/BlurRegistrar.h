#import <Foundation/Foundation.h>
#import "BackgroundBlurProcessor.h"

NS_ASSUME_NONNULL_BEGIN

API_AVAILABLE(ios(15.0))
@interface BlurRegistrar : NSObject

+ (nullable BackgroundBlurProcessor *)sharedProcessor;

@end

NS_ASSUME_NONNULL_END
