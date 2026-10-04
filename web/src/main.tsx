import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { OrgProvider } from './auth/OrgContext'
import { SessionQueryProvider } from './auth/SessionQueryProvider'
import { OnboardingProvider } from './onboarding/OnboardingContext'
import { App } from './App'
import './i18n'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <OrgProvider>
          <SessionQueryProvider>
            <OnboardingProvider>
              <App />
            </OnboardingProvider>
          </SessionQueryProvider>
        </OrgProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
