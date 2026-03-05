"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.useBackgroundBlur = useBackgroundBlur;
var _reactNativeClient = require("@fishjam-cloud/react-native-client");
var _react = require("react");
var _NativeBackgroundBlur = _interopRequireDefault(require("./NativeBackgroundBlur.js"));
var _reactNative = require("react-native");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
_NativeBackgroundBlur.default.initialize();
function useBackgroundBlur(options = {}) {
  const {
    setCameraTrackMiddleware,
    currentCameraMiddleware
  } = (0, _reactNativeClient.useCamera)();
  const blurMiddleware = (0, _react.useCallback)(track => {
    if (options.blurRadius !== undefined) {
      _NativeBackgroundBlur.default.setBlurRadius(options.blurRadius);
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
  }, [options.blurRadius]);
  const isBlurEnabled = currentCameraMiddleware === blurMiddleware;
  const toggleBlur = () => setCameraTrackMiddleware(isBlurEnabled ? null : blurMiddleware);
  return {
    toggleBlur,
    isBlurEnabled
  };
}
//# sourceMappingURL=useBackgroundBlur.js.map