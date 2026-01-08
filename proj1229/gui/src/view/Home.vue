<script setup>
import { ref, inject, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { House } from '@element-plus/icons-vue';
import { showSuccess, showError } from '../utils/tool.js';
import RepoSidebar from '../components/RepoSidebar.vue';
import RepoWorkspace from '../components/RepoWorkspace.vue';
import UserExplorer from '../components/UserExplorer.vue';

const user = inject('user');
const router = useRouter();
const activeTab = ref('code');
const currentRepo = ref(null);
const allUsers = ref([]);
const exploreLoading = ref(false);
const showCreateDialog = ref(false);
const newRepoName = ref('');
const newRepoPublic = ref(true);
const createLoading = ref(false);

onMounted(async () => {
    if (!user.isLogin()) {
        router.push('/login');
        return;
    }
    user.setRepoCallback(() => {
        if (currentRepo.value && !user.repos.find(r => r.repoId === currentRepo.value.repoId)) {
            currentRepo.value = null;
        }
    });
    if (user.repos.length > 0) {
        selectRepo(user.repos[0]);
    }
});
const selectRepo = (repo) => {
    currentRepo.value = repo;
    activeTab.value = 'code';
};
const loadAllUsers = async () => {
    activeTab.value = 'explore';
    exploreLoading.value = true;
    try {
        const resp = await user.getAllUser();
        if (Array.isArray(resp)) {
            allUsers.value = resp;
        }
    } catch (e) {
        showError("获取用户列表失败");
    } finally {
        exploreLoading.value = false;
    }
};
const handleCreateRepo = async () => {
    if (!newRepoName.value) return;
    createLoading.value = true;
    try {
        await user.createRepo(newRepoName.value, newRepoPublic.value);
        showSuccess("仓库创建成功");
        showCreateDialog.value = false;
        newRepoName.value = '';
    } catch (e) {
        showError("创建失败");
    } finally {
        createLoading.value = false;
    }
};

const handleDeleteRepo = async (repoId) => {
    try {
        await user.delRepo(repoId);
        showSuccess("仓库已删除");
        currentRepo.value = null;
    } catch (e) {
        showError("删除失败");
    }
};

const logout = () => {
    user.logout();
    router.push('/login');
};
const handleExploreRepos = () => {
    router.push('/list');
};
</script>

<template>
    <el-container class="home-container">
        <RepoSidebar :user="user" :current-repo-id="currentRepo?.repoId" @select-repo="selectRepo"
            @create-repo="showCreateDialog = true" @show-explore="loadAllUsers" @explore-repos="handleExploreRepos"
            @logout="logout" />
        <el-main class="main-content">
            <UserExplorer v-if="activeTab === 'explore'" :users="allUsers" :loading="exploreLoading"
                @refresh="loadAllUsers" />
            <RepoWorkspace v-else-if="currentRepo" :repo="currentRepo" :readonly="false"
                @delete-repo="handleDeleteRepo" />
            <div v-else class="welcome-view">
                <el-icon size="64" color="#409eff">
                    <House />
                </el-icon>
                <h1>欢迎使用 Code Hub</h1>
                <p>请从左侧选择一个仓库开始工作,或者创建一个新仓库.</p>
            </div>
        </el-main>

        <el-dialog v-model="showCreateDialog" title="创建新仓库" width="30%">
            <el-form @submit.prevent> <!--修复按enter回菜单的bug-->
                <el-form-item label="仓库名称">
                    <el-input v-model="newRepoName" placeholder="my-project" @keyup.enter="handleCreateRepo" />
                </el-form-item>
                <el-form-item label="可见性">
                    <el-switch v-model="newRepoPublic" active-text="公开" inactive-text="私有" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showCreateDialog = false">取消</el-button>
                <el-button type="primary" :loading="createLoading" @click="handleCreateRepo">创建</el-button>
            </template>
        </el-dialog>
    </el-container>
</template>

<style scoped>
.home-container {
    height: 100vh;
    background-color: #0d1117;
    color: #c9d1d9;
}

.main-content {
    padding: 0;
    display: flex;
    flex-direction: column;
}

.welcome-view {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: #8b949e;
}
</style>