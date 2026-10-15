import { defineStore } from 'pinia'
import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import { favoriteWork, likeWork, unfavoriteWork, unlikeWork } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

export interface WorkInteractionState {
  liked: boolean
  favorited: boolean
  likeCount: number
  favoriteCount: number
  pending: boolean
}

/**
 * 作品互动状态（点赞 / 收藏）的单一数据源。
 *
 * 列表卡片和详情页共享同一份状态，避免"卡片显示未点赞、点进去已点赞"的不一致；
 * 所有写操作都先乐观更新，失败再回滚。
 */
export const useWorkInteractionStore = defineStore('workInteraction', () => {
  const states = reactive<Record<string, WorkInteractionState>>({})

  function key(work: any) {
    return String(work?.id ?? work?.workId ?? '')
  }

  /** 获取（必要时初始化）某个作品的互动状态 */
  function ensure(work: any): WorkInteractionState {
    const id = key(work)
    if (!states[id]) {
      states[id] = {
        liked: !!work?.liked,
        favorited: !!work?.favorited,
        likeCount: Number(work?.likeCount ?? 0),
        favoriteCount: Number(work?.favoriteCount ?? 0),
        pending: false,
      }
    }
    return states[id]
  }

  /** 详情页加载后，用服务端的权威数据覆盖本地状态 */
  function sync(work: any) {
    const id = key(work)
    if (!id) return
    states[id] = {
      liked: !!work?.liked,
      favorited: !!work?.favorited,
      likeCount: Number(work?.likeCount ?? 0),
      favoriteCount: Number(work?.favoriteCount ?? 0),
      pending: false,
    }
  }

  function requireLogin() {
    const loginUserStore = useLoginUserStore()
    if (!loginUserStore.loginUser?.id) {
      message.warning('请先登录后再互动')
      return false
    }
    return true
  }

  async function toggleLike(work: any) {
    if (!requireLogin()) return
    const st = ensure(work)
    if (st.pending) return
    st.pending = true
    const next = !st.liked
    // 乐观更新
    st.liked = next
    st.likeCount = Math.max(0, st.likeCount + (next ? 1 : -1))
    try {
      const res: any = next ? await likeWork({ workId: key(work) }) : await unlikeWork({ workId: key(work) })
      if (res.data.code !== 0) throw new Error(res.data.message || '操作失败')
    } catch (e: any) {
      // 回滚
      st.liked = !next
      st.likeCount = Math.max(0, st.likeCount + (next ? -1 : 1))
      message.error(e?.message || '操作失败，请稍后重试')
    } finally {
      st.pending = false
    }
  }

  async function toggleFavorite(work: any) {
    if (!requireLogin()) return
    const st = ensure(work)
    if (st.pending) return
    st.pending = true
    const next = !st.favorited
    st.favorited = next
    st.favoriteCount = Math.max(0, st.favoriteCount + (next ? 1 : -1))
    try {
      const res: any = next ? await favoriteWork({ workId: key(work) }) : await unfavoriteWork({ workId: key(work) })
      if (res.data.code !== 0) throw new Error(res.data.message || '操作失败')
      message.success(next ? '已收藏' : '已取消收藏')
    } catch (e: any) {
      st.favorited = !next
      st.favoriteCount = Math.max(0, st.favoriteCount + (next ? -1 : 1))
      message.error(e?.message || '操作失败，请稍后重试')
    } finally {
      st.pending = false
    }
  }

  return { states, ensure, sync, toggleLike, toggleFavorite }
})
