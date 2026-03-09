"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.useBackgroundBlur = useBackgroundBlur;
var _reactNativeClient = require("@fishjam-cloud/react-native-client");
var _react = require("react");
var _reactNative = require("react-native");
var _NativeBackgroundBlur = _interopRequireDefault(require("./NativeBackgroundBlur.js"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
_NativeBackgroundBlur.default.initialize();
function useBackgroundBlur(options = {}) {
  const {
    setCameraTrackMiddleware,
    currentCameraMiddleware
  } = (0, _reactNativeClient.useCamera)();
  const blurRadiusRef = (0, _react.useRef)(options.blurRadius);
  blurRadiusRef.current = options.blurRadius;
  const blurMiddleware = (0, _react.useCallback)(track => {
    if (blurRadiusRef.current !== undefined) {
      _NativeBackgroundBlur.default.setBlurRadius(blurRadiusRef.current);
    }
    const nativeTrack = track;
    nativeTrack._setVideoEffect("backgroundBlur");
    return {
      track,
      onClear: () => {
        nativeTrack._setVideoEffects(
        // Internally Android expects null, to actually disable the effect. An empty array would cause the app to crash.
        _reactNative.Platform.OS === "ios" ? [] : null);
      }
    };
  }, []);
  const isBlurEnabled = currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () => setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);
  const disableBlur = (0, _react.useCallback)(() => {
    if (currentCameraMiddleware === blurMiddleware) {
      return setCameraTrackMiddleware(null);
    }
    return Promise.resolve();
  }, [setCameraTrackMiddleware, blurMiddleware, currentCameraMiddleware]);
  const enableBlur = (0, _react.useCallback)(() => {
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