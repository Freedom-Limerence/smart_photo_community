/* ============================================================
 * 光影星图 v2（/cosmos）：两层知识图谱宇宙
 * Hub = 标签枢纽（大光点 + 文字标签，点击跳标签聚合页）
 * Leaf = 图片节点（小光点，环绕所属 Hub，点击开灯箱）
 * LOD：远看只有 Hub 星座，推近后 Leaf 渐显展开
 * ============================================================ */
import * as THREE from 'three'

export interface GraphHub {
  tag: string
  count: number
}
export interface GraphLeaf {
  workId: string
  title: string
  author: string
  cover: string
  heat: number
  hub: string
  tags: string[]
}
export interface GraphData {
  hubs: GraphHub[]
  leaves: GraphLeaf[]
  edges: { hub: string; leaf: string }[]
}
export interface GraphHandle {
  dispose: () => void
}

const HUB_PALETTE = [0x8fd8f0, 0xf0c987, 0xb8a7e8, 0xf0a7b8, 0xa7e8c9, 0xf0d8a7, 0xc9b8f0, 0xa7d8c9]

/** 文字标签 sprite（canvas 2D 绘制，无跨域问题） */
function makeLabelSprite(text: string, color: string): THREE.Sprite {
  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 128
  const ctx = canvas.getContext('2d')!
  ctx.font = '600 52px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.shadowColor = 'rgba(0,0,0,.9)'
  ctx.shadowBlur = 12
  ctx.fillStyle = color
  ctx.fillText(text, 256, 64)
  const tex = new THREE.CanvasTexture(canvas)
  tex.needsUpdate = true
  const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthWrite: false })
  const sprite = new THREE.Sprite(mat)
  sprite.scale.set(6.4, 1.6, 1)
  return sprite
}

export function createGraphUniverse(
  canvas: HTMLCanvasElement,
  data: GraphData,
  opts: {
    onPickLeaf?: (leafIndex: number) => void
    onOpenHub?: (tag: string) => void
    onHover?: (text: string | null, screen: { x: number; y: number }) => void
  } = {}
): GraphHandle {
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1))
  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 500)

  const hubs = data.hubs
  const leaves = data.leaves
  const hubIdx = new Map(hubs.map((h, i) => [h.tag, i]))
  const H = hubs.length
  const L = leaves.length

  // ---- 布局：Hub 按共现（共享 leaf）弹簧成团；Leaf 环绕所属 Hub ----
  const hubPos: THREE.Vector3[] = hubs.map((_, i) => {
    const a = (i / Math.max(1, H)) * Math.PI * 2
    return new THREE.Vector3(Math.cos(a) * 11, (Math.random() - 0.5) * 2, Math.sin(a) * 11)
  })
  // 共现弹簧（两个 hub 拥有同一 leaf 时视为共现——leaf 已去重，改用标签字符相似度近似）
  for (let it = 0; it < 200; it++) {
    const cool = 1 - it / 200
    for (let i = 0; i < H; i++) {
      for (let j = i + 1; j < H; j++) {
        const d = hubPos[j].clone().sub(hubPos[i])
        const dist = Math.max(1, d.length())
        const f = (3.5 / (dist * dist)) * cool
        hubPos[i].add(d.normalize().multiplyScalar(f))
        hubPos[j].sub(d.normalize().multiplyScalar(f))
      }
      hubPos[i].add(hubPos[i].clone().multiplyScalar(-0.02 * cool))
    }
  }
  // Leaf：环绕所属 Hub
  const leafPos: THREE.Vector3[] = []
  const hubLeafCount = new Map<number, number>()
  const leafHub = leaves.map((lf) => hubIdx.get(lf.hub) ?? 0)
  leaves.forEach((_, i) => {
    const h = leafHub[i]
    const k = hubLeafCount.get(h) ?? 0
    hubLeafCount.set(h, k + 1)
    const a = (k / 8) * Math.PI * 2 + h
    leafPos.push(hubPos[h].clone().add(new THREE.Vector3(Math.cos(a) * 3.4, (Math.random() - 0.5) * 1.2, Math.sin(a) * 3.4)))
  })

  // 簇色
  const hubColors = hubs.map((_, i) => new THREE.Color(HUB_PALETTE[i % HUB_PALETTE.length]))
  const leafColors = leaves.map((lf, i) => hubColors[leafHub[i]].clone().multiplyScalar(0.92))

  // ---- Hub 渲染：大光点 + 文字标签 ----
  const hubPosArr = new Float32Array(H * 3)
  const hubColArr = new Float32Array(H * 3)
  const hubSizeArr = new Float32Array(H)
  const hubPhaseArr = new Float32Array(H)
  hubs.forEach((h, i) => {
    hubPosArr.set([hubPos[i].x, hubPos[i].y, hubPos[i].z], i * 3)
    hubColArr.set([hubColors[i].r, hubColors[i].g, hubColors[i].b], i * 3)
    hubSizeArr[i] = 40 + Math.min(26, h.count * 4)   // 关联作品越多越大
    hubPhaseArr[i] = i * 1.7
  })
  const hubGeo = new THREE.BufferGeometry()
  hubGeo.setAttribute('position', new THREE.BufferAttribute(hubPosArr, 3))
  hubGeo.setAttribute('aColor', new THREE.BufferAttribute(hubColArr, 3))
  hubGeo.setAttribute('aSize', new THREE.BufferAttribute(hubSizeArr, 1))
  hubGeo.setAttribute('aPhase', new THREE.BufferAttribute(hubPhaseArr, 1))
  const nodeVert = `
    attribute vec3 aColor; attribute float aSize; attribute float aPhase;
    uniform float uTime; uniform float uPixelRatio;
    varying vec3 vColor; varying float vGlow;
    void main() {
      vColor = aColor;
      vec3 p = position;
      p.y += sin(uTime * 0.8 + aPhase) * 0.4;
      p.x += cos(uTime * 0.55 + aPhase * 1.3) * 0.22;
      vec4 mv = modelViewMatrix * vec4(p, 1.0);
      float pulse = 0.88 + 0.12 * sin(uTime * 1.3 + aPhase);
      gl_PointSize = min(aSize * pulse * uPixelRatio * (140.0 / -mv.z), 190.0);
      vGlow = pulse;
      gl_Position = projectionMatrix * mv;
    }`
  const nodeFrag = `
    varying vec3 vColor; varying float vGlow;
    void main() {
      vec2 uv = gl_PointCoord - 0.5;
      float d = length(uv);
      if (d > 0.5) discard;
      float core = smoothstep(0.14, 0.0, d);
      float halo = pow(1.0 - d * 2.0, 2.4);
      gl_FragColor = vec4(vColor * (0.85 + vGlow * 0.35), core + halo * 0.65);
    }`
  const hubMat = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    uniforms: { uTime: { value: 0 }, uPixelRatio: { value: Math.min(2, window.devicePixelRatio || 1) } },
    vertexShader: nodeVert, fragmentShader: nodeFrag,
  })
  const hubPoints = new THREE.Points(hubGeo, hubMat)
  scene.add(hubPoints)

  // Hub 文字标签 sprite（永远可见）
  const labelSprites: THREE.Sprite[] = []
  hubs.forEach((h, i) => {
    const sp = makeLabelSprite(h.tag, '#' + hubColors[i].getHexString())
    sp.position.copy(hubPos[i]).add(new THREE.Vector3(0, 2.6, 0))
    scene.add(sp)
    labelSprites.push(sp)
  })

  // ---- Leaf 渲染：小光点（LOD：随相机距离渐显）----
  const leafPosArr = new Float32Array(L * 3)
  const leafColArr = new Float32Array(L * 3)
  const leafSizeArr = new Float32Array(L)
  const leafPhaseArr = new Float32Array(L)
  leaves.forEach((_, i) => {
    leafPosArr.set([leafPos[i].x, leafPos[i].y, leafPos[i].z], i * 3)
    leafColArr.set([leafColors[i].r, leafColors[i].g, leafColors[i].b], i * 3)
    leafSizeArr[i] = 15 + (leaves[i].heat > 0 ? 6 : 0)
    leafPhaseArr[i] = i * 2.3
  })
  const leafGeo = new THREE.BufferGeometry()
  leafGeo.setAttribute('position', new THREE.BufferAttribute(leafPosArr, 3))
  leafGeo.setAttribute('aColor', new THREE.BufferAttribute(leafColArr, 3))
  leafGeo.setAttribute('aSize', new THREE.BufferAttribute(leafSizeArr, 1))
  leafGeo.setAttribute('aPhase', new THREE.BufferAttribute(leafPhaseArr, 1))
  const leafMat = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    uniforms: { uTime: { value: 0 }, uPixelRatio: { value: Math.min(2, window.devicePixelRatio || 1) }, uLod: { value: 0 } },
    vertexShader: `
      attribute vec3 aColor; attribute float aSize; attribute float aPhase;
      uniform float uTime; uniform float uPixelRatio; uniform float uLod;
      varying vec3 vColor; varying float vAlpha;
      void main() {
        vColor = aColor;
        vec3 p = position;
        p.y += sin(uTime * 1.0 + aPhase) * 0.25;
        vec4 mv = modelViewMatrix * vec4(p, 1.0);
        float pulse = 0.85 + 0.15 * sin(uTime * 1.5 + aPhase);
        gl_PointSize = aSize * pulse * uLod * uPixelRatio * (140.0 / -mv.z);
        vAlpha = uLod;
        gl_Position = projectionMatrix * mv;
      }`,
    fragmentShader: `
      varying vec3 vColor; varying float vAlpha;
      void main() {
        if (vAlpha < 0.02) discard;
        vec2 uv = gl_PointCoord - 0.5;
        float d = length(uv);
        if (d > 0.5) discard;
        float core = smoothstep(0.15, 0.0, d);
        float halo = pow(1.0 - d * 2.0, 2.4);
        gl_FragColor = vec4(vColor, (core + halo * 0.6) * vAlpha);
      }`,
  })
  const leafPoints = new THREE.Points(leafGeo, leafMat)
  scene.add(leafPoints)

  // ---- 连线：Hub→Leaf + Hub 间微弱星桥 ----
  const lineData: number[] = []
  const lineT: number[] = []
  const lineC: number[] = []
  leaves.forEach((lf, i) => {
    const h = leafHub[i]
    lineData.push(hubPos[h].x, hubPos[h].y, hubPos[h].z, leafPos[i].x, leafPos[i].y, leafPos[i].z)
    lineT.push(0, 1)
    const c = hubColors[h]
    lineC.push(c.r, c.g, c.b, c.r * 0.7, c.g * 0.7, c.b * 0.7)
  })
  const lineGeo = new THREE.BufferGeometry()
  lineGeo.setAttribute('position', new THREE.BufferAttribute(new Float32Array(lineData), 3))
  lineGeo.setAttribute('aT', new THREE.BufferAttribute(new Float32Array(lineT), 1))
  lineGeo.setAttribute('aColor', new THREE.BufferAttribute(new Float32Array(lineC), 3))
  const lineMat = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    uniforms: { uTime: { value: 0 }, uDim: { value: 0 } },
    vertexShader: `
      attribute float aT; attribute vec3 aColor;
      uniform float uTime;
      varying float vT; varying vec3 vColor;
      void main() {
        vT = aT; vColor = aColor;
        vec3 p = position;
        float ph = fract(sin(dot(position.xy, vec2(12.9898, 78.233))) * 43758.5453) * 6.2831;
        p.y += sin(uTime * 0.8 + ph) * 0.4;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(p, 1.0);
      }`,
    fragmentShader: `
      uniform float uTime; uniform float uDim;
      varying float vT; varying vec3 vColor;
      void main() {
        float flow = fract(vT * 2.0 - uTime * 0.4);
        float pulse = smoothstep(0.0, 0.25, flow) * smoothstep(0.55, 0.3, flow);
        float a = (0.3 + pulse * 0.55) * (1.0 - uDim * 0.7);
        gl_FragColor = vec4(vColor + pulse * 0.3, a);
      }`,
  })
  const lines = new THREE.LineSegments(lineGeo, lineMat)
  scene.add(lines)

  // ---- 相机与交互 ----
  let yaw = 0.4, pitch = 0.18, dist = 46, distTarget = 20   // 入场拉近
  let yawT = yaw, pitchT = pitch
  let dragging = false, moved = 0, lastX = 0, lastY = 0
  const raycaster = new THREE.Raycaster()
  raycaster.params.Points = { threshold: 1.1 }
  const mouseNDC = new THREE.Vector2(-2, -2)
  let hoverKind: 'hub' | 'leaf' | null = null
  let hoverIdx = -1

  function resize() {
    const w = canvas.clientWidth || window.innerWidth
    const h = canvas.clientHeight || window.innerHeight
    renderer.setSize(w, h, false)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  }
  resize()
  window.addEventListener('resize', resize)

  function onDown(e: PointerEvent) {
    dragging = true; moved = 0; lastX = e.clientX; lastY = e.clientY
    try { canvas.setPointerCapture(e.pointerId) } catch { /* noop */ }
  }
  function onMove(e: PointerEvent) {
    const rect = canvas.getBoundingClientRect()
    mouseNDC.set(((e.clientX - rect.left) / rect.width) * 2 - 1, -((e.clientY - rect.top) / rect.height) * 2 + 1)
    if (!dragging) return
    const dx = e.clientX - lastX
    const dy = e.clientY - lastY
    moved += Math.abs(dx) + Math.abs(dy)
    yawT += dx * 0.005
    pitchT = Math.max(-0.9, Math.min(0.9, pitchT + dy * 0.004))
    lastX = e.clientX; lastY = e.clientY
  }
  function onUp(e: PointerEvent) {
    dragging = false
    try { canvas.releasePointerCapture(e.pointerId) } catch { /* noop */ }
    if (moved < 5) {
      if (hoverKind === 'hub') opts.onOpenHub?.(hubs[hoverIdx].tag)
      else if (hoverKind === 'leaf') opts.onPickLeaf?.(hoverIdx)
    }
  }
  function onWheel(e: WheelEvent) { e.preventDefault(); distTarget = Math.max(11, Math.min(70, distTarget + e.deltaY * 0.022)) }
  function onTouchMove(e: TouchEvent) { e.preventDefault() }

  canvas.addEventListener('pointerdown', onDown)
  canvas.addEventListener('pointermove', onMove)
  canvas.addEventListener('pointerup', onUp)
  canvas.addEventListener('pointercancel', () => { dragging = false })
  canvas.addEventListener('wheel', onWheel, { passive: false })
  canvas.addEventListener('touchmove', onTouchMove, { passive: false })
  canvas.addEventListener('pointerleave', () => { mouseNDC.set(-2, -2) })

  const clock = new THREE.Clock()
  let raf = 0
  let disposed = false
  function frame() {
    if (disposed) return
    const t = clock.getElapsedTime()
    hubMat.uniforms.uTime.value = t
    leafMat.uniforms.uTime.value = t
    lineMat.uniforms.uTime.value = t

    // LOD：相机越近 Leaf 越显现（7~34 映射 1~0）
    const lod = Math.max(0, Math.min(1, (34 - dist) / 24))
    leafMat.uniforms.uLod.value = lod

    // hover：hub 优先
    raycaster.setFromCamera(mouseNDC, camera)
    let kind: 'hub' | 'leaf' | null = null
    let idx = -1
    const hubHits = raycaster.intersectObject(hubPoints)
    if (hubHits.length) { kind = 'hub'; idx = hubHits[0].index ?? -1 }
    else if (lod > 0.3) {
      raycaster.params.Points = { threshold: 0.8 }
      const leafHits = raycaster.intersectObject(leafPoints)
      if (leafHits.length) { kind = 'leaf'; idx = leafHits[0].index ?? -1 }
      raycaster.params.Points = { threshold: 1.1 }
    }
    if (kind !== hoverKind || idx !== hoverIdx) {
      hoverKind = kind; hoverIdx = idx
      lineMat.uniforms.uDim.value = kind ? 1 : 0
      const rect = canvas.getBoundingClientRect()
      if (kind === 'hub') opts.onHover?.(hubs[idx].tag + ' · ' + hubs[idx].count + ' 幅作品 · 点击进入', { x: (mouseNDC.x + 1) / 2 * rect.width, y: (1 - mouseNDC.y) / 2 * rect.height })
      else if (kind === 'leaf') opts.onHover?.(leaves[idx].title + ' · ' + leaves[idx].author, { x: (mouseNDC.x + 1) / 2 * rect.width, y: (1 - mouseNDC.y) / 2 * rect.height })
      else opts.onHover?.(null, { x: 0, y: 0 })
      canvas.style.cursor = kind ? 'pointer' : 'grab'
    }

    // 文字标签随距离缩放（避免推近时占满屏）
    const labelScale = Math.max(0.45, Math.min(1.6, dist / 20))
    for (const sp of labelSprites) sp.scale.set(6.4 * labelScale, 1.6 * labelScale, 1)

    dist += (distTarget - dist) * 0.045
    yaw += (yawT - yaw) * 0.09
    pitch += (pitchT - pitch) * 0.09
    camera.position.set(Math.sin(yaw) * Math.cos(pitch) * dist, Math.sin(pitch) * dist, Math.cos(yaw) * Math.cos(pitch) * dist)
    camera.lookAt(0, 0, 0)
    renderer.render(scene, camera)
    raf = requestAnimationFrame(frame)
  }
  raf = requestAnimationFrame(frame)

  function onVis() {
    if (document.hidden && raf) { cancelAnimationFrame(raf); raf = 0 }
    else if (!document.hidden && !raf) { clock.getDelta(); raf = requestAnimationFrame(frame) }
  }
  document.addEventListener('visibilitychange', onVis)

  return {
    dispose() {
      disposed = true
      if (raf) cancelAnimationFrame(raf)
      window.removeEventListener('resize', resize)
      document.removeEventListener('visibilitychange', onVis)
      canvas.removeEventListener('pointerdown', onDown)
      canvas.removeEventListener('pointermove', onMove)
      canvas.removeEventListener('pointerup', onUp)
      canvas.removeEventListener('wheel', onWheel)
      canvas.removeEventListener('touchmove', onTouchMove)
      hubGeo.dispose(); hubMat.dispose()
      leafGeo.dispose(); leafMat.dispose()
      lineGeo.dispose(); lineMat.dispose()
      labelSprites.forEach((sp) => { sp.material.map?.dispose(); sp.material.dispose() })
      renderer.dispose()
    },
  }
}
