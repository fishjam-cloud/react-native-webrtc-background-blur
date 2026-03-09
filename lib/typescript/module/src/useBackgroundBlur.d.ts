type UseBackgroundBlurOptions = {
    blurRadius?: number;
};
export declare function useBackgroundBlur(options?: UseBackgroundBlurOptions): {
    blurMiddleware: (track: MediaStreamTrack) => import("@fishjam-cloud/react-client").MiddlewareResult | Promise<import("@fishjam-cloud/react-client").MiddlewareResult>;
};
export {};
//# sourceMappingURL=useBackgroundBlur.d.ts.map