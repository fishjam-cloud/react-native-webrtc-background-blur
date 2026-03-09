#import <React/RCTBridgeModule.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <RNBackgroundBlurSpec/RNBackgroundBlurSpec.h>
#endif

NS_ASSUME_NONNULL_BEGIN

@interface BackgroundBlurModule : NSObject <RCTBridgeModule
#ifdef RCT_NEW_ARCH_ENABLED
  , NativeBackgroundBlurSpec
#endif
>
@end

NS_ASSUME_NONNULL_END
