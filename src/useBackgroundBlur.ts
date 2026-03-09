import { type TrackMiddleware } from "@fishjam-cloud/react-native-client";
import { useCallback, useEffect } from "react";
import { Platform } from "react-native";
import NativeBackgroundBlur from "./NativeBackgroundBlur";

type NativeMediaStreamTrack = MediaStreamTrack & {
  _setVideoEffect: (name: string) => void;
  _setVideoEffects: (names: string[]) => void;
};

type UseBackgroundBlurOptions = {
  blurRadius: number;
};

NativeBackgroundBlur.initialize();

export function useBackgroundBlur(
  options: UseBackgroundBlurOptions = { blurRadius: 15 },
) {
  useEffect(() => {
    NativeBackgroundBlur.setBlurRadius(options.blurRadius);
  }, [options.blurRadius]);

  const blurMiddleware: TrackMiddleware = useCallback(
    (track: MediaStreamTrack) => {
      const nativeTrack = track as NativeMediaStreamTrack;
      nativeTrack._setVideoEffect("backgroundBlur");
      return {
        track,
        onClear: () => {
          nativeTrack._setVideoEffects(
            // Internally Android expects null, to actually disable the effect. An empty array would cause the app to crash.
            Platform.OS === "ios" ? [] : (null as any),
          );
        },
      };
    },
    [options.blurRadius],
  );

  return { blurMiddleware };
}
