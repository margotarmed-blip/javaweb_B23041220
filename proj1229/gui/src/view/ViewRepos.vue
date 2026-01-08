<script setup>
import { ref, inject, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Search, Back, CopyDocument, User as UserIcon, Refresh, View, Delete } from '@element-plus/icons-vue';
import { showSuccess, showError, showInfo } from '../utils/tool.js';
import { Repo } from '../utils/api.js';
import RepoWorkspace from '../components/RepoWorkspace.vue';
const user = inject('user');
const router = useRouter();
const viewMode = ref('list');
const keyword = ref('');
const repoList = ref([]);
const loading = ref(false);
const forkingId = ref(null);
const selectedRepo = ref(null);
onMounted(() => {
    if (!user.isLogin()) {
        router.push('/login');
        return;
    }
    handleSearch();
});
const handleAdminDelete = async (repoId) => {
    try {
        await user.delRepo(repoId);
        showSuccess("已强制删除仓库");
        handleSearch();
    } catch (e) {
        showError("删除失败");
    }
};
const handleSearch = async () => {
    loading.value = true;
    try {
        const res = await user.searchPublicRepos(keyword.value);
        repoList.value = res || [];
        if (repoList.value.length === 0 && keyword.value) {
            showInfo("未找到匹配的公开仓库");
        }
    } catch (e) {
        showError("搜索失败: " + e.message);
    } finally {
        loading.value = false;
    }
};
const handleView = (repoData) => {
    const isPublicVal = (repoData.isPublic !== undefined) ? repoData.isPublic : repoData.public;
    const repoInstance = new Repo(
        repoData.id,
        repoData.name,
        repoData.owner,
        isPublicVal,
        repoData.commitId || repoData.commit_id,
        user.sessionId
    );

    selectedRepo.value = repoInstance;
    viewMode.value = 'detail';
};

const backToList = () => {
    selectedRepo.value = null;
    viewMode.value = 'list';
};
const handleFork = async (repo) => {
    if (forkingId.value) return;
    if (repo.owner === user.userId) {
        showInfo("不可以Fork自己的仓库");
        return;
    }
    forkingId.value = repo.id;
    try {
        await user.fork(repo.id);
        showSuccess(`成功 Fork 仓库: ${repo.name}`);
    } catch (e) {
        showError("Fork 失败");
    } finally {
        forkingId.value = null;
    }
};

const goHome = () => {
    router.push('/home');
};
</script>

<template>
    <div class="explore-container">
        <div class="header">
            <div class="header-left">
                <el-button v-if="viewMode === 'detail'" link :icon="Back" @click="backToList" class="back-btn">
                    返回搜索列表
                </el-button>
                <el-button v-else link :icon="Back" @click="goHome" class="back-btn">
                    返回工作台
                </el-button>
            </div>
            <div class="user-badge">
                <el-avatar :size="32" :icon="UserIcon" />
                <span>{{ user.username }}</span>
            </div>
        </div>
        <div v-if="viewMode === 'list'" class="list-view">
            <div class="search-section">
                <div class="search-box">
                    <el-input v-model="keyword" placeholder="搜索仓库名称..." class="search-input" size="large"
                        @keyup.enter="handleSearch" clearable>
                        <template #prefix><el-icon>
                                <Search />
                            </el-icon></template>
                    </el-input>
                    <el-button type="primary" size="large" @click="handleSearch" :loading="loading">搜索</el-button>
                </div>
            </div>
            <div class="repo-grid-container" v-loading="loading">
                <div v-if="repoList.length > 0" class="repo-grid">
                    <div v-for="repo in repoList" :key="repo.id" class="repo-card">
                        <div class="card-header">
                            <div class="repo-icon"><el-icon>
                                    <CopyDocument />
                                </el-icon></div>
                            <div class="repo-info">
                                <h3 class="repo-name" @click="handleView(repo)">
                                    {{ repo.name }}
                                    <el-tag v-if="!repo.isPublic && !repo.public" size="small" type="info" effect="dark"
                                        style="margin-left: 8px">
                                        Private
                                    </el-tag>
                                </h3>
                                <span class="repo-owner">Owner ID: {{ repo.owner }}</span>
                            </div>
                        </div>

                        <div class="card-body">
                            <div class="meta-item">
                                <span class="label">Latest Commit:</span>
                                <span class="value">{{ repo.commitId ? repo.commitId.substring(0, 7) : 'Initial'
                                }}</span>
                            </div>
                        </div>

                        <div class="card-footer">
                            <el-button-group style="width: 100%; display: flex;">
                                <el-button type="info" plain style="flex: 1" :icon="View" @click="handleView(repo)">
                                    浏览代码
                                </el-button>
                                <el-popconfirm v-if="user.isAdmin()" title="确定要强制删除此仓库吗？"
                                    @confirm="handleAdminDelete(repo.id)">
                                    <template #reference>
                                        <el-button type="danger" plain style="flex: 1" :icon="Delete">删除</el-button>
                                    </template>
                                </el-popconfirm>
                                <el-button type="primary" plain style="flex: 1" :icon="Refresh"
                                    :loading="forkingId === repo.id" @click="handleFork(repo)">
                                    Fork
                                </el-button>
                            </el-button-group>
                        </div>
                    </div>
                </div>
                <div v-else-if="!loading" class="empty-state">
                    <el-empty description="暂无公开仓库" />
                </div>
            </div>
        </div>
        <div v-else-if="viewMode === 'detail' && selectedRepo" class="detail-view">
            <RepoWorkspace :repo="selectedRepo" :readonly="true" />
        </div>
    </div>
</template>

<style scoped src="../css/view-repos.css"></style>