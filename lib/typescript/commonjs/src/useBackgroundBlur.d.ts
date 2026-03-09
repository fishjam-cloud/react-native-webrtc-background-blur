type UseBackgroundBlurOptions = {
    blurRadius?: number;
};
export declare function useBackgroundBlur(options?: UseBackgroundBlurOptions): {
    toggleBlur: () => Promise<void>;
    isBlurEnabled: boolean;
    disableBlur: () => Promise<void>;
    enableBlur: () => Promise<void>;
};
export {};
//# sourceMappingURL=useBackgroundBlur.d.ts.map