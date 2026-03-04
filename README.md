# @fishjam-cloud/react-native-webrtc-background-blur

Real-time background blur video effect for [`@fishjam-cloud/react-native-webrtc`](https://github.com/nickhope/react-native-webrtc).

Uses on-device person segmentation — **Vision** (iOS) and **ML Kit** (Android) — to separate the subject from the background, then applies a GPU-accelerated Gaussian blur.

## Requirements

| Platform | Minimum version |
| -------- | --------------- |
| iOS      | 15.0            |
| Android  | SDK 24          |
| React Native | 0.74+       |

Peer dependencies:

- `@fishjam-cloud/react-native-client`
- `@fishjam-cloud/react-native-webrtc`

## Installation

The package is not yet published to npm. Install directly from GitHub:

```sh
# npm
npm install https://github.com/fishjam-cloud/react-native-webrtc-background-blur

# yarn
yarn add https://github.com/fishjam-cloud/react-native-webrtc-background-blur
```

Then install iOS pods:

```sh
cd ios && pod install
```

## Usage

### `useBackgroundBlur`

A React hook that integrates with `@fishjam-cloud/react-native-client`'s camera middleware system.

```tsx
import { useBackgroundBlur } from '@fishjam-cloud/react-native-webrtc-background-blur';

function CallScreen() {
  const { toggleBlur, isBlurEnabled } = useBackgroundBlur({ blurRadius: 15 });

  return (
    <Button
      title={isBlurEnabled ? 'Disable Blur' : 'Enable Blur'}
      onPress={toggleBlur}
    />
  );
}
```

#### Options

| Option       | Type     | Default     | Description                        |
| ------------ | -------- | ----------- | ---------------------------------- |
| `blurRadius` | `number` | `undefined` | Gaussian blur sigma. Higher values produce a stronger blur. |

#### Return value

| Property       | Type         | Description                            |
| -------------- | ------------ | -------------------------------------- |
| `toggleBlur`   | `() => void` | Toggles background blur on/off.        |
| `isBlurEnabled`| `boolean`    | Whether blur is currently active.      |

### `NativeBackgroundBlur`

Low-level native module for direct access:

```ts
import { NativeBackgroundBlur } from '@fishjam-cloud/react-native-webrtc-background-blur';

NativeBackgroundBlur.setBlurRadius(20);

const available: boolean = NativeBackgroundBlur.isAvailable();
```

## License

MIT
