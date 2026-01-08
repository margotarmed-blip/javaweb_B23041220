<script setup>
import { ref, computed, watch, nextTick } from 'vue';
import {
    Folder, Document, Upload, Check, Delete, Back, FolderAdd, Download, Edit, Clock
} from '@element-plus/icons-vue';
import { showSuccess, showError, showWarning } from '../utils/tool.js';
import JSZip from 'jszip';
import { saveAs } from 'file-saver';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import '../css/markdown.css';
const props = defineProps({
    repo: { type: Object, required: true },
    readonly: { type: Boolean, default: false }
});
const emit = defineEmits(['delete-repo']);
const currentPath = ref([]);
const currentFile = ref(null);
const fileContent = ref('');
const isEditing = ref(false);
const loading = ref(false);
const showCommitDialog = ref(false);
const commitMsg = ref('');
const readmeHtml = ref('');
const visibilityLoading = ref(false);
const showHistoryDialog = ref(false);
const commitHistory = ref([]);
const historyLoading = ref(false);
const fetchHistory = async () => {
    historyLoading.value = true;
    showHistoryDialog.value = true;
    try {
        const data = await props.repo.getHistory();
        if (Array.isArray(data)) {
            commitHistory.value = data;
        }
    } catch (e) {
        showError("获取历史失败");
    } finally {
        historyLoading.value = false;
    }
};
const formatDate = (ts) => {
    return new Date(ts).toLocaleString();
};
const handleToggleVisibility = async () => {
    if (props.readonly) return;

    const actionName = props.repo.is_public ? "私有 (Private)" : "公开 (Public)";
    if (!confirm(`确定要将仓库状态更改为 ${actionName} 吗？`)) return;

    visibilityLoading.value = true;
    try {
        const success = await props.repo.toggleVisibility();
        if (success) {
            showSuccess(`仓库已切换为 ${props.repo.is_public ? 'Public' : 'Private'}`);
        } else {
            showError("切换状态失败");
        }
    } catch (e) {
        showError("操作异常: " + e.message);
    } finally {
        visibilityLoading.value = false;
    }
};
const currentDirFiles = computed(() => {
    if (!props.repo) return [];
    let node = props.repo.root;
    for (const name of currentPath.value) {
        node = node.children.find(c => c.name === name);
        if (!node) return [];
    }
    return [...node.children].sort((a, b) => {
        const aIsFile = a.isFile();
        const bIsFile = b.isFile();
        if (aIsFile === bIsFile) return a.name.localeCompare(b.name);
        return aIsFile ? 1 : -1;
    });
});

const loadReadme = async () => {
    readmeHtml.value = '';
    const readmeFile = currentDirFiles.value.find(f => f.isFile() && f.name.toLowerCase() === 'readme.md');
    if (readmeFile) {
        try {
            const content = await readmeFile.tryDecodeUTF8();
            if (content) readmeHtml.value = DOMPurify.sanitize(marked.parse(content));
        } catch (e) {
            console.error("Failed to load README", e);
        }
    }
};

watch(() => props.repo, async (newRepo) => {
    if (newRepo) {
        currentPath.value = [];
        currentFile.value = null;
        isEditing.value = false;
        loading.value = true;
        try {
            await newRepo.syncData();
        } finally {
            loading.value = false;
        }
    }
}, { immediate: true });

watch(currentDirFiles, () => {
    nextTick(() => {
        loadReadme();
    });
}, { deep: true });

const breadcrumbList = computed(() => {
    const list = [{ name: props.repo?.name || 'Root', path: [] }];
    const tempPath = [];
    for (const p of currentPath.value) {
        tempPath.push(p);
        list.push({ name: p, path: [...tempPath] });
    }
    return list;
});

const navigateTo = (pathArray) => {
    currentPath.value = pathArray;
    currentFile.value = null;
    isEditing.value = false;
};

const enterFolder = (folder) => {
    currentPath.value.push(folder.name);
    isEditing.value = false;
    currentFile.value = null;
};

const openFile = async (file) => {
    if (currentFile.value && currentFile.value.name === file.name) return;

    loading.value = true;
    try {
        const content = await file.tryDecodeUTF8();
        if (content !== null) {
            fileContent.value = content;
            currentFile.value = file;
            isEditing.value = true;
            nextTick(() => {
                const editorEl = document.querySelector('.file-editor');
                if (editorEl) editorEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
        } else {
            showWarning("二进制文件无法在线预览/编辑");
        }
    } catch (e) {
        showError("读取文件失败");
    } finally {
        loading.value = false;
    }
};

const closeFile = () => {
    currentFile.value = null;
    isEditing.value = false;
    fileContent.value = '';
};

const saveFile = async () => {
    if (props.readonly) return;
    if (!currentFile.value) return;
    try {
        await currentFile.value.uploadBlob(fileContent.value);
        showSuccess("文件已暂存，请记得提交(Commit)版本");
    } catch (e) {
        showError("保存失败");
    }
};

const deleteItem = async (item) => {
    if (props.readonly) return;
    try {
        const pathPrefix = currentPath.value.join('/');
        const fullPath = pathPrefix ? `${pathPrefix}/${item.name}` : item.name;
        const success = await props.repo.removeFile(fullPath);
        if (currentFile.value && currentFile.value.name === item.name) closeFile();
        if (success) showSuccess("已删除 (未提交)");
        else showError("删除失败");
    } catch (e) {
        showError("操作异常");
    }
};
const triggerUpload = () => document.getElementById('file-input').click();
const triggerFolderUpload = () => document.getElementById('folder-input').click();
const handleFileUpload = async (event) => {
    if (props.readonly) return;
    const files = event.target.files;
    if (!files.length) return;
    loading.value = true;
    try {
        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            const arrayBuffer = await file.arrayBuffer();
            const pathPrefix = currentPath.value.join('/');
            const fullPath = pathPrefix ? `${pathPrefix}/${file.name}` : file.name;
            await props.repo.uploadFile(fullPath, new Uint8Array(arrayBuffer));
        }
        showSuccess(`成功上传 ${files.length} 个文件`);
        commitMsg.value = `Upload ${files.length} files`;
        showCommitDialog.value = true;
        const temp = currentPath.value;
        currentPath.value = [];
        nextTick(() => currentPath.value = temp);
    } catch (e) {
        showError("上传失败");
    } finally {
        loading.value = false;
        event.target.value = '';
    }
};
const handleFolderUpload = async (event) => {
    if (props.readonly) return;
    const files = event.target.files;
    if (!files.length) return;
    loading.value = true;
    try {
        const currentPrefix = currentPath.value.join('/');
        let count = 0;
        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            const relativePath = file.webkitRelativePath;
            if (!relativePath) continue;
            const fullPath = currentPrefix ? `${currentPrefix}/${relativePath}` : relativePath;
            const arrayBuffer = await file.arrayBuffer();
            await props.repo.uploadFile(fullPath, new Uint8Array(arrayBuffer));
            count++;
        }
        showSuccess(`成功加载文件夹结构，包含 ${count} 个文件`);
        commitMsg.value = `Upload folder structure`;
        showCommitDialog.value = true;
        const temp = currentPath.value;
        currentPath.value = [];
        nextTick(() => currentPath.value = temp);
    } catch (e) {
        showError("文件夹解析失败");
    } finally {
        loading.value = false;
        event.target.value = '';
    }
};
const handleCommit = async () => {
    if (props.readonly) return;
    if (!commitMsg.value) {
        showWarning("请输入提交信息");
        return;
    }
    loading.value = true;
    try {
        const success = await props.repo.commit(commitMsg.value);
        if (success) {
            showSuccess("提交成功");
            showCommitDialog.value = false;
            commitMsg.value = '';
            await props.repo.syncData();
        } else {
            showError("提交失败");
        }
    } catch (e) {
        showError("提交异常: " + e.message);
    } finally {
        loading.value = false;
    }
};

const handleDownloadRepo = async () => {
    loading.value = true;
    try {
        const zip = new JSZip();
        const repoName = props.repo.name;
        const processNode = async (node, zipFolder) => {
            const promises = node.children.map(async (child) => {
                if (child.isFile()) {
                    const content = await child.getBlob();
                    if (content) zipFolder.file(child.name, content);
                } else {
                    const newFolder = zipFolder.folder(child.name);
                    await processNode(child, newFolder);
                }
            });
            await Promise.all(promises);
        };
        showSuccess("开始打包下载...");
        await processNode(props.repo.root, zip);
        const content = await zip.generateAsync({ type: "blob" });
        saveAs(content, `${repoName}.zip`);
        showSuccess("下载完成");
    } catch (e) {
        showError("下载失败: " + e.message);
    } finally {
        loading.value = false;
    }
};

const handleDeleteRepo = async () => {
    if (props.readonly) return;
    if (!confirm(`确定要删除仓库 ${props.repo.name} 吗?`)) return;
    emit('delete-repo', props.repo.repoId);
};
</script>

<template>
    <div class="repo-view" v-loading="loading">
        <div class="repo-header">
            <div class="repo-title">
                <h2>{{ repo.name }}</h2>
                <el-tag :type="repo.is_public ? 'success' : 'warning'" class="visibility-tag"
                    @click="handleToggleVisibility" v-loading="visibilityLoading">
                    {{ repo.is_public ? 'Public' : 'Private' }}
                </el-tag>
                <span class="commit-id" v-if="repo.commitId">
                    Latest Commit: {{ repo.commitId.substring(0, 7) }}
                </span>
            </div>
            <div class="repo-actions">
                <el-button type="info" plain :icon="Clock" @click="fetchHistory">历史记录</el-button>
                <el-button type="warning" plain :icon="Download" @click="handleDownloadRepo">下载 ZIP</el-button>
                <template v-if="!readonly">
                    <el-button type="danger" plain :icon="Delete" @click="handleDeleteRepo">删除仓库</el-button>
                    <el-button type="primary" :icon="Check" @click="showCommitDialog = true">提交更改</el-button>
                </template>
            </div>
        </div>
        <div class="file-toolbar">
            <el-breadcrumb separator="/">
                <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
                    <a @click="navigateTo(item.path)" style="cursor: pointer; font-weight: bold;">
                        {{ item.name }}
                    </a>
                </el-breadcrumb-item>
            </el-breadcrumb>

            <div class="file-actions">
                <template v-if="!isEditing && !readonly">
                    <input type="file" id="file-input" multiple style="display: none" @change="handleFileUpload">
                    <input type="file" id="folder-input" webkitdirectory directory multiple style="display: none"
                        @change="handleFolderUpload">
                    <el-button-group>
                        <el-button type="primary" size="small" :icon="Upload" @click="triggerUpload">上传文件</el-button>
                        <el-button type="primary" size="small" :icon="FolderAdd"
                            @click="triggerFolderUpload">上传文件夹</el-button>
                    </el-button-group>
                </template>
            </div>
        </div>

        <div class="file-browser">
            <el-table :data="currentDirFiles" style="width: 100%"
                @row-click="(row) => row.isFile() ? openFile(row) : enterFolder(row)">
                <el-table-column width="40">
                    <template #default="scope">
                        <el-icon v-if="!scope.row.isFile()">
                            <Folder style="color: #409eff" />
                        </el-icon>
                        <el-icon v-else>
                            <Document style="color: #909399" />
                        </el-icon>
                    </template>
                </el-table-column>
                <el-table-column label="名称" prop="name">
                    <template #default="scope">
                        <span class="file-name"
                            :class="{ 'active-file': currentFile && currentFile.name === scope.row.name }">
                            {{ scope.row.name }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="Hash" prop="hash" width="150">
                    <template #default="scope">
                        <span class="hash-text">{{ scope.row.hash ? scope.row.hash.substring(0, 7) : '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column align="right" width="100" v-if="!readonly">
                    <template #default="scope">
                        <el-button type="danger" link :icon="Delete" @click.stop="deleteItem(scope.row)"></el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div v-if="currentDirFiles.length === 0" class="empty-dir">此文件夹为空</div>

            <div v-if="isEditing" class="file-editor">
                <div class="editor-header">
                    <div class="editor-title">
                        <el-icon>
                            <Edit />
                        </el-icon>
                        <span>正在编辑: {{ currentFile.name }}</span>
                        <span v-if="readonly" style="margin-left: 8px; font-weight: normal; color: #8b949e;">(只读)</span>
                    </div>
                    <div class="editor-actions">
                        <el-button size="small" :icon="Back" @click="closeFile">关闭</el-button>
                        <el-button v-if="!readonly" type="success" size="small" :icon="Check"
                            @click="saveFile">暂存保存</el-button>
                    </div>
                </div>
                <el-input v-model="fileContent" type="textarea" :rows="20" :readonly="readonly" placeholder="请输入代码..."
                    class="code-input" />
            </div>

            <div v-else-if="readmeHtml" class="readme-container">
                <div class="readme-header">
                    <el-icon>
                        <Document />
                    </el-icon> README.md
                </div>
                <div class="markdown-body" v-html="readmeHtml"></div>
            </div>
        </div>

        <el-dialog v-model="showCommitDialog" title="提交更改" width="40%">
            <el-input v-model="commitMsg" type="textarea" placeholder="请输入提交信息 (Commit Message)" :rows="3" />
            <template #footer>
                <el-button @click="showCommitDialog = false">取消</el-button>
                <el-button type="primary" @click="handleCommit">提交 (Commit)</el-button>
            </template>
        </el-dialog>
        <el-dialog v-model="showHistoryDialog" title="提交历史 (Commit History)" width="60%">
            <el-table :data="commitHistory" v-loading="historyLoading" stripe>
                <el-table-column label="Commit Hash" width="120">
                    <template #default="scope">
                        <el-tag size="small" font-family="monospace">{{ scope.row.id.substring(0, 7) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="message" label="提交信息" />
                <el-table-column label="提交者 ID" prop="ownerId" width="100" />
                <el-table-column label="时间" width="180">
                    <template #default="scope">
                        {{ formatDate(scope.row.time) }}
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>

    </div>
</template>

<style scoped src="../css/repo-workspace.css"></style>