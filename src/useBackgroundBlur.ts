import {
  type TrackMiddleware,
  useCamera,
} from "@fishjam-cloud/react-native-client";
import { useCallback, useRef } from "react";
import { Platform } from "react-native";
import NativeBackgroundBlur from "./NativeBackgroundBlur";

type NativeMediaStreamTrack = MediaStreamTrack & {
  _setVideoEffect: (name: string) => void;
  _setVideoEffects: (names: string[]) => void;
};

type UseBackgroundBlurOptions = {
  blurRadius?: number;
};

NativeBackgroundBlur.initialize();

export function useBackgroundBlur(options: UseBackgroundBlurOptions = {}) {
  const { setCameraTrackMiddleware, currentCameraMiddleware } = useCamera();
  const blurRadiusRef = useRef(options.blurRadius);
  blurRadiusRef.current = options.blurRadius;

  const blurMiddleware: TrackMiddleware = useCallback(
    (track: MediaStreamTrack) => {
      if (blurRadiusRef.current !== undefined) {
        NativeBackgroundBlur.setBlurRadius(blurRadiusRef.current);
      }
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
    [],
  );

  const isBlurEnabled = currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () =>
    setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);

  const disableBlur = useCallback(() => {
    if (currentCameraMiddleware === blurMiddleware) {
      return setCameraTrackMiddleware(null);
    }

    return Promise.resolve();
  }, [setCameraTrackMiddleware, blurMiddleware, currentCameraMiddleware]);

  const enableBlur = useCallback(() => {
    return setCameraTrackMiddleware(blurMiddleware);
  }, [setCameraTrackMiddleware, blurMiddleware]);

  return { toggleBlur, isBlurEnabled, disableBlur, enableBlur };
}
