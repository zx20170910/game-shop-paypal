import { capturePayPalOrder, createPayPalOrder, CheckoutResponse } from './api'

type PayPalPaymentMethods = {
  isEligible: (method: string) => boolean
}

type PayPalPaymentSession = {
  start: (options: { presentationMode: 'auto' }, order: Promise<{ orderId: string }>) => Promise<void>
}

type PayPalSdkInstance = {
  findEligibleMethods: (options: { currencyCode: string }) => Promise<PayPalPaymentMethods>
  createPayPalOneTimePaymentSession: (callbacks: {
    onApprove: (data: { orderId: string }) => Promise<void>
    onCancel: () => void
    onError: (error: unknown) => void
  }) => PayPalPaymentSession
}

type PayPalGlobal = {
  createInstance: (options: {
    clientId: string
    components: string[]
    pageType: 'checkout'
    locale: string
  }) => Promise<PayPalSdkInstance>
}

declare global {
  interface Window {
    paypal?: PayPalGlobal
  }
}

const sdkUrl = import.meta.env.VITE_PAYPAL_SDK_URL ?? 'https://www.sandbox.paypal.com/web-sdk/v6/core'
const clientId = import.meta.env.VITE_PAYPAL_CLIENT_ID ?? ''

function loadSdk(): Promise<PayPalGlobal> {
  if (!clientId) return Promise.reject(new Error('PayPal client ID is not configured'))
  if (window.paypal) return Promise.resolve(window.paypal)

  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-paypal-v6]')
    if (existing) {
      existing.addEventListener('load', () => window.paypal ? resolve(window.paypal) : reject(new Error('PayPal SDK was not initialized')))
      existing.addEventListener('error', () => reject(new Error('PayPal SDK failed to load')))
      return
    }
    const script = document.createElement('script')
    script.src = sdkUrl
    script.async = true
    script.dataset.paypalV6 = 'true'
    script.onload = () => window.paypal ? resolve(window.paypal) : reject(new Error('PayPal SDK was not initialized'))
    script.onerror = () => reject(new Error('PayPal SDK failed to load'))
    document.head.appendChild(script)
  })
}

export type PayPalButtonState = 'loading' | 'ready' | 'unconfigured' | 'unavailable' | 'creating' | 'capturing' | 'error'

export async function preparePayPalButton(
  container: HTMLDivElement,
  checkout: CheckoutResponse,
  locale: string,
  callbacks: {
    onState: (state: PayPalButtonState) => void
    onCaptured: (result: Awaited<ReturnType<typeof capturePayPalOrder>>) => void
    onMessage: (message: 'cancelled' | 'error') => void
  },
) {
  if (!clientId) {
    callbacks.onState('unconfigured')
    return () => undefined
  }

  callbacks.onState('loading')
  const sdk = await loadSdk()
  const instance = await sdk.createInstance({ clientId, components: ['paypal-payments'], pageType: 'checkout', locale })
  const methods = await instance.findEligibleMethods({ currencyCode: checkout.currency })
  if (!methods.isEligible('paypal')) {
    callbacks.onState('unavailable')
    return () => undefined
  }

  const paymentSession = instance.createPayPalOneTimePaymentSession({
    async onApprove(data) {
      callbacks.onState('capturing')
      try {
        const result = await capturePayPalOrder(data.orderId, `paypal-capture-${checkout.orderId}`)
        callbacks.onCaptured(result)
        callbacks.onState('ready')
      } catch {
        callbacks.onState('error')
        callbacks.onMessage('error')
      }
    },
    onCancel() {
      callbacks.onState('ready')
      callbacks.onMessage('cancelled')
    },
    onError() {
      callbacks.onState('error')
      callbacks.onMessage('error')
    },
  })

  const button = document.createElement('paypal-button')
  button.setAttribute('hidden', 'true')
  const onClick = async () => {
    callbacks.onState('creating')
    try {
      const paymentAttempt = await createPayPalOrder(checkout.sessionId, `paypal-create-${checkout.orderId}`)
      button.removeAttribute('hidden')
      callbacks.onState('ready')
      await paymentSession.start({ presentationMode: 'auto' }, Promise.resolve({ orderId: paymentAttempt.paypalOrderId }))
    } catch {
      callbacks.onState('error')
      callbacks.onMessage('error')
    }
  }
  button.addEventListener('click', onClick)
  container.replaceChildren(button)
  button.removeAttribute('hidden')
  callbacks.onState('ready')
  return () => {
    button.removeEventListener('click', onClick)
    button.remove()
  }
}
