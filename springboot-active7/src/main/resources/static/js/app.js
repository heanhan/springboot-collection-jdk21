(function () {
    'use strict';

    var ACCESS_TOKEN_KEY = 'active7.accessToken';
    var TOKEN_EXPIRES_KEY = 'active7.tokenExpiresAt';

    function saveSession(result) {
        sessionStorage.setItem(ACCESS_TOKEN_KEY, result.accessToken);
        sessionStorage.setItem(TOKEN_EXPIRES_KEY, String(Date.now() + result.expiresIn * 1000));
    }

    function clearSession() {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
        sessionStorage.removeItem(TOKEN_EXPIRES_KEY);
    }

    function expireSession() {
        clearSession();
        window.location.replace('/active/login?expired=1');
    }

    var loginForm = document.getElementById('loginForm');
    if (loginForm) {
        document.querySelectorAll('.demo-account').forEach(function (button) {
            button.addEventListener('click', function () {
                document.getElementById('username').value = button.getAttribute('data-username');
                loginForm.querySelector('[name="password"]').value = button.getAttribute('data-password');
                document.querySelectorAll('.demo-account').forEach(function (item) {
                    item.classList.remove('selected');
                });
                button.classList.add('selected');
                loginForm.querySelector('button[type="submit"]').focus();
            });
        });
        loginForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            if (!loginForm.reportValidity()) {
                return;
            }
            var button = loginForm.querySelector('button[type="submit"]');
            var alert = document.getElementById('loginAlert');
            button.disabled = true;
            alert.hidden = true;
            try {
                var response = await fetch('/active/api/auth/login', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    credentials: 'same-origin',
                    body: JSON.stringify({
                        username: document.getElementById('username').value.trim(),
                        password: loginForm.querySelector('[name="password"]').value
                    })
                });
                var payload = await response.json().catch(function () {
                    return null;
                });
                if (!response.ok || !payload || payload.code !== 200) {
                    throw new Error(payload && payload.message ? payload.message : '登录失败，请稍后重试');
                }
                saveSession(payload.result);
                window.location.href = '/active/index';
            } catch (error) {
                alert.textContent = error.message || '登录失败，请稍后重试';
                alert.hidden = false;
                button.disabled = false;
            }
        });
        return;
    }

    var workspace = document.getElementById('workspace');
    if (!workspace) {
        return;
    }

    var API = workspace.getAttribute('data-base') + '/api';
    var state = {user: null, requests: [], tasks: [], balance: null, rbac: null};
    var refreshPromise = null;
    var leaveTypes = {
        ANNUAL: '年假',
        SICK: '病假',
        PERSONAL: '事假',
        MARRIAGE: '婚假',
        BEREAVEMENT: '丧假'
    };
    var statuses = {
        SUBMITTED: ['待经理审批', 'badge-purple'],
        RETURNED: ['已退回补充', 'badge-orange'],
        MANAGER_APPROVED: ['经理已同意', 'badge-blue'],
        HR_APPROVED: ['HR 已同意', 'badge-blue'],
        DIRECTOR_APPROVED: ['总监已同意', 'badge-blue'],
        CONFIRM_PENDING: ['待员工确认', 'badge-orange'],
        APPROVED: ['已通过', 'badge-green'],
        REJECTED: ['已拒绝', 'badge-red'],
        WITHDRAWN: ['已撤回', '']
    };
    var nodes = {
        managerApproval: '部门经理审批',
        hrApproval: 'HR 审批',
        directorApproval: '总监审批',
        employeeResubmit: '员工补充重填',
        employeeConfirm: '员工确认安排',
        END: '流程结束'
    };
    var actions = {
        SUBMIT: '提交申请', CLAIM: '签收任务', APPROVE: '同意', REJECT: '拒绝',
        RETURN: '退回补充', RESUBMIT: '重新提交', WITHDRAW: '撤回', CONFIRM: '确认安排', END: '流程结束'
    };
    var toastTimer;

    function escapeHtml(value) {
        return String(value === null || value === undefined ? '' : value).replace(/[&<>"']/g, function (character) {
            return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[character];
        });
    }

    function formatDate(value) {
        if (!value) {
            return '--';
        }
        return String(value).replace('T', ' ').slice(0, 16);
    }

    function typeLabel(value) {
        return leaveTypes[value] || value || '--';
    }

    function statusBadge(value) {
        var status = statuses[value] || [value || '--', ''];
        return '<span class="badge ' + status[1] + '">' + escapeHtml(status[0]) + '</span>';
    }

    function showToast(message, type) {
        var toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = 'toast ' + (type || '');
        toast.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () {
            toast.hidden = true;
        }, 3500);
    }

    async function refreshAccessToken() {
        if (refreshPromise) {
            return refreshPromise;
        }
        refreshPromise = (async function () {
            var response = await fetch(API + '/auth/refresh', {
                method: 'POST',
                headers: {'X-Refresh-Request': 'true'},
                credentials: 'same-origin'
            });
            var payload = await response.json().catch(function () {
                return null;
            });
            if (!response.ok || !payload || payload.code !== 200) {
                throw new Error(payload && payload.message ? payload.message : '登录已过期，请重新登录');
            }
            saveSession(payload.result);
            return payload.result.accessToken;
        })().finally(function () {
            refreshPromise = null;
        });
        return refreshPromise;
    }

    async function accessToken() {
        var token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
        var expiresAt = Number(sessionStorage.getItem(TOKEN_EXPIRES_KEY));
        if (token && expiresAt > Date.now() + 30000) {
            return token;
        }
        try {
            return await refreshAccessToken();
        } catch (error) {
            expireSession();
            throw error;
        }
    }

    async function api(path, options, retried) {
        options = options || {};
        var method = (options.method || 'GET').toUpperCase();
        var headers = Object.assign({}, options.headers || {});
        var body = options.body;
        headers['Authorization'] = 'Bearer ' + await accessToken();
        if (body && !(body instanceof FormData)) {
            body = JSON.stringify(body);
            headers['Content-Type'] = 'application/json';
        }
        var response = await fetch(API + path, {
            method: method,
            headers: headers,
            body: body,
            credentials: 'same-origin'
        });
        if (response.status === 401 && !retried) {
            try {
                await refreshAccessToken();
                return await api(path, options, true);
            } catch (error) {
                expireSession();
                throw error;
            }
        }
        if (response.status === 401) {
            expireSession();
            throw new Error('登录已过期，请重新登录');
        }
        var payload = null;
        try {
            payload = await response.json();
        } catch (error) {
            payload = null;
        }
        if (!response.ok || !payload || payload.code !== 200) {
            throw new Error(payload && payload.message ? payload.message : '请求失败（HTTP ' + response.status + '）');
        }
        return payload.result;
    }

    async function runButton(button, action) {
        if (!button.disabled) {
            button.disabled = true;
        }
        try {
            await action();
        } catch (error) {
            showToast(error.message || '操作失败', 'error');
        } finally {
            button.disabled = false;
        }
    }

    function currentRoles() {
        return state.user && state.user.roles ? state.user.roles : [];
    }

    function currentPermissions() {
        return state.user && state.user.permissions ? state.user.permissions : [];
    }

    function hasPermission(permission) {
        return currentPermissions().indexOf(permission) >= 0;
    }

    function canViewProcess() {
        return hasPermission('process:view');
    }

    function canViewRbac() {
        return hasPermission('rbac:view');
    }

    function renderUser() {
        var user = state.user;
        document.getElementById('userName').textContent = user.displayName + ' · ' + user.username;
        document.getElementById('userDepartment').textContent = user.department;
        document.getElementById('userAvatar').textContent = user.displayName.substring(0, 1);
        document.getElementById('requestNav').hidden = !hasPermission('leave:view:mine') && !hasPermission('leave:view:all');
        document.getElementById('adminNav').hidden = !canViewProcess();
        document.getElementById('rbacNav').hidden = !canViewRbac();
        document.querySelectorAll('#view-admin').forEach(function (view) {
            view.hidden = !canViewProcess();
        });
        document.querySelectorAll('#view-rbac').forEach(function (view) {
            view.hidden = !canViewRbac();
        });
        document.querySelectorAll('[data-permission]').forEach(function (element) {
            element.hidden = !hasPermission(element.getAttribute('data-permission'));
        });
        document.getElementById('requestViewTitle').textContent = hasPermission('leave:view:all') ? '全部申请' : '我的申请';

        var role = currentRoles()[0] || 'ROLE_EMPLOYEE';
        var roleMap = {
            ROLE_EMPLOYEE: ['EMPLOYEE', '提交、查看、补充、确认和撤回自己的请假'],
            ROLE_MANAGER: ['MANAGER', '审批经理任务，可同意、拒绝或退回'],
            ROLE_HR: ['HR', '审批长假和特殊假期任务'],
            ROLE_DIRECTOR: ['DIRECTOR', '审批 5 天及以上的长假任务'],
            ROLE_ADMIN: ['ADMIN', '查看全部申请，并按授权管理流程与 RBAC']
        };
        var roleInfo = roleMap[role] || roleMap.ROLE_EMPLOYEE;
        document.getElementById('roleName').textContent = roleInfo[0];
        document.getElementById('roleDescription').textContent = roleInfo[1];
        renderRoleMatrix();
    }

    function renderRoleMatrix() {
        var names = {
            'auth:view': '查看当前身份',
            'leave:create': '提交请假',
            'leave:view:mine': '查看我的请假',
            'leave:view:all': '查看全部请假',
            'leave:view:balance': '查看年假余额',
            'leave:withdraw': '撤回请假',
            'leave:task:view': '查看我的待办',
            'leave:task:claim': '签收任务',
            'leave:task:approve': '办理审批',
            'leave:task:resubmit': '补充并重交',
            'process:view': '查看流程管理',
            'process:deploy': '部署流程',
            'rbac:view': '查看 RBAC 模型',
            'rbac:manage': '管理 RBAC 关系'
        };
        var rows = currentPermissions().map(function (permission) {
            return [names[permission] || permission, permission];
        });
        if (!rows.length) {
            rows = [['尚未授权', '请联系管理员分配角色或权限']];
        }
        document.getElementById('roleMatrix').innerHTML = rows.map(function (row) {
            return '<div class="role-row"><strong>' + escapeHtml(row[0]) + '</strong><p>' + escapeHtml(row[1]) + '</p></div>';
        }).join('');
    }

    function renderStats() {
        document.getElementById('requestCount').textContent = state.requests.length;
        document.getElementById('taskCount').textContent = state.tasks.length;
        document.getElementById('myRequests').textContent = state.requests.length;
        document.getElementById('pendingTasks').textContent = state.tasks.length;
        if (state.balance) {
            document.getElementById('balanceDays').textContent = state.balance.remainingDays;
            document.getElementById('balanceDetail').textContent = state.balance.balanceYear + ' 年 · 已用 ' + state.balance.usedDays + ' / ' + state.balance.annualDays + ' 天';
        }
    }

    function renderRecentRequests() {
        var target = document.getElementById('recentRequests');
        if (!state.requests.length) {
            target.innerHTML = '<div class="empty-state"><strong>还没有请假申请</strong>点击“新建请假”开始第一条流程。</div>';
            return;
        }
        target.innerHTML = state.requests.slice(0, 5).map(function (request) {
            return '<div class="recent-item"><div><strong>' + escapeHtml(typeLabel(request.leaveType)) + ' · ' + escapeHtml(request.leaveDays) + ' 天</strong><p>' + escapeHtml(formatDate(request.startTime)) + ' 至 ' + escapeHtml(formatDate(request.endTime)) + '</p></div>' + statusBadge(request.status) + '<button class="text-button" data-request-id="' + escapeHtml(request.id) + '">轨迹</button></div>';
        }).join('');
    }

    function renderRequests() {
        var target = document.getElementById('requestTable');
        if (!state.requests.length) {
            target.innerHTML = '<div class="empty-state"><strong>暂无申请</strong>提交后可以在这里查看状态和完整审批轨迹。</div>';
            return;
        }
        var rows = state.requests.map(function (request) {
            var canWithdraw = hasPermission('leave:withdraw') && (request.status === 'SUBMITTED' || request.status === 'RETURNED');
            return '<tr><td><strong>' + escapeHtml(typeLabel(request.leaveType)) + ' · ' + escapeHtml(request.leaveDays) + ' 天</strong><br><small>申请单 ' + escapeHtml(request.id.substring(0, 8)) + '</small></td><td>' + escapeHtml(formatDate(request.startTime)) + '<br><small>至 ' + escapeHtml(formatDate(request.endTime)) + '</small></td><td>' + statusBadge(request.status) + '<br><small>' + escapeHtml(nodes[request.currentNode] || request.currentNode) + '</small></td><td>' + escapeHtml(formatDate(request.createdAt)) + '</td><td><div class="table-actions"><button class="button button-secondary button-small" data-request-id="' + escapeHtml(request.id) + '">查看轨迹</button>' + (canWithdraw ? '<button class="button button-danger button-small" data-withdraw-id="' + escapeHtml(request.id) + '">撤回</button>' : '') + '</div></td></tr>';
        }).join('');
        target.innerHTML = '<table class="data-table"><thead><tr><th>请假类型</th><th>请假时间</th><th>状态 / 节点</th><th>提交时间</th><th>操作</th></tr></thead><tbody>' + rows + '</tbody></table>';
    }

    function renderTasks() {
        var target = document.getElementById('taskList');
        if (!state.tasks.length) {
            target.innerHTML = '<div class="empty-state"><strong>当前没有待办</strong>只有候选账号是你或已经签收给你的任务才会出现。</div>';
            renderStats();
            return;
        }
        target.innerHTML = state.tasks.map(function (task) {
            var assigned = !!task.assignee;
            var personal = task.taskDefinitionKey === 'employeeResubmit' || task.taskDefinitionKey === 'employeeConfirm';
            var buttons = '';
            if (task.taskDefinitionKey === 'employeeResubmit') {
                if (hasPermission('leave:task:resubmit')) {
                    buttons = '<button class="button button-primary button-small" data-task-action="resubmit" data-task-id="' + escapeHtml(task.taskId) + '">修改并重交</button>';
                }
            } else if (task.taskDefinitionKey === 'employeeConfirm') {
                if (hasPermission('leave:task:approve')) {
                    buttons = '<button class="button button-success button-small" data-task-action="confirm" data-task-id="' + escapeHtml(task.taskId) + '">确认安排</button>';
                }
            } else if (!assigned) {
                if (hasPermission('leave:task:claim')) {
                    buttons = '<button class="button button-primary button-small" data-task-action="claim" data-task-id="' + escapeHtml(task.taskId) + '">签收任务</button>';
                }
            } else if (hasPermission('leave:task:approve')) {
                buttons = '<button class="button button-success button-small" data-task-action="approve" data-task-id="' + escapeHtml(task.taskId) + '">同意</button><button class="button button-danger button-small" data-task-action="reject" data-task-id="' + escapeHtml(task.taskId) + '">拒绝</button>';
                if (task.taskDefinitionKey === 'managerApproval') {
                    buttons += '<button class="button button-secondary button-small" data-task-action="return" data-task-id="' + escapeHtml(task.taskId) + '">退回补充</button>';
                }
            }
            return '<article class="task-card ' + (assigned ? 'assigned' : '') + ' ' + (personal ? 'personal' : '') + '"><div class="task-card-head"><div><h3>' + escapeHtml(task.taskDefinitionKeyLabel || task.taskName) + '</h3><p>' + escapeHtml(task.applicantName) + ' · ' + escapeHtml(task.department) + '</p></div>' + (assigned ? '<span class="badge badge-green">已签收</span>' : '<span class="badge badge-orange">待签收</span>') + '</div><div class="task-summary"><div><span>请假类型 / 天数</span><strong>' + escapeHtml(typeLabel(task.leaveType)) + ' · ' + escapeHtml(task.leaveDays) + ' 天</strong></div><div><span>开始时间</span><strong>' + escapeHtml(formatDate(task.startTime)) + '</strong></div><div><span>结束时间</span><strong>' + escapeHtml(formatDate(task.endTime)) + '</strong></div><div><span>证明材料</span><strong>' + (task.hasMedicalProof ? '已提供' : '无需 / 未提供') + '</strong></div></div><p class="task-reason">事由：' + escapeHtml(task.reason) + '</p><div class="task-meta"><span>申请单 ' + escapeHtml(task.requestId.substring(0, 8)) + '</span><span>创建于 ' + escapeHtml(formatDate(task.createTime)) + '</span></div><div class="task-actions">' + buttons + '<span class="assignee-line">' + (assigned ? '办理人：' + escapeHtml(task.assignee) : '候选账号：' + escapeHtml(state.user.username === 'wangwu' || state.user.username === 'zhaoliu' || state.user.username === 'sunqi' ? state.user.username : '当前账号')) + '</span></div></article>';
        }).join('');
        renderStats();
    }

    function openModal(title, html) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalBody').innerHTML = html;
        document.getElementById('modal').hidden = false;
        document.body.style.overflow = 'hidden';
        setTimeout(function () {
            var focusable = document.querySelector('#modalBody input:not([type="hidden"]), #modalBody textarea, #modalBody button');
            if (focusable) {
                focusable.focus();
            }
        }, 30);
    }

    function closeModal() {
        document.getElementById('modal').hidden = true;
        document.getElementById('modalBody').innerHTML = '';
        document.body.style.overflow = '';
    }

    function leaveTypeOptions(selected) {
        return Object.keys(leaveTypes).map(function (key) {
            return '<option value="' + key + '"' + (key === selected ? ' selected' : '') + '>' + leaveTypes[key] + '</option>';
        }).join('');
    }

    function localInputValue(date) {
        function pad(value) {
            return String(value).padStart(2, '0');
        }
        return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
    }

    function openLeaveForm(task) {
        var start = new Date();
        start.setDate(start.getDate() + 1);
        start.setHours(9, 0, 0, 0);
        var end = new Date(start);
        end.setHours(18, 0, 0, 0);
        var isResubmit = !!task;
        var taskValue = isResubmit ? escapeHtml(task.taskId) : '';
        openModal(isResubmit ? '修改并重新提交' : '新建请假申请', '<form id="leaveForm" class="form-stack" data-task-id="' + taskValue + '"><div class="form-grid"><label><span>请假类型</span><select name="leaveType" required>' + leaveTypeOptions(isResubmit ? task.leaveType : 'ANNUAL') + '</select></label><label><span>请假天数</span><input name="leaveDays" type="number" min="0.5" max="30" step="0.5" value="' + (isResubmit ? escapeHtml(task.leaveDays) : '1') + '" required></label></div><div class="form-grid"><label><span>开始时间</span><input name="startTime" type="datetime-local" value="' + localInputValue(start) + '" required></label><label><span>结束时间</span><input name="endTime" type="datetime-local" value="' + localInputValue(end) + '" required></label></div><p class="form-hint">天数必须是 0.5 的整数倍；单次 0.5～30 天。</p><label><span>请假事由</span><textarea name="reason" maxlength="500" placeholder="请说明请假原因和工作交接安排" required>' + (isResubmit ? escapeHtml(task.reason) : '') + '</textarea></label><label class="checkbox-line"><input name="hasMedicalProof" type="checkbox"' + (isResubmit && task.hasMedicalProof ? ' checked' : '') + '><span>病假超过 1 天时，我已提供医疗证明</span></label><div class="note-box"><strong>系统会自动计算审批链路</strong><p>超过 3 天或病假 / 婚假 / 丧假进入 HR；5 天及以上再进入总监。</p></div><div class="modal-actions"><button class="button button-secondary" type="button" data-close-modal>取消</button><button class="button button-primary" type="submit">' + (isResubmit ? '重新提交' : '提交申请') + '</button></div></form>');
        document.getElementById('leaveForm').addEventListener('submit', function (event) {
            event.preventDefault();
            var form = event.currentTarget;
            if (!form.reportValidity()) {
                return;
            }
            var values = new FormData(form);
            var payload = {
                leaveType: values.get('leaveType'),
                startTime: values.get('startTime') + ':00',
                endTime: values.get('endTime') + ':00',
                leaveDays: Number(values.get('leaveDays')),
                reason: values.get('reason'),
                hasMedicalProof: form.querySelector('[name="hasMedicalProof"]').checked
            };
            runButton(form.querySelector('[type="submit"]'), async function () {
                if (isResubmit) {
                    await api('/leave/tasks/' + encodeURIComponent(task.taskId) + '/resubmit', {method: 'POST', body: payload});
                    showToast('已重新提交，流程回到经理审批', 'success');
                } else {
                    await api('/leave/requests', {method: 'POST', body: payload});
                    showToast('请假申请已提交', 'success');
                }
                closeModal();
                await refreshAll();
            });
        });
    }

    async function showRequest(requestId) {
        try {
            var results = await Promise.all([
                api('/leave/requests/' + encodeURIComponent(requestId)),
                api('/leave/requests/' + encodeURIComponent(requestId) + '/records')
            ]);
            var request = results[0];
            var records = results[1];
            var timeline = records.length ? records.map(function (record) {
                return '<div class="timeline-item"><strong>' + escapeHtml(nodes[record.node] || record.node) + ' · ' + escapeHtml(actions[record.action] || record.action) + '</strong><p>' + escapeHtml(record.operator) + (record.comment ? '：' + escapeHtml(record.comment) : '') + '</p><time>' + escapeHtml(formatDate(record.createdAt)) + '</time></div>';
            }).join('') : '<div class="empty-state">暂无审批轨迹</div>';
            openModal('请假申请轨迹', '<div class="detail-hero"><div><h3>' + escapeHtml(typeLabel(request.leaveType)) + ' · ' + escapeHtml(request.leaveDays) + ' 天</h3><p>申请单 ' + escapeHtml(request.id) + '</p></div>' + statusBadge(request.status) + '</div><div class="detail-grid"><div class="detail-item"><span>开始时间</span><strong>' + escapeHtml(formatDate(request.startTime)) + '</strong></div><div class="detail-item"><span>结束时间</span><strong>' + escapeHtml(formatDate(request.endTime)) + '</strong></div><div class="detail-item"><span>当前节点</span><strong>' + escapeHtml(nodes[request.currentNode] || request.currentNode) + '</strong></div><div class="detail-item"><span>医疗证明</span><strong>' + (request.hasMedicalProof ? '已提供' : '未提供') + '</strong></div></div><div class="detail-reason">' + escapeHtml(request.reason) + '</div><h3 style="margin-top:22px">审批轨迹</h3><div class="timeline">' + timeline + '</div><div class="modal-actions"><button class="button button-secondary" type="button" data-close-modal>关闭</button>' + (request.status === 'SUBMITTED' || request.status === 'RETURNED' ? '<button id="withdrawButton" class="button button-danger" type="button">撤回申请</button>' : '') + '</div>');
            var withdraw = document.getElementById('withdrawButton');
            if (withdraw) {
                withdraw.addEventListener('click', function () {
                    if (!window.confirm('确认撤回这次请假申请吗？')) {
                        return;
                    }
                    runButton(withdraw, async function () {
                        await api('/leave/requests/' + encodeURIComponent(requestId) + '/withdraw', {method: 'POST'});
                        showToast('申请已撤回', 'success');
                        closeModal();
                        await refreshAll();
                    });
                });
            }
        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    function openApproval(task, mode) {
        var modeInfo = {
            approve: ['同意审批', '同意后流程会按 BPMN 条件进入下一节点。'],
            reject: ['拒绝申请', '拒绝会直接结束流程，请填写明确意见。'],
            return: ['退回员工补充', '员工修改后将重新回到经理审批节点。'],
            confirm: ['确认请假安排', '确认后流程结束；年假申请会扣减相应余额。']
        }[mode];
        var confirmLabel = mode === 'confirm' ? '确认安排' : '确认' + modeInfo[0].substring(0, 2);
        openModal(modeInfo[0], '<div class="approval-context"><strong>' + escapeHtml(task.applicantName) + ' · ' + escapeHtml(typeLabel(task.leaveType)) + ' ' + escapeHtml(task.leaveDays) + ' 天</strong><br>' + escapeHtml(formatDate(task.startTime)) + ' 至 ' + escapeHtml(formatDate(task.endTime)) + '<br>' + escapeHtml(modeInfo[1]) + '</div><form id="approvalForm" class="form-stack"><label><span>审批意见' + (mode === 'reject' || mode === 'return' ? '（必填）' : '') + '</span><textarea name="comment" maxlength="500" placeholder="填写本次办理意见"' + (mode === 'reject' || mode === 'return' ? ' required' : '') + '></textarea></label><div class="modal-actions"><button class="button button-secondary" type="button" data-close-modal>取消</button><button class="button ' + (mode === 'approve' || mode === 'confirm' ? 'button-success' : 'button-danger') + '" type="submit">' + escapeHtml(confirmLabel) + '</button></div></form>');
        document.getElementById('approvalForm').addEventListener('submit', function (event) {
            event.preventDefault();
            var form = event.currentTarget;
            if (!form.reportValidity()) {
                return;
            }
            var comment = form.elements.comment.value.trim();
            var payload = {approved: mode === 'approve' || mode === 'confirm', returnToApplicant: mode === 'return', comment: comment};
            runButton(form.querySelector('[type="submit"]'), async function () {
                await api('/leave/tasks/' + encodeURIComponent(task.taskId) + '/approve', {method: 'POST', body: payload});
                showToast('任务办理完成', 'success');
                closeModal();
                await refreshAll();
            });
        });
    }

    function findTask(taskId) {
        return state.tasks.find(function (task) {
            return task.taskId === taskId;
        });
    }

    async function refreshTasks() {
        state.tasks = await api('/leave/tasks/mine');
        renderTasks();
    }

    async function refreshAll() {
        var calls = [];
        if (hasPermission('leave:view:balance')) {
            calls.push(api('/leave/balance'));
        }
        if (hasPermission('leave:view:all')) {
            calls.push(api('/leave/requests'));
        } else if (hasPermission('leave:view:mine')) {
            calls.push(api('/leave/requests/mine'));
        }
        if (hasPermission('leave:task:view')) {
            calls.push(api('/leave/tasks/mine'));
        }
        var results = await Promise.all(calls);
        var index = 0;
        state.balance = null;
        state.requests = [];
        state.tasks = [];
        if (hasPermission('leave:view:balance')) {
            state.balance = results[index++];
        }
        if (hasPermission('leave:view:all') || hasPermission('leave:view:mine')) {
            state.requests = results[index++];
        }
        if (hasPermission('leave:task:view')) {
            state.tasks = results[index++];
        }
        renderStats();
        renderRecentRequests();
        renderRequests();
        renderTasks();
        if (canViewProcess()) {
            await loadAdmin();
        }
        if (canViewRbac()) {
            await loadRbac();
        }
    }

    async function loadAdmin() {
        if (!canViewProcess()) {
            return;
        }
        var results = await Promise.all([
            api('/admin/definitions'),
            api('/admin/deployments'),
            api('/admin/tasks')
        ]);
        document.getElementById('definitionList').innerHTML = results[0].length ? results[0].map(function (definition) {
            var xmlButton = '<button class="button button-secondary button-small" data-definition-id="' + escapeHtml(definition.id) + '">查看 XML</button>';
            return '<div class="admin-entry"><div><strong>' + escapeHtml(definition.name) + '</strong><small>key=' + escapeHtml(definition.key) + ' · v' + escapeHtml(definition.version) + ' · ' + escapeHtml(definition.id) + '</small></div>' + xmlButton + '</div>';
        }).join('') : '<div class="empty-state">暂无流程定义</div>';
        document.getElementById('deploymentList').innerHTML = results[1].length ? results[1].map(function (deployment) {
            return '<div class="admin-entry"><div><strong>' + escapeHtml(deployment.name || deployment.id) + '</strong><small>' + escapeHtml(formatDate(deployment.deploymentTime)) + ' · ' + escapeHtml(deployment.id) + '</small></div></div>';
        }).join('') : '<div class="empty-state">暂无部署记录</div>';
        document.getElementById('allTaskList').innerHTML = results[2].length ? '<div class="table-wrap"><table class="data-table"><thead><tr><th>任务</th><th>节点</th><th>签收人</th><th>流程实例</th></tr></thead><tbody>' + results[2].map(function (task) {
            return '<tr><td>' + escapeHtml(task.name) + '</td><td><code>' + escapeHtml(task.id) + '</code></td><td>' + escapeHtml(task.assignee || '未签收') + '</td><td>' + escapeHtml(task.processInstanceId) + '</td></tr>';
        }).join('') + '</tbody></table></div>' : '<div class="empty-state">当前没有运行时任务</div>';
    }

    async function loadRbac() {
        if (!canViewRbac()) {
            return;
        }
        var results = await Promise.all([
            api('/admin/rbac/users'),
            api('/admin/rbac/roles'),
            api('/admin/rbac/permissions')
        ]);
        state.rbac = {users: results[0], roles: results[1], permissions: results[2]};
        var canManage = hasPermission('rbac:manage');
        document.getElementById('rbacUserList').innerHTML = results[0].length ? results[0].map(function (user) {
            var roles = (user.roles || []).map(function (role) {
                return '<span class="badge badge-purple">' + escapeHtml(role) + '</span>';
            }).join(' ');
            var action = canManage ? '<button class="button button-secondary button-small" data-rbac-user="' + escapeHtml(user.userId) + '">分配角色</button>' : '';
            return '<div class="admin-entry"><div><strong>' + escapeHtml(user.displayName) + ' · ' + escapeHtml(user.username) + '</strong><small>' + escapeHtml(user.department) + ' · ' + escapeHtml(user.userId) + '</small><div class="table-actions" style="margin-top:6px">' + roles + '</div></div>' + action + '</div>';
        }).join('') : '<div class="empty-state">暂无用户</div>';
        document.getElementById('rbacRoleList').innerHTML = results[1].length ? results[1].map(function (role) {
            var action = canManage ? '<button class="button button-secondary button-small" data-rbac-role="' + escapeHtml(role.roleCode) + '">配置权限</button>' : '';
            return '<div class="admin-entry"><div><strong>' + escapeHtml(role.roleName) + ' · ' + escapeHtml(role.roleCode) + '</strong><small>' + escapeHtml(role.description || '暂无说明') + '</small><small>已授权 ' + (role.permissions || []).length + ' 项</small></div>' + action + '</div>';
        }).join('') : '<div class="empty-state">暂无角色</div>';
    }

    function openUserRolePicker(userId) {
        var user = state.rbac.users.find(function (item) {
            return item.userId === userId;
        });
        if (!user) {
            return;
        }
        var options = state.rbac.roles.map(function (role) {
            var checked = (user.roles || []).indexOf(role.roleCode) >= 0 ? ' checked' : '';
            return '<label class="checkbox-line permission-option"><input type="checkbox" name="role" value="' + escapeHtml(role.roleCode) + '"' + checked + '><span>' + escapeHtml(role.roleName) + ' · ' + escapeHtml(role.roleCode) + '<small>' + escapeHtml(role.description || '') + '</small></span></label>';
        }).join('');
        openModal('分配角色 · ' + user.displayName, '<form id="rbacUserForm" class="form-stack"><div class="permission-picker">' + options + '</div><div class="note-box"><strong>多对多关系</strong><p>一个用户可以同时拥有多个角色，最终权限取所有角色权限的并集。</p></div><div class="modal-actions"><button class="button button-secondary" type="button" data-close-modal>取消</button><button class="button button-primary" type="submit">保存角色</button></div></form>');
        document.getElementById('rbacUserForm').addEventListener('submit', function (event) {
            event.preventDefault();
            var form = event.currentTarget;
            var selected = Array.from(form.querySelectorAll('input[name="role"]:checked')).map(function (input) {
                return input.value;
            });
            runButton(form.querySelector('[type="submit"]'), async function () {
                await api('/admin/rbac/users/' + encodeURIComponent(userId) + '/roles', {method: 'PUT', body: selected});
                closeModal();
                await loadRbac();
                state.user = await api('/auth/me');
                renderUser();
                await refreshAll();
                showToast('用户角色已更新', 'success');
            });
        });
    }

    function openRolePermissionPicker(roleCode) {
        var role = state.rbac.roles.find(function (item) {
            return item.roleCode === roleCode;
        });
        if (!role) {
            return;
        }
        var required = roleCode === 'ADMIN' ? ['auth:view', 'leave:view:all', 'process:view', 'process:deploy', 'rbac:view', 'rbac:manage'] : [];
        var options = state.rbac.permissions.map(function (permission) {
            var checked = (role.permissions || []).indexOf(permission.permissionCode) >= 0;
            var locked = required.indexOf(permission.permissionCode) >= 0;
            return '<label class="checkbox-line permission-option"><input type="checkbox" name="permission" value="' + escapeHtml(permission.permissionCode) + '"' + (checked || locked ? ' checked' : '') + (locked ? ' disabled' : '') + '><span>' + escapeHtml(permission.permissionName) + ' · ' + escapeHtml(permission.permissionCode) + '<small>' + escapeHtml(permission.description || '') + (locked ? ' · 管理员必需权限' : '') + '</small></span></label>';
        }).join('');
        openModal('配置权限 · ' + role.roleName, '<form id="rbacRoleForm" class="form-stack"><div class="permission-picker">' + options + '</div><div class="note-box"><strong>权限即时生效</strong><p>保存后，用户下一次请求会通过联表 SQL 重新加载角色和权限。</p></div><div class="modal-actions"><button class="button button-secondary" type="button" data-close-modal>取消</button><button class="button button-primary" type="submit">保存权限</button></div></form>');
        document.getElementById('rbacRoleForm').addEventListener('submit', function (event) {
            event.preventDefault();
            var form = event.currentTarget;
            var selected = Array.from(form.querySelectorAll('input[name="permission"]:checked')).map(function (input) {
                return input.value;
            });
            runButton(form.querySelector('[type="submit"]'), async function () {
                await api('/admin/rbac/roles/' + encodeURIComponent(roleCode) + '/permissions', {method: 'PUT', body: selected});
                closeModal();
                await loadRbac();
                state.user = await api('/auth/me');
                renderUser();
                await refreshAll();
                showToast('角色权限已更新', 'success');
            });
        });
    }

    function switchView(name) {
        var allowed = {
            requests: hasPermission('leave:view:mine') || hasPermission('leave:view:all'),
            tasks: hasPermission('leave:task:view'),
            admin: canViewProcess(),
            rbac: canViewRbac()
        };
        if (allowed[name] === false) {
            name = 'overview';
        }
        document.querySelectorAll('.view').forEach(function (view) {
            view.hidden = view.id !== 'view-' + name;
            view.classList.toggle('active', view.id === 'view-' + name);
        });
        document.querySelectorAll('.nav-item[data-view]').forEach(function (item) {
            item.classList.toggle('active', item.getAttribute('data-view') === name);
        });
        if (name === 'admin') {
            loadAdmin().catch(function (error) {
                showToast(error.message, 'error');
            });
        } else if (name === 'rbac') {
            loadRbac().catch(function (error) {
                showToast(error.message, 'error');
            });
        }
        window.history.replaceState(null, '', '#' + name);
    }

    document.addEventListener('click', function (event) {
        var viewButton = event.target.closest('[data-view]');
        if (viewButton) {
            switchView(viewButton.getAttribute('data-view'));
            return;
        }
        var actionButton = event.target.closest('[data-action]');
        if (actionButton) {
            var action = actionButton.getAttribute('data-action');
            if (action === 'new-request') {
                openLeaveForm(null);
            } else if (action === 'refresh-tasks') {
                runButton(actionButton, async function () {
                    await refreshTasks();
                    showToast('任务已刷新', 'success');
                });
            } else if (action === 'refresh-admin') {
                runButton(actionButton, async function () {
                    await loadAdmin();
                    showToast('流程信息已刷新', 'success');
                });
            } else if (action === 'refresh-rbac') {
                runButton(actionButton, async function () {
                    await loadRbac();
                    showToast('RBAC 关系已刷新', 'success');
                });
            }
            return;
        }
        if (event.target.closest('[data-close-modal]')) {
            closeModal();
            return;
        }
        var requestButton = event.target.closest('[data-request-id]');
        if (requestButton) {
            showRequest(requestButton.getAttribute('data-request-id'));
            return;
        }
        var withdrawButton = event.target.closest('[data-withdraw-id]');
        if (withdrawButton) {
            var withdrawId = withdrawButton.getAttribute('data-withdraw-id');
            if (window.confirm('确认撤回这次请假申请吗？')) {
                runButton(withdrawButton, async function () {
                    await api('/leave/requests/' + encodeURIComponent(withdrawId) + '/withdraw', {method: 'POST'});
                    showToast('申请已撤回', 'success');
                    await refreshAll();
                });
            }
            return;
        }
        var taskButton = event.target.closest('[data-task-action]');
        if (taskButton) {
            var task = findTask(taskButton.getAttribute('data-task-id'));
            var taskAction = taskButton.getAttribute('data-task-action');
            if (!task) {
                return;
            }
            if (taskAction === 'claim') {
                runButton(taskButton, async function () {
                    await api('/leave/tasks/' + encodeURIComponent(task.taskId) + '/claim', {method: 'POST'});
                    showToast('任务已签收', 'success');
                    await refreshAll();
                });
            } else if (taskAction === 'resubmit') {
                openLeaveForm(task);
            } else {
                openApproval(task, taskAction);
            }
            return;
        }
        var rbacUserButton = event.target.closest('[data-rbac-user]');
        if (rbacUserButton) {
            openUserRolePicker(rbacUserButton.getAttribute('data-rbac-user'));
            return;
        }
        var rbacRoleButton = event.target.closest('[data-rbac-role]');
        if (rbacRoleButton) {
            openRolePermissionPicker(rbacRoleButton.getAttribute('data-rbac-role'));
            return;
        }
        var xmlButton = event.target.closest('[data-definition-id]');
        if (xmlButton) {
            api('/admin/process-xml/' + encodeURIComponent(xmlButton.getAttribute('data-definition-id'))).then(function (xml) {
                openModal('流程定义 XML', '<pre class="xml-view">' + escapeHtml(xml) + '</pre><div class="modal-actions"><button class="button button-secondary" type="button" data-close-modal>关闭</button></div>');
            }).catch(function (error) {
                showToast(error.message, 'error');
            });
        }
    });

    document.getElementById('modalClose').addEventListener('click', closeModal);
    document.getElementById('modal').addEventListener('click', function (event) {
        if (event.target === event.currentTarget) {
            closeModal();
        }
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !document.getElementById('modal').hidden) {
            closeModal();
        }
    });

    var deployForm = document.getElementById('deployForm');
    deployForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var fileInput = document.getElementById('deployFile');
        if (!fileInput.files.length) {
            showToast('请选择 ZIP 文件', 'error');
            return;
        }
        var data = new FormData();
        data.append('file', fileInput.files[0]);
        runButton(deployForm.querySelector('[type="submit"]'), async function () {
            await api('/admin/deploy/zip', {method: 'POST', body: data});
            showToast('流程部署成功', 'success');
            deployForm.reset();
            await loadAdmin();
        });
    });

    document.getElementById('logoutButton').addEventListener('click', async function () {
        var button = this;
        button.disabled = true;
        try {
            await fetch(API + '/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + sessionStorage.getItem(ACCESS_TOKEN_KEY),
                    'X-Refresh-Request': 'true'
                },
                credentials: 'same-origin'
            });
        } catch (error) {
            button.disabled = false;
        } finally {
            clearSession();
            window.location.replace('/active/login');
        }
    });

    api('/auth/me').then(async function (user) {
        state.user = user;
        renderUser();
        await refreshAll();
        var initialView = window.location.hash.substring(1) || 'overview';
        switchView(initialView);
    }).catch(function (error) {
        var alert = document.getElementById('globalAlert');
        alert.textContent = error.message;
        alert.className = 'alert alert-error';
        alert.hidden = false;
    });
})();
