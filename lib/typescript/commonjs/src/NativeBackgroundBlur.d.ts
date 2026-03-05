import type { TurboModule } from 'react-native';
export interface Spec extends TurboModule {
    initialize(): void;
    deinitialize(): void;
    setBlurRadius(radius: number): void;
    isAvailable(): boolean;
}
declare const _default: Spec;
export default _default;
//# sourceMappingURL=NativeBackgroundBlur.d.ts.map