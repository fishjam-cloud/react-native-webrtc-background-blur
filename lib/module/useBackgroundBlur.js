"use strict";

import { useCamera } from "@fishjam-cloud/react-native-client";
import { useCallback } from "react";
import NativeBackgroundBlur from "./NativeBackgroundBlur.js";
import { Platform } from "react-native";
NativeBackgroundBlur.initialize();
export function useBackgroundBlur(options = {}) {
  const {
    setCameraTrackMiddleware,
    currentCameraMiddleware
  } = useCamera();
  const blurMiddleware = useCallback(track => {
    if (options.blurRadius !== undefined) {
      NativeBackgroundBlur.setBlurRadius(options.blurRadius);
    }
    const nativeTrack = track;
    nativeTrack._setVideoEffect("backgroundBlur");
    return {
      track,
      onClear: () => {
        nativeTrack._setVideoEffects(
        // Internally Android expects null, to actually disable the effect. An empty array would cause the app to crash.
        Platform.OS === "ios" ? [] : null);
      }
    };
  }, [options.blurRadius]);
  const isBlurEnabled = currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () => setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);
  return {
    toggleBlur,
    isBlurEnabled
  };
}
//# sourceMappingURL=useBackgroundBlur.js.map