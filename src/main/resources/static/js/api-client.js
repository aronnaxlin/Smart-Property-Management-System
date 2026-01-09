/**
 * API客户端工具
 * 统一封装后端API调用，提供GET和POST方法
 *
 * 使用方法：
 * - GET请求：window.api.get('/endpoint', { param1: value1 })
 * - POST请求：window.api.post('/endpoint', { data: value })
 * - POST表单：window.api.postForm('/endpoint', { param1: value1 })
 */

const API_BASE = '/api';

const apiClient = {
    /**
     * 发送GET请求
     *
     * @param {string} endpoint - API端点路径
     * @param {Object} params - 查询参数对象
     * @returns {Promise<Object>} API响应数据
     */
    async get(endpoint, params = {}) {
        const url = new URL(API_BASE + endpoint, window.location.origin);
        Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));

        try {
            const response = await fetch(url);
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API GET Error:', error);
            throw error;
        }
    },

    /**
     * 发送POST请求（JSON格式）
     *
     * @param {string} endpoint - API端点路径
     * @param {Object} body - 请求体数据
     * @param {boolean} is JSON - 是否为JSON格式（默认true）
     * @returns {Promise<Object>} API响应数据
     */
    async post(endpoint, body = {}, isJson = true) {
        const url = API_BASE + endpoint;
        const options = {
            method: 'POST',
            headers: {}
        };

        if (isJson) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        } else {
            // 如果是FormData，body应该是FormData对象
            options.body = body;
        }

        try {
            const response = await fetch(url, options);
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API POST Error:', error);
            throw error;
        }
    },

    /**
     * 发送POST请求（表单格式，参数附加到URL）
     * 适用于某些特定接口
     *
     * @param {string} endpoint - API端点路径
     * @param {Object} params - 表单参数对象
     * @returns {Promise<Object>} API响应数据
     */
    async postForm(endpoint, params = {}) {
        const url = new URL(API_BASE + endpoint, window.location.origin);
        Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));

        try {
            const response = await fetch(url, { method: 'POST' });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API POST Form Error:', error);
            throw error;
        }
    },

    /**
     * 统一处理API响应
     *
     * @param {Response} response - Fetch API响应对象
     * @returns {Promise<Object>} 解析后的响应数据
     * @throws {Error} 当响应状态码非2xx时抛出错误
     */
    async handleResponse(response) {
        if (!response.ok) {
            // 尝试解析错误信息
            try {
                const errData = await response.json();
                throw new Error(errData.message || `HTTP Error ${response.status}`);
            } catch (e) {
                // 如果响应体为空或解析失败，抛出通用错误
                if (e.message && e.message !== 'Unexpected end of JSON input') throw e;
                throw new Error(`HTTP Error ${response.status}`);
            }
        }
        // 返回后端的Result<T>结构
        return await response.json();
    }
};

// 全局暴露API客户端
window.api = apiClient;
