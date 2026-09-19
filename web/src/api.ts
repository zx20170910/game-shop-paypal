export type Game = {
  id: string
  code: string
  name: string
  publisher?: string
  supportedPlatforms?: string
  deliveryType?: string
  status?: string
}

export type Product = {
  id: string
  gameId: string
  sku: string
  name: string
  amountMinor: number
  currency: string
  platform: string
  serverRegion: string
  deliveryType: string
  status?: string
}

export type Server = {
  id: string
  gameId: string
  code: string
  name: string
  region: string
  status?: string
}

export type AdminSession = {
  accessToken: string
  expiresInSeconds: number
  username: string
  roles: string[]
}

export type CheckoutResponse = {
  sessionId: string
  orderId: string
  accessToken: string
  amountMinor: number
  currency: string
  expiresAt: string
}

export type OrderStatus = {
  orderId: string
  orderStatus: string
  paymentStatus: string
  fulfillmentStatus: string
  captureId?: string
  playerUid: string
  deliveredAt?: string
  riskStatus?: string
  riskReason?: string
}

export type PaymentAttempt = {
  paymentAttemptId: string
  provider: string
  paypalOrderId: string
  status: string
}

export const demoGames: Game[] = [
  { id: 'game-f76', code: 'F76', name: 'Fallout 76', publisher: 'Bethesda', supportedPlatforms: 'PC · Xbox · PlayStation', deliveryType: 'MANUAL', status: 'ACTIVE' },
  { id: 'game-arc', code: 'ARC', name: 'ARC Raiders', publisher: 'Embark Studios', supportedPlatforms: 'PC · PlayStation', deliveryType: 'MANUAL', status: 'ACTIVE' },
]

export const demoProducts: Product[] = [
  { id: 'product-f76-caps', gameId: 'game-f76', sku: 'F76-CAPS-5000', name: '5,000 Caps', amountMinor: 1299, currency: 'USD', platform: 'PC', serverRegion: 'US', deliveryType: 'MANUAL', status: 'ACTIVE' },
  { id: 'product-f76-scrip', gameId: 'game-f76', sku: 'F76-SCRIP-1000', name: '1,000 Legendary Scrip', amountMinor: 899, currency: 'USD', platform: 'PC', serverRegion: 'US', deliveryType: 'MANUAL', status: 'ACTIVE' },
  { id: 'product-arc-kit', gameId: 'game-arc', sku: 'ARC-STARTER-KIT', name: 'Starter Supply Kit', amountMinor: 1499, currency: 'USD', platform: 'PC', serverRegion: 'US', deliveryType: 'MANUAL', status: 'ACTIVE' },
]

const apiRoot = import.meta.env.VITE_API_BASE ?? '/api/v1'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiRoot}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  })
  if (!response.ok) {
    const body = await response.text()
    throw new Error(body || `Request failed with ${response.status}`)
  }
  const payload = await response.json() as { data: T }
  return payload.data
}

async function adminRequest<T>(token: string, path: string, init?: RequestInit): Promise<T> {
  return request<T>(path, {
    ...init,
    headers: { Authorization: `Bearer ${token}`, ...(init?.headers ?? {}) },
  })
}

export function loginAdmin(username: string, password: string) {
  return request<AdminSession>('/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export async function loadAdminCatalog(token: string) {
  const [games, products, servers] = await Promise.all([
    adminRequest<Game[]>(token, '/admin/catalog/games'),
    adminRequest<Product[]>(token, '/admin/catalog/products'),
    adminRequest<Server[]>(token, '/admin/catalog/servers'),
  ])
  return { games, products, servers }
}

export function saveAdminGame(token: string, id: string | null, payload: {
  code: string
  name: string
  publisher: string
  supportedPlatforms: string
  deliveryType: string
  status: string
}) {
  return adminRequest<Game>(token, id ? `/admin/catalog/games/${encodeURIComponent(id)}` : '/admin/catalog/games', {
    method: id ? 'PUT' : 'POST',
    body: JSON.stringify(payload),
  })
}

export function saveAdminProduct(token: string, id: string | null, payload: {
  gameId: string
  sku: string
  name: string
  amountMinor: number
  currency: string
  platform: string
  serverRegion: string
  deliveryType: string
  status: string
}) {
  return adminRequest<Product>(token, id ? `/admin/catalog/products/${encodeURIComponent(id)}` : '/admin/catalog/products', {
    method: id ? 'PUT' : 'POST',
    body: JSON.stringify(payload),
  })
}

export function saveAdminServer(token: string, id: string | null, payload: {
  gameId: string
  code: string
  name: string
  region: string
  status: string
}) {
  return adminRequest<Server>(token, id ? `/admin/catalog/servers/${encodeURIComponent(id)}` : '/admin/catalog/servers', {
    method: id ? 'PUT' : 'POST',
    body: JSON.stringify(payload),
  })
}

export async function loadCatalog(): Promise<{ games: Game[]; products: Product[]; demo: boolean }> {
  try {
    const [games, products] = await Promise.all([
      request<Game[]>('/catalog/games'),
      request<Product[]>('/catalog/products'),
    ])
    return { games, products, demo: false }
  } catch {
    return { games: demoGames, products: demoProducts, demo: true }
  }
}

export function createCheckout(payload: {
  productId: string
  quantity: number
  gameId: string
  playerUid: string
  country: string
  currency: string
}, idempotencyKey: string) {
  return request<CheckoutResponse>('/checkout/sessions', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(payload),
  })
}

export function createDemoCheckout(product: Product, quantity: number, playerUid: string, currency: string): CheckoutResponse {
  const sessionId = `demo-session-${Date.now()}`
  return {
    sessionId,
    orderId: `DEMO-${new Date().toISOString().slice(0, 10).replaceAll('-', '')}-${Math.floor(Math.random() * 9000 + 1000)}`,
    accessToken: `demo-token-${sessionId}`,
    amountMinor: product.amountMinor * quantity,
    currency,
    expiresAt: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
  }
}

export function createPayPalOrder(sessionId: string, idempotencyKey: string) {
  return request<PaymentAttempt>(`/checkout/sessions/${sessionId}/payments/paypal/order`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export function capturePayPalOrder(paypalOrderId: string, idempotencyKey: string) {
  return request<{ orderId: string; paymentStatus: string; fulfillmentStatus: string; captureId: string }>(`/payments/paypal/orders/${paypalOrderId}/capture`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export function getOrderStatus(orderNo: string, accessToken: string) {
  return request<OrderStatus>(`/orders/${encodeURIComponent(orderNo)}/status`, {
    headers: { 'X-Order-Access-Token': accessToken },
  })
}
