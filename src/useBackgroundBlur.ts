import { type TrackMiddleware, useCamera } from '@fishjam-cloud/react-native-client';
import { useCallback } from 'react';

type NativeMediaStreamTrack = MediaStreamTrack & {
  _setVideoEffect: (name: string) => void;
  _setVideoEffects: (names: string[]) => void;
};

export function useBackgroundBlur() {
  const camera = useCamera();

  const blurMiddleware: TrackMiddleware = useCallback((track: MediaStreamTrack) => {
    const nativeTrack = track as NativeMediaStreamTrack;
    nativeTrack._setVideoEffect('backgroundBlur');
    return {
      track,
      onClear: () => {
        nativeTrack._setVideoEffects([]);
      },
    };
  }, []);

  const isBlurEnabled = camera.currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () => camera.setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);

  return { toggleBlur, isBlurEnabled };
}
