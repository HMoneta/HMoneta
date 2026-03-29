/**
 * plugins/vuetify.js
 *
 * Framework documentation: https://vuetifyjs.com`
 */

// Styles
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'

// Composables
import { createVuetify} from 'vuetify'
import { VFileUpload } from 'vuetify/labs/VFileUpload'

// https://vuetifyjs.com/en/introduction/why-vuetify/#feature-guides
export default createVuetify({
  theme: {
    defaultTheme: 'hmonetaDark',
    themes: {
      hmonetaDark: {
        dark: true,
        colors: {
          background: '#0a0a0a',
          surface: '#171717',
          'surface-bright': '#262626',
          'surface-light': '#262626',
          'surface-variant': '#404040',
          'on-surface-variant': '#a3a3a3',
          primary: '#10b981',
          secondary: '#6366f1',
          error: '#ef4444',
          warning: '#f59e0b',
          info: '#3b82f6',
          success: '#10b981',
          'on-background': '#e5e5e5',
          'on-surface': '#e5e5e5',
          'on-primary': '#ffffff',
          'on-secondary': '#ffffff',
        },
      },
      hmonetaLight: {
        dark: false,
        colors: {
          background: '#fafafa',
          surface: '#ffffff',
          'surface-bright': '#f5f5f5',
          'surface-light': '#f5f5f5',
          'surface-variant': '#e0e0e0',
          'on-surface-variant': '#616161',
          primary: '#10b981',
          secondary: '#6366f1',
          error: '#ef4444',
          warning: '#f59e0b',
          info: '#3b82f6',
          success: '#10b981',
          'on-background': '#1a1a1a',
          'on-surface': '#1a1a1a',
          'on-primary': '#ffffff',
          'on-secondary': '#ffffff',
        },
      },
    },
  },
  components: {
    VFileUpload,
  },
});
