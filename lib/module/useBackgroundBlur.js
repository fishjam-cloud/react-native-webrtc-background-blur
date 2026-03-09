"use strict";

import { useCallback } from "react";
import { Platform } from "react-native";
import NativeBackgroundBlur from "./NativeBackgroundBlur.js";
NativeBackgroundBlur.initialize();
export function useBackgroundBlur(options = {}) {
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
  return {
    blurMiddleware
  };
}
//# sourceMappingURL=useBackgroundBlur.js.map