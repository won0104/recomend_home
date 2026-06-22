const KAKAO_APP_KEY = 'b971f44d602a58e2c609026645b47a00'
const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 }

let loadingPromise = null

export function loadKakaoMap() {
  if (window.kakao?.maps) {
    return Promise.resolve(window.kakao)
  }
  if (loadingPromise) return loadingPromise

  loadingPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_APP_KEY}&libraries=services&autoload=false`
    script.async = true
    script.onload = () => {
      if (!window.kakao?.maps) {
        reject(new Error('Kakao Map SDK is unavailable.'))
        return
      }
      window.kakao.maps.load(() => resolve(window.kakao))
    }
    script.onerror = () => reject(new Error('Failed to load Kakao Map SDK.'))
    document.head.appendChild(script)
  })

  return loadingPromise
}

export function defaultCenter() {
  return DEFAULT_CENTER
}
