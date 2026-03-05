import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  initialize(): void;
  deinitialize(): void;
  setBlurRadius(radius: number): void;
  isAvailable(): boolean;
}

export default TurboModuleRegistry.getEnforcing<Spec>('BackgroundBlur');
