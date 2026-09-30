import { useUserStore } from '@/store/user'

export const auth = (el, binding) => {
  const userStore = useUserStore()
  if (!el) return
  if (userStore.buttonAuths?.includes(binding.value) || userStore.buttonAuths?.includes('*')) {
    el.style.display = ''
  } else {
    el.style.display = 'none'
  }
}

export const hasAuth = (authName) => {
  const userStore = useUserStore()
  if (!userStore.buttonAuths) return false
  return userStore.buttonAuths.includes('*') || userStore.buttonAuths.includes(authName)
}
