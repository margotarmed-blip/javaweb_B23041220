async function sha256(message) {
    let msgBuffer;
    if (typeof message === 'string') {
        msgBuffer = new TextEncoder().encode(message);
    } else {
        msgBuffer = message;
    }
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

function bufferToBase64(buffer) {
    let binary = '';
    const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
    for (let i = 0; i < bytes.byteLength; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
}

function base64ToArrayBuffer(base64) {
    const binaryString = window.atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

async function post(method_type, body_raw) {
    const payload = { type: method_type, body: body_raw };
    try {
        const response = await fetch("/api", {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        return response;
    } catch (e) {
        console.error(e);
        return null;
    }
}

async function handle(res) {
    if (!res) return null;
    return await res.json();
}

const API = {};

API.login = async function (name, pwdHash) {
    const res = await post("Login", JSON.stringify({ name, pwdHash }));
    return await handle(res);
}

API.register = async function (name, pwdHash) {
    const res = await post("Register", JSON.stringify({ name, pwdHash }));
    return await handle(res);
}
API.updateRepo = async function (sessionId, repoId, isPublic) {
    const res = await post("UpdateRepo", JSON.stringify({ sessionId, repoId, isPublic }));
    return await handle(res);
}

API.getRepo = async function (sessionId) {
    const res = await post("GetRepo", JSON.stringify({ sessionId }));
    return await handle(res);
}
API.getCommitHistory = async function (sessionId, repoId) {
    const res = await post("GetCommitHistory", JSON.stringify({ sessionId, repoId }));
    return await handle(res);
}
API.createRepo = async function (sessionId, name, isPublic) {
    const res = await post("CreateRepo", JSON.stringify({ sessionId, name, isPublic }));
    return await handle(res);
}

API.delRepo = async function (sessionId, repoId) {
    const res = await post("DelRepo", JSON.stringify({ sessionId, repoId }));
    return await handle(res);
}

API.fork = async function (sessionId, repoId) {
    const res = await post("Fork", JSON.stringify({ sessionId, repoId }));
    return await handle(res);
}

API.getLatestFiles = async function (sessionId, repoId) {
    const res = await post("GetLatestFiles", JSON.stringify({ sessionId, repoId }));
    return await handle(res);
}
API.searchRepo = async function (sessionId, keyword) {
    const res = await post("SearchRepo", JSON.stringify({ sessionId, keyword }));
    return await handle(res);
}
API.commit = async function (sessionId, repoId, message, tree) {
    const res = await post("Commit", JSON.stringify({ sessionId, repoId, message, tree }));
    return await handle(res);
}

API.getBlob = async function (sessionId, filename, hash) {
    const res = await post("GetBlob", JSON.stringify({ sessionId, filename, hash }));
    return await handle(res);
}

API.uploadBlob = async function (sessionId, filename, hash, content) {
    const res = await post("UploadBlob", JSON.stringify({ sessionId, filename, hash, content }));
    return await handle(res);
}

API.getAllUser = async function (sessionId) {
    const res = await post("GetAllUser", JSON.stringify({ sessionId }));
    return await handle(res);
}
API.updateUserPermission = async function (sessionId, targetUserId, newPermission) {
    const res = await post("UpdateUserPermission", JSON.stringify({ sessionId, targetUserId, newPermission }));
    return await handle(res);
}

API.deleteUser = async function (sessionId, targetUserId) {
    const res = await post("DeleteUser", JSON.stringify({ sessionId, targetUserId }));
    return await handle(res);
}
class File {
    constructor(name, hash, root) {
        this.name = name;
        this.hash = hash;
        this.children = [];
        this.root = root;
        this.content = null;
    }
    isText() {
        const lowerName = this.name.toLowerCase();
        const parts = lowerName.split('.');
        const extension = parts.length > 1 ? parts.pop() : '';
        const textExtensions = new Set([
            'js', 'mjs', 'cjs', 'ts', 'tsx', 'jsx', 'vue', 'java', 'c', 'cpp', 'h', 'hpp', 'cc', 'cxx',
            'cs', 'go', 'rs', 'rb', 'php', 'swift', 'kt', 'kts', 'py', 'pyw', 'pl', 'pm', 'lua', 'dart',
            'sh', 'bash', 'zsh', 'ps1', 'bat', 'cmd', 'sql', 'r', 'scala', 'erl', 'ex', 'exs', 'hs', 'clj',
            'html', 'htm', 'xhtml', 'xml', 'svg', 'css', 'scss', 'less', 'sass', 'styl',
            'json', 'json5', 'yaml', 'yml', 'toml', 'ini', 'conf', 'cfg', 'env', 'properties',
            'proto', 'graphql', 'gql', 'csv', 'tsv',
            'md', 'markdown', 'txt', 'rst', 'tex', 'asciidoc', 'adoc',
            'editorconfig', 'gitignore', 'gitattributes', 'npmrc', 'dockerignore', 'lock'
        ]);
        const exactFilenames = new Set(['dockerfile', 'makefile', 'rakefile', 'gemfile', 'procfile', 'license', 'readme', 'authors', 'caddyfile']);
        return textExtensions.has(extension) || exactFilenames.has(lowerName);
    }
    async tryDecodeUTF8() {
        if (!this.isText()) return null;
        if (this.content == null) await this.getBlob();
        return new TextDecoder("utf-8").decode(this.content);
    }
    async getBlob() {
        if (!this.isFile()) return null;
        const resp = await API.getBlob(this.root.sessionId, this.name, this.hash);
        if (resp && resp.content) {
            this.content = base64ToArrayBuffer(resp.content);
            return this.content;
        }
        return null;
    }
    async uploadBlob(ctx) {
        let data = ctx;
        if (typeof ctx === 'string') {
            data = new TextEncoder().encode(ctx);
        }
        this.content = data;
        const _hash = await sha256(data);
        const base64Content = bufferToBase64(data);
        const resp = await API.uploadBlob(this.root.sessionId, this.name, _hash, base64Content);
        if (resp && resp.success) {
            this.hash = _hash;
            return true;
        }
        return false;
    }
    isFile() {
        return this.hash !== null;
    }
    addChild(fileOrFolder) {
        this.children.push(fileOrFolder);
    }
}

export class Repo {
    constructor(repoId, name, owner, is_public, commit, sessionId) {
        this.name = name;
        this.repoId = repoId;
        this.owner = owner;
        this.commitId = commit;
        this.is_public = is_public;
        this.sessionId = sessionId;
        this.root = new File("", null, this);
    }
    async getHistory() {
        return await API.getCommitHistory(this.sessionId, this.repoId);
    }
    async syncData() {
        const files = await API.getLatestFiles(this.sessionId, this.repoId);
        if (files && files.tree) {
            this._parseTree(files.tree);
        } else {
            this.root.children = [];
        }
    }
    async commit(message) {
        const treeStr = this.getTree();
        const resp = await API.commit(this.sessionId, this.repoId, message, treeStr);
        if (resp && resp.success) {
            this.commitId = resp.commitId;
            return true;
        }
        return false;
    }
    async toggleVisibility() {
        const newStatus = !this.is_public;
        const resp = await API.updateRepo(this.sessionId, this.repoId, newStatus);
        if (resp && resp.success) {
            this.is_public = newStatus;
            return true;
        }
        return false;
    }
    async uploadFile(path, content) {
        if (path.startsWith('/')) path = path.substring(1);
        const parts = path.split('/');
        let current = this.root;
        for (let i = 0; i < parts.length; i++) {
            const name = parts[i];
            const isLast = i === parts.length - 1;
            let next = current.children.find(c => c.name === name);
            if (!next) {
                next = new File(name, isLast ? "" : null, this);
                current.addChild(next);
            }
            current = next;
        }
        return await current.uploadBlob(content);
    }

    async removeFile(path) {
        if (path.startsWith('/')) path = path.substring(1);
        const parts = path.split('/');
        const fileName = parts.pop();
        let current = this.root;
        for (const part of parts) {
            current = current.children.find(c => c.name === part && !c.isFile());
            if (!current) return false;
        }
        const index = current.children.findIndex(c => c.name === fileName);
        if (index !== -1) {
            current.children.splice(index, 1);
            return true;
        }
        return false;
    }
    getFile(path) {
        if (path.startsWith('/')) path = path.substring(1);
        const parts = path.split('/');
        let current = this.root;
        for (const part of parts) {
            current = current.children.find(c => c.name === part);
            if (!current) return null;
        }
        if (!current.isFile()) return null;
        return current;
    }
    async getFileContent(path) {
        return await this.getFile(path).getBlob();
    }
    getTree() {
        const lines = [];
        const traverse = (node, currentPath) => {
            for (const child of node.children) {
                const path = currentPath ? `${currentPath}/${child.name}` : child.name;
                if (child.isFile()) {
                    lines.push(`${path}|${child.hash}`);
                } else {
                    traverse(child, path);
                }
            }
        };
        traverse(this.root, "");
        return lines.join('\n');
    }
    _parseTree(treeStr) {
        this.root.children = [];
        if (!treeStr) return;
        const lines = treeStr.split('\n');
        for (const line of lines) {
            if (!line.trim()) continue;
            const [path, hash] = line.split('|');
            const parts = path.split('/');
            let currentNode = this.root;
            for (let i = 0; i < parts.length; i++) {
                const partName = parts[i];
                const isLastPart = (i === parts.length - 1);
                let nextNode = currentNode.children.find(c => c.name === partName);
                if (!nextNode) {
                    const nodeHash = isLastPart ? hash : null;
                    nextNode = new File(partName, nodeHash, this);
                    currentNode.addChild(nextNode);
                }
                currentNode = nextNode;
            }
        }
    }
}

export class User {
    constructor() {
        this.username = null;
        this.userId = 0;
        this.pwdhash = null;
        this.permission = 0;
        this.repos = [];
        this.sessionId = null;
        this._callback = null;
    }
    isAdmin() {
        return this.permission > 0;
    }
    isLogin() {
        return this.sessionId != null;
    }
    async _repoChange() {
        if (this._callback) this._callback();
    }
    setRepoCallback(cb) {
        this._callback = cb;
    }
    async register(username, pwd) {
        this.pwdhash = await sha256(pwd);
        const resp = await API.register(username, this.pwdhash);
        return resp.success;
    }
    async searchPublicRepos(keyword = "") {
        return await API.searchRepo(this.sessionId, keyword);
    }
    async login(username, pwd) {
        this.pwdhash = await sha256(pwd);
        this.username = username;
        const resp = await API.login(username, this.pwdhash);
        if (resp.success) {
            this.sessionId = resp.sessionId;
            this.userId = resp.userId;
            this.permission = resp.permission || 0;
            this.syncRepos();
            return true;
        }
        return false;
    }
    async syncRepos() {
        const repo = await API.getRepo(this.sessionId);
        this.repos = [];
        for (var i = 0; i < repo.length; ++i) {
            const item = repo[i];
            const isPublicVal = (item.isPublic !== undefined) ? item.isPublic : item.public;
            this.repos.push(new Repo(item.id, item.name, item.owner, isPublicVal, item.commitId, this.sessionId));
        }
        this._repoChange();
    }
    async createRepo(name, isPublic) {
        const resp = await API.createRepo(this.sessionId, name, isPublic);
        if (resp.success) this.syncRepos();
    }
    async getRepoTree(repoId) {
        return await API.getLatestFiles(this.sessionId, repoId);
    }
    async getBlobContent(filename, hash) {
        return await API.getBlob(this.sessionId, filename, hash);
    }
    async delRepo(repoId) {
        await API.delRepo(this.sessionId, repoId);
        this.syncRepos();
    }
    async fork(repoId) {
        const resp = await API.fork(this.sessionId, repoId);
        if (resp && resp.success === true) this.syncRepos();
    }
    logout() {
        this.sessionId = null;
        this.pwdhash = null;
        this.username = null;
        this.userId = null;
        this.permission = 0;
        this.repos = [];
    }
    async getAllUser() {
        return await API.getAllUser(this.sessionId);
    }
    async updateUserPermission(targetUserId, newPermission) {
        const resp = await API.updateUserPermission(this.sessionId, targetUserId, newPermission);
        return resp && resp.success;
    }
    async deleteUser(targetUserId) {
        const resp = await API.deleteUser(this.sessionId, targetUserId);
        return resp && resp.success;
    }
}