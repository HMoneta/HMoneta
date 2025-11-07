import {userNotificationStore} from '@/stores/app.js';

const BASE_URL = import.meta.env.VITE_API_BASE_URL;
const NO_AUTH_URLS = ['/login', '/register'];

// 封装 request
async function request(url, options = {}) {
  const defaultHeaders = {
    'Content-Type': 'application/json',
  };

  const config = {
    method: options.method || 'GET',
    headers: {...defaultHeaders, ...(options.headers || {})},
    ...options,
  };
  // 如果 body 是 FormData，删除 'Content-Type'，让浏览器自动设置 multipart/form-data
  if (config.body instanceof FormData) {
    delete config.headers['Content-Type'];
  }

  // 自动把对象类型 body 转成 JSON（但跳过 FormData）
  if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
    config.body = JSON.stringify(config.body);
  }

  // 登录相关接口不加 token
  const needAuth = !NO_AUTH_URLS.some(u => url.startsWith(u));
  if (needAuth) {
    const token = sessionStorage.getItem('HMToken');
    if (token) {
      config.headers['HMToken'] = token;
    }
  }

  try {
    const res = await fetch(BASE_URL + url, config);

    if (!res.ok) {
      const errorText = await res.text();
      const notificationStore = userNotificationStore();
      notificationStore.showError(`请求失败: ${errorText || res.statusText}`);
      throw new Error(`HTTP ${res.status}: ${errorText}`);
    }

    // 尝试解析 JSON，如果不是 JSON 则返回文本
    const contentType = res.headers.get('Content-Type') || '';
    if (contentType.includes('application/json')) {
      return await res.json();
    } else {
      return await res.text();
    }
  } catch (err) {
    if (!err.message.startsWith('HTTP')) {
      const notificationStore = userNotificationStore();
      notificationStore.showError('网络连接失败，请检查网络');
    }
    throw err;
  }
}

// 对外导出 get / post / put / delete
export const http = {
  get: (url, params, options = {}) => {
    const query = params ? '?' + new URLSearchParams(params).toString() : '';
    return request(url + query, {...options, method: 'GET'});
  },

  post: (url, body, options = {}) =>
    request(url, {...options, method: 'POST', body}),

  put: (url, body, options = {}) =>
    request(url, {...options, method: 'PUT', body}),

  delete: (url, options = {}) =>
    request(url, {...options, method: 'DELETE'}),
};
