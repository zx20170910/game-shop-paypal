import { FormEvent, useEffect, useState } from 'react'
import {
  AdminSession,
  Game,
  loadAdminCatalog,
  loginAdmin,
  Product,
  saveAdminGame,
  saveAdminProduct,
  saveAdminServer,
  Server,
} from './api'
import { Language, TranslationKey, translate } from './i18n'

type Section = 'games' | 'products' | 'servers'
type EditorState = { section: Section; id: string | null }
type Draft = Record<string, string>

type AdminCatalogProps = { language: Language }

const initialDrafts: Record<Section, Draft> = {
  games: { code: '', name: '', publisher: '', supportedPlatforms: '', deliveryType: 'MANUAL', status: 'ACTIVE' },
  products: { gameId: '', sku: '', name: '', amountMinor: '0', currency: 'USD', platform: 'PC', serverRegion: 'US', deliveryType: 'MANUAL', status: 'ACTIVE' },
  servers: { gameId: '', code: '', name: '', region: '', status: 'ACTIVE' },
}

export default function AdminCatalog({ language }: AdminCatalogProps) {
  const t = (key: TranslationKey) => translate(language, key)
  const [session, setSession] = useState<AdminSession | null>(null)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [games, setGames] = useState<Game[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [servers, setServers] = useState<Server[]>([])
  const [activeSection, setActiveSection] = useState<Section>('products')
  const [editor, setEditor] = useState<EditorState | null>(null)
  const [draft, setDraft] = useState<Draft>({})
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState<TranslationKey | ''>('')

  const load = async (token: string) => {
    setLoading(true)
    setError('')
    try {
      const catalog = await loadAdminCatalog(token)
      setGames(catalog.games)
      setProducts(catalog.products)
      setServers(catalog.servers)
    } catch {
      setError(t('adminLoadFailed'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (session) void load(session.accessToken)
  }, [session])

  const submitLogin = async (event: FormEvent) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const next = await loginAdmin(username.trim(), password)
      if (!next.roles.includes('ADMIN')) {
        setError(t('adminRoleRequired'))
        return
      }
      setSession(next)
      setPassword('')
    } catch {
      setError(t('adminLoadFailed'))
    } finally {
      setLoading(false)
    }
  }

  const startCreate = (section: Section) => {
    setActiveSection(section)
    setEditor({ section, id: null })
    setDraft({ ...initialDrafts[section], ...(section !== 'games' ? { gameId: games[0]?.id ?? '' } : {}) })
    setError('')
  }

  const startEdit = (section: Section, item: Game | Product | Server) => {
    setActiveSection(section)
    setEditor({ section, id: item.id })
    if (section === 'games') {
      const game = item as Game
      setDraft({ code: game.code, name: game.name, publisher: game.publisher ?? '', supportedPlatforms: game.supportedPlatforms ?? '', deliveryType: game.deliveryType ?? 'MANUAL', status: game.status ?? 'ACTIVE' })
    } else if (section === 'products') {
      const product = item as Product
      setDraft({ gameId: product.gameId, sku: product.sku, name: product.name, amountMinor: String(product.amountMinor), currency: product.currency, platform: product.platform, serverRegion: product.serverRegion, deliveryType: product.deliveryType, status: product.status ?? 'ACTIVE' })
    } else {
      const server = item as Server
      setDraft({ gameId: server.gameId, code: server.code, name: server.name, region: server.region, status: server.status ?? 'ACTIVE' })
    }
    setError('')
  }

  const updateDraft = (key: string, value: string) => setDraft((current) => ({ ...current, [key]: value }))

  const save = async (event: FormEvent) => {
    event.preventDefault()
    if (!session || !editor) return
    setSaving(true)
    setError('')
    try {
      if (editor.section === 'games') {
        await saveAdminGame(session.accessToken, editor.id, {
          code: draft.code,
          name: draft.name,
          publisher: draft.publisher,
          supportedPlatforms: draft.supportedPlatforms,
          deliveryType: draft.deliveryType,
          status: draft.status,
        })
      } else if (editor.section === 'products') {
        await saveAdminProduct(session.accessToken, editor.id, {
          gameId: draft.gameId,
          sku: draft.sku,
          name: draft.name,
          amountMinor: Number(draft.amountMinor),
          currency: draft.currency,
          platform: draft.platform,
          serverRegion: draft.serverRegion,
          deliveryType: draft.deliveryType,
          status: draft.status,
        })
      } else {
        await saveAdminServer(session.accessToken, editor.id, {
          gameId: draft.gameId,
          code: draft.code,
          name: draft.name,
          region: draft.region,
          status: draft.status,
        })
      }
      setEditor(null)
      setNotice('adminSaveSuccess')
      await load(session.accessToken)
    } catch {
      setError(t('adminSaveFailed'))
    } finally {
      setSaving(false)
    }
  }

  if (!session) {
    return <main className="section-wrap admin-page"><section className="admin-login-card"><p className="eyebrow">ADMIN / CATALOG</p><h1>{t('adminLoginTitle')}</h1><p className="admin-hint">{t('adminLoginHint')}</p><form onSubmit={submitLogin}><label className="field-label">{t('username')}<input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" required /></label><label className="field-label">{t('password')}<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required /></label>{error && <p className="form-error">{error}</p>}<button className="primary-button" disabled={loading}>{loading ? t('loggingIn') : t('login')}</button></form></section></main>
  }

  const sectionTitle = t(activeSection)
  const list = activeSection === 'games' ? games : activeSection === 'products' ? products : servers

  return <main className="section-wrap admin-page">
    <div className="admin-heading"><div><p className="eyebrow">ADMIN / CATALOG</p><h1>{t('catalogAdmin')}</h1><p>{t('catalogAdminHint')}</p></div><div className="admin-account"><span>{session.username}</span><button className="secondary-button" onClick={() => { setSession(null); setEditor(null) }}>{t('logout')}</button></div></div>
    <div className="admin-toolbar"><div className="admin-tabs">{(['products', 'games', 'servers'] as Section[]).map((section) => <button key={section} className={activeSection === section ? 'active' : ''} onClick={() => { setActiveSection(section); setEditor(null) }}>{t(section)}</button>)}</div><button className="primary-button admin-add" onClick={() => startCreate(activeSection)}>{activeSection === 'games' ? `+ ${t('addGame')}` : activeSection === 'products' ? `+ ${t('addProduct')}` : `+ ${t('addServer')}`}</button></div>
    {error && <p className="form-error admin-message">{error}</p>}
    {notice && <p className="admin-success">{t(notice)}</p>}
    {editor ? <form className="admin-editor" onSubmit={save}><div className="section-heading"><div><p className="eyebrow">{editor.id ? t('edit') : editor.section === 'games' ? t('addGame') : editor.section === 'products' ? t('addProduct') : t('addServer')}</p><h2>{sectionTitle}</h2></div><button type="button" className="text-button" onClick={() => setEditor(null)}>{t('cancel')}</button></div><EditorFields section={editor.section} draft={draft} games={games} updateDraft={updateDraft} t={t} /><div className="admin-editor-actions"><button type="button" className="secondary-button" onClick={() => setEditor(null)}>{t('cancel')}</button><button className="primary-button" disabled={saving}>{saving ? t('saving') : t('save')}</button></div></form> : <CatalogTable section={activeSection} list={list} games={games} loading={loading} onEdit={startEdit} t={t} />}
  </main>
}

function EditorFields({ section, draft, games, updateDraft, t }: { section: Section; draft: Draft; games: Game[]; updateDraft: (key: string, value: string) => void; t: (key: TranslationKey) => string }) {
  const input = (key: string, label: string, type = 'text') => <label className="field-label" key={key}>{label}<input type={type} value={draft[key] ?? ''} onChange={(event) => updateDraft(key, event.target.value)} required={['code', 'name', 'sku', 'gameId', 'amountMinor', 'currency', 'platform', 'serverRegion', 'region', 'status'].includes(key)} /></label>
  const select = (key: string, label: string, options: Array<{ value: string; label: string }>) => <label className="field-label" key={key}>{label}<select value={draft[key] ?? ''} onChange={(event) => updateDraft(key, event.target.value)} required>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
  if (section === 'games') return <div className="admin-form-grid">{input('code', t('code'))}{input('name', t('name'))}{input('publisher', t('publisher'), 'text')}{input('supportedPlatforms', t('platforms'))}{select('deliveryType', t('deliveryType'), [{ value: 'MANUAL', label: t('manual') }])}{select('status', t('status'), [{ value: 'ACTIVE', label: t('active') }, { value: 'INACTIVE', label: t('inactive') }])}</div>
  if (section === 'products') return <div className="admin-form-grid">{select('gameId', t('game'), games.map((game) => ({ value: game.id, label: `${game.code} · ${game.name}` })))}{input('sku', t('sku'))}{input('name', t('name'))}{input('amountMinor', t('amountMinor'), 'number')}{input('currency', t('currency'))}{input('platform', t('platform'))}{input('serverRegion', t('serverRegion'))}{select('deliveryType', t('deliveryType'), [{ value: 'MANUAL', label: t('manual') }])}{select('status', t('status'), [{ value: 'ACTIVE', label: t('active') }, { value: 'INACTIVE', label: t('inactive') }])}</div>
  return <div className="admin-form-grid">{select('gameId', t('game'), games.map((game) => ({ value: game.id, label: `${game.code} · ${game.name}` })))}{input('code', t('code'))}{input('name', t('name'))}{input('region', t('region'))}{select('status', t('status'), [{ value: 'ACTIVE', label: t('active') }, { value: 'INACTIVE', label: t('inactive') }])}</div>
}

function CatalogTable({ section, list, games, loading, onEdit, t }: { section: Section; list: Array<Game | Product | Server>; games: Game[]; loading: boolean; onEdit: (section: Section, item: Game | Product | Server) => void; t: (key: TranslationKey) => string }) {
  const gameName = (id: string) => games.find((game) => game.id === id)?.name ?? id
  if (loading) return <div className="admin-empty">{t('loading')}</div>
  if (!list.length) return <div className="admin-empty">{t('noData')}</div>
  return <div className="admin-table-wrap"><table className="admin-table"><thead><tr>{section === 'games' ? <><th>{t('code')}</th><th>{t('name')}</th><th>{t('publisher')}</th><th>{t('status')}</th></> : section === 'products' ? <><th>{t('sku')}</th><th>{t('name')}</th><th>{t('game')}</th><th>{t('amountMinor')}</th><th>{t('status')}</th></> : <><th>{t('code')}</th><th>{t('name')}</th><th>{t('game')}</th><th>{t('region')}</th><th>{t('status')}</th></>}<th /></tr></thead><tbody>{list.map((item) => <tr key={item.id}>{section === 'games' ? <><td>{(item as Game).code}</td><td>{(item as Game).name}</td><td>{(item as Game).publisher || '—'}</td><td><StatusBadge value={(item as Game).status} t={t} /></td></> : section === 'products' ? <><td>{(item as Product).sku}</td><td>{(item as Product).name}</td><td>{gameName((item as Product).gameId)}</td><td>{(item as Product).amountMinor} {(item as Product).currency}</td><td><StatusBadge value={(item as Product).status} t={t} /></td></> : <><td>{(item as Server).code}</td><td>{(item as Server).name}</td><td>{gameName((item as Server).gameId)}</td><td>{(item as Server).region}</td><td><StatusBadge value={(item as Server).status} t={t} /></td></>}<td><button className="text-button" onClick={() => onEdit(section, item)}>{t('edit')}</button></td></tr>)}</tbody></table></div>
}

function StatusBadge({ value, t }: { value?: string; t: (key: TranslationKey) => string }) {
  return <span className={`status-badge ${value === 'ACTIVE' ? 'active' : 'inactive'}`}>{value === 'ACTIVE' ? t('active') : t('inactive')}</span>
}
