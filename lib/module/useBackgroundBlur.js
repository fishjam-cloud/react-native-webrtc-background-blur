"use strict";

import { useCallback, useEffect } from "react";
import { Platform } from "react-native";
import NativeBackgroundBlur from "./NativeBackgroundBlur.js";
NativeBackgroundBlur.initialize();
export function useBackgroundBlur(options = {
  blurRadius: 15
}) {
  useEffect(() => {
    NativeBackgroundBlur.setBlurRadius(options.blurRadius);
  }, [options.blurRadius]);
  const blurMiddleware = useCallback(track => {
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
  return {
    blurMiddleware
  };
}
//# sourceMappingURL=useBackgroundBlur.js.map