"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.useBackgroundBlur = useBackgroundBlur;
var _reactNativeClient = require("@fishjam-cloud/react-native-client");
var _react = require("react");
var _NativeBackgroundBlur = _interopRequireDefault(require("./NativeBackgroundBlur.js"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
function useBackgroundBlur(options = {}) {
  const camera = (0, _reactNativeClient.useCamera)();
  const blurMiddleware = (0, _react.useCallback)(track => {
    if (options.blurRadius !== undefined) {
      _NativeBackgroundBlur.default.setBlurRadius(options.blurRadius);
    }
    const nativeTrack = track;
    nativeTrack._setVideoEffect('backgroundBlur');
    return {
      track,
      onClear: () => {
        nativeTrack._setVideoEffects([]);
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