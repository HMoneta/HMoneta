/**
 * main.js
 *
 * Bootstraps Vuetify and other plugins then mounts the App`
 */

// Plugins
import { registerPlugins } from '@/plugins'

// Components
import App from './App.vue'

// Composables
import { createApp } from 'vue'

import { createPinia } from 'pinia';

// Styles
import 'unfonts.css'
import router from "@/router/index.js";
import {useUserStore} from "@/stores/app.js";

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
registerPlugins(app)

const userStore = useUserStore();
userStore.checkAuth();
app.mount('#app')

// 确保路由准备好后再挂载应用
// router.isReady().then(() => {
//   app.mount('#app')
// })
