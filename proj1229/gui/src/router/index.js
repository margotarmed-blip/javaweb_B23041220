import { createRouter, createWebHistory } from 'vue-router';
import Login from '../view/Login.vue';
import Home from '../view/Home.vue';
import Repos from '../view/ViewRepos.vue';
const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/home', component: Home },
    { path: '/list', component: Repos },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;