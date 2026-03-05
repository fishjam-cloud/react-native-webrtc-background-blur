"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.initializeBackgroundBlur = exports.deinitializeBackgroundBlur = void 0;
var _NativeBackgroundBlur = _interopRequireDefault(require("./NativeBackgroundBlur.js"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
const initializeBackgroundBlur = () => {
  _NativeBackgroundBlur.default.initialize();
};
exports.initializeBackgroundBlur = initializeBackgroundBlur;
const deinitializeBackgroundBlur = () => {
  _NativeBackgroundBlur.default.deinitialize();
};
exports.deinitializeBackgroundBlur = deinitializeBackgroundBlur;
//# sourceMappingURL=BackgroundBlur.js.map