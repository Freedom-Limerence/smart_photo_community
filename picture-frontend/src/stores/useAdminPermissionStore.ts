import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getRbacMy } from '@/api/adminConsole'
import { ADMIN_MENU } from '@/config/adminMenu'

/**
 * 后台权限：角色 + 权限点，来源 /admin/rbac/my。
 * 菜单可见性、路由守卫、落地页都由它决定。
 */
export const useAdminPermissionStore = defineStore('adminPermission', () => {
  const loading = ref(false)
  const loaded = ref(false)
  const roleCode = ref('')
  const permCodes = ref<string[]>([])
  let loadedAt = 0

  const isSuperAdmin = computed(() => permCodes.value.includes('rbac:manage'))
  const canEnterAdmin = computed(() => permCodes.value.length > 0)

  /** 是否拥有某权限点 */
  function has(perm?: string) {
    if (!perm) return true
    return permCodes.value.includes(perm)
  }

  /** 可见菜单 */
  const visibleMenu = computed(() => ADMIN_MENU.filter((item) => has(item.permission)))

  /** 有权限时进入后台的落地页 */
  function firstAccessiblePath(): string {
    return visibleMenu.value[0]?.key || '/'
  }

  /** 拉取权限（force=false 时命中缓存不再请求） */
  async function load(force = false) {
    const stale = Date.now() - loadedAt > 60_000
    if (loaded.value && !force && !stale) return
    loading.value = true
    try {
      const res: any = await getRbacMy()
      if (res.data.code === 0) {
        roleCode.value = res.data.data?.roleCode || ''
        permCodes.value = res.data.data?.permCodes || []
      } else {
        roleCode.value = ''
        permCodes.value = []
      }
      loaded.value = true
      loadedAt = Date.now()
    } catch {
      roleCode.value = ''
      permCodes.value = []
    } finally {
      loading.value = false
    }
  }

  function reset() {
    loaded.value = false
    roleCode.value = ''
    permCodes.value = []
  }

  return { loading, loaded, roleCode, permCodes, isSuperAdmin, canEnterAdmin, visibleMenu, has, load, reset, firstAccessiblePath }
})
