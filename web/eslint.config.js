import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

// Mirrors admin/eslint.config.js, with two deliberate differences:
//  - no eslint-plugin-react-refresh: it is not a dependency here.
//  - reactHooks.configs['recommended-latest'] instead of .configs.flat.recommended,
//    because this package is on eslint-plugin-react-hooks 5.x, which does not
//    expose a `flat` namespace (admin is on 7.x, which does).
export default defineConfig([
  globalIgnores(['dist', 'playwright-report', 'test-results']),
  {
    files: ['src/**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs['recommended-latest'],
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
  },
  {
    // Build/test config and Playwright specs run under Node, not the browser.
    files: ['*.config.ts', 'e2e/**/*.ts'],
    extends: [js.configs.recommended, tseslint.configs.recommended],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.node,
    },
  },
])
