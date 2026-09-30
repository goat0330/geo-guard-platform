import * as Cesium from 'mars3d-cesium'

Cesium.Ion.defaultAccessToken = import.meta.env.VITE_CESIUM_ION_TOKEN?.trim() || ''
