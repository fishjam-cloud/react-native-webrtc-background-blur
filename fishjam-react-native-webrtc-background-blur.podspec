require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = 'fishjam-react-native-webrtc-background-blur'
  s.version      = package['version']
  s.summary      = package['description']
  s.homepage     = 'https://github.com/fishjam-cloud/react-native-webrtc-background-blur'
  s.license      = 'MIT'
  s.author       = { 'Fishjam Cloud' => 'https://github.com/fishjam-cloud' }
  s.source       = { :git => 'https://github.com/fishjam-cloud/react-native-webrtc-background-blur.git', :tag => s.version.to_s }

  s.platforms    = { :ios => '15.0' }

  s.source_files = 'ios/**/*.{h,m,metal}'
  s.frameworks   = 'Metal', 'MetalPerformanceShaders', 'Vision', 'CoreVideo'

  s.resource_bundles = {
    'BackgroundBlurShaders' => ['ios/CocoaPodsBundledResourcePlaceholder']
  }

  s.pod_target_xcconfig = {
    'METAL_LIBRARY_OUTPUT_DIR' => '${TARGET_BUILD_DIR}/BackgroundBlurShaders.bundle/'
  }

  s.dependency 'FishjamReactNativeWebrtc'
end
