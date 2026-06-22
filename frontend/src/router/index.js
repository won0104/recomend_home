import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import BoardListView from '../views/BoardListView.vue'
import BoardDetailView from '../views/BoardDetailView.vue'
import BoardFormView from '../views/BoardFormView.vue'
import NoticeListView from '../views/NoticeListView.vue'
import NoticeDetailView from '../views/NoticeDetailView.vue'
import NoticeFormView from '../views/NoticeFormView.vue'
import HouseListView from '../views/HouseListView.vue'
import HouseDetailView from '../views/HouseDetailView.vue'
import HouseFormView from '../views/HouseFormView.vue'
import MyPageView from '../views/MyPageView.vue'
import AdminUsersView from '../views/AdminUsersView.vue'
import NotFoundView from '../views/NotFoundView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/boards', name: 'boards', component: BoardListView },
    { path: '/boards/new', name: 'board-new', component: BoardFormView },
    { path: '/boards/:id', name: 'board-detail', component: BoardDetailView },
    { path: '/boards/:id/edit', name: 'board-edit', component: BoardFormView },
    { path: '/notices', name: 'notices', component: NoticeListView },
    { path: '/notices/new', name: 'notice-new', component: NoticeFormView },
    { path: '/notices/:id', name: 'notice-detail', component: NoticeDetailView },
    { path: '/notices/:id/edit', name: 'notice-edit', component: NoticeFormView },
    { path: '/houses', name: 'houses', component: HouseListView },
    { path: '/houses/:aptSeq', name: 'house-detail', component: HouseDetailView },
    { path: '/houses/:aptSeq/edit', name: 'house-edit', component: HouseFormView },
    { path: '/mypage', name: 'mypage', component: MyPageView },
    { path: '/admin/users', name: 'admin-users', component: AdminUsersView },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
  ]
})

export default router
