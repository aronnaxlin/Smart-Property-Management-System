/**
 * 业主/用户管理页面初始化
 * 整合了用户CRUD和业主房产查看功能
 */
window.initOwners = function() {
    loadOwners(); // 加载所有用户/业主
};

// 存储当前筛选类型
let currentUserTypeFilter = '';

/**
 * 加载所有用户/业主
 */
async function loadOwners() {
    const tbody = document.getElementById('ownersTableBody');
    if (!tbody) {
        console.error('ownersTableBody element not found');
        return;
    }
    const loading = '<tr><td colspan="7" style="text-align:center;">加载中...</td></tr>';
    tbody.innerHTML = loading;

    try {
        const res = await window.api.get('/user/list');
        if (res.code === 200) {
            renderOwnerTable(res.data);
        } else {
            alert('加载失败: ' + res.message);
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--accent-pink);">加载失败</td></tr>';
        }
    } catch (e) {
        console.error('加载错误:', e);
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--accent-pink);">系统错误</td></tr>';
    }
}

/**
 * 搜索用户/业主
 */
async function searchOwners() {
    const input = document.getElementById('ownerSearchInput');
    const keyword = input ? input.value.trim() : '';

    if (keyword.length > 50) {
        alert('搜索关键词过长，请控制在50字以内');
        return;
    }

    const tbody = document.getElementById('ownersTableBody');
    if (!tbody) {
        console.error('ownersTableBody element not found');
        return;
    }
    const loading = '<tr><td colspan="7" style="text-align:center;">搜索中...</td></tr>';
    tbody.innerHTML = loading;

    try {
        const res = await window.api.get('/user/search', { keyword: keyword });
        if (res.code === 200) {
            renderOwnerTable(res.data);
        } else {
            alert('搜索失败: ' + res.message);
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--accent-pink);">搜索失败</td></tr>';
        }
    } catch (e) {
        console.error('搜索错误:', e);
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--accent-pink);">系统错误</td></tr>';
    }
}

/**
 * 按用户类型筛选
 */
function filterByUserType() {
    currentUserTypeFilter = document.getElementById('userTypeFilter').value;
    loadOwners(); // 重新加载并应用筛选
}

/**
 * 渲染用户/业主表格
 */
function renderOwnerTable(list) {
    const tbody = document.getElementById('ownersTableBody');
    if (!tbody) {
        console.error('ownersTableBody element not found');
        return;
    }

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无数据</td></tr>';
        return;
    }

    // 应用用户类型筛选
    let filteredList = list;
    if (currentUserTypeFilter) {
        filteredList = list.filter(user => user.userType === currentUserTypeFilter);
    }

    if (filteredList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无符合条件的数据</td></tr>';
        return;
    }

    let html = '';
    filteredList.forEach(user => {
        html += `
            <tr>
                <td>#${user.userId}</td>
                <td class="text-cyan">${user.userName || '-'}</td>
                <td>${user.name || '-'}</td>
                <td>${user.gender || '-'}</td>
                <td>${user.phone || '-'}</td>
                <td>${translateUserType(user.userType)}</td>
                <td>
                    <button class="sci-btn" onclick="viewOwnerDetail(${user.userId})" style="margin-right: 5px;">详情</button>
                    <button class="sci-btn" onclick="showEditUserModal(${user.userId})" style="margin-right: 5px;">编辑</button>
                    <button class="sci-btn danger" onclick="deleteUser(${user.userId})">删除</button>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

/**
 * 查看用户详情(包含房产信息,如果是业主)
 */
async function viewOwnerDetail(id) {
    const modal = document.getElementById('ownerDetailModal');
    const content = document.getElementById('ownerDetailContent');

    if (!modal || !content) {
        console.error('Modal elements not found');
        return;
    }

    modal.style.display = 'block';
    content.innerHTML = '加载中...';

    try {
        // 首先获取用户基本信息
        const userRes = await window.api.get(`/user/${id}`);
        if (userRes.code !== 200) {
            content.innerHTML = `<p style="color:var(--accent-pink);">加载失败：${userRes.message}</p>`;
            return;
        }

        const user = userRes.data;
        let detailHtml = `
            <div class="row">
                <div class="col-6">
                    <h4>基本信息</h4>
                    <p><strong>用户ID:</strong> ${user.userId}</p>
                    <p><strong>用户名:</strong> ${user.userName}</p>
                    <p><strong>姓名:</strong> ${user.name}</p>
                    <p><strong>性别:</strong> ${user.gender || '-'}</p>
                    <p><strong>电话:</strong> ${user.phone || '-'}</p>
                    <p><strong>用户类型:</strong> ${translateUserType(user.userType)}</p>
                </div>
                <div class="col-6">
        `;

        // 如果是业主,尝试获取房产信息
        if (user.userType === 'OWNER') {
            try {
                const ownerRes = await window.api.get(`/owner/${id}`);
                if (ownerRes.code === 200 && ownerRes.data.properties) {
                    const properties = ownerRes.data.properties;
                    let propsHtml = '';
                    if (properties && properties.length > 0) {
                        propsHtml = '<ul class="prop-list">' + properties.map(p => {
                            // 兼容处理多种字段名: pstatus, pStatus, p_status
                            const status = p.pstatus || p.pStatus || p.p_status || p.status;
                            const statusHtml = status ? ` - <span class="text-cyan">${translateStatus(status)}</span>` : '';
                            return `
                            <li>
                                ${p.buildingNo}栋 ${p.unitNo}单元 ${p.roomNo}室
                                (${p.area}㎡)${statusHtml}
                            </li>
                        `;
                        }).join('') + '</ul>';
                    } else {
                        propsHtml = '<p class="text-dim">暂无房产</p>';
                    }
                    detailHtml += `<h4>名下房产</h4>${propsHtml}`;
                }
            } catch (e) {
                // 如果获取房产失败,不影响基本信息显示
                detailHtml += '<h4>名下房产</h4><p class="text-dim">无法加载房产信息</p>';
            }
        } else {
            detailHtml += '<h4>权限信息</h4><p>该用户为管理员,无房产信息</p>';
        }

        detailHtml += `
                </div>
            </div>
        `;
        content.innerHTML = detailHtml;
    } catch (e) {
        console.error('加载用户详情失败:', e);
        content.innerText = '系统错误，加载失败';
    }
}

/**
 * 关闭详情模态框
 */
function closeOwnerModal() {
    document.getElementById('ownerDetailModal').style.display = 'none';
}

/**
 * 显示新建用户模态框
 */
function showCreateUserModal() {
    document.getElementById('userModalTitle').innerText = '新建用户';
    document.getElementById('userModalId').value = '';
    document.getElementById('userModalUserName').value = '';
    document.getElementById('userModalPassword').value = '';
    document.getElementById('userModalName').value = '';
    document.getElementById('userModalPhone').value = '';
    document.getElementById('userModalUserType').value = '';

    // 切换性别字段为下拉选择
    const genderField = document.getElementById('userModalGender');
    if (!genderField) {
        console.error('userModalGender element not found');
        return;
    }
    const genderContainer = genderField.parentElement;
    genderContainer.innerHTML = `
        <label>性别:</label>
        <select id="userModalGender" class="sci-input">
            <option value="">请选择</option>
            <option value="男">男</option>
            <option value="女">女</option>
        </select>
    `;

    document.getElementById('userModal').style.display = 'block';
}

/**
 * 显示编辑用户模态框
 */
async function showEditUserModal(userId) {
    try {
        const res = await window.api.get(`/user/${userId}`);
        if (res.code === 200) {
            const user = res.data;
            document.getElementById('userModalTitle').innerText = '编辑用户';
            document.getElementById('userModalId').value = user.userId;
            document.getElementById('userModalUserName').value = user.userName;
            document.getElementById('userModalPassword').value = ''; // 不显示密码
            document.getElementById('userModalName').value = user.name;
            document.getElementById('userModalPhone').value = user.phone || '';
            document.getElementById('userModalUserType').value = user.userType || '';

            // 切换性别字段为只读文本
            const genderContainer = document.getElementById('userModalGender').parentElement;
            genderContainer.innerHTML = `
                <label>性别 <span style="color: var(--text-dim); font-size: 0.85rem;">(创建后不可修改)</span>:</label>
                <input type="text" id="userModalGender" class="sci-input" value="${user.gender || '-'}" readonly style="background-color: var(--bg-surface); cursor: not-allowed;">
            `;

            document.getElementById('userModal').style.display = 'block';
        } else {
            alert('获取用户详情失败: ' + res.message);
        }
    } catch (e) {
        console.error('获取用户详情错误:', e);
        alert('系统错误');
    }
}

/**
 * 关闭用户模态框
 */
function closeUserModal() {
    document.getElementById('userModal').style.display = 'none';
}

/**
 * 保存用户（创建或更新）
 */
async function saveUser() {
    const userId = document.getElementById('userModalId').value;
    const userName = document.getElementById('userModalUserName').value.trim();
    const password = document.getElementById('userModalPassword').value.trim();
    const name = document.getElementById('userModalName').value.trim();
    const phone = document.getElementById('userModalPhone').value.trim();
    const userType = document.getElementById('userModalUserType').value;

    // 表单验证
    if (!userName) {
        alert('请输入用户名');
        return;
    }
    if (!name) {
        alert('请输入真实姓名');
        return;
    }
    if (!userType) {
        alert('请选择用户类型');
        return;
    }

    const userData = {
        userName: userName,
        name: name,
        phone: phone,
        userType: userType
    };

    try {
        let res;
        if (userId) {
            // 更新用户 - 不包含性别
            userData.userId = parseInt(userId);
            if (password) {
                userData.password = password; // 仅在填写了密码时才更新
            }
            res = await window.api.put('/user/update', userData);
        } else {
            // 创建新用户 - 包含性别
            if (!password) {
                alert('请输入密码');
                return;
            }
            const gender = document.getElementById('userModalGender').value;
            userData.password = password;
            userData.gender = gender; // 仅在创建时设置性别
            res = await window.api.post('/user/create', userData);
        }

        if (res.code === 200) {
            alert(userId ? '更新成功' : '创建成功');
            closeUserModal();
            loadOwners(); // 重新加载列表
        } else {
            alert('操作失败: ' + res.message);
        }
    } catch (e) {
        console.error('保存用户错误:', e);
        alert('系统错误');
    }
}

/**
 * 删除用户
 */
async function deleteUser(userId) {
    if (!confirm('确认删除该用户？此操作不可恢复！')) {
        return;
    }

    try {
        const res = await window.api.delete(`/user/${userId}`);
        if (res.code === 200) {
            alert('删除成功');
            loadOwners(); // 重新加载列表
        } else {
            alert('删除失败: ' + res.message);
        }
    } catch (e) {
        console.error('删除用户错误:', e);
        alert('系统错误');
    }
}

/**
 * 导出CSV
 */
function exportOwners() {
    window.open('/api/export/users', '_blank');
}

/**
 * 翻译房产状态
 */
function translateStatus(status) {
    const map = { 'SOLD': '已售', 'UNSOLD': '待售', 'RENTED': '出租' };
    return map[status] || status;
}

/**
 * 翻译用户类型
 */
function translateUserType(type) {
    const map = { 'ADMIN': '管理员', 'OWNER': '业主' };
    return map[type] || type;
}
