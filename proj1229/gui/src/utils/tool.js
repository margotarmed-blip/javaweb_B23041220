import { ElNotification } from 'element-plus';
export const showError = (text_obj) => ElNotification({ title: '错误', message: text_obj.toString(), type: 'error', duration: 3000 });
export const showInfo = (text_obj) => ElNotification({ title: '信息', message: text_obj.toString(), type: 'info', duration: 1000 });
export const showWarning = (text_obj) => ElNotification({ title: '警告', message: text_obj.toString(), type: 'warning', duration: 2000 });
export const showSuccess = (text_obj) => ElNotification({ title: '成功', message: text_obj.toString(), type: 'success', duration: 1000 });