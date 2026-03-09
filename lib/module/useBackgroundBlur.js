"use strict";

import { useCamera } from "@fishjam-cloud/react-native-client";
import { useCallback, useRef } from "react";
import { Platform } from "react-native";
import NativeBackgroundBlur from "./NativeBackgroundBlur.js";
NativeBackgroundBlur.initialize();
export function useBackgroundBlur(options = {}) {
  const {
    setCameraTrackMiddleware,
    currentCameraMiddleware
  } = useCamera();
  const blurRadiusRef = useRef(options.blurRadius);
  blurRadiusRef.current = options.blurRadius;
  const blurMiddleware = useCallback(track => {
    if (blurRadiusRef.current !== undefined) {
      NativeBackgroundBlur.setBlurRadius(blurRadiusRef.current);
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
  }, []);
  const isBlurEnabled = currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () => setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);
  const disableBlur = useCallback(() => {
    if (currentCameraMiddleware === blurMiddleware) {
      return setCameraTrackMiddleware(null);
    }
    return Promise.resolve();
  }, [setCameraTrackMiddleware, blurMiddleware, currentCameraMiddleware]);
  const enableBlur = useCallback(() => {
    return setCameraTrackMiddleware(blurMiddleware);
  }, [setCameraTrackMiddleware, blurMiddleware]);
  return {
    toggleBlur,
    isBlurEnabled,
    disableBlur,
    enableBlur
  };
}
//# sourceMappingURL=useBackgroundBlur.js.map