# @fishjam-cloud/react-native-webrtc-background-blur

Real-time background blur video effect for [`@fishjam-cloud/react-native-webrtc`](https://github.com/nickhope/react-native-webrtc).

Uses on-device person segmentation — **Vision** (iOS) and **ML Kit** (Android) — to separate the subject from the background, then applies a GPU-accelerated Gaussian blur.

## Requirements

| Platform     | Minimum version |
| ------------ | --------------- |
| iOS          | 15.0            |
| Android      | SDK 24          |
| React Native | 0.74+           |

Peer dependencies:

- `@fishjam-cloud/react-native-client`
- `@fishjam-cloud/react-native-webrtc`

## Installation

The package is not yet published to npm. Install directly from GitHub:

```sh
npm install @fishjam-cloud/react-native-webrtc-background-blur@github:fishjam-cloud/react-native-webrtc-background-blur
```

Or add it manually to your `package.json`:

```json
{
  "dependencies": {
    "@fishjam-cloud/react-native-webrtc-background-blur": "github:fishjam-cloud/react-native-webrtc-background-blur"
  }
}
```

Then run `npm install` or `yarn install`.

Then install iOS pods:

```sh
cd ios && pod install
```

## Usage

### `useBackgroundBlur`

A React hook that returns a camera middleware for background blur. You apply it yourself via `setCameraTrackMiddleware` from `useCamera`, which means you can combine it with other middlewares however you like.

```tsx
import { useCamera } from "@fishjam-cloud/react-native-client";
import { useBackgroundBlur } from "@fishjam-cloud/react-native-webrtc-background-blur";

function CallScreen() {
  const { setCameraTrackMiddleware, currentCameraMiddleware } = useCamera();
  const { blurMiddleware } = useBackgroundBlur({ blurRadius: 15 });

  const isBlurEnabled = currentCameraMiddleware === blurMiddleware;

  const toggleBlur = () =>
    setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);

  return (
    <Button
      title={isBlurEnabled ? "Disable Blur" : "Enable Blur"}
      onPress={toggleBlur}
    />
  );
}
```

If you change `blurRadius` while blur is active, the new value takes effect the next time `setCameraTrackMiddleware(blurMiddleware)` is called with the updated middleware reference.

#### Options

| Option       | Type     | Default     | Description                                                 |
| ------------ | -------- | ----------- | ----------------------------------------------------------- |
| `blurRadius` | `number` | `undefined` | Gaussian blur sigma. Higher values produce a stronger blur. |

#### Return value

| Property         | Type              | Description                                       |
| ---------------- | ----------------- | ------------------------------------------------- |
| `blurMiddleware` | `TrackMiddleware` | Middleware to pass to `setCameraTrackMiddleware`. |

### `NativeBackgroundBlur`

Low-level native module for direct access:

```ts
import { NativeBackgroundBlur } from "@fishjam-cloud/react-native-webrtc-background-blur";

NativeBackgroundBlur.setBlurRadius(20);

const available: boolean = NativeBackgroundBlur.isAvailable();
```

## License

MIT
