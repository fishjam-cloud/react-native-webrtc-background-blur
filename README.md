# @fishjam-cloud/react-native-webrtc-background-blur

Background blur video effect for `@fishjam-cloud/react-native-webrtc`, with a ready-to-use React hook for `@fishjam-cloud/react-native-client`.

## Installation

```bash
npm install @fishjam-cloud/react-native-webrtc-background-blur
# or
yarn add @fishjam-cloud/react-native-webrtc-background-blur
```

Then install native dependencies:

```bash
cd ios && pod install
```

### Peer dependencies

This package requires the following peer dependencies:

- `@fishjam-cloud/react-native-client`
- `@fishjam-cloud/react-native-webrtc`
- `react`
- `react-native`

### Android

ML Kit selfie segmentation model (~5 MB) is downloaded automatically on first use. To pre-download at install time, add to your `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.mlkit.vision.DEPENDENCIES"
    android:value="selfie_segmentation" />
```

### iOS

Requires iOS 15.0+. Metal, MetalPerformanceShaders, and Vision frameworks are linked automatically via the podspec.

## Usage

```typescript
import { useBackgroundBlur } from '@fishjam-cloud/react-native-webrtc-background-blur';

function VideoCall() {
  const { toggleBlur, isBlurEnabled } = useBackgroundBlur();

  return (
    <Button
      title={isBlurEnabled ? 'Disable Blur' : 'Enable Blur'}
      onPress={toggleBlur}
    />
  );
}
```

The `useBackgroundBlur` hook integrates with `@fishjam-cloud/react-native-client`'s camera middleware system. It returns:

- **`toggleBlur`** -- toggles background blur on/off for the active camera track.
- **`isBlurEnabled`** -- whether blur is currently active.

### Manual usage

You can also control the effect directly on a track without the hook:

```typescript
import { mediaDevices } from '@fishjam-cloud/react-native-webrtc';

const stream = await mediaDevices.getUserMedia({ video: true });
const track = stream.getVideoTracks()[0];

track._setVideoEffect('backgroundBlur');

track._setVideoEffects([]);
```

## How It Works

Native code for Android and iOS auto-registers with the core library's `ProcessorProvider`:

- **Android**: A `ContentProvider` (`BlurRegistrar`) declared in the library's `AndroidManifest.xml` gets merged into the app manifest. Its `onCreate()` registers the blur processor at app startup.
- **iOS**: A `+load` method in `BlurRegistrar.m` registers the blur processor when the Objective-C runtime loads the class from the linked pod.

When you call `track._setVideoEffect('backgroundBlur')`, the core library looks up the registered processor by name and applies it to the video frames.

## Creating Custom Video Effects

You can create your own video effects using the same plugin pattern. No changes to the core library are needed.

### Android

1. Implement `VideoFrameProcessor`:

```java
package com.myapp.effects;

import com.oney.WebRTCModule.videoEffects.VideoFrameProcessor;
import org.webrtc.VideoFrame;
import org.webrtc.SurfaceTextureHelper;

public class MyEffectProcessor implements VideoFrameProcessor {
    @Override
    public VideoFrame process(VideoFrame frame, SurfaceTextureHelper helper) {
        frame.retain();
        return frame;
    }
}
```

2. Implement `VideoFrameProcessorFactoryInterface`:

```java
package com.myapp.effects;

import com.oney.WebRTCModule.videoEffects.VideoFrameProcessorFactoryInterface;
import com.oney.WebRTCModule.videoEffects.VideoFrameProcessor;

public class MyEffectFactory implements VideoFrameProcessorFactoryInterface {
    @Override
    public VideoFrameProcessor build() {
        return new MyEffectProcessor();
    }
}
```

3. Register via a `ContentProvider` (auto-runs at app startup):

```java
package com.myapp.effects;

import android.content.ContentProvider;
import com.oney.WebRTCModule.videoEffects.ProcessorProvider;

public class MyEffectRegistrar extends ContentProvider {
    @Override
    public boolean onCreate() {
        ProcessorProvider.addProcessor("myEffect", new MyEffectFactory());
        return false;
    }
    // ... required empty overrides for query/insert/update/delete/getType
}
```

4. Declare in `AndroidManifest.xml`:

```xml
<provider
    android:name="com.myapp.effects.MyEffectRegistrar"
    android:authorities="${applicationId}.my-effect-registrar"
    android:exported="false" />
```

### iOS

1. Implement `VideoFrameProcessorDelegate`:

```objc
// MyEffectProcessor.h
#import <WebRTC/RTCVideoCapturer.h>
#import <WebRTC/RTCVideoFrame.h>
#import "VideoFrameProcessor.h"

@interface MyEffectProcessor : NSObject <VideoFrameProcessorDelegate>
@end

// MyEffectProcessor.m
@implementation MyEffectProcessor
- (RTCVideoFrame *)capturer:(RTCVideoCapturer *)capturer
       didCaptureVideoFrame:(RTCVideoFrame *)frame {
    return frame;
}
@end
```

2. Register via `+load` (auto-runs when the class is loaded):

```objc
// MyEffectRegistrar.m
#import "ProcessorProvider.h"
#import "MyEffectProcessor.h"

@implementation MyEffectRegistrar
+ (void)load {
    MyEffectProcessor *processor = [[MyEffectProcessor alloc] init];
    [ProcessorProvider addProcessor:processor forName:@"myEffect"];
}
@end
```

Then use from TypeScript:

```typescript
track._setVideoEffect('myEffect');
```

## License

MIT
