<script setup>
import { Plus, Document, User as UserIcon, Share, Search } from '@element-plus/icons-vue';

const props = defineProps({
    user: { type: Object, required: true },
    currentRepoId: { type: Number, default: null }
});

const emit = defineEmits(['select-repo', 'create-repo', 'show-explore', 'logout', 'explore-repos']);
</script>

<template>
    <el-aside width="260px" class="sidebar">
        <div class="sidebar-header">
            <div class="user-info">
                <el-avatar :icon="UserIcon" class="avatar" />
                <span class="username">{{ user.username }}</span>
            </div>
            <el-button type="danger" link size="small" @click="emit('logout')">退出</el-button>
        </div>

        <div class="repo-section">
            <div class="section-title">
                <span>我的仓库</span>
                <el-button type="primary" size="small" :icon="Plus" circle @click="emit('create-repo')" />
            </div>
            <el-scrollbar>
                <ul class="repo-list">
                    <li v-for="repo in user.repos" :key="repo.repoId" 
                        :class="{ active: currentRepoId === repo.repoId }"
                        @click="emit('select-repo', repo)">
                        <el-icon><Document /></el-icon>
                        <span class="repo-name">{{ repo.name }}</span>
                        <el-tag size="small" effect="dark" class="repo-tag">
                            {{ repo.is_public ? 'Public' : 'Private' }}
                        </el-tag>
                    </li>
                </ul>
            </el-scrollbar>
        </div>
        
        <div class="sidebar-footer">
            <el-button type="primary" plain class="footer-btn" :icon="Search" @click="emit('explore-repos')">
                发现公开仓库
            </el-button>
            <el-button type="info" plain class="footer-btn" :icon="Share" @click="emit('show-explore')" v-if="user.isAdmin()">
                用户管理 (Admin)
            </el-button>
        </div>
    </el-aside>
</template>

<style scoped src="../css/repo-sidebar.css"></style>