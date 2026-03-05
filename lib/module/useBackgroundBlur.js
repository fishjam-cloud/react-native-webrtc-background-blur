"use strict";

import { useCamera } from '@fishjam-cloud/react-native-client';
import { useCallback } from 'react';
import NativeBackgroundBlur from "./NativeBackgroundBlur.js";
export function useBackgroundBlur(options = {}) {
  const camera = useCamera();
  const blurMiddleware = useCallback(track => {
    NativeBackgroundBlur.initialize();
    if (options.blurRadius !== undefined) {
      NativeBackgroundBlur.setBlurRadius(options.blurRadius);
    }
    const nativeTrack = track;
    nativeTrack._setVideoEffect('backgroundBlur');
    return {
      track,
      onClear: () => {
        nativeTrack._setVideoEffects([]);
        NativeBackgroundBlur.deinitialize();
      }
    };
  }, [options.blurRadius]);
  const isBlurEnabled = camera.currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () => camera.setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);
  return {
    toggleBlur,
    isBlurEnabled
  };
}
//# sourceMappingURL=useBackgroundBlur.js.map