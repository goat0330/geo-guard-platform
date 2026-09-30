import http from '@/utils/http.js'

// 代理前缀 /tts 会被 Vite/Nginx 代理重写剥离，最终请求目标服务的 POST /tts
const TTS_URL = '/tts/tts'

/**
 * 请求一段文本对应的 WAV 音频数据。
 * @param {object} options - 语音合成参数。
 * @param {string} options.text - 需要合成的文本。
 * @param {string} [options.voiceId='man'] - 音色 ID（支持 'man'、'women' 或已注册的声音）。
 * @param {number} [options.maxTokens=120] - 单句最大 token 数（范围 1~512）。
 * @param {AbortSignal} [options.signal] - 请求取消信号。
 * @returns {Promise<ArrayBuffer>} 完整的 WAV 音频数据。
 */
export async function synthesizeTts({ text, voiceId = 'man', maxTokens = 120, signal } = {}) {
  const audioData = await http.post(
    TTS_URL,
    {
      text,
      voice_id: voiceId,
      max_text_tokens_per_sentence: maxTokens,
    },
    {
      // http 实例默认前缀为 /api，TTS 使用独立的同源代理路径。
      baseURL: '/',
      responseType: 'arraybuffer',
      noCheckCode: true,
      throwRes: true,
      notUseError: true,
      signal,
    },
  )

  // http 的 throwRes 模式会将请求异常作为返回值交给调用方。
  if (audioData instanceof Error) {
    throw audioData
  }
  if (!(audioData instanceof ArrayBuffer) || !audioData.byteLength) {
    throw new Error('TTS 返回的音频数据为空或格式不正确')
  }

  return audioData
}
