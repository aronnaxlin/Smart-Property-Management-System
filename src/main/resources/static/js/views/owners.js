/**
 * 业主管理页面初始化
 * 加载所有业主列表
 */
window.initOwners = function() {
    searchOwners(); // 首次加载所有业主
};

/**
 * 搜索业主
 * 支持按关键词模糊搜索
 */
async function searchOwners() {
    const input = document.getElementById('ownerSearchInput');
    const keyword = input ? input.value.trim() : '';

    // 防止恶意超长输入
    if (keyword.length > 50) {
        alert('搜索关键词过长，请控制在50字以内');
        return;
    }

    const tbody = document.getElementById('ownersTableBody');
    const loading = '<tr><td colspan="5" style="text-align:center;">加载中...</td></tr>';
    tbody.innerHTML = loading;

    try {
        const res = await window.api.get('/owner/search', { keyword: keyword });
        if (res.code === 200) {
            renderOwnerTable(res.data);
        } else {
            alert('查询失败: ' + res.message);
            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--accent-pink);">查询失败</td></tr>';
        }
    } catch (e) {
        console.error('搜索错误:', e);
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--accent-pink);">系统错误</td></tr>';
    }
}

/**
 * 渲染业主列表
 *
 * @param {Array} list - 业主数据列表
 */
function renderOwnerTable(list) {
    const tbody = document.getElementById('ownersTableBody');

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">暂无数据</td></tr>';
        return;
    }

    let html = '';
    list.forEach(owner => {
        // 渲染业主信息行
        // 数据来源：OwnerService.searchOwners 返回 Map<String, Object>
        html += `
            <tr>
                <td>#${owner.user_id}</td>
                <td class="text-cyan">${owner.name || '-'}</td>
                <td>${owner.phone || '-'}</td>
                <td>${owner.gender || '-'}</td>
                <td>
                    <button class="sci-btn" onclick="viewOwnerDetail(${owner.user_id})">详情</button>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

/**
 * 查看业主详情
 * 在模态框中显示业主信息和名下房产
 *
 * @param {number} id - 业主ID
 */
async function viewOwnerDetail(id) {
    const modal = document.getElementById('ownerDetailModal');
    const content = document.getElementById('ownerDetailContent');

    // 显示模态框并显示加载状态
    modal.style.display = 'block';
    content.innerHTML = '加载中...';

    try {
        const res = await window.api.get(`/owner/${id}`);
        if (res.code === 200) {
            const data = res.data;
            const owner = data;
            const properties = data.properties;

            // 渲染房产列表
            let propsHtml = '';
            if (properties && properties.length > 0) {
                propsHtml = '<ul class="prop-list">' + properties.map(p => `
                    <li>
                        ${p.buildingNo}栋 ${p.unitNo}单元 ${p.roomNo}室
                        (${p.area}㎡) - <span class="text-cyan">${translateStatus(p.pstatus)}</span>
                    </li>
                `).join('') + '</ul>';
            } else {
                propsHtml = '<p class="text-dim">暂无房产</p>';
            }

            // 渲染业主详细信息
            content.innerHTML = `
                <div class="row">
                    <div class="col-6">
                        <p><strong>ID:</strong> ${owner.user_id}</p>
                        <p><strong>姓名:</strong> ${owner.name}</p>
                        <p><strong>电话:</strong> ${owner.phone}</p>
                        <p><strong>性别:</strong> ${owner.gender}</p>
                    </div>
                    <div class="col-6">
                        <h4>名下房产</h4>
                        ${propsHtml}
                    </div>
                </div>
            `;
        } else {
            content.innerHTML = `<p style="color:var(--accent-pink);">加载失败：${res.message}</p>`;
        }
    } catch (e) {
        console.error('加载业主详情失败:', e);
        content.innerText = '系统错误，加载失败';
    }
}

/**
 * 翻译房产状态
 *
 * @param {string} status - 状态代码
 * @returns {string} 中文状态名称
 */
function translateStatus(status) {
    const map = { 'SOLD': '已售', 'UNSOLD': '待售', 'RENTED': '出租' };
    return map[status] || status;
}

/**
 * 关闭业主详情模态框
 */
function closeOwnerModal() {
    document.getElementById('ownerDetailModal').style.display = 'none';
}
