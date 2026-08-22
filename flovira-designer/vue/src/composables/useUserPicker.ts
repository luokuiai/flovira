import { nextTick, reactive, ref, toRefs, watch } from 'vue';
import { designerCapabilities, designerResourceItems, designerResources } from '@/api/flow/definition';
import { resourcesToTree, unwrapData } from '@/data/contracts';
import { useI18n } from '@/i18n';

/** 办理人选择弹窗的 props 形态（与 selectUser.vue 的 defineProps 对齐） */
export interface UseUserPickerProps {
  /** 弹窗显隐（v-model:userVisible） */
  userVisible?: boolean;
  /** 已选用户存储主键集合（v-model:selectUser） */
  selectUser?: any[];
  /** 已选办理人行（回显用） */
  permissionRows?: any[];
  /** 当前节点选定的审批人策略 */
  resourceType?: string;
  /** 是否允许多选 */
  multiple?: boolean;
}

/** 办理人选择弹窗的 emit 形态（与 selectUser.vue 的 defineEmits 对齐） */
export interface UseUserPickerEmit {
  (e: 'update:userVisible', visible: boolean): void;
  (e: 'handleUserSelect', checkedItemList: any[]): void;
}

/**
 * 办理人选择弹窗（selectUser）的逻辑层：集中 state / 数据请求 / 勾选与已选维护，
 * 让 SFC 只保留模板与编排（Roadmap ④ selectUser 拆分的 useUserPicker + ⑥ 逻辑分层）。
 *
 * 行为与原 selectUser.vue 内联脚本完全一致，仅把 `proxy.$refs` / `proxy.$nextTick`
 * 替换为模板 ref（groupTreeRef / queryRef）与 vue 的 nextTick，去除 getCurrentInstance 依赖。
 *
 * @param props selectUser 的 props（userVisible / selectUser / permissionRows）
 * @param emit selectUser 的 emit（update:userVisible / handleUserSelect）
 * @author warm
 */
export function useUserPicker(props: UseUserPickerProps, emit: UseUserPickerEmit) {
  const { t } = useI18n();

  // 部门树 / 查询表单的模板 ref（替代原 proxy.$refs）
  const groupTreeRef = ref<any>(null);
  const queryRef = ref<any>(null);

  const tabsValue = ref('');
  const tableList = ref<any[]>([]);
  const loading = ref(true);
  const total = ref(0);
  const dateRange = ref<any[]>([]);
  const groupName = ref('');
  const groupOptions = ref<any>(undefined);
  // 树形区域折叠状态（默认展开，数据加载后根据高度自动判断）
  const treeCollapsed = ref(window.innerWidth <= 768);
  // 移动端分页：每页10条，滚动加载更多
  const MOBILE_PAGE_SIZE = 10;
  const mobileLoadingMore = ref(false);
  const mobileNoMore = ref(false);
  const tabsList = ref<any[]>([]);
  // 列显隐信息
  const columns = ref([
    { key: 0, label: t('between.handlerStorageId'), visible: true },
    { key: 1, label: t('selectUser.permCode'), visible: true },
    { key: 2, label: t('between.handlerName'), visible: true },
    { key: 3, label: t('selectUser.permGroup'), visible: true },
    { key: 4, label: t('selectUser.createTime'), visible: true }
  ]);
  const checkedItemList = ref<any[]>([]); // 已选的itemList
  const strategyLabels: Record<string, string> = {
    USER: '用户',
    ROLE: '角色',
    ORGANIZATION: '组织',
  };
  const approverStrategyLabel = (strategy: string) => strategyLabels[strategy] || strategy;
  const data = reactive({
    queryParams: {
      pageNum: 1,
      pageSize: 10,
      userName: undefined,
      status: '0',
      groupId: undefined
    } as Record<string, any>,
    checkAllInfo: {
      isIndeterminate: false,
      isChecked: false,
    }
  });

  const { queryParams, checkAllInfo } = toRefs(data);

  /** 根据名称筛选部门树（filter 转发给 UserPickerTree 暴露的 el-tree 方法） */
  watch(groupName, val => {
    groupTreeRef.value?.filter(val);
  });
  watch(() => props.selectUser, (val, oldVal) => {
    if (oldVal) {
      nextTick(() => {
        checkedItemList.value = checkedItemList.value.filter(e => {
          let index = val ? val.findIndex(v => v === e.storageId) : -1;
          tableList.value.forEach(u => {
            if (u.storageId === e.storageId) u.isChecked = index !== -1;
          });
          return index !== -1;
        });
      });
    } else checkedItemList.value = val ? props.permissionRows!.filter(n => n.storageId) : [];
  }, { deep: true, immediate: true });

  /** 获取tabs列表 */
  function getTabsType() {
    designerCapabilities().then(async res => {
      const capabilities = unwrapData(res);
      const resourceTypes = new Set(capabilities?.resourceTypes || []);
      tabsList.value = props.resourceType && resourceTypes.has(props.resourceType) ? [props.resourceType] : [];
      if (tabsList.value.length > 0) {
        tabsValue.value = tabsList.value[0]
      }
      const organizations = await designerResourceItems({
        resourceType: 'ORGANIZATION',
        pageNum: 1,
        pageSize: 1000,
      });
      groupOptions.value = resourcesToTree(organizations);
      // 移动端初始加载使用10条分页
      if (window.innerWidth <= 768 && !queryParams.value.pageSize) {
        queryParams.value.pageSize = MOBILE_PAGE_SIZE;
        queryParams.value.pageNum = 1;
      }
      getList();
    }).catch(() => {
      // 接口异常也结束 loading，展示空状态而非一直转圈
      loading.value = false;
      mobileLoadingMore.value = false;
    });
  }

  /** 查询用户列表 */
  function getList(append = false) {
    if (append) {
      mobileLoadingMore.value = true;
    } else {
      loading.value = true;
    }
    if (!tabsValue.value) {
      loading.value = false;
      mobileLoadingMore.value = false;
      return;
    }
    dateRange.value = Array.isArray(dateRange.value) ? dateRange.value : [];
    designerResources({
      resourceType: tabsValue.value,
      keyword: queryParams.value.handlerName || queryParams.value.handlerCode || queryParams.value.userName,
      scopeId: queryParams.value.groupId,
      pageNum: queryParams.value.pageNum,
      pageSize: queryParams.value.pageSize,
      parameters: {
        beginTime: dateRange.value[0],
        endTime: dateRange.value[1],
      },
    }).then(res => {
      loading.value = false;
      mobileLoadingMore.value = false;
      const page = unwrapData(res) || { items: [], total: 0 };
      const rows: any[] = page.items.map(item => ({
        storageId: item.id,
        resourceType: item.resourceType || tabsValue.value,
        handlerCode: item.code,
        handlerName: item.name,
        groupName: item.metadata?.groupName,
        createTime: item.metadata?.createTime,
      }));
      rows.forEach(item => {
        item.isChecked = checkedItemList.value.findIndex(e => e.storageId === item.storageId) !== -1;
      });
      if (append && window.innerWidth <= 768) {
        tableList.value = [...tableList.value, ...rows];
      } else {
        tableList.value = rows;
      }
      total.value = page.total;
      nextTick(() => {
        if (groupName.value) groupTreeRef.value?.filter(groupName.value);
      });
      isCheckedAll();
    }).catch(() => {
      // 接口异常也结束 loading，展示空状态而非一直转圈
      loading.value = false;
      mobileLoadingMore.value = false;
    });
  }

  /** 移动端滚动加载更多 */
  function loadMoreMobile() {
    if (mobileLoadingMore.value || mobileNoMore.value || loading.value) return;
    if (tableList.value.length >= total.value) {
      mobileNoMore.value = true;
      return;
    }
    queryParams.value.pageNum++;
    queryParams.value.pageSize = MOBILE_PAGE_SIZE;
    getList(true);
  }

  /** 移动端卡片列表滚动事件 */
  function onMobileScroll(e: any) {
    const el = e.target;
    if (!el) return;
    // 距离底部 60px 时触发加载
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 60) {
      loadMoreMobile();
    }
  }

  /** 重置移动端分页状态 */
  function resetMobilePagination() {
    mobileLoadingMore.value = false;
    mobileNoMore.value = false;
    queryParams.value.pageNum = 1;
    if (window.innerWidth <= 768) {
      queryParams.value.pageSize = MOBILE_PAGE_SIZE;
    }
  }
  /** 节点单击事件 */
  function handleNodeClick(data: any) {
    queryParams.value.groupId = data.id;
    handleQuery();
  }

  /** tab切换 */
  function tabChange() {
    queryParams.value.groupId = undefined;
    resetQuery();
  }

  /** 搜索按钮操作 */
  function handleQuery() {
    resetMobilePagination();
    getList();
  }

  /** 重置按钮操作 */
  function resetQuery() {
    dateRange.value = [];
    groupName.value = '';
    queryRef.value?.resetFields();
    queryParams.value.groupId = undefined;
    groupTreeRef.value?.setCurrentKey(null);
    handleQuery();
  }

  // 是否全选中
  function isCheckedAll() {
    const len = tableList.value.length;
    let count = 0;
    tableList.value.map(item => {
      if (item.isChecked) count += 1;
    });
    checkAllInfo.value.isChecked = len === count && len > 0;
    checkAllInfo.value.isIndeterminate = count > 0 && count < len;
  }

  // 全选
  function handleCheckAll() {
    if (props.multiple === false) return;
    const checkedArr = checkedItemList.value;
    checkAllInfo.value.isIndeterminate = false;
    if (checkAllInfo.value.isChecked) {
      tableList.value = tableList.value.map(item => {
        item.isChecked = true;
        if (checkedItemList.value.findIndex(e => e.storageId === item.storageId) === -1) {
          checkedArr.push({ storageId: item.storageId, handlerName: item.handlerName, resourceType: item.resourceType });
        }
        return item;
      });
    } else {
      tableList.value = tableList.value.map(item => {
        item.isChecked = false;
        let index = checkedArr.findIndex(e => e.storageId === item.storageId);
        if (index !== -1) checkedArr.splice(index, 1);
        return item;
      });
    }
    checkedItemList.value = checkedArr;
  }

  function handleCheck(row: any) {
    if (props.multiple === false && !row.isChecked) {
      tableList.value.forEach(e => { e.isChecked = false; });
      checkedItemList.value = [];
    }
    tableList.value.forEach(e => {
      if (e.storageId === row.storageId) e.isChecked = !e.isChecked;
    });
    const checkedArr = [...checkedItemList.value];
    if (row.isChecked) {
      if (checkedArr.findIndex(e => e.storageId === row.storageId) === -1) {
        checkedArr.push({ storageId: row.storageId, handlerName: row.handlerName, resourceType: row.resourceType });
      }
    } else {
      const index = checkedArr.findIndex(n => n.storageId === row.storageId);
      if (index !== -1) checkedArr.splice(index, 1);
    }
    checkedItemList.value = checkedArr;
    isCheckedAll();
  }

  // 删除标签
  function handleClose(storageId: any) {
    tableList.value.forEach(e => {
      if (e.storageId === storageId) e.isChecked = !e.isChecked;
    });
    const checkedArr = checkedItemList.value
    const index = checkedArr.findIndex(n => n.storageId === storageId);
    if (index !== -1) checkedArr.splice(index, 1);
    checkedItemList.value = checkedArr;
    isCheckedAll();
  }

  // 清空所有已选
  function clearAllSelected() {
    tableList.value.forEach(e => { e.isChecked = false; });
    checkedItemList.value = [];
    checkAllInfo.value.isChecked = false;
    checkAllInfo.value.isIndeterminate = false;
  }

  // 取消按钮
  function cancel() {
    emit('update:userVisible', false);
  }

  // 提交按钮
  function submitForm() {
    emit('handleUserSelect', checkedItemList.value);
    cancel();
  }

  getTabsType();

  return {
    // 模板 ref
    groupTreeRef,
    queryRef,
    // state
    tabsValue,
    tableList,
    loading,
    total,
    dateRange,
    groupName,
    groupOptions,
    treeCollapsed,
    mobileLoadingMore,
    mobileNoMore,
    tabsList,
    approverStrategyLabel,
    columns,
    checkedItemList,
    queryParams,
    checkAllInfo,
    // methods（模板事件）
    getList,
    onMobileScroll,
    handleNodeClick,
    tabChange,
    handleQuery,
    resetQuery,
    handleCheckAll,
    handleCheck,
    handleClose,
    clearAllSelected,
    submitForm,
  };
}
