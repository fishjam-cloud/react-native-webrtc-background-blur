import { type TrackMiddleware, useCamera } from '@fishjam-cloud/react-native-client';
import { useCallback } from 'react';
import NativeBackgroundBlur from './NativeBackgroundBlur';

type NativeMediaStreamTrack = MediaStreamTrack & {
  _setVideoEffect: (name: string) => void;
  _setVideoEffects: (names: string[]) => void;
};

type UseBackgroundBlurOptions = {
  blurRadius?: number;
};

export function useBackgroundBlur(options: UseBackgroundBlurOptions = {}) {
  const camera = useCamera();

  const blurMiddleware: TrackMiddleware = useCallback(
    (track: MediaStreamTrack) => {
      NativeBackgroundBlur.initialize();
      if (options.blurRadius !== undefined) {
        NativeBackgroundBlur.setBlurRadius(options.blurRadius);
      }
      const nativeTrack = track as NativeMediaStreamTrack;
      nativeTrack._setVideoEffect('backgroundBlur');
      return {
        track,
        onClear: () => {
          nativeTrack._setVideoEffects([]);
          NativeBackgroundBlur.deinitialize();
        },
      };
    },
    [options.blurRadius],
  );

  const isBlurEnabled = camera.currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () =>
    camera.setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);

  return { toggleBlur, isBlurEnabled };
}
