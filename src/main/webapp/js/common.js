(function () {
    'use strict';

    // 根据当前脚本地址自动推导应用上下文路径，兼容 /LibraryWeb2026 与 ROOT 部署
    var current = document.currentScript;
    var scriptSrc = current ? current.src : (location.origin + '/LibraryWeb2026/js/common.js');
    var CTX = scriptSrc.replace(/\/js\/common\.js(?:\?.*)?$/, '');

    var api = axios.create({
        baseURL: CTX + '/api',
        timeout: 20000
    });

    api.interceptors.response.use(function (response) {
        var body = response.data;
        if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
            if (body.code === 0) {
                return body;
            }
            var error = new Error(body.message || '操作失败');
            error.business = body;
            return Promise.reject(error);
        }
        return { code: 0, message: 'ok', data: body };
    }, function (error) {
        var status = error.response && error.response.status;
        if (status === 401) {
            if (!/login\.html$/.test(location.pathname)) {
                toast('登录已失效，请重新登录', 'error');
                setTimeout(function () {
                    location.replace(CTX + '/login.html');
                }, 500);
            }
        } else if (status === 403) {
            toast((error.response.data && error.response.data.message) || '没有操作权限', 'error');
        } else if (error.code === 'ECONNABORTED') {
            toast('请求超时，请稍后重试', 'error');
        } else if (!error.response) {
            toast('网络异常，请确认服务已启动', 'error');
        }
        return Promise.reject(error);
    });

    function toForm(data) {
        var params = new URLSearchParams();
        Object.keys(data || {}).forEach(function (key) {
            var value = data[key];
            if (value !== undefined && value !== null) {
                params.append(key, value);
            }
        });
        return params;
    }

    function get(url, params) {
        return api.get(url, { params: params || {} });
    }

    function post(url, data) {
        return api.post(url, toForm(data || {}), {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }
        });
    }

    function toast(message, type) {
        var box = document.getElementById('toast-box');
        if (!box) {
            box = document.createElement('div');
            box.id = 'toast-box';
            document.body.appendChild(box);
        }
        var item = document.createElement('div');
        item.className = 'toast ' + (type === 'error' ? 'toast-error' : (type === 'warn' ? 'toast-warn' : 'toast-ok'));
        item.textContent = message;
        box.appendChild(item);
        setTimeout(function () {
            item.classList.add('toast-out');
            setTimeout(function () {
                item.remove();
            }, 300);
        }, 2600);
    }

    function errorMessage(error) {
        if (!error) {
            return '操作失败';
        }
        if (error.business && error.business.message) {
            return error.business.message;
        }
        if (error.response && error.response.data && error.response.data.message) {
            return error.response.data.message;
        }
        return error.message || '操作失败';
    }

    function fmt(value) {
        if (value === null || value === undefined || value === '') {
            return '—';
        }
        var text = String(value).replace('T', ' ');
        return text.length > 19 ? text.substring(0, 19) : text;
    }

    function fmtDate(value) {
        if (!value) {
            return '—';
        }
        return String(value).replace('T', ' ').substring(0, 10);
    }

    function parseTime(value) {
        if (!value) {
            return null;
        }
        var time = new Date(String(value).replace('T', ' ').replace(/-/g, '/')).getTime();
        return isNaN(time) ? null : time;
    }

    function isOverdue(loan) {
        if (!loan || loan.status === '已还' || loan.returnDate) {
            return false;
        }
        var due = parseTime(loan.dueDate);
        return due !== null && due < Date.now();
    }

    function loanStatus(loan) {
        if (!loan) {
            return '—';
        }
        if (loan.status === '已还' || loan.returnDate) {
            return '已还';
        }
        return isOverdue(loan) ? '超期' : (loan.status || '未还');
    }

    function statusClass(status) {
        switch (status) {
            case '正常':
            case '已还':
            case '已通过':
                return 'badge-success';
            case '未还':
                return 'badge-info';
            case '超期':
            case '已驳回':
            case '已废弃':
                return 'badge-danger';
            case '待审核':
                return 'badge-warn';
            default:
                return 'badge-muted';
        }
    }

    function roleClass(role) {
        switch (role) {
            case '管理员':
                return 'role-admin';
            case '操作员':
                return 'role-operator';
            case '教师':
                return 'role-teacher';
            default:
                return 'role-student';
        }
    }

    var NAV = [
        { label: '首页', href: 'home.html', icon: '🏠', always: true },
        { label: '用户查询', href: 'users.html', icon: '👤', perm: '用户信息查询' },
        { label: '图书管理', href: 'books.html', icon: '📚', any: ['添加书籍', '查看所有书籍', '废弃图书', '编辑图书', '图书副本编辑'] },
        { label: '借阅查询', href: 'loan-query.html', icon: '🔎', perm: '借阅记录查询' },
        { label: '数据统计', href: 'stats.html', icon: '📊', perm: '借阅数据统计' },
        { label: '借书', href: 'borrow.html', icon: '📖', perm: '借书' },
        { label: '还书', href: 'return.html', icon: '↩️', perm: '还书' },
        { label: '我的借阅', href: 'my-loans.html', icon: '🗂️', perm: '个人借阅查询' },
        { label: '推荐图书', href: 'recommend.html', icon: '✍️', perm: '推荐图书' },
        { label: '审阅推荐', href: 'review.html', icon: '✅', perm: '审阅推荐图书' }
    ];

    var meCache = null;

    async function requireMe(force) {
        if (meCache && !force) {
            return meCache;
        }
        var response = await get('/auth/me');
        if (!response.data) {
            location.replace(CTX + '/login.html');
            return null;
        }
        meCache = response.data;
        window.me = meCache;
        return meCache;
    }

    function buildSidebar(active) {
        var sidebar = document.getElementById('sidebar');
        if (!sidebar) {
            return;
        }
        var permissions = (meCache && meCache.permissions) || [];
        var html = '<div class="nav-title">功能导航</div>';
        NAV.forEach(function (item) {
            var visible = item.always
                || (item.perm && permissions.indexOf(item.perm) >= 0)
                || (item.any && item.any.some(function (perm) { return permissions.indexOf(perm) >= 0; }));
            if (!visible) {
                return;
            }
            var activeClass = active && item.label === active ? ' active' : '';
            html += '<a class="nav-item' + activeClass + '" href="' + item.href + '">'
                + '<span class="nav-icon">' + item.icon + '</span>'
                + '<span>' + item.label + '</span></a>';
        });
        sidebar.innerHTML = html;
    }

    async function initPage(active) {
        var me = await requireMe();
        if (!me) {
            return null;
        }
        buildSidebar(active);
        var activeItem = NAV.filter(function (item) { return item.label === active; })[0];
        if (activeItem) {
            document.title = activeItem.label + ' - 图书馆管理系统';
        }
        return me;
    }

    function can(permission) {
        var permissions = (meCache && meCache.permissions) || [];
        return permissions.indexOf(permission) >= 0;
    }

    async function logout() {
        try {
            await post('/auth/logout', {});
        } catch (e) {
            // 会话可能已经失效，直接回到登录页即可
        }
        location.replace(CTX + '/login.html');
    }

    function today() {
        var date = new Date();
        var month = String(date.getMonth() + 1).padStart(2, '0');
        var day = String(date.getDate()).padStart(2, '0');
        return date.getFullYear() + '-' + month + '-' + day;
    }

    window.Lib = {
        CTX: CTX,
        api: api,
        get: get,
        post: post,
        toast: toast,
        errorMessage: errorMessage,
        fmt: fmt,
        fmtDate: fmtDate,
        parseTime: parseTime,
        isOverdue: isOverdue,
        loanStatus: loanStatus,
        statusClass: statusClass,
        roleClass: roleClass,
        requireMe: requireMe,
        initPage: initPage,
        can: can,
        logout: logout,
        today: today
    };
})();