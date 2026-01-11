/**
 * 房产管理页面逻辑
 */

/**
 * 初始化函数
 */
function initProperties() {
    loadProperties();
    loadOwners();
}

/**
 * 加载所有房产
 */
async function loadProperties() {
    try {
        const res = await window.api.get('/property/list');
        if (res.code === 200) {
            renderPropertyTable(res.data);
        } else {
            alert('加载房产列表失败: ' + res.message);
        }
    } catch (e) {
        console.error('加载房产列表错误:', e);
        alert('系统错误');
    }
}

/**
 * 搜索房产
 */
async function searchProperties() {
    const building = document.getElementById('searchBuilding').value.trim();
    const unit = document.getElementById('searchUnit').value.trim();
    const room = document.getElementById('searchRoom').value.trim();

    // 如果全部为空,加载所有
    if (!building && !unit && !room) {
        loadProperties();
        return;
    }

    try {
        const params = new URLSearchParams();
        if (building) params.append('building', building);
        if (unit) params.append('unit', unit);
        if (room) params.append('room', room);

        const res = await window.api.get(`/property/search?${params.toString()}`);
        if (res.code === 200) {
            renderPropertyTable(res.data);
        } else {
            alert('搜索失败: ' + res.message);
        }
    } catch (e) {
        console.error('搜索错误:', e);
        alert('系统错误');
    }
}

/**
 * 渲染房产表格
 */
function renderPropertyTable(properties) {
    const tbody = document.getElementById('propertyTableBody');

    if (!properties || properties.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-dim">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = properties.map(p => {
        // 格式化水电卡ID显示
        const cardInfo = [];
        if (p.waterCardId) cardInfo.push(`水卡:${p.waterCardId}`);
        if (p.electricityCardId) cardInfo.push(`电卡:${p.electricityCardId}`);
        const cardDisplay = cardInfo.length > 0 ? cardInfo.join('<br>') : '-';

        return `
        <tr>
            <td>${p.pId}</td>
            <td>${p.buildingNo}</td>
            <td>${p.unitNo}</td>
            <td>${p.roomNo}</td>
            <td>${p.area}</td>
            <td><span class="status-badge status-${p.pStatus}">${translateStatus(p.pStatus)}</span></td>
            <td>${p.ownerName || '-'}</td>
            <td>${cardDisplay}</td>
            <td>
                <button class="sci-btn" onclick="showEditPropertyModal(${p.pId})" style="margin-right: 5px;">编辑</button>
                <button class="sci-btn danger" onclick="deleteProperty(${p.pId})">删除</button>
            </td>
        </tr>
        `;
    }).join('');
}

/**
 * 加载所有业主到下拉列表
 */
async function loadOwners() {
    try {
        const res = await window.api.get('/user/list');
        if (res.code === 200) {
            const ownerSelect = document.getElementById('propertyModalOwner');
            ownerSelect.innerHTML = '<option value="">无业主(待售)</option>';

            res.data.forEach(user => {
                if (user.userType === 'OWNER') {
                    const option = document.createElement('option');
                    option.value = user.userId;
                    option.textContent = `${user.name} (ID: ${user.userId})`;
                    ownerSelect.appendChild(option);
                }
            });
        }
    } catch (e) {
        console.error('加载业主列表错误:', e);
    }
}

/**
 * 显示新建房产模态框
 */
function showCreatePropertyModal() {
    document.getElementById('propertyModalTitle').innerText = '新建房产';
    document.getElementById('propertyModalId').value = '';
    document.getElementById('propertyModalBuilding').value = '';
    document.getElementById('propertyModalUnit').value = '';
    document.getElementById('propertyModalRoom').value = '';
    document.getElementById('propertyModalArea').value = '';
    document.getElementById('propertyModalStatus').value = '';
    document.getElementById('propertyModalOwner').value = '';

    const cardInfoElement = document.getElementById('propertyModalCardInfo');
    if (cardInfoElement) {
        cardInfoElement.value = '创建后自动生成';
    }

    const backdrop = document.getElementById('propertyModalBackdrop');
    if (backdrop) {
        backdrop.style.display = 'block';
    }
    document.getElementById('propertyModal').style.display = 'block';
}

/**
 * 显示编辑房产模态框
 */
async function showEditPropertyModal(propertyId) {
    try {
        const res = await window.api.get(`/property/${propertyId}`);
        if (res.code === 200) {
            const property = res.data;
            document.getElementById('propertyModalTitle').innerText = '编辑房产';
            document.getElementById('propertyModalId').value = property.pId;
            document.getElementById('propertyModalBuilding').value = property.buildingNo;
            document.getElementById('propertyModalUnit').value = property.unitNo;
            document.getElementById('propertyModalRoom').value = property.roomNo;
            document.getElementById('propertyModalArea').value = property.area;
            document.getElementById('propertyModalStatus').value = property.pStatus;
            document.getElementById('propertyModalOwner').value = property.userId || '';

            // 查询并显示水电卡信息
            const cardsRes = await window.api.get(`/property/list`);
            if (cardsRes.code === 200) {
                const propWithCards = cardsRes.data.find(p => p.pId === propertyId);
                if (propWithCards) {
                    const cardInfo = [];
                    if (propWithCards.waterCardId) cardInfo.push(`水卡ID: ${propWithCards.waterCardId}`);
                    if (propWithCards.electricityCardId) cardInfo.push(`电卡ID: ${propWithCards.electricityCardId}`);

                    const cardDisplay = cardInfo.length > 0 ? cardInfo.join(', ') : '暂无水电卡';
                    const cardInfoElement = document.getElementById('propertyModalCardInfo');
                    if (cardInfoElement) {
                        cardInfoElement.value = cardDisplay;
                    }
                }
            }

            const backdrop = document.getElementById('propertyModalBackdrop');
            if (backdrop) {
                backdrop.style.display = 'block';
            }
            document.getElementById('propertyModal').style.display = 'block';
        } else {
            alert('获取房产详情失败: ' + res.message);
        }
    } catch (e) {
        console.error('获取房产详情错误:', e);
        alert('系统错误');
    }
}

/**
 * 保存房产(创建或更新)
 */
async function saveProperty() {
    const propertyId = document.getElementById('propertyModalId').value;
    const buildingNo = document.getElementById('propertyModalBuilding').value.trim();
    const unitNo = document.getElementById('propertyModalUnit').value.trim();
    const roomNo = document.getElementById('propertyModalRoom').value.trim();
    const area = document.getElementById('propertyModalArea').value.trim();
    const pStatus = document.getElementById('propertyModalStatus').value;
    const ownerId = document.getElementById('propertyModalOwner').value;

    // 表单验证
    if (!buildingNo) {
        alert('请输入楼栋号');
        return;
    }
    if (!unitNo) {
        alert('请输入单元号');
        return;
    }
    if (!roomNo) {
        alert('请输入房号');
        return;
    }
    if (!area || parseFloat(area) <= 0) {
        alert('请输入有效的面积');
        return;
    }
    if (!pStatus) {
        alert('请选择房产状态');
        return;
    }

    const propertyData = {
        buildingNo: buildingNo,
        unitNo: unitNo,
        roomNo: roomNo,
        area: parseFloat(area),
        pStatus: pStatus,
        userId: ownerId ? parseInt(ownerId) : null
    };

    try {
        let res;
        if (propertyId) {
            // 更新房产
            propertyData.pId = parseInt(propertyId);
            res = await window.api.put('/property/update', propertyData);
        } else {
            // 创建新房产
            res = await window.api.post('/property/create', propertyData);
        }

        if (res.code === 200) {
            alert(propertyId ? '更新成功' : '创建成功');
            closePropertyModal();
            loadProperties(); // 重新加载列表
        } else {
            alert('操作失败: ' + res.message);
        }
    } catch (e) {
        console.error('保存房产错误:', e);
        alert('系统错误');
    }
}

/**
 * 删除房产
 */
async function deleteProperty(propertyId) {
    if (!confirm('确定要删除这个房产吗？')) {
        return;
    }

    try {
        const res = await window.api.delete(`/property/${propertyId}`);
        if (res.code === 200) {
            alert('删除成功');
            loadProperties(); // 重新加载列表
        } else {
            alert('删除失败: ' + res.message);
        }
    } catch (e) {
        console.error('删除房产错误:', e);
        alert('系统错误');
    }
}

/**
 * 关闭房产模态框
 */
function closePropertyModal() {
    const backdrop = document.getElementById('propertyModalBackdrop');
    if (backdrop) {
        backdrop.style.display = 'none';
    }
    document.getElementById('propertyModal').style.display = 'none';
}

/**
 * 导出房产CSV
 */
function exportProperties() {
    window.location.href = '/api/export/properties';
}

/**
 * 翻译房产状态
 */
function translateStatus(status) {
    const map = { 'SOLD': '已售', 'UNSOLD': '待售', 'RENTED': '出租' };
    return map[status] || status;
}

// 导出初始化函数到全局
window.initProperties = initProperties;
