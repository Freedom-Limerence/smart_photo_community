/* ============================================================
 * 光影星图（/cosmos）：作品节点宇宙 —— photoCosmos 的星图扩展
 * 节点 = 发光星（Points + 程序化光晕），连线 = 共享标签流光
 * 拖拽旋转视角（不改节点位置），滚轮缩放，点击节点开灯箱
 * ============================================================ */
import * as THREE from 'three'

export interface GraphNode {
  id: string
  title: string
  author: string
  cover: string
  tags: string[]
  likeCount: number
}
export interface GraphEdge {
  source: string
  target: string
  weight: number
}
export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}
export interface GraphHandle {
  dispose: () => void
}

/** 简化力导向布局：斥力 + 弹簧，离线结算出静态位置（标签簇自然成团） */
function settleLayout(data: GraphData, iterations = 260): { pos: THREE.Vector3[]; colors: THREE.Color[] } {
  const n = data.nodes.length
  const pos: THREE.Vector3[] = data.nodes.map((_, i) => {
    const a = (i / Math.max(1, n)) * Math.PI * 2
    const r = 9 + (i % 5) * 1.4
    return new THREE.Vector3(Math.cos(a) * r, (Math.random() - 0.5) * 3, Math.sin(a) * r)
  })
  const vel = data.nodes.map(() => new THREE.Vector3())
  const edgeIdx = data.edges.map((e) => [Number(e.source), Number(e.target), e.weight] as const)

  for (let it = 0; it < iterations; it++) {
    const cool = 1 - it / iterations
    // 斥力
    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        const d = pos[i].clone().sub(pos[j])
        const dist = Math.max(0.6, d.length())
        const f = (2.2 / (dist * dist)) * cool
        vel[i].add(d.normalize().multiplyScalar(f))
        vel[j].sub(d.normalize().multiplyScalar(f))
      }
    }
    // 弹簧（共享标签越多越紧）
    for (const [a, b, w] of edgeIdx) {
      const d = pos[b].clone().sub(pos[a])
      const dist = Math.max(0.5, d.length())
      const rest = 5.5 - Math.min(2.5, w * 0.5)
      const f = dist - rest > 0 ? (dist - rest) * 0.06 * cool : 0
      if (f) {
        vel[a].add(d.normalize().multiplyScalar(f))
        vel[b].sub(d.normalize().multiplyScalar(f))
      }
    }
    // 向心 + 限速
    for (let i = 0; i < n; i++) {
      vel[i].add(pos[i].clone().multiplyScalar(-0.03 * cool))
      vel[i].multiplyScalar(0.82)
      pos[i].add(vel[i].clone().multiplyScalar(2.4))
    }
  }
  // 簇色：按标签内容哈希出稳定色相（同簇连线自然同调）
  const palette = [0x7fd8d0, 0xf0c987, 0xb8a7e8, 0x8fc9f0, 0xf0a7b8, 0xa7e8a7]
  const colors = data.nodes.map((node, i) => {
    let h = 0
    for (const t of node.tags) h = (h + t.charCodeAt(0)) % 997
    return new THREE.Color(palette[(h + i) % palette.length])
  })
  return { pos, colors }
}

export function createGraphUniverse(
  canvas: HTMLCanvasElement,
  data: GraphData,
  opts: { onPick?: (index: number) => void; onHover?: (index: number | null, screen: { x: number; y: number }) => void } = {}
): GraphHandle {
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1))
  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 400)
  const { pos, colors } = settleLayout(data)
  const n = data.nodes.length

  // ---- 节点：Points + 程序化光晕（呼吸浮动在 GPU 内做）----
  const positions = new Float32Array(n * 3)
  const cArr = new Float32Array(n * 3)
  const sizes = new Float32Array(n)
  const phases = new Float32Array(n)
  for (let i = 0; i < n; i++) {
    positions.set([pos[i].x, pos[i].y, pos[i].z], i * 3)
    cArr.set([colors[i].r, colors[i].g, colors[i].b], i * 3)
    const big = i < 3 ? 2.2 : 1
    sizes[i] = (data.nodes[i].tags.length > 2 ? 34 : 24) * big
    phases[i] = Math.random() * Math.PI * 2
  }
  const nodeGeo = new THREE.BufferGeometry()
  nodeGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  nodeGeo.setAttribute('aColor', new THREE.BufferAttribute(cArr, 3))
  nodeGeo.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  nodeGeo.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  const nodeMat = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: { uTime: { value: 0 }, uPixelRatio: { value: Math.min(2, window.devicePixelRatio || 1) } },
    vertexShader: `
      attribute vec3 aColor; attribute float aSize; attribute float aPhase;
      uniform float uTime; uniform float uPixelRatio;
      varying vec3 vColor; varying float vGlow;
      void main() {
        vColor = aColor;
        vec3 p = position;
        p.y += sin(uTime * 0.9 + aPhase) * 0.35;
        p.x += cos(uTime * 0.6 + aPhase * 1.3) * 0.18;
        vec4 mv = modelViewMatrix * vec4(p, 1.0);
        float pulse = 0.86 + 0.14 * sin(uTime * 1.4 + aPhase);
        gl_PointSize = aSize * pulse * uPixelRatio * (140.0 / -mv.z);
        vGlow = pulse;
        gl_Position = projectionMatrix * mv;
      }`,
    fragmentShader: `
      varying vec3 vColor; varying float vGlow;
      void main() {
        vec2 uv = gl_PointCoord - 0.5;
        float d = length(uv);
        if (d > 0.5) discard;
        float core = smoothstep(0.16, 0.0, d);
        float halo = pow(1.0 - d * 2.0, 2.6);
        float a = core + halo * 0.7;
        gl_FragColor = vec4(vColor * (0.8 + vGlow * 0.4), a);
      }`,
  })
  const points = new THREE.Points(nodeGeo, nodeMat)
  scene.add(points)

  // ---- 连线：LineSegments + 流光着色 ----
  const eIdx = data.edges.map((e) => [Number(e.source), Number(e.target), e.weight] as const)
  const linePos = new Float32Array(eIdx.length * 6)
  const lineT = new Float32Array(eIdx.length * 2)
  const lineC = new Float32Array(eIdx.length * 6)
  eIdx.forEach(([a, b], i) => {
    linePos.set([pos[a].x, pos[a].y, pos[a].z, pos[b].x, pos[b].y, pos[b].z], i * 6)
    lineT[i * 2] = 0
    lineT[i * 2 + 1] = 1
    const c = colors[a].clone().lerp(colors[b], 0.5)
    lineC.set([c.r, c.g, c.b, c.r, c.g, c.b], i * 6)
  })
  const lineGeo = new THREE.BufferGeometry()
  lineGeo.setAttribute('position', new THREE.BufferAttribute(linePos, 3))
  lineGeo.setAttribute('aT', new THREE.BufferAttribute(lineT, 1))
  lineGeo.setAttribute('aColor', new THREE.BufferAttribute(lineC, 3))
  const lineMat = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: { uTime: { value: 0 }, uDim: { value: 0 } },
    vertexShader: `
      attribute float aT; attribute vec3 aColor;
      uniform float uTime;
      varying float vT; varying vec3 vColor;
      void main() {
        vT = aT; vColor = aColor;
        vec3 p = position;
        float ph = fract(sin(dot(position.xy, vec2(12.9898, 78.233))) * 43758.5453) * 6.2831;
        p.y += sin(uTime * 0.9 + ph) * 0.35;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(p, 1.0);
      }`,
    fragmentShader: `
      uniform float uTime; uniform float uDim;
      varying float vT; varying vec3 vColor;
      void main() {
        float flow = fract(vT * 2.0 - uTime * 0.35);
        float pulse = smoothstep(0.0, 0.25, flow) * smoothstep(0.55, 0.3, flow);
        float a = (0.35 + pulse * 0.5) * (1.0 - uDim * 0.75);
        gl_FragColor = vec4(vColor + pulse * 0.35, a);
      }`,
  })
  const lines = new THREE.LineSegments(lineGeo, lineMat)
  scene.add(lines)

  // ---- 相机与交互：拖拽旋转视角 / 滚轮缩放 / 点击拾取 ----
  let yaw = 0.4, pitch = 0.22, dist = 34, distTarget = 16   // 入场：从远处拉近（转场动画）
  let yawT = yaw, pitchT = pitch
  let dragging = false, moved = 0, lastX = 0, lastY = 0
  const raycaster = new THREE.Raycaster()
  raycaster.params.Points = { threshold: 1.2 }
  const mouseNDC = new THREE.Vector2(-2, -2)
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
    dragging = true
    moved = 0
    lastX = e.clientX
    lastY = e.clientY
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
    lastX = e.clientX
    lastY = e.clientY
  }
  function onUp(e: PointerEvent) {
    dragging = false
    try { canvas.releasePointerCapture(e.pointerId) } catch { /* noop */ }
    // 严格点击阈值：移动 <5px 才算点击
    if (moved < 5 && hoverIdx >= 0) opts.onPick?.(hoverIdx)
  }
  function onWheel(e: WheelEvent) {
    e.preventDefault()
    distTarget = Math.max(6, Math.min(60, distTarget + e.deltaY * 0.02))
  }
  function onTouchMove(e: TouchEvent) { e.preventDefault() }   // 移动端防页面滚动

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
    nodeMat.uniforms.uTime.value = t
    lineMat.uniforms.uTime.value = t

    // hover 拾取
    raycaster.setFromCamera(mouseNDC, camera)
    const hits = raycaster.intersectObject(points)
    const newHover = hits.length ? hits[0].index ?? -1 : -1
    if (newHover !== hoverIdx) {
      hoverIdx = newHover
      lineMat.uniforms.uDim.value = hoverIdx >= 0 ? 1 : 0   // hover 时其余连线变暗
      if (hoverIdx >= 0) {
        const rect = canvas.getBoundingClientRect()
        opts.onHover?.(hoverIdx, { x: (mouseNDC.x + 1) / 2 * rect.width, y: (1 - mouseNDC.y) / 2 * rect.height })
      } else opts.onHover?.(null, { x: 0, y: 0 })
    }

    // 相机：入场拉近 + 拖拽旋转 + 滚轮缩放
    dist += (distTarget - dist) * 0.045
    yaw += (yawT - yaw) * 0.09
    pitch += (pitchT - pitch) * 0.09
    camera.position.set(
      Math.sin(yaw) * Math.cos(pitch) * dist,
      Math.sin(pitch) * dist,
      Math.cos(yaw) * Math.cos(pitch) * dist
    )
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
      nodeGeo.dispose()
      nodeMat.dispose()
      lineGeo.dispose()
      lineMat.dispose()
      renderer.dispose()
    },
  }
}
