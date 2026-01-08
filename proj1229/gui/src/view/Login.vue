<script setup>
import { ref, inject } from 'vue';
import { useRouter } from 'vue-router';
import { User, Lock } from '@element-plus/icons-vue';
import { showWarning, showError, showSuccess } from '../utils/tool.js';
const user = inject('user');
const router = useRouter();
const username = ref('');
const password = ref('');
const isRegister = ref(false);
const loading = ref(false);

const toggleMode = () => {
    isRegister.value = !isRegister.value;
    password.value = '';
};
const handleSubmit = async () => {
    if (!username.value || !password.value) {
        showWarning('请输入用户名和密码');
        return;
    }
    loading.value = true;
    try {
        if (isRegister.value) {
            const success = await user.register(username.value, password.value);
            if (success) {
                showSuccess('注册成功，即将跳转...');
                if (await user.login(username.value, password.value)) {
                    router.push('/home');
                } else {
                    showError("发生未知错误");
                }
            } else {
                showError('注册失败，用户名可能已存在');
            }
            isRegister.value = false;
            password.value = '';
        } else {
            const success = await user.login(username.value, password.value);
            if (success) {
                showSuccess('登录成功');
                router.push('/home');
            } else {
                showError('登录失败，用户名或密码错误');
            }
        }
    } catch (e) {
        console.error(e);
        showError('网络错误或服务器异常');
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <div class="login-container">
        <el-card class="login-card">
            <template #header>
                <div class="card-header">
                    <h2>{{ isRegister ? '注册账号' : '系统登录' }}</h2>
                </div>
            </template>

            <el-form @keyup.enter="handleSubmit">
                <el-form-item>
                    <el-input v-model="username" placeholder="用户名" :prefix-icon="User" size="large" />
                </el-form-item>

                <el-form-item>
                    <el-input v-model="password" type="password" placeholder="密码" :prefix-icon="Lock" show-password
                        size="large" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit" size="large">
                        {{ isRegister ? '立即注册' : '登 录' }}
                    </el-button>
                </el-form-item>

                <div class="form-footer">
                    <el-link underline="never" class="toggle-link" @click="toggleMode">
                        {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
                    </el-link>
                </div>
            </el-form>
        </el-card>
    </div>
</template>

<style scoped src="../css/login.css"></style>