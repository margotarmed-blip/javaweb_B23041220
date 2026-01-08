<script setup>
import { ref, inject } from 'vue';
import { Edit, Delete, Warning } from '@element-plus/icons-vue';
import { showSuccess, showError } from '../utils/tool.js';

const props = defineProps({
    users: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false }
});

const currentUser = inject('user');
const emit = defineEmits(['refresh']);

const showEditDialog = ref(false);
const editForm = ref({ id: 0, name: '', permission: 0 });
const editLoading = ref(false);

const openEdit = (row) => {
    editForm.value = { ...row };
    showEditDialog.value = true;
};

const handleUpdatePermission = async () => {
    editLoading.value = true;
    try {
        const success = await currentUser.updateUserPermission(editForm.value.id, parseInt(editForm.value.permission));
        if (success) {
            showSuccess(`用户 ${editForm.value.name} 权限已更新`);
            showEditDialog.value = false;
            emit('refresh');
        } else {
            showError("更新失败");
        }
    } catch (e) {
        showError("操作异常: " + e.message);
    } finally {
        editLoading.value = false;
    }
};

const handleDeleteUser = async (row) => {
    if (row.id === currentUser.userId) {
        showError("不能删除自己");
        return;
    }
    try {
        const success = await currentUser.deleteUser(row.id);
        if (success) {
            showSuccess(`用户 ${row.name} 已删除`);
            emit('refresh');
        } else {
            showError("删除失败");
        }
    } catch (e) {
        showError("操作异常: " + e.message);
    }
};
</script>

<template>
    <div class="explore-view">
        <div class="header-row">
            <h2>用户管理</h2>
            <el-button :icon="Warning" link @click="emit('refresh')">刷新列表</el-button>
        </div>
        <el-table :data="users" style="width: 100%" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="name" label="用户名" />
            <el-table-column prop="permission" label="权限等级" width="120" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.permission > 0 ? 'danger' : 'info'">
                        {{ scope.row.permission > 0 ? '管理员' : '普通用户' }}
                    </el-tag>
                </template>
            </el-table-column>

            <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                    <el-button type="primary" link :icon="Edit" @click="openEdit(scope.row)">
                        权限
                    </el-button>
                    <el-popconfirm title="确定要删除此用户吗？这将同时删除其所有仓库！" confirm-button-text="删除" cancel-button-text="取消"
                        confirm-button-type="danger" @confirm="handleDeleteUser(scope.row)">
                        <template #reference>
                            <el-button type="danger" link :icon="Delete"
                                :disabled="scope.row.id === currentUser.userId">
                                删除
                            </el-button>
                        </template>
                    </el-popconfirm>
                </template>
            </el-table-column>
        </el-table>
        <el-dialog v-model="showEditDialog" title="修改用户权限" width="400px">
            <el-form :model="editForm" label-width="80px">
                <el-form-item label="用户名">
                    <el-input v-model="editForm.name" disabled />
                </el-form-item>
                <el-form-item label="权限等级">
                    <el-input-number v-model="editForm.permission" :min="0" :max="99" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showEditDialog = false">取消</el-button>
                <el-button type="primary" :loading="editLoading" @click="handleUpdatePermission">
                    保存修改
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.explore-view {
    padding: 20px;
    color: #c9d1d9;
}

.header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.tip-text {
    font-size: 12px;
    color: #8b949e;
    margin-top: 5px;
}
</style>