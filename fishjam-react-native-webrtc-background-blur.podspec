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
  s.framework    = 'Metal', 'MetalPerformanceShaders', 'Vision', 'CoreVideo'
  s.dependency   'FishjamReactNativeWebrtc'
end
