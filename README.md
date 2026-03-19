# @fishjam-cloud/react-native-webrtc-background-blur

Real-time background blur video effect for [`@fishjam-cloud/react-native-webrtc`](https://github.com/nickhope/react-native-webrtc).

Uses on-device person segmentation — **Vision** (iOS) and **ML Kit** (Android) — to separate the subject from the background, then applies a GPU-accelerated Gaussian blur.

## Requirements

| Platform     | Minimum version |
| ------------ | --------------- |
| iOS          | 15.0            |
| Android      | SDK 24          |
| React Native | 0.74+           |

Supports both old and new React Native architecture.

Peer dependencies:

- `@fishjam-cloud/react-native-client`
- `@fishjam-cloud/react-native-webrtc`

## Installation

```sh
npm install @fishjam-cloud/react-native-webrtc-background-blur
```

or

```sh
yarn add @fishjam-cloud/react-native-webrtc-background-blur
```

Then install iOS pods:

```sh
cd ios && pod install
```

The package is published on npm: [@fishjam-cloud/react-native-webrtc-background-blur](https://www.npmjs.com/package/@fishjam-cloud/react-native-webrtc-background-blur)

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

Changing `blurRadius` takes effect immediately — the hook updates the native blur radius via a `useEffect` whenever the value changes.

#### Options

| Option       | Type     | Default | Description                                                 |
| ------------ | -------- | ------- | ----------------------------------------------------------- |
| `blurRadius` | `number` | `15`    | Gaussian blur sigma. Higher values produce a stronger blur. |

#### Return value

| Property         | Type              | Description                                       |
| ---------------- | ----------------- | ------------------------------------------------- |
| `blurMiddleware` | `TrackMiddleware` | Middleware to pass to `setCameraTrackMiddleware`. |

### `NativeBackgroundBlur`

Low-level native module for direct access:

```ts
import { NativeBackgroundBlur } from "@fishjam-cloud/react-native-webrtc-background-blur";

NativeBackgroundBlur.initialize();
NativeBackgroundBlur.setBlurRadius(20);

const available: boolean = NativeBackgroundBlur.isAvailable();

NativeBackgroundBlur.deinitialize();
```

#### Methods

| Method                  | Description                                    |
| ----------------------- | ---------------------------------------------- |
| `initialize()`          | Initializes the native blur engine.            |
| `deinitialize()`        | Releases native resources.                     |
| `setBlurRadius(radius)` | Sets the Gaussian blur sigma.                  |
| `isAvailable()`         | Returns `true` if blur is supported on device. |

## License

MIT
