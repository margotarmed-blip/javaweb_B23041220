import { createApp, reactive } from 'vue'
import App from './App.vue'
import router from './router'
import { User } from './utils/api.js'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import ElementPlus from 'element-plus'
document.documentElement.classList.add('dark');
const user = new User();
window.user = user;
const app = createApp(App);
app.provide('user', reactive(user));
app.use(ElementPlus);
app.use(router);
app.mount('#app');
