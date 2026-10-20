import Lenis from 'lenis'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

/**
 * 电影级动效系统（来自 cinematic-gsap-lenis-motion-system skill）：
 * - Lenis 由 GSAP ticker 驱动，与 ScrollTrigger 保持帧同步
 * - data-magnetic：磁吸按钮（CTA 用）
 * - data-scrub-fade：元素随滚动淡入上浮（scrub 1）
 * - data-scrub-words：大字按词随滚动显影（宣言/巨型页脚字用）
 * 全部尊重 prefers-reduced-motion；initMotion 幂等。
 */

gsap.registerPlugin(ScrollTrigger)
gsap.defaults({ ease: 'power3.out', duration: 0.85 })

export const reducedMotion = () =>
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

let lenis: Lenis | null = null
export function getLenis() {
  return lenis
}

export function initMotion(): void {
  if (reducedMotion()) return

  if (!lenis) {
    lenis = new Lenis({
      lerp: 0.09,
      smoothWheel: true,
      wheelMultiplier: 0.92,
      anchors: true,
    })
    lenis.on('scroll', ScrollTrigger.update)
    gsap.ticker.add((time) => {
      lenis!.raf(time * 1000)
    })
    gsap.ticker.lagSmoothing(0)
  }

  // 磁吸按钮：data-magnetic，光标靠近时被"吸"过去（motionsites/Lusion 标配）
  document.querySelectorAll<HTMLElement>('[data-magnetic]').forEach((el) => {
    if ((el as any)._magnetic) return
    ;(el as any)._magnetic = true
    const strength = 0.35
    const xTo = gsap.quickTo(el, 'x', { duration: 0.4, ease: 'power3.out' })
    const yTo = gsap.quickTo(el, 'y', { duration: 0.4, ease: 'power3.out' })
    el.addEventListener('mousemove', (e: MouseEvent) => {
      const r = el.getBoundingClientRect()
      xTo((e.clientX - (r.left + r.width / 2)) * strength)
      yTo((e.clientY - (r.top + r.height / 2)) * strength)
    })
    el.addEventListener('mouseleave', () => {
      xTo(0)
      yTo(0)
    })
  })

  // 滚动淡入：data-scrub-fade（比 v-reveal 更电影化的 scrub 版本，用于区块级）
  document.querySelectorAll<HTMLElement>('[data-scrub-fade]').forEach((el) => {
    if ((el as any)._scrubFade) return
    ;(el as any)._scrubFade = true
    gsap.fromTo(
      el,
      { opacity: 0, y: 36 },
      {
        opacity: 1,
        y: 0,
        scrollTrigger: { trigger: el, start: 'top 85%', scrub: 1 },
      },
    )
  })

  // 大字滚动显影：data-scrub-words——按词拆分，随滚动逐词从雾里亮起
  document.querySelectorAll<HTMLElement>('[data-scrub-words]').forEach((el) => {
    if ((el as any)._scrubWords) return
    ;(el as any)._scrubWords = true
    const text = el.textContent || ''
    const words = text.split(/(\s+)/).filter(Boolean)
    el.textContent = ''
    const spans = words.map((word) => {
      const span = document.createElement('span')
      span.textContent = word
      span.style.display = 'inline-block'
      span.style.willChange = 'opacity, filter'
      el.appendChild(span)
      return span
    })
    gsap.fromTo(
      spans,
      { opacity: 0.08, filter: 'blur(6px)' },
      {
        opacity: 1,
        filter: 'blur(0px)',
        stagger: 0.08,
        ease: 'none',
        scrollTrigger: { trigger: el, start: 'top 88%', end: 'top 30%', scrub: 1 },
      },
    )
  })

  // 磁吸范围刷新交给调用方在数据加载后调 ScrollTrigger.refresh()
}

export function refreshMotion(): void {
  if (!reducedMotion()) ScrollTrigger.refresh()
}
