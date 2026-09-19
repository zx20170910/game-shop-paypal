import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  capturePayPalOrder,
  CheckoutResponse,
  Game,
  getOrderStatus,
  loadCatalog,
  Product,
  createCheckout,
  createDemoCheckout,
  OrderStatus,
} from './api'
import { initialLanguage, Language, translate } from './i18n'
import AdminCatalog from './AdminCatalog'
import { PayPalButtonState, preparePayPalButton } from './paypal'

type View = 'shop' | 'orders' | 'ops' | 'admin'
type CheckoutState = 'idle' | 'creating' | 'created' | 'error'

const currencies: Record<string, string> = { US: 'USD', GB: 'GBP', DE: 'EUR' }

const money = (amountMinor: number, currency: string) => new Intl.NumberFormat('en-US', {
  style: 'currency', currency, maximumFractionDigits: 2,
}).format(amountMinor / 100)

function App() {
  const [view, setView] = useState<View>('shop')
  const [games, setGames] = useState<Game[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [catalogDemo, setCatalogDemo] = useState(false)
  const [selectedGameId, setSelectedGameId] = useState('all')
  const [selectedProductId, setSelectedProductId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [country, setCountry] = useState('US')
  const [playerUid, setPlayerUid] = useState('')
  const [checkoutState, setCheckoutState] = useState<CheckoutState>('idle')
  const [checkout, setCheckout] = useState<CheckoutResponse | null>(null)
  const [checkoutDemo, setCheckoutDemo] = useState(false)
  const [error, setError] = useState('')
  const [orderNo, setOrderNo] = useState('')
  const [accessToken, setAccessToken] = useState('')
  const [orderStatus, setOrderStatus] = useState<OrderStatus | null>(null)
  const [orderLoading, setOrderLoading] = useState(false)
  const [notice, setNotice] = useState('')
  const [language, setLanguage] = useState<Language>(initialLanguage)
  const t = (key: Parameters<typeof translate>[1], params?: Record<string, string | number>) => translate(language, key, params)

  const switchLanguage = (next: Language) => {
    setLanguage(next)
    window.localStorage.setItem('gamevault-language', next)
  }

  const statusLabel = (value: string) => ({
    CREATED: t('statusCreated'),
    CAPTURED: t('statusCaptured'),
    FULFILLED: t('statusFulfilled'),
    NOT_REQUIRED: t('statusNotRequired'),
    PROTECTED: t('statusProtected'),
  }[value] ?? value.replaceAll('_', ' '))

  useEffect(() => {
    loadCatalog().then((catalog) => {
      setGames(catalog.games)
      setProducts(catalog.products)
      setCatalogDemo(catalog.demo)
      setSelectedProductId(catalog.products[0]?.id ?? '')
    })
  }, [])

  const selectedGame = games.find((game) => game.id === selectedGameId)
  const visibleProducts = useMemo(() => products.filter((product) => selectedGameId === 'all' || product.gameId === selectedGameId), [products, selectedGameId])
  const selectedProduct = products.find((product) => product.id === selectedProductId) ?? visibleProducts[0]
  const currency = currencies[country]
  const total = selectedProduct ? selectedProduct.amountMinor * quantity : 0

  useEffect(() => {
    if (selectedProduct && !visibleProducts.some((product) => product.id === selectedProduct.id)) {
      setSelectedProductId(visibleProducts[0]?.id ?? '')
    }
  }, [selectedProduct, visibleProducts])

  const chooseProduct = (product: Product) => {
    setSelectedProductId(product.id)
    setSelectedGameId(product.gameId)
    setCheckoutState('idle')
    setError('')
  }

  const submitCheckout = async () => {
    if (!selectedProduct || !playerUid.trim()) {
      setError(t('uidRequired'))
      return
    }
    if (selectedProduct.currency !== currency) {
      setError(t('currencyMismatch', { currency: selectedProduct.currency }))
      return
    }
    setCheckoutState('creating')
    setError('')
    const key = `web-${crypto.randomUUID()}`
    try {
      const result = await createCheckout({
        productId: selectedProduct.id,
        quantity,
        gameId: selectedProduct.gameId,
        playerUid: playerUid.trim(),
        country,
        currency,
      }, key)
      setCheckout(result)
      setCheckoutDemo(false)
      setOrderNo(result.orderId)
      setAccessToken(result.accessToken)
      setCheckoutState('created')
      setNotice(t('orderReadyNotice'))
    } catch {
      const result = createDemoCheckout(selectedProduct, quantity, playerUid.trim(), currency)
      setCheckout(result)
      setCheckoutDemo(true)
      setOrderNo(result.orderId)
      setAccessToken(result.accessToken)
      setCheckoutState('created')
      setNotice(t('demoOrderNotice'))
    }
  }

  const queryOrder = async () => {
    if (!orderNo.trim() || !accessToken.trim()) {
      setError(t('orderFieldsRequired'))
      return
    }
    setOrderLoading(true)
    setError('')
    try {
      const result = await getOrderStatus(orderNo.trim(), accessToken.trim())
      setOrderStatus(result)
    } catch {
      setError(t('orderQueryFailed'))
    } finally {
      setOrderLoading(false)
    }
  }

  const handlePayPalCaptured = useCallback(async () => {
    setNotice(translate(language, 'paypalCaptured'))
    if (!checkout) return
    try {
      setOrderStatus(await getOrderStatus(checkout.orderId, checkout.accessToken))
    } catch {
      // Capture already succeeded; the order lookup can be retried from the Orders tab.
    }
  }, [checkout, language])

  const handlePayPalMessage = useCallback((message: 'cancelled' | 'error') => {
    setNotice(translate(language, message === 'cancelled' ? 'paypalCancelled' : 'paypalError'))
  }, [language])

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand" onClick={() => setView('shop')} aria-label={t('navShop')}>
          <span className="brand-mark">G</span>
          <span><strong>GameVault</strong><small>{t('tagline')}</small></span>
        </button>
        <nav className="nav-tabs" aria-label={t('mainNav')}>
          <button className={view === 'shop' ? 'active' : ''} onClick={() => setView('shop')}>{t('navShop')}</button>
          <button className={view === 'orders' ? 'active' : ''} onClick={() => setView('orders')}>{t('navOrders')}</button>
          <button className={view === 'ops' ? 'active' : ''} onClick={() => setView('ops')}>{t('navOps')}</button>
          <button className={view === 'admin' ? 'active' : ''} onClick={() => setView('admin')}>{t('navAdmin')}</button>
        </nav>
        <div className="locale-tools"><div className="language-switch"><button className={language === 'zh-CN' ? 'active' : ''} onClick={() => switchLanguage('zh-CN')}>中</button><button className={language === 'en-US' ? 'active' : ''} onClick={() => switchLanguage('en-US')}>EN</button></div><div className="locale-chip"><span className="status-dot" /> {t('sandbox')}</div></div>
      </header>

      {catalogDemo && <div className="demo-banner"><span>DEMO FALLBACK</span> {t('demoBanner')}</div>}
      {notice && <div className="notice" role="status">{notice}<button aria-label={t('close')} onClick={() => setNotice('')}>×</button></div>}

      {view === 'shop' && (
        <main>
          <section className="hero section-wrap">
            <div className="hero-copy">
              <p className="eyebrow">{t('heroEyebrow')}</p>
              <h1>{t('heroTitle')}</h1>
              <p className="hero-intro">{t('heroIntro')}</p>
              <div className="hero-pills"><span>✓ {t('secureCheckout')}</span><span>✓ {t('humanDelivery')}</span><span>✓ {t('traceableOrders')}</span></div>
            </div>
            <div className="hero-orbit" aria-hidden="true"><span className="orbit-ring ring-one" /><span className="orbit-ring ring-two" /><span className="orbit-core">GAME<br /><b>VAULT</b></span><i className="orbit-star star-one">✦</i><i className="orbit-star star-two">✦</i></div>
          </section>

          <section className="section-wrap catalog-layout">
            <div className="catalog-panel">
              <div className="section-heading"><div><p className="eyebrow">{t('catalogEyebrow')}</p><h2>{t('pickDrop')}</h2></div><span className="result-count">{t('items', { count: visibleProducts.length })}</span></div>
              <div className="filter-row">
                <button className={selectedGameId === 'all' ? 'filter active' : 'filter'} onClick={() => setSelectedGameId('all')}>{t('allGames')}</button>
                {games.map((game) => <button key={game.id} className={selectedGameId === game.id ? 'filter active' : 'filter'} onClick={() => setSelectedGameId(game.id)}>{game.name}</button>)}
              </div>
              {selectedGame && <p className="filter-context">{selectedGame.publisher} · {selectedGame.supportedPlatforms}</p>}
              <div className="product-grid">
                {visibleProducts.map((product, index) => {
                  const game = games.find((item) => item.id === product.gameId)
                  const selected = selectedProduct?.id === product.id
                  return <button key={product.id} className={selected ? 'product-card selected' : 'product-card'} onClick={() => chooseProduct(product)}>
                    <span className={`product-art art-${index % 3}`}><span>{game?.code ?? 'ITEM'}</span><b>{index === 0 ? '✦' : index === 1 ? '◈' : '◒'}</b></span>
                    <span className="product-meta"><span className="product-game">{game?.name ?? t('gameItem')}</span><strong>{product.name}</strong><small>{product.platform} · {product.serverRegion} · {t('humanDelivery')}</small></span>
                    <span className="product-price">{money(product.amountMinor, product.currency)}<i>/{product.currency}</i></span>
                    <span className="selection-dot">{selected ? '✓' : '+'}</span>
                  </button>
                })}
              </div>
            </div>

            <aside className="checkout-panel">
              <div className="section-heading compact"><div><p className="eyebrow">{t('checkoutEyebrow')}</p><h2>{t('yourLoadout')}</h2></div><span className="secure-lock">⌁ {t('secure')}</span></div>
              {selectedProduct ? <>
                <div className="checkout-product"><span className="mini-art">{games.find((game) => game.id === selectedProduct.gameId)?.code ?? 'GV'}</span><div><strong>{selectedProduct.name}</strong><small>{games.find((game) => game.id === selectedProduct.gameId)?.name}</small></div><b>{money(total, selectedProduct.currency)}</b></div>
                <label className="field-label">{t('playerUid')}<input value={playerUid} onChange={(event) => setPlayerUid(event.target.value)} placeholder={t('uidPlaceholder')} /></label>
                <div className="field-split"><label className="field-label">{t('country')}<select value={country} onChange={(event) => setCountry(event.target.value)}><option value="US">{t('countryUS')}</option><option value="GB">{t('countryGB')}</option><option value="DE">{t('countryDE')}</option></select></label><label className="field-label">{t('quantity')}<div className="quantity"><button aria-label="-" onClick={() => setQuantity(Math.max(1, quantity - 1))}>−</button><span>{quantity}</span><button aria-label="+" onClick={() => setQuantity(Math.min(99, quantity + 1))}>+</button></div></label></div>
                <div className="total-line"><span>{t('total')} <small>{currency}</small></span><strong>{money(total, currency)}</strong></div>
                {error && <p className="form-error">{error}</p>}
                <button className="primary-button" disabled={checkoutState === 'creating'} onClick={submitCheckout}>{checkoutState === 'creating' ? t('creatingOrder') : t('continuePaypal')}</button>
                <p className="fine-print">{t('terms')}</p>
              </> : <div className="empty-state">{t('noProducts')}</div>}
            </aside>
          </section>

          {checkoutState === 'created' && checkout && <section className="section-wrap payment-card">
            <div><p className="eyebrow">{t('paymentEyebrow')}</p><h2>{t('paymentTitle')}</h2><p className="muted">{t('orderNumber')} <strong>{checkout.orderId}</strong> · {t('expires', { time: new Date(checkout.expiresAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) })}</p></div>
            {checkoutDemo ? <div className="paypal-placeholder"><span className="paypal-logo">P</span><div><strong>{t('sandboxPending')}</strong><small>{t('paypalHint')}</small></div><span className="pending-badge">{t('demo')}</span></div> : <PayPalButton checkout={checkout} language={language} onCaptured={handlePayPalCaptured} onMessage={handlePayPalMessage} />}
            <button className="secondary-button" onClick={() => { setOrderNo(checkout.orderId); setAccessToken(checkout.accessToken); setView('orders') }}>{t('viewOrder')}</button>
          </section>}
        </main>
      )}

      {view === 'orders' && <main className="section-wrap page-view"><div className="page-heading"><p className="eyebrow">{t('orderEyebrow')}</p><h1>{t('trackDelivery')}</h1><p>{t('orderIntro')}</p></div><div className="order-layout"><section className="form-card"><h2>{t('findOrder')}</h2><label className="field-label">{t('orderNumber')}<input value={orderNo} onChange={(event) => setOrderNo(event.target.value)} placeholder={t('orderNumberPlaceholder')} /></label><label className="field-label">{t('accessToken')}<input value={accessToken} onChange={(event) => setAccessToken(event.target.value)} placeholder={t('accessTokenPlaceholder')} /></label><button className="primary-button" disabled={orderLoading} onClick={queryOrder}>{orderLoading ? t('checking') : t('checkStatus')}</button>{error && <p className="form-error">{error}</p>}</section><section className="status-card">{orderStatus ? <><div className="status-card-top"><span className="status-icon">✓</span><div><p className="eyebrow">{t('liveOrder')}</p><h2>{orderStatus.orderId}</h2></div></div><div className="status-grid"><StatusItem label={t('orderNumber')} value={statusLabel(orderStatus.orderStatus)} /><StatusItem label={t('paymentCaptured')} value={statusLabel(orderStatus.paymentStatus)} /><StatusItem label={t('humanFulfillment')} value={statusLabel(orderStatus.fulfillmentStatus)} /><StatusItem label={t('playerUid')} value={statusLabel(orderStatus.playerUid)} /></div><div className="timeline"><span className="done">{t('orderCreated')}</span><span className={orderStatus.paymentStatus === 'CAPTURED' ? 'done' : ''}>{t('paymentCaptured')}</span><span className={orderStatus.fulfillmentStatus === 'FULFILLED' ? 'done' : ''}>{t('humanFulfillment')}</span></div></> : <div className="empty-state"><span className="empty-icon">◌</span><h3>{t('noOrder')}</h3><p>{t('noOrderHint')}</p></div>}</section></div></main>}

      {view === 'ops' && <main className="section-wrap page-view"><div className="page-heading"><p className="eyebrow">{t('opsEyebrow')}</p><h1>{t('deliveryDesk')}</h1><p>{t('opsIntro')}</p></div><div className="ops-grid"><OpsCard label="MANUAL_PENDING" value="—" note={t('pendingTasks')} tone="amber" /><OpsCard label="AWAITING_REVIEW" value="—" note={t('awaitingReview')} tone="blue" /><OpsCard label="FULFILLED" value="—" note={t('fulfilled')} tone="green" /><div className="ops-callout"><span>{t('adminReady')}</span><strong>{t('adminReadyTitle')}</strong><small>{t('adminReadyHint')}</small></div></div></main>}

      {view === 'admin' && <AdminCatalog language={language} />}

      <footer className="footer section-wrap"><span>{t('footerLeft')}</span><span>{t('footerRight')}</span></footer>
    </div>
  )
}

function PayPalButton({ checkout, language, onCaptured, onMessage }: {
  checkout: CheckoutResponse
  language: Language
  onCaptured: (result: Awaited<ReturnType<typeof capturePayPalOrder>>) => void
  onMessage: (message: 'cancelled' | 'error') => void
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [state, setState] = useState<PayPalButtonState>('loading')

  useEffect(() => {
    if (!containerRef.current) return
    let cleanup: () => void = () => undefined
    let disposed = false
    preparePayPalButton(containerRef.current, checkout, language, {
      onState: (next) => { if (!disposed) setState(next) },
      onCaptured: (result) => { if (!disposed) onCaptured(result) },
      onMessage: (message) => { if (!disposed) onMessage(message) },
    }).then((dispose) => { cleanup = dispose }).catch(() => {
      if (!disposed) {
        setState('error')
        onMessage('error')
      }
    })
    return () => {
      disposed = true
      cleanup()
    }
  }, [checkout, language, onCaptured, onMessage])

  const statusKey = state === 'unconfigured' || state === 'unavailable'
    ? 'paypalUnavailable'
    : state === 'loading'
      ? 'paypalLoading'
      : state === 'creating'
        ? 'paypalCreating'
        : state === 'capturing'
          ? 'paypalCapturing'
          : state === 'error' ? 'paypalError' : null

  return <div className="paypal-sdk-area"><div ref={containerRef} className="paypal-button-container" /><span className="pending-badge">{state === 'ready' ? translate(language, 'ready') : ''}</span>{statusKey && <p className="paypal-status">{translate(language, statusKey)}</p>}</div>
}

function StatusItem({ label, value }: { label: string; value: string }) {
  return <div><small>{label}</small><strong>{value.replaceAll('_', ' ')}</strong></div>
}

function OpsCard({ label, value, note, tone }: { label: string; value: string; note: string; tone: string }) {
  return <div className={`ops-card ${tone}`}><span>{label}</span><strong>{value}</strong><small>{note}</small></div>
}

export default App
