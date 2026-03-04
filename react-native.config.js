module.exports = {
  dependency: {
    platforms: {
      android: {
        packageImportPath: 'import com.fishjam.blur.BackgroundBlurPackage;',
        packageInstance: 'new BackgroundBlurPackage()',
      },
      ios: {
        podspecPath: './fishjam-react-native-webrtc-background-blur.podspec',
      },
    },
  },
};
