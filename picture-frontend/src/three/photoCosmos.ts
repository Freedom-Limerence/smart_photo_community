import * as THREE from 'three'
import { toProxyUrl } from '@/utils'

/**
 * 三维照片星环（着陆页标志性 3D 场景）
 * - 12 张真实作品漂浮成双层星环，缓慢自转，镜头随鼠标轻微漂移
 * - reduced-motion：渲染单帧静态后停止
 * - 离屏（IntersectionObserver）暂停 rAF，页面隐藏暂停
 * - dispose() 完整清理几何体/材质/纹理/渲染器
 */

export interface CosmosHandle {
  dispose: () => void
  /** 设置当前高亮（选中）的卡片索引；-1 表示无 */
  setHighlight: (index: number) => void
  /** 沉浸模式：卡片/背板平滑淡出（星尘保留），false=隐藏 */
  setCardsVisible: (visible: boolean) => void
  /** 键盘旋转星环（弧度增量） */
  rotateBy: (delta: number) => void
  /** 键盘缩放视角（1 = 拉近一档） */
  zoomBy: (dir: number) => void
}

export function createPhotoCosmos(
  canvas: HTMLCanvasElement,
  imageUrls: string[],
  opts?: { reduced?: boolean; onPick?: (index: number) => void },
): CosmosHandle {
  const reduced = !!opts?.reduced
  const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true })
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1))

  const scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x0b0b0c, 0.05)

  const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 100)
  camera.position.set(0, 0.4, 10.5)

  const group = new THREE.Group()
  scene.add(group)

  const disposables: Array<{ dispose: () => void }> = []

  const cardGeo = new THREE.PlaneGeometry(2.5, 1.66)
  const frameGeo = new THREE.PlaneGeometry(2.62, 1.78)
  disposables.push(cardGeo, frameGeo)

  const urls = imageUrls.slice(0, 12)
  const perRing = Math.ceil(urls.length / 2)

  // 通用材质（MeshBasicMaterial：headless/软件渲染下最稳；失败卡自动隐藏）
  function makeMaterials() {
    const frameMat = new THREE.MeshBasicMaterial({
      color: 0x2a2e2a,
      transparent: true,
      opacity: 0.92,
      side: THREE.DoubleSide,
      depthWrite: false,
      fog: true,
    })
    ;(frameMat.userData as any) = { baseOpacity: 0.92 }
    const cardMat = new THREE.MeshBasicMaterial({
      color: 0xffffff,
      transparent: true,
      opacity: 0.96,
      side: THREE.DoubleSide,
      fog: true,
    })
    ;(cardMat.userData as any) = { baseOpacity: 0.96 }
    disposables.push(frameMat, cardMat)
    return { frameMat, cardMat }
  }

  urls.forEach((url, i) => {
    const ring = i < perRing ? 0 : 1
    const idx = i < perRing ? i : i - perRing
    const radius = ring === 0 ? 4.6 : 7.4
    const count = ring === 0 ? perRing : urls.length - perRing
    const angle = (idx / count) * Math.PI * 2
    const y = (ring === 0 ? 0.3 : -0.6) + Math.sin(i * 12.9898) * 0.55
    const x = Math.cos(angle) * radius
    const z = Math.sin(angle) * radius

    const { frameMat, cardMat } = makeMaterials()

    const nx = Math.cos(angle)
    const nz = Math.sin(angle)

    const frame = new THREE.Mesh(frameGeo, frameMat)
    frame.position.set(x, y, z)
    frame.lookAt(0, y, 0)
    frame.userData.index = i
    group.add(frame)

    const card = new THREE.Mesh(cardGeo, cardMat)
    card.position.set(x + nx * 0.03, y, z + nz * 0.03)
    card.lookAt(0, y, 0)
    card.userData.index = i
    group.add(card)

    // 纹理异步贴上：原生 Image（与 <img> 同路径，不受 CORS/crossOrigin 影响）
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.decoding = 'async'
    img.onload = () => {
      const t = new THREE.Texture(img)
      t.colorSpace = THREE.SRGBColorSpace
      t.minFilter = THREE.LinearFilter
      t.generateMipmaps = false
      t.needsUpdate = true
      cardMat.map = t
      cardMat.needsUpdate = true
    }
    img.onerror = () => {
      cardMat.color.set(0x1c1f1c)
      cardMat.opacity = 0.6
    }
    img.src = toProxyUrl(url) || url
  })

  // 点击拾取：Raycaster 命中卡片回调 onPick；悬停变 pointer 光标
  const raycaster = new THREE.Raycaster()
  const pointerV = new THREE.Vector2()
  function pickAt(e: MouseEvent): number | null {
    const r = canvas.getBoundingClientRect()
    pointerV.x = ((e.clientX - r.left) / r.width) * 2 - 1
    pointerV.y = -((e.clientY - r.top) / r.height) * 2 + 1
    raycaster.setFromCamera(pointerV, camera)
    const hits = raycaster.intersectObjects(group.children, false)
    for (const h of hits) {
      const idx = h.object.userData?.index
      if (idx != null) return idx as number
    }
    return null
  }
  let downX = 0
  let downY = 0
  let downTime = 0
  function onDown(e: PointerEvent) {
    downX = e.clientX
    downY = e.clientY
    downTime = performance.now()
    dragging = true
    spinVelocity = 0
  }
  function onPointerMove(e: PointerEvent) {
    if (!dragging) return
    const dx = e.clientX - downX
    downX = e.clientX
    group.rotation.y += dx * 0.0045
    spinVelocity = dx * 0.18
  }
  function onUp(e: PointerEvent) {
    if (!dragging) return
    dragging = false
    const moved = Math.hypot(e.clientX - downX, e.clientY - downY)
    const quick = performance.now() - downTime < 450
    if (moved <= 8 && quick) {
      const idx = pickAt(e)
      if (idx != null) opts?.onPick?.(idx)
    }
  }
  let hoverFrame = false
  function onHoverMove(e: MouseEvent) {
    if (dragging || hoverFrame) return
    hoverFrame = true
    requestAnimationFrame(() => {
      hoverFrame = false
      const hit = pickAt(e)
      hoveringCard = hit != null
      canvas.style.cursor = hit != null ? 'pointer' : 'grab'
    })
  }
  canvas.addEventListener('pointerdown', onDown)
  canvas.addEventListener('pointermove', onPointerMove)
  canvas.addEventListener('pointerup', onUp)
  canvas.addEventListener('pointercancel', () => { dragging = false })
  canvas.addEventListener('mousemove', onHoverMove, { passive: true })

  // 选中光晕：单张 additive 光晕面，跟随选中卡（外发光效果）
  const glowGeo = new THREE.PlaneGeometry(3.3, 2.25)
  const glowMat = new THREE.MeshBasicMaterial({
    color: 0xa3b6a8,
    transparent: true,
    opacity: 0.22,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    side: THREE.DoubleSide,
  })
  const glow = new THREE.Mesh(glowGeo, glowMat)
  glow.visible = false
  scene.add(glow)
  disposables.push(glowGeo, glowMat)

  let highlightIndex = -1
  const cardScales = urls.map(() => 1)

  // 旋转状态机：自动环绕 / hover 卡片暂停 / 拖拽接管 + 惯性
  let hoveringCard = false
  let dragging = false
  let spinVelocity = 0
  const AUTO_SPEED = 0.12
  let cardsTargetOpacity = 1
  let cardsOpacity = 1
  let zoomLevel = 10.5

  // 星尘
  const starGeo = new THREE.BufferGeometry()
  const starCount = 140
  const positions = new Float32Array(starCount * 3)
  for (let i = 0; i < starCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 24
    positions[i * 3 + 1] = (Math.random() - 0.5) * 12
    positions[i * 3 + 2] = (Math.random() - 0.5) * 24
  }
  starGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const starMat = new THREE.PointsMaterial({
    color: 0x8fa394,
    size: 0.05,
    transparent: true,
    opacity: 0.7,
    sizeAttenuation: true,
  })
  const stars = new THREE.Points(starGeo, starMat)
  scene.add(stars)
  disposables.push(starGeo, starMat)

  const host = canvas.parentElement as HTMLElement
  function resize() {
    const w = host.clientWidth || 1
    const h = host.clientHeight || 1
    renderer.setSize(w, h, false)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  }
  resize()
  window.addEventListener('resize', resize)

  let mx = 0
  let my = 0
  function onMove(e: MouseEvent) {
    const r = canvas.getBoundingClientRect()
    mx = ((e.clientX - r.left) / r.width - 0.5) * 2
    my = ((e.clientY - r.top) / r.height - 0.5) * 2
  }
  window.addEventListener('mousemove', onMove, { passive: true })

  let raf = 0
  let running = true
  const clock = new THREE.Clock()

  function frame() {
    const dt = Math.min(0.1, clock.getDelta())
    if (dragging) {
      // 拖拽时 rotation 由 pointermove 直接驱动
    } else {
      if (!hoveringCard) group.rotation.y += dt * AUTO_SPEED
      if (Math.abs(spinVelocity) > 0.0004) {
        group.rotation.y += spinVelocity
        spinVelocity *= Math.pow(0.02, dt)
      } else {
        spinVelocity = 0
      }
    }
    stars.rotation.y -= dt * 0.012
    cardsOpacity += (cardsTargetOpacity - cardsOpacity) * Math.min(1, dt * 5)
    for (let i = 0; i < group.children.length; i++) {
      const obj = group.children[i] as THREE.Mesh
      const idx = obj.userData?.index
      if (idx == null) continue
      const target = idx === highlightIndex ? 1.08 : 1
      const cur = cardScales[idx] || 1
      const next = cur + (target - cur) * Math.min(1, dt * 8)
      cardScales[idx] = next
      obj.scale.setScalar(next)
    }
    if (highlightIndex >= 0 && highlightIndex < urls.length) {
      const card = group.children.find(
        (o) => (o as THREE.Mesh).userData?.index === highlightIndex && (o as THREE.Mesh).geometry === cardGeo,
      ) as THREE.Mesh | undefined
      if (card) {
        glow.visible = true
        const wp = card.getWorldPosition(new THREE.Vector3())
        glow.position.copy(wp)
        glow.quaternion.copy(card.quaternion)
        glow.position.multiplyScalar(1.012)
      }
    } else {
      glow.visible = false
    }
    const camX = 0.4 + mx * 0.9
    const camY = 0.4 - my * 0.5
    camera.position.x += (camX - camera.position.x) * 0.04
    camera.position.y += (camY - camera.position.y) * 0.04
    camera.position.z += (zoomLevel - camera.position.z) * Math.min(1, dt * 6)
    camera.lookAt(0, 0, 0)
    renderer.render(scene, camera)
    if (running && !reduced) raf = requestAnimationFrame(frame)
  }

  if (reduced) {
    renderer.render(scene, camera)
  } else {
    raf = requestAnimationFrame(frame)
  }

  let visible = true
  const io = new IntersectionObserver((entries) => {
    visible = entries[0]?.isIntersecting ?? true
    if (visible && !reduced && !raf) raf = requestAnimationFrame(frame)
    if (!visible && raf) {
      cancelAnimationFrame(raf)
      raf = 0
    }
  }, { threshold: 0.02 })
  io.observe(canvas)

  function onVis() {
    if (document.hidden && raf) { cancelAnimationFrame(raf); raf = 0 }
    else if (!document.hidden && visible && !reduced && !raf) raf = requestAnimationFrame(frame)
  }
  document.addEventListener('visibilitychange', onVis)

  return {
    setHighlight(index: number) {
      highlightIndex = index
    },
    setCardsVisible(visible: boolean) {
      cardsTargetOpacity = visible ? 1 : 0
    },
    rotateBy(delta: number) {
      group.rotation.y += delta
    },
    zoomBy(dir: number) {
      zoomLevel = Math.max(6.5, Math.min(16, zoomLevel - dir * 1.2))
    },
    dispose() {
      running = false
      if (raf) cancelAnimationFrame(raf)
      io.disconnect()
      window.removeEventListener('resize', resize)
      canvas.removeEventListener('pointerdown', onDown)
      canvas.removeEventListener('pointermove', onPointerMove)
      canvas.removeEventListener('pointerup', onUp)
      canvas.removeEventListener('mousemove', onHoverMove)
      window.removeEventListener('mousemove', onMove)
      document.removeEventListener('visibilitychange', onVis)
      disposables.forEach((d) => d.dispose())
      renderer.dispose()
    },
  }
}
