import * as THREE from 'three'

/**
 * 三维照片星环（着陆页标志性 3D 场景）
 * - 12 张真实作品漂浮成双层星环，缓慢自转，镜头随鼠标轻微漂移
 * - reduced-motion：渲染单帧静态后停止
 * - 离屏（IntersectionObserver）暂停 rAF，页面隐藏暂停
 * - dispose() 完整清理几何体/材质/纹理/渲染器
 */

export interface CosmosHandle {
  dispose: () => void
}

const VERT = `
  varying vec2 vUv;
  void main() {
    vUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
  }
`

const FRAG = `
  varying vec2 vUv;
  uniform sampler2D map;
  uniform float uOpacity;
  void main() {
    vec4 tex = texture2D(map, vUv);
    if (tex.a < 0.05) discard;
    gl_FragColor = vec4(tex.rgb, uOpacity);
  }
`

export function createPhotoCosmos(
  canvas: HTMLCanvasElement,
  imageUrls: string[],
  opts?: { reduced?: boolean },
): CosmosHandle {
  const reduced = !!opts?.reduced
  const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true })
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1))

  const scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x0b0b0c, 0.055)

  const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 100)
  camera.position.set(0, 0.4, 10.5)

  const group = new THREE.Group()
  scene.add(group)

  const loader = new THREE.TextureLoader()
  loader.setCrossOrigin('anonymous')
  const disposables: Array<{ dispose: () => void }> = []

  const cardGeo = new THREE.PlaneGeometry(2.5, 1.66)
  disposables.push(cardGeo)
  const frameGeo = new THREE.PlaneGeometry(2.62, 1.78)
  disposables.push(frameGeo)

  const urls = imageUrls.slice(0, 12)
  const perRing = Math.ceil(urls.length / 2)

  urls.forEach((url, i) => {
    const ring = i < perRing ? 0 : 1
    const idx = i < perRing ? i : i - perRing
    const radius = ring === 0 ? 4.6 : 7.4
    const count = ring === 0 ? perRing : urls.length - perRing
    const angle = (idx / count) * Math.PI * 2
    const y = (ring === 0 ? 0.3 : -0.6) + (Math.sin(i * 12.9898) * 0.55)

    const matRef = { uOpacity: 0.96 }
    const tex = loader.load(
      url,
      (t) => {
        t.colorSpace = THREE.SRGBColorSpace
        t.minFilter = THREE.LinearFilter
      },
      undefined,
      () => {
        // 加载失败（如防盗链 451）：隐藏该卡，避免黑块
        matRef.uOpacity = 0
        frameMat.uniforms.uOpacity.value = 0
      },
    )
    disposables.push(tex)

    // 背板（暗框 + 轻微外发光描边感）
    const frameMat = new THREE.ShaderMaterial({
      vertexShader: VERT,
      fragmentShader: FRAG,
      uniforms: { map: { value: null }, uOpacity: { value: 0.9 } },
      transparent: true,
      depthWrite: false,
    })
    frameMat.fragmentShader = FRAG.replace('vec4 tex = texture2D(map, vUv);', 'vec4 tex = vec4(0.24, 0.27, 0.24, 1.0);')
    frameMat.needsUpdate = true
    const frame = new THREE.Mesh(frameGeo, frameMat)
    frame.position.set(Math.cos(angle) * radius, y, Math.sin(angle) * radius)
    frame.lookAt(0, y, 0)
    group.add(frame)
    disposables.push(frameMat)

    // 照片
    const mat = new THREE.ShaderMaterial({
      vertexShader: VERT,
      fragmentShader: FRAG,
      uniforms: { map: { value: tex }, uOpacity: { value: 0.96 } },
      onBeforeCompile: (sh) => { /* keep ref for failure fade */ },
      transparent: true,
      depthWrite: false,
    })
    disposables.push(mat)
    const card = new THREE.Mesh(cardGeo, mat)
    card.position.set(Math.cos(angle) * radius, y, Math.sin(angle) * radius)
    card.lookAt(0, y, 0)
    group.add(card)
  })

  // 中央微光星点
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

  // 尺寸
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

  // 鼠标视差（lerp 追随）
  let mx = 0
  let my = 0
  function onMove(e: MouseEvent) {
    const r = canvas.getBoundingClientRect()
    mx = ((e.clientX - r.left) / r.width - 0.5) * 2
    my = ((e.clientY - r.top) / r.height - 0.5) * 2
  }
  window.addEventListener('mousemove', onMove, { passive: true })

  // 主循环
  let raf = 0
  let running = true
  const clock = new THREE.Clock()

  function frame() {
    const dt = clock.getDelta()
    group.rotation.y += dt * 0.045
    stars.rotation.y -= dt * 0.012
    const targetX = my * 0.9
    const targetY = 0.4 + mx * 0.6
    camera.position.y += (targetX * 0.4 - camera.position.y + 0.4) * 0.04
    camera.position.x += (targetY * 1.6 - camera.position.x) * 0.04
    camera.lookAt(0, 0, 0)
    renderer.render(scene, camera)
    if (running && !reduced) raf = requestAnimationFrame(frame)
  }

  if (reduced) {
    // 单帧静态（星环正面）
    renderer.render(scene, camera)
  } else {
    raf = requestAnimationFrame(frame)
  }

  // 离屏暂停 / 恢复
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
    dispose() {
      running = false
      if (raf) cancelAnimationFrame(raf)
      io.disconnect()
      window.removeEventListener('resize', resize)
      window.removeEventListener('mousemove', onMove)
      document.removeEventListener('visibilitychange', onVis)
      disposables.forEach((d) => d.dispose())
      renderer.dispose()
    },
  }
}
