import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './en.json'
import sv from './sv.json'

const savedLang = localStorage.getItem('verdant-lang') ?? 'sv'

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      sv: { translation: sv },
    },
    lng: savedLang,
    fallbackLng: 'sv',
    interpolation: { escapeValue: false },
  })

export default i18n
