// Stands in for vite-plugin-pwa's virtual module, which only exists in a Vite build; tests mock it as they need.
export function useRegisterSW() {
  return { needRefresh: [false, () => {}], offlineReady: [false, () => {}], updateServiceWorker: async () => {} }
}
