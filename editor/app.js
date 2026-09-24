const state = {
  catalog: [],
  layout: createEmptyLayout(),
  history: [],
  activePage: 0,
  targetPage: 0,
  targetDockSlot: 0,
  targetFolderId: "",
  search: "",
  activeFolderArea: null,
  activeFolderId: null,
  lastLoadedFolderId: null,
  catalogSelection: new Set(),
  boardSelection: new Set(),
  dockSelection: new Set(),
  folderAppSelection: new Set(),
  redoHistory: []
};

const marquee = {
  appList: null,
  folderList: null,
  board: null,
  suppressAppListClick: false,
  suppressFolderListClick: false,
  suppressBoardClick: false
};

const pressDrag = {
  pending: null,
  active: null,
  overlay: null,
  hoverTarget: null,
  suppressClickUntil: 0
};

const els = {
  appsFile: document.querySelector("#apps-file"),
  layoutFile: document.querySelector("#layout-file"),
  exportLayout: document.querySelector("#export-layout"),
  resetLayout: document.querySelector("#reset-layout"),
  gridCols: document.querySelector("#grid-cols"),
  gridRows: document.querySelector("#grid-rows"),
  selectionSummary: document.querySelector("#selection-summary"),
  selectedItemDetails: document.querySelector("#selected-item-details"),
  clearSelection: document.querySelector("#clear-selection"),
  deleteSelection: document.querySelector("#delete-selection"),
  targetPage: document.querySelector("#target-page"),
  homePage: document.querySelector("#home-page"),
  targetDockSlot: document.querySelector("#target-dock-slot"),
  targetFolder: document.querySelector("#target-folder"),
  movePage: document.querySelector("#move-page"),
  moveDock: document.querySelector("#move-dock"),
  moveFolder: document.querySelector("#move-folder"),
  setHomeCurrent: document.querySelector("#set-home-current"),
  folderTitle: document.querySelector("#folder-title"),
  renameFolder: document.querySelector("#rename-folder"),
  search: document.querySelector("#search"),
  dockCount: document.querySelector("#dock-count"),
  dockContext: document.querySelector("#dock-context"),
  dockList: document.querySelector("#dock-list"),
  folderAppCount: document.querySelector("#folder-app-count"),
  folderContext: document.querySelector("#folder-context"),
  folderAppList: document.querySelector("#folder-app-list"),
  appCount: document.querySelector("#app-count"),
  appList: document.querySelector("#app-list"),
  pageTabs: document.querySelector("#page-tabs"),
  pageTitle: document.querySelector("#page-title"),
  addPage: document.querySelector("#add-page"),
  removePage: document.querySelector("#remove-page"),
  movePageLeft: document.querySelector("#move-page-left"),
  movePageRight: document.querySelector("#move-page-right"),
  board: document.querySelector("#board")
};

const HISTORY_LIMIT = 80;
const RECENT_APPS_LIMIT = 15;
const DOCK_SLOT_COUNT = 5;

bindEvents();
render();

function bindEvents() {
  bindMarqueeSelection(els.appList, "appList");
  bindMarqueeSelection(els.folderAppList, "folderList");
  bindMarqueeSelection(els.board, "board");

  els.appsFile.addEventListener("change", async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const parsed = JSON.parse(await file.text());
    const apps = Array.isArray(parsed) ? parsed : parsed.apps;
    state.catalog = normalizeCatalog(apps || []);
    clearSelectionState();
    render();
  });

  els.layoutFile.addEventListener("change", async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    state.layout = normalizeLayout(JSON.parse(await file.text()));
    clearHistory();
    state.activePage = state.layout.homePageIndex;
    state.targetPage = state.layout.homePageIndex;
    clearSelectionState();
    syncGridInputs();
    render();
  });

  els.exportLayout.addEventListener("click", () => {
    const exportedLayout = normalizeLayout(state.layout);
    exportedLayout.exportedAt = new Date().toISOString();
    state.layout = exportedLayout;
    downloadJson("launcher-backup.json", exportedLayout);
    render();
  });

  els.resetLayout.addEventListener("click", () => {
    const updated = createEmptyLayout(Number(els.gridCols.value), Number(els.gridRows.value));
    commitLayout(updated);
    state.activePage = 0;
    state.targetPage = 0;
    clearSelectionState();
    render();
  });

  els.gridCols.addEventListener("change", updateGrid);
  els.gridRows.addEventListener("change", updateGrid);

  els.clearSelection.addEventListener("click", () => {
    clearSelectionState();
    render();
  });

  els.deleteSelection.addEventListener("click", () => {
    deleteSelectedItems({ interactive: true });
  });

  els.targetPage.addEventListener("change", (event) => {
    state.targetPage = Number(event.target.value || 0);
    renderSummary();
  });

  els.homePage.addEventListener("change", (event) => {
    const updated = deepClone(state.layout);
    updated.homePageIndex = clampPageIndex(event.target.value);
    commitLayout(updated);
    render();
  });

  els.targetDockSlot.addEventListener("change", (event) => {
    state.targetDockSlot = clampDockSlot(event.target.value);
    renderDock();
    renderSummary();
  });

  els.targetFolder.addEventListener("change", (event) => {
    state.targetFolderId = String(event.target.value || "");
    renderSummary();
  });

  els.setHomeCurrent.addEventListener("click", () => {
    const updated = deepClone(state.layout);
    updated.homePageIndex = state.activePage;
    commitLayout(updated);
    render();
  });

  els.renameFolder.addEventListener("click", () => {
    const activeFolder = getActiveFolderItem();
    const nextTitle = els.folderTitle.value.trim();
    if (!activeFolder) {
      alert("Select a folder first.");
      return;
    }
    if (!nextTitle) {
      alert("Enter a folder name first.");
      return;
    }
    const updated = deepClone(state.layout);
    const targetFolder = getActiveFolderItem(updated);
    if (!targetFolder) return;
    targetFolder.title = nextTitle;
    commitLayout(updated);
    render();
  });

  els.movePage.addEventListener("click", () => {
    const selectedItems = getSelectedBoardItems();
    const selectedDockItems = getSelectedDockItems();
    const selectedApps = getSelectedCatalogEntries();
    const selectedFolderRefs = getSelectedFolderRefs();
    if (selectedItems.length === 0 && selectedDockItems.length === 0 && selectedApps.length === 0 && selectedFolderRefs.length === 0) {
      alert("Select apps on the left, items on the right, or both.");
      return;
    }

    const updated = deepClone(state.layout);
    const destinationPageIndex = clampPageIndex(state.targetPage);
    const sourcePage = updated.pages[state.activePage];
    const selectedIds = new Set(selectedItems.map((item) => item.id));
    const selectedDockIds = new Set(selectedDockItems.map((item) => item.id));
    const sourceFolder = getActiveFolderItem(updated);
    if (sourceFolder && selectedFolderRefs.length > 0) {
      selectedIds.delete(sourceFolder.id);
      selectedDockIds.delete(sourceFolder.id);
    }
    const movingItems = sourcePage.items.filter((item) => selectedIds.has(item.id));
    sourcePage.items = sourcePage.items.filter((item) => !selectedIds.has(item.id));
    const movingDockItems = updated.dock.filter((item) => selectedDockIds.has(item.id));
    updated.dock = updated.dock.filter((item) => !selectedDockIds.has(item.id));
    if (sourceFolder && selectedFolderRefs.length > 0) {
      const selectedFolderKeys = new Set(selectedFolderRefs.map(getRefKey));
      sourceFolder.appRefs = sourceFolder.appRefs.filter((ref) => !selectedFolderKeys.has(getRefKey(ref)));
    }

    const destinationPage = updated.pages[destinationPageIndex];

    for (const item of movingItems.concat(movingDockItems)) {
      const target = findFirstAvailableArea(updated, destinationPageIndex, item.spanX, item.spanY);
      if (!target) {
        alert("Not enough space on the target page for the current selection.");
        return;
      }
      item.x = target.x;
      item.y = target.y;
      destinationPage.items.push(item);
    }

    for (const app of selectedApps) {
      const target = findFirstAvailableArea(updated, destinationPageIndex, 1, 1);
      if (!target) {
        alert("Not enough space on the target page for the current selection.");
        return;
      }
      destinationPage.items.push(createAppItem(app, target));
    }

    for (const ref of selectedFolderRefs) {
      const target = findFirstAvailableArea(updated, destinationPageIndex, 1, 1);
      if (!target) {
        alert("Not enough space on the target page for the current selection.");
        return;
      }
      destinationPage.items.push(createAppItem(ref, target));
    }

    if (sourceFolder && selectedFolderRefs.length > 0) {
      removeEmptyFolderIfNeeded(updated, state.activeFolderArea, state.activePage, sourceFolder.id);
    }

    commitLayout(updated);
    state.activePage = destinationPageIndex;
    state.targetPage = destinationPageIndex;
    clearSelectionState();
    render();
  });

  els.moveDock.addEventListener("click", () => {
    const selectedItems = getSelectedBoardItems();
    const selectedDockItems = getSelectedDockItems();
    const selectedApps = getSelectedCatalogEntries();
    const selectedFolderRefs = getSelectedFolderRefs();
    if (selectedItems.length === 0 && selectedDockItems.length === 0 && selectedApps.length === 0 && selectedFolderRefs.length === 0) {
      alert("Select apps, page items, dock items, or folder apps first.");
      return;
    }

    const updated = deepClone(state.layout);
    const sourcePage = updated.pages[state.activePage];
    const sourceFolder = getActiveFolderItem(updated);
    const selectedPageIds = new Set(selectedItems.map((item) => item.id));
    const selectedDockIds = new Set(selectedDockItems.map((item) => item.id));
    if (sourceFolder && selectedFolderRefs.length > 0) {
      selectedPageIds.delete(sourceFolder.id);
      selectedDockIds.delete(sourceFolder.id);
    }

    const movingPageItems = sourcePage.items.filter((item) => selectedPageIds.has(item.id));
    sourcePage.items = sourcePage.items.filter((item) => !selectedPageIds.has(item.id));

    const movingDockItems = updated.dock.filter((item) => selectedDockIds.has(item.id));
    updated.dock = updated.dock.filter((item) => !selectedDockIds.has(item.id));

    if (sourceFolder && selectedFolderRefs.length > 0) {
      const selectedFolderKeys = new Set(selectedFolderRefs.map(getRefKey));
      sourceFolder.appRefs = sourceFolder.appRefs.filter((ref) => !selectedFolderKeys.has(getRefKey(ref)));
    }

    const itemsToInsert = [];
    for (const item of movingPageItems.concat(movingDockItems)) {
      if (item.type === "widget") {
        alert("Widgets cannot be placed in the dock.");
        return;
      }
      itemsToInsert.push(item);
    }
    for (const app of selectedApps) {
      itemsToInsert.push(createAppItem(app, { x: 0, y: 0 }));
    }
    for (const ref of selectedFolderRefs) {
      itemsToInsert.push(createAppItem(ref, { x: 0, y: 0 }));
    }

    const availableSlots = getDockAvailableSlots(updated, state.targetDockSlot);
    if (itemsToInsert.length > availableSlots.length) {
      alert("Not enough room in the dock for the current selection.");
      return;
    }

    itemsToInsert.forEach((item, index) => {
      setDockItemAtSlot(updated, item, availableSlots[index]);
    });

    if (sourceFolder && selectedFolderRefs.length > 0) {
      removeEmptyFolderIfNeeded(updated, state.activeFolderArea, state.activePage, sourceFolder.id);
    }

    commitLayout(updated);
    clearSelectionState();
    render();
  });

  els.moveFolder.addEventListener("click", () => {
    const selectedItems = getSelectedBoardItems();
    const selectedDockItems = getSelectedDockItems();
    const selectedApps = getSelectedCatalogEntries();
    const selectedFolderRefs = getSelectedFolderRefs();
    if (selectedItems.length === 0 && selectedDockItems.length === 0 && selectedApps.length === 0 && selectedFolderRefs.length === 0) {
      alert("Select apps, folder apps, or board items first.");
      return;
    }

    const updated = deepClone(state.layout);
    const page = updated.pages[state.activePage];
    const selectedPageIds = new Set(selectedItems.map((item) => item.id));
    const selectedDockIds = new Set(selectedDockItems.map((item) => item.id));
    const selectedOnPage = page.items.filter((item) => selectedPageIds.has(item.id));
    const selectedOnDock = updated.dock.filter((item) => selectedDockIds.has(item.id));
    const sourceFolder = getActiveFolderItem(updated);
    const explicitDestinationFolder = findFolderById(updated, state.targetFolderId, state.activePage);
    const selectedDestinationFolder = selectedOnPage.find((item) => item.type === "folder" && item.id !== sourceFolder?.id) || null;
    const selectedDockDestinationFolder = selectedOnDock.find((item) => item.type === "folder" && item.id !== sourceFolder?.id) || null;
    const destinationFolder =
      (explicitDestinationFolder && explicitDestinationFolder.item.id !== sourceFolder?.id ? explicitDestinationFolder : null) ||
      (selectedDestinationFolder ? { area: "page", item: selectedDestinationFolder } : null) ||
      (selectedDockDestinationFolder ? { area: "dock", item: selectedDockDestinationFolder } : null) ||
      null;
    const refsToAdd = [];

    selectedOnPage.forEach((item) => {
      if ((destinationFolder && item.id === destinationFolder.item.id) || (sourceFolder && item.id === sourceFolder.id)) return;
      refsToAdd.push(...itemToRefs(item));
    });
    selectedOnDock.forEach((item) => {
      if ((destinationFolder && item.id === destinationFolder.item.id) || (sourceFolder && item.id === sourceFolder.id)) return;
      refsToAdd.push(...itemToRefs(item));
    });
    selectedApps.forEach((app) => {
      refsToAdd.push({ packageName: app.packageName, activityName: app.activityName });
    });
    refsToAdd.push(...selectedFolderRefs);

    const dedupedRefs = dedupeRefs(refsToAdd);
    const folderTitle = els.folderTitle.value.trim();
    const pageItemsToRemove = new Set(selectedOnPage.filter((item) => item.id !== destinationFolder?.item.id && item.id !== sourceFolder?.id).map((item) => item.id));
    const dockItemsToRemove = new Set(selectedOnDock.filter((item) => item.id !== destinationFolder?.item.id && item.id !== sourceFolder?.id).map((item) => item.id));

    if (destinationFolder) {
      if (dedupedRefs.length === 0 && !folderTitle) {
        alert("Select apps to add, or provide a new folder name.");
        return;
      }
      page.items = page.items.filter((item) => !pageItemsToRemove.has(item.id));
      updated.dock = updated.dock.filter((item) => !dockItemsToRemove.has(item.id));
      reindexDock(updated.dock);
      if (sourceFolder && selectedFolderRefs.length > 0) {
        const selectedFolderKeys = new Set(selectedFolderRefs.map(getRefKey));
        sourceFolder.appRefs = sourceFolder.appRefs.filter((ref) => !selectedFolderKeys.has(getRefKey(ref)));
      }
      destinationFolder.item.appRefs = dedupeRefs(destinationFolder.item.appRefs.concat(dedupedRefs));
      if (folderTitle) {
        destinationFolder.item.title = folderTitle;
      }
    } else {
      if (dedupedRefs.length === 0) {
        alert("Select apps or app shortcuts to build a folder.");
        return;
      }

      page.items = page.items.filter((item) => !pageItemsToRemove.has(item.id));
      updated.dock = updated.dock.filter((item) => !dockItemsToRemove.has(item.id));
      reindexDock(updated.dock);
      if (sourceFolder && selectedFolderRefs.length > 0) {
        const selectedFolderKeys = new Set(selectedFolderRefs.map(getRefKey));
        sourceFolder.appRefs = sourceFolder.appRefs.filter((ref) => !selectedFolderKeys.has(getRefKey(ref)));
      }
      const target = resolveNewFolderTarget(updated, state.activePage, selectedItems.concat(selectedDockItems));
      if (!target) {
        alert("No free spot is available for a new folder on this page.");
        return;
      }
      page.items.push(createFolderItem(target, dedupedRefs, folderTitle || "Folder"));
    }

    if (sourceFolder && selectedFolderRefs.length > 0) {
      removeEmptyFolderIfNeeded(updated, state.activeFolderArea, state.activePage, sourceFolder.id);
    }

    commitLayout(updated);
    clearSelectionState();
    render();
  });

  els.search.addEventListener("input", (event) => {
    state.search = String(event.target.value || "").toLowerCase();
    renderAppList();
  });

  els.addPage.addEventListener("click", () => {
    const updated = deepClone(state.layout);
    updated.pages.push({ index: updated.pages.length, title: null, items: [] });
    commitLayout(updated);
    state.activePage = state.layout.pages.length - 1;
    state.targetPage = state.activePage;
    clearSelectionState();
    render();
  });

  els.removePage.addEventListener("click", () => {
    if (state.layout.pages.length === 1) {
      const updated = deepClone(state.layout);
      updated.pages[0].items = [];
      commitLayout(updated);
      clearSelectionState();
      render();
      return;
    }

    const updated = deepClone(state.layout);
    updated.pages.splice(state.activePage, 1);
    commitLayout(updated);
    state.targetPage = clampPageIndex(state.targetPage);
    clearSelectionState();
    render();
  });

  els.pageTitle.addEventListener("input", (event) => {
    const updated = deepClone(state.layout);
    updated.pages[state.activePage].title = event.target.value || null;
    commitLayout(updated, { skipHistory: true });
    renderPageTabs();
    renderSummary();
  });

  els.movePageLeft.addEventListener("click", () => {
    if (state.activePage <= 0) return;
    const updated = deepClone(state.layout);
    const [removed] = updated.pages.splice(state.activePage, 1);
    updated.pages.splice(state.activePage - 1, 0, removed);

    if (updated.homePageIndex === state.activePage) {
      updated.homePageIndex -= 1;
    } else if (updated.homePageIndex === state.activePage - 1) {
      updated.homePageIndex += 1;
    }

    updated.pages.forEach((p, i) => (p.index = i));
    commitLayout(updated);
    state.activePage -= 1;
    state.targetPage = state.activePage;
    render();
  });

  els.movePageRight.addEventListener("click", () => {
    if (state.activePage >= state.layout.pages.length - 1) return;
    const updated = deepClone(state.layout);
    const [removed] = updated.pages.splice(state.activePage, 1);
    updated.pages.splice(state.activePage + 1, 0, removed);

    if (updated.homePageIndex === state.activePage) {
      updated.homePageIndex += 1;
    } else if (updated.homePageIndex === state.activePage + 1) {
      updated.homePageIndex -= 1;
    }

    updated.pages.forEach((p, i) => (p.index = i));
    commitLayout(updated);
    state.activePage += 1;
    state.targetPage = state.activePage;
    render();
  });

  document.addEventListener("dragover", (event) => {
    event.preventDefault();
    if (!pressDrag.active) {
      updateDragHoverTarget(event.clientX, event.clientY);
    }
  });

  document.addEventListener("dragleave", (event) => {
    if (!pressDrag.active) {
      clearDragHoverTarget();
    }
  });

  document.addEventListener("drop", (event) => {
    event.preventDefault();
    if (pressDrag.active) return;

    const hoverTarget = pressDrag.hoverTarget;
    clearDragHoverTarget();

    if (!hoverTarget) return;

    let itemsToDrop = [];

    try {
      const nativeJsonData = event.dataTransfer.getData("application/json");
      if (nativeJsonData) {
        const parsed = JSON.parse(nativeJsonData);
        if (Array.isArray(parsed)) {
          itemsToDrop.push(...parsed);
        }
      }
    } catch(e) {}

    if (itemsToDrop.length === 0) {
      if (event.dataTransfer.files && event.dataTransfer.files.length > 0) {
        for (const file of event.dataTransfer.files) {
          itemsToDrop.push({
            packageName: file.name,
            activityName: file.name
          });
        }
      } else {
        const text = event.dataTransfer.getData("text/plain") || event.dataTransfer.getData("text/uri-list");
        if (text) {
          const lines = text.split("\n").map(s => s.trim()).filter(Boolean);
          for (const line of lines) {
            itemsToDrop.push({
              packageName: line,
              activityName: line
            });
          }
        }
      }
    }

    if (itemsToDrop.length > 0) {
      const refs = dedupeRefs(itemsToDrop);
      if (!refs.length) return;

      const missingApps = refs.filter(ref => !state.catalog.some(app => app.packageName === ref.packageName && app.activityName === ref.activityName));
      if (missingApps.length > 0) {
        const missingEntries = missingApps.map(ref => ({
          label: ref.packageName,
          packageName: ref.packageName,
          activityName: ref.activityName
        }));
        state.catalog = normalizeCatalog([...state.catalog, ...missingEntries]);
      }

      const descriptor = {
        kind: "refs",
        source: "external",
        refs: refs,
        title: "External Drop",
        subtitle: ""
      };
      applyDragDrop(descriptor, hoverTarget);
    }
  });

  window.addEventListener("keydown", handleGlobalKeydown);
}

function handleGlobalKeydown(event) {
  if (isTypingTarget(event.target)) return;
  const key = String(event.key || "").toLowerCase();

  if (event.ctrlKey || event.metaKey) {
    if (!event.shiftKey && key === "z") {
      event.preventDefault();
      undoLayout();
      return;
    }
    if ((event.shiftKey && key === "z") || key === "y") {
      event.preventDefault();
      redoLayout();
      return;
    }
    if (key === "a") {
      event.preventDefault();
      selectAllCurrentContext();
      return;
    }
  }

  if (!event.ctrlKey && !event.metaKey && !event.altKey && (key === "delete" || key === "backspace")) {
    if (deleteSelectedItems()) {
      event.preventDefault();
    }
  }
}

function bindMarqueeSelection(container, kind) {
  container.addEventListener("pointerdown", (event) => startMarqueeSelection(kind, event));
  container.addEventListener(
    "click",
    (event) => {
      if (kind === "appList" && marquee.suppressAppListClick) {
        event.preventDefault();
        event.stopPropagation();
        marquee.suppressAppListClick = false;
      }
      if (kind === "folderList" && marquee.suppressFolderListClick) {
        event.preventDefault();
        event.stopPropagation();
        marquee.suppressFolderListClick = false;
      }
      if (kind === "board" && marquee.suppressBoardClick) {
        event.preventDefault();
        event.stopPropagation();
        marquee.suppressBoardClick = false;
      }
    },
    true
  );
}

function shouldSuppressClick() {
  return Date.now() < pressDrag.suppressClickUntil;
}

function beginLongPressDrag(event, descriptor) {
  if (!descriptor) return;
  if (event.button !== 0) return;
  event.stopPropagation();

  clearPendingPressDrag();
  pressDrag.pending = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    descriptor
  };

  window.addEventListener("pointermove", handlePendingPressDragMove, true);
  window.addEventListener("pointerup", cancelPendingPressDrag, true);
  window.addEventListener("pointercancel", cancelPendingPressDrag, true);
}

function handlePendingPressDragMove(event) {
  const pending = pressDrag.pending;
  if (!pending || pending.pointerId !== event.pointerId) return;
  if (Math.hypot(event.clientX - pending.startX, event.clientY - pending.startY) > 6) {
    startLongPressDrag(pending, event);
  }
}

function cancelPendingPressDrag(event) {
  const pending = pressDrag.pending;
  if (!pending || pending.pointerId !== event.pointerId) return;
  clearPendingPressDrag();
}

function clearPendingPressDrag() {
  pressDrag.pending = null;
  window.removeEventListener("pointermove", handlePendingPressDragMove, true);
  window.removeEventListener("pointerup", cancelPendingPressDrag, true);
  window.removeEventListener("pointercancel", cancelPendingPressDrag, true);
}

function startLongPressDrag(pending, event) {
  clearPendingPressDrag();
  pressDrag.active = {
    pointerId: pending.pointerId,
    descriptor: pending.descriptor,
    x: event.clientX,
    y: event.clientY
  };
  pressDrag.suppressClickUntil = Date.now() + 400;
  document.body.classList.add("item-dragging");
  renderDragOverlay();
  updateDragHoverTarget(event.clientX, event.clientY);
  window.addEventListener("pointermove", handleActivePressDragMove, true);
  window.addEventListener("pointerup", handleActivePressDragEnd, true);
  window.addEventListener("pointercancel", handleActivePressDragEnd, true);
}

function handleActivePressDragMove(event) {
  const active = pressDrag.active;
  if (!active || active.pointerId !== event.pointerId) return;
  active.x = event.clientX;
  active.y = event.clientY;
  renderDragOverlay();
  updateDragHoverTarget(event.clientX, event.clientY);
  event.preventDefault();
}

function handleActivePressDragEnd(event) {
  const active = pressDrag.active;
  if (!active || active.pointerId !== event.pointerId) return;
  const hoverTarget = pressDrag.hoverTarget;
  cleanupActivePressDrag();
  if (hoverTarget) {
    applyDragDrop(active.descriptor, hoverTarget);
  }
}

function cleanupActivePressDrag() {
  clearDragHoverTarget();
  if (pressDrag.overlay) {
    pressDrag.overlay.remove();
    pressDrag.overlay = null;
  }
  pressDrag.active = null;
  document.body.classList.remove("item-dragging");
  window.removeEventListener("pointermove", handleActivePressDragMove, true);
  window.removeEventListener("pointerup", handleActivePressDragEnd, true);
  window.removeEventListener("pointercancel", handleActivePressDragEnd, true);
}

function renderDragOverlay() {
  const active = pressDrag.active;
  if (!active) return;
  if (!pressDrag.overlay) {
    pressDrag.overlay = document.createElement("div");
    pressDrag.overlay.className = "drag-ghost";
    document.body.appendChild(pressDrag.overlay);
  }
  pressDrag.overlay.innerHTML = `<strong>${escapeHtml(active.descriptor.title)}</strong><small>${escapeHtml(active.descriptor.subtitle)}</small>`;
  pressDrag.overlay.style.left = `${active.x + 18}px`;
  pressDrag.overlay.style.top = `${active.y + 18}px`;
}

function updateDragHoverTarget(clientX, clientY) {
  clearDragHoverTarget();
  const element = document.elementFromPoint(clientX, clientY);
  if (!element) return;

  const dockCell = element.closest(".dock-cell");
  if (dockCell && els.dockList.contains(dockCell)) {
    const slot = Number(dockCell.dataset.slot || 0);
    const itemId = dockCell.dataset.itemId || null;
    pressDrag.hoverTarget = { kind: "dock", slot, itemId, element: dockCell };
    dockCell.classList.add("drop-target");
    return;
  }

  const folderRow = element.closest(".folder-app-row");
  if (folderRow && els.folderAppList.contains(folderRow) && getActiveFolderItem()) {
    const rowRect = folderRow.getBoundingClientRect();
    const placement = clientY < rowRect.top + rowRect.height / 2 ? "before" : "after";
    pressDrag.hoverTarget = {
      kind: "folderList",
      key: folderRow.dataset.key,
      placement,
      element: folderRow
    };
    folderRow.classList.add("drop-target");
    return;
  }

  const boardCell = element.closest(".cell");
  if (boardCell && els.board.contains(boardCell)) {
    const cellId = boardCell.dataset.cellId;
    const { x, y } = parseCellId(cellId);
    const occupancy = buildOccupancyMap(getActivePage());
    const occupant = occupancy.get(`${x}:${y}`) || null;
    pressDrag.hoverTarget = {
      kind: "board",
      x,
      y,
      occupantId: occupant?.item?.id || null,
      element: boardCell
    };
    boardCell.classList.add("drop-target");
  }
}

function clearDragHoverTarget() {
  if (!pressDrag.hoverTarget?.element) {
    pressDrag.hoverTarget = null;
    return;
  }
  pressDrag.hoverTarget.element.classList.remove("drop-target");
  pressDrag.hoverTarget = null;
}

function startMarqueeSelection(kind, event) {
  if (event.button !== 0) return;
  const forceMarquee = event.ctrlKey || event.metaKey;
  if ((kind === "appList" || kind === "folderList") && event.target.closest('input[type="checkbox"]')) return;
  if (kind === "appList" && event.target.closest(".app-row") && !forceMarquee) return;
  if (kind === "folderList" && event.target.closest(".folder-app-row") && !forceMarquee) return;
  if (kind === "board") {
    const cell = event.target.closest(".cell");
    if (cell && !cell.classList.contains("empty") && !forceMarquee) return;
  }

  marquee[kind] = {
    kind,
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    currentX: event.clientX,
    currentY: event.clientY,
    active: false,
    additive: event.ctrlKey || event.metaKey,
    baseSelection: new Set(
      kind === "appList" ? state.catalogSelection :
      kind === "folderList" ? state.folderAppSelection :
      state.boardSelection
    ),
    box: null
  };

  window.addEventListener("pointermove", handleMarqueeMove);
  window.addEventListener("pointerup", handleMarqueeEnd, true);
  window.addEventListener("pointercancel", handleMarqueeEnd, true);
}

function handleMarqueeMove(event) {
  ["appList", "folderList", "board"].forEach((kind) => {
    const drag = marquee[kind];
    if (!drag || drag.pointerId !== event.pointerId) return;

    drag.currentX = event.clientX;
    drag.currentY = event.clientY;

    if (!drag.active && Math.hypot(drag.currentX - drag.startX, drag.currentY - drag.startY) < 8) {
      return;
    }

    if (!drag.active) {
      drag.active = true;
      drag.box = document.createElement("div");
      drag.box.className = "selection-box";
      document.body.appendChild(drag.box);
      document.body.classList.add("app-list-dragging");
    }

    updateSelectionBox(drag);
    applyMarqueeSelection(drag);
    event.preventDefault();
  });
}

function handleMarqueeEnd(event) {
  ["appList", "folderList", "board"].forEach((kind) => {
    const drag = marquee[kind];
    if (!drag || drag.pointerId !== event.pointerId) return;

    const wasActive = drag.active;
    if (drag.box) {
      drag.box.remove();
    }
    marquee[kind] = null;
    document.body.classList.remove("app-list-dragging");

    if (wasActive) {
      if (kind === "appList") {
        marquee.suppressAppListClick = true;
        window.setTimeout(() => {
          marquee.suppressAppListClick = false;
        }, 0);
      } else if (kind === "folderList") {
        marquee.suppressFolderListClick = true;
        window.setTimeout(() => {
          marquee.suppressFolderListClick = false;
        }, 0);
      } else {
        marquee.suppressBoardClick = true;
        window.setTimeout(() => {
          marquee.suppressBoardClick = false;
        }, 0);
      }
      event.preventDefault();
    }
  });

  if (!marquee.appList && !marquee.folderList && !marquee.board) {
    window.removeEventListener("pointermove", handleMarqueeMove);
    window.removeEventListener("pointerup", handleMarqueeEnd, true);
    window.removeEventListener("pointercancel", handleMarqueeEnd, true);
  }
}

function updateSelectionBox(drag) {
  if (!drag.box) return;
  const rect = buildRect(drag.startX, drag.startY, drag.currentX, drag.currentY);
  drag.box.style.left = `${rect.left}px`;
  drag.box.style.top = `${rect.top}px`;
  drag.box.style.width = `${rect.width}px`;
  drag.box.style.height = `${rect.height}px`;
}

function applyMarqueeSelection(drag) {
  const rect = buildRect(drag.startX, drag.startY, drag.currentX, drag.currentY);
  if (drag.kind === "appList") {
    const hits = new Set();
    els.appList.querySelectorAll(".app-row").forEach((row) => {
      if (rectanglesIntersect(rect, row.getBoundingClientRect())) {
        hits.add(row.dataset.key);
      }
    });
    state.catalogSelection = drag.additive ? new Set([...drag.baseSelection, ...hits]) : hits;
  } else if (drag.kind === "folderList") {
    const hits = new Set();
    els.folderAppList.querySelectorAll(".folder-app-row").forEach((row) => {
      if (rectanglesIntersect(rect, row.getBoundingClientRect())) {
        hits.add(row.dataset.key);
      }
    });
    state.folderAppSelection = drag.additive ? new Set([...drag.baseSelection, ...hits]) : hits;
  } else {
    const hits = new Set();
    els.board.querySelectorAll(".cell").forEach((cell) => {
      if (rectanglesIntersect(rect, cell.getBoundingClientRect())) {
        hits.add(cell.dataset.cellId);
      }
    });
    state.boardSelection = drag.additive ? new Set([...drag.baseSelection, ...hits]) : hits;
  }

  syncVisibleSelectionState();
}

function buildRect(startX, startY, endX, endY) {
  const left = Math.min(startX, endX);
  const top = Math.min(startY, endY);
  const right = Math.max(startX, endX);
  const bottom = Math.max(startY, endY);
  return {
    left,
    top,
    right,
    bottom,
    width: right - left,
    height: bottom - top
  };
}

function rectanglesIntersect(a, b) {
  return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top;
}

function createEmptyLayout(cols = 5, rows = 6) {
  const grid = normalizeGrid({ cols, rows });
  return {
    version: 2,
    exportedAt: null,
    grid,
    dock: [],
    pages: [{ index: 0, title: null, items: [] }],
    launchHistory: [],
    homePageIndex: 0,
    wallpaperPath: null
  };
}

function normalizeCatalog(apps) {
  return apps
    .filter((entry) => entry?.packageName && entry?.activityName)
    .map((entry) => ({
      label: entry.label || entry.packageName,
      packageName: entry.packageName,
      activityName: entry.activityName
    }))
    .sort((a, b) => a.label.localeCompare(b.label));
}

function normalizeLayout(layout) {
  const grid = normalizeGrid(layout?.grid);
  const pages = Array.isArray(layout?.pages) && layout.pages.length
    ? [...layout.pages]
        .sort((a, b) => clampToInt(a?.index ?? 0, 0) - clampToInt(b?.index ?? 0, 0))
        .map((page, index) => normalizePage(page, index, grid))
    : [{ index: 0, title: null, items: [] }];

  return {
    version: Math.max(2, Number(layout?.version || 2)),
    exportedAt: layout?.exportedAt ?? null,
    grid,
    dock: normalizeDock(layout?.dock),
    pages,
    launchHistory: normalizeRefs(layout?.launchHistory, RECENT_APPS_LIMIT),
    homePageIndex: clampNumber(layout?.homePageIndex, 0, Math.max(pages.length - 1, 0), 0),
    wallpaperPath: normalizeNullableText(layout?.wallpaperPath)
  };
}

function normalizeItems(items) {
  return Array.isArray(items)
    ? items
        .map((item) => normalizeItem(item, { maxCols: state.layout.grid.cols, maxRows: state.layout.grid.rows }))
        .filter(Boolean)
    : [];
}

function updateGrid() {
  const updated = deepClone(state.layout);
  updated.grid = normalizeGrid({
    cols: Number(els.gridCols.value || 5),
    rows: Number(els.gridRows.value || 6)
  });
  commitLayout(updated);
  clearBoardSelection();
  render();
}

function deleteSelectedItems(options = {}) {
  const interactive = Boolean(options.interactive);
  const selectedFolderRefs = getSelectedFolderRefs();
  const selectedItems = getSelectedBoardItems();
  const selectedDockItems = getSelectedDockItems();

  if (selectedFolderRefs.length > 0) {
    const updated = deepClone(state.layout);
    const sourceFolder = getActiveFolderItem(updated);
    if (!sourceFolder) {
      clearSelectionState();
      render();
      return false;
    }
    const selectedKeys = new Set(selectedFolderRefs.map(getRefKey));
    sourceFolder.appRefs = sourceFolder.appRefs.filter((ref) => !selectedKeys.has(getRefKey(ref)));
    removeEmptyFolderIfNeeded(updated, state.activeFolderArea, state.activePage, sourceFolder.id);
    commitLayout(updated);
    restoreFolderAfterDelete(sourceFolder.id, state.activeFolderArea);
    render();
    return true;
  }

  if (selectedItems.length === 0 && selectedDockItems.length === 0) {
    if (interactive) {
      alert("Select page items, dock items, or folder apps first.");
    }
    return false;
  }

  const updated = deepClone(state.layout);
  const page = updated.pages[state.activePage];
  const selectedIds = new Set(selectedItems.map((item) => item.id));
  const selectedDockIds = new Set(selectedDockItems.map((item) => item.id));
  page.items = page.items.filter((item) => !selectedIds.has(item.id));
  updated.dock = updated.dock.filter((item) => !selectedDockIds.has(item.id));
  reindexDock(updated.dock);
  commitLayout(updated);
  clearSelectionState();
  render();
  return true;
}

function restoreFolderAfterDelete(folderId, folderArea) {
  clearSelectionState();
  if (!folderId || !folderArea) return;

  const folder = findFolderById(state.layout, folderId, state.activePage);
  if (!folder) return;

  state.activeFolderArea = folder.area;
  state.activeFolderId = folder.item.id;
  if (folder.area === "dock") {
    state.dockSelection = new Set([folder.item.id]);
    return;
  }
  state.boardSelection = new Set([getCellId(state.activePage, folder.item.x, folder.item.y)]);
}

function captureHistorySnapshot() {
  return {
    layout: deepClone(state.layout),
    activePage: state.activePage,
    targetPage: state.targetPage,
    targetDockSlot: state.targetDockSlot,
    targetFolderId: state.targetFolderId,
    activeFolderArea: state.activeFolderArea,
    activeFolderId: state.activeFolderId
  };
}

function pushHistorySnapshot() {
  state.history.push(captureHistorySnapshot());
  state.redoHistory = [];
  if (state.history.length > HISTORY_LIMIT) {
    state.history.shift();
  }
}

function clearHistory() {
  state.history = [];
  state.redoHistory = [];
}

function restoreHistorySnapshot(snapshot) {
  if (!snapshot?.layout) return false;
  state.layout = normalizeLayout(snapshot.layout);
  state.layout.pages.forEach((page, index) => {
    page.index = index;
  });
  reindexDock(state.layout.dock);
  state.activePage = clampPageIndex(snapshot.activePage ?? 0);
  state.targetPage = clampPageIndex(snapshot.targetPage ?? state.activePage);
  state.targetDockSlot = clampDockSlot(snapshot.targetDockSlot ?? 0);
  state.targetFolderId = getValidTargetFolderId(snapshot.targetFolderId || "", state.layout, state.activePage);
  state.activeFolderArea = snapshot.activeFolderArea || null;
  state.activeFolderId = snapshot.activeFolderId || null;
  state.lastLoadedFolderId = null;
  state.catalogSelection = new Set();
  state.boardSelection = new Set();
  state.dockSelection = new Set();
  state.folderAppSelection = new Set();

  if (!state.activeFolderId) return true;
  const folder = getActiveFolderItem();
  if (!folder) {
    state.activeFolderArea = null;
    state.activeFolderId = null;
    return true;
  }

  if (state.activeFolderArea === "dock") {
    state.dockSelection = new Set([folder.id]);
  } else {
    state.boardSelection = new Set([getCellId(state.activePage, folder.x, folder.y)]);
  }
  return true;
}

function undoLayout() {
  if (!state.history.length) return false;
  const snapshot = state.history.pop();
  state.redoHistory.push(captureHistorySnapshot());
  restoreHistorySnapshot(snapshot);
  render();
  return true;
}

function redoLayout() {
  if (!state.redoHistory.length) return false;
  const snapshot = state.redoHistory.pop();
  state.history.push(captureHistorySnapshot());
  restoreHistorySnapshot(snapshot);
  render();
  return true;
}

function selectAllCurrentContext() {
  const folder = getActiveFolderItem();
  if (folder) {
    state.folderAppSelection = new Set(folder.appRefs.map(getRefKey));
    syncVisibleSelectionState();
    renderSummary();
    return;
  }

  const page = getActivePage();
  const nextBoard = new Set();
  page.items.forEach((item) => {
    nextBoard.add(getCellId(state.activePage, item.x, item.y));
  });
  state.boardSelection = nextBoard;

  const nextDock = new Set();
  state.layout.dock.forEach((item) => {
    nextDock.add(item.id);
  });
  state.dockSelection = nextDock;

  syncVisibleSelectionState();
  renderSummary();
}

function commitLayout(updatedLayout, options = {}) {
  if (!options.skipHistory) {
    pushHistorySnapshot();
  }
  state.layout = normalizeLayout(updatedLayout);
  state.layout.pages.forEach((page, index) => {
    page.index = index;
  });
  reindexDock(state.layout.dock);
  state.activePage = clampPageIndex(state.activePage);
  state.targetPage = clampPageIndex(state.targetPage);
  state.targetDockSlot = clampDockSlot(state.targetDockSlot);
  state.targetFolderId = getValidTargetFolderId(state.targetFolderId);
}

function clearSelectionState() {
  state.catalogSelection = new Set();
  state.folderAppSelection = new Set();
  clearBoardSelection();
}

function clearBoardSelection() {
  state.boardSelection = new Set();
  state.dockSelection = new Set();
  state.activeFolderArea = null;
  state.activeFolderId = null;
  state.folderAppSelection = new Set();
}

function clampPageIndex(value) {
  return Math.max(0, Math.min(Number(value || 0), state.layout.pages.length - 1));
}

function clampDockSlot(value) {
  return Math.max(0, Math.min(Number(value || 0), DOCK_SLOT_COUNT - 1));
}

function getActivePage() {
  return state.layout.pages[state.activePage];
}

function findFolderById(layout, folderId, pageIndex = state.activePage) {
  const normalizedId = String(folderId || "");
  if (!normalizedId) return null;
  const pageFolder = layout.pages[pageIndex]?.items?.find((item) => item.type === "folder" && item.id === normalizedId);
  if (pageFolder) return { area: "page", item: pageFolder };
  const dockFolder = layout.dock.find((item) => item.type === "folder" && item.id === normalizedId);
  if (dockFolder) return { area: "dock", item: dockFolder };
  return null;
}

function getAvailableTargetFolders(layout = state.layout, pageIndex = state.activePage) {
  const page = layout.pages[pageIndex];
  const sourceFolderId = layout === state.layout ? state.activeFolderId : getActiveFolderItem(layout)?.id;
  const candidates = [];

  if (page) {
    page.items.forEach((item) => {
      if (item.type === "folder" && item.id !== sourceFolderId) {
        candidates.push({ area: "page", item });
      }
    });
  }

  layout.dock.forEach((item) => {
    if (item.type === "folder" && item.id !== sourceFolderId) {
      candidates.push({ area: "dock", item });
    }
  });

  return candidates;
}

function getValidTargetFolderId(candidateId, layout = state.layout, pageIndex = state.activePage) {
  const normalizedId = String(candidateId || "");
  if (!normalizedId) return "";
  return getAvailableTargetFolders(layout, pageIndex).some((entry) => entry.item.id === normalizedId) ? normalizedId : "";
}

function normalizeGrid(grid) {
  return {
    cols: clampNumber(grid?.cols, 3, 8, 5),
    rows: clampNumber(grid?.rows, 4, 8, 6)
  };
}

function normalizeDock(items) {
  if (!Array.isArray(items)) return [];
  const usedSlots = new Set();
  const normalized = [];

  [...items]
    .sort((a, b) => clampToInt(a?.x ?? 0, 0) - clampToInt(b?.x ?? 0, 0))
    .forEach((item) => {
      const slot = clampDockSlot(item?.x ?? 0);
      if (usedSlots.has(slot)) return;
      const normalizedItem = normalizeItem(item, { maxCols: DOCK_SLOT_COUNT, maxRows: 1, forcedX: slot, forcedY: 0 });
      if (!normalizedItem) return;
      normalizedItem.x = slot;
      normalizedItem.y = 0;
      normalizedItem.spanX = 1;
      normalizedItem.spanY = 1;
      usedSlots.add(slot);
      normalized.push(normalizedItem);
    });

  return normalized.sort((a, b) => a.x - b.x).slice(0, DOCK_SLOT_COUNT);
}

function normalizePage(page, index, grid) {
  const occupied = new Set();
  const items = [];

  (Array.isArray(page?.items) ? page.items : []).forEach((item) => {
    const normalized = normalizeItem(item, { maxCols: grid.cols, maxRows: grid.rows });
    if (!normalized) return;

    const cellKeys = getOccupiedCellKeys(normalized);
    if (cellKeys.some((key) => occupied.has(key))) return;
    cellKeys.forEach((key) => occupied.add(key));
    items.push(normalized);
  });

  return {
    index,
    title: normalizeNullableText(page?.title),
    items
  };
}

function normalizeItem(item, options) {
  const maxCols = options.maxCols;
  const maxRows = options.maxRows;
  const type = item?.type === "folder" || item?.type === "widget" ? item.type : "app";
  const spanX = clampNumber(item?.spanX ?? 1, 1, maxCols, 1);
  const spanY = clampNumber(item?.spanY ?? 1, 1, maxRows, 1);

  if (!Number.isFinite(Number(options.forcedX ?? item?.x)) || !Number.isFinite(Number(options.forcedY ?? item?.y))) {
    return null;
  }

  const normalized = {
    id: normalizeId(item?.id),
    type,
    x: clampNumber(options.forcedX ?? item?.x, 0, maxCols - spanX, 0),
    y: clampNumber(options.forcedY ?? item?.y, 0, maxRows - spanY, 0),
    spanX,
    spanY,
    packageName: normalizeNullableText(item?.packageName),
    activityName: normalizeNullableText(item?.activityName),
    title: normalizeNullableText(item?.title),
    appRefs: normalizeRefs(item?.appRefs),
    appWidgetId: Number.isFinite(Number(item?.appWidgetId)) ? Math.trunc(Number(item.appWidgetId)) : null,
    widgetProvider: normalizeNullableText(item?.widgetProvider)
  };

  if (type === "folder") {
    return normalized.appRefs.length
      ? { ...normalized, packageName: null, activityName: null, appWidgetId: null, widgetProvider: null }
      : null;
  }

  if (type === "widget") {
    return normalized.appWidgetId !== null
      ? { ...normalized, packageName: null, activityName: null, appRefs: [] }
      : null;
  }

  return normalized.packageName && normalized.activityName
    ? { ...normalized, appRefs: [], appWidgetId: null, widgetProvider: null }
    : null;
}

function normalizeId(value) {
  const id = String(value || "").trim();
  return id || createId();
}

function normalizeNullableText(value) {
  if (value === null || value === undefined) return null;
  const trimmed = String(value).trim();
  return trimmed || null;
}

function normalizeRefs(refs, limit = Number.POSITIVE_INFINITY) {
  return dedupeRefs(Array.isArray(refs) ? refs : []).slice(0, limit);
}

function getOccupiedCellKeys(item) {
  const cells = [];
  for (let y = item.y; y < item.y + item.spanY; y += 1) {
    for (let x = item.x; x < item.x + item.spanX; x += 1) {
      cells.push(`${x}:${y}`);
    }
  }
  return cells;
}

function getActiveFolderItem(layout = state.layout) {
  if (!state.activeFolderId) return null;
  if (state.activeFolderArea === "dock") {
    return layout.dock.find((item) => item.id === state.activeFolderId && item.type === "folder") || null;
  }
  const page = layout.pages[state.activePage];
  if (!page) return null;
  return page.items.find((item) => item.id === state.activeFolderId && item.type === "folder") || null;
}

function getAppKey(app) {
  return `${app.packageName}/${app.activityName}`;
}

function getRefKey(ref) {
  return `${ref.packageName}/${ref.activityName}`;
}

function getCellId(pageIndex, x, y) {
  return `${pageIndex}:${x}:${y}`;
}

function parseCellId(cellId) {
  const [pageIndex, x, y] = String(cellId).split(":").map(Number);
  return { pageIndex, x, y };
}

function getSelectedBoardCells(pageIndex = state.activePage) {
  return [...state.boardSelection]
    .map(parseCellId)
    .filter((cell) => cell.pageIndex === pageIndex)
    .sort((a, b) => (a.y - b.y) || (a.x - b.x));
}

function getSelectedBoardItems() {
  const page = getActivePage();
  const occupancy = buildOccupancyMap(page);
  const selectedItems = [];
  const seenIds = new Set();

  getSelectedBoardCells().forEach((cell) => {
    const occupant = occupancy.get(`${cell.x}:${cell.y}`);
    if (!occupant || seenIds.has(occupant.item.id)) return;
    seenIds.add(occupant.item.id);
    selectedItems.push(occupant.item);
  });

  return selectedItems;
}

function getSelectedDockItems(layout = state.layout) {
  return layout.dock.filter((item) => state.dockSelection.has(item.id));
}

function getBoardDragItem(cellId, layout = state.layout) {
  const { x, y } = parseCellId(cellId);
  const occupancy = buildOccupancyMap(layout.pages[state.activePage]);
  const occupant = occupancy.get(`${x}:${y}`);
  if (!occupant?.item) return null;
  return occupant.item;
}

function getDockDragItem(itemId, layout = state.layout) {
  return layout.dock.find((item) => item.id === itemId) || null;
}

function getSelectedCatalogEntries() {
  return state.catalog.filter((app) => state.catalogSelection.has(getAppKey(app)));
}

function getCatalogDragRefs(key) {
  const selected = getSelectedCatalogEntries();
  if (state.catalogSelection.has(key) && selected.length > 1) {
    return selected.map((app) => ({ packageName: app.packageName, activityName: app.activityName }));
  }
  const app = state.catalog.find((candidate) => getAppKey(candidate) === key);
  return app ? [{ packageName: app.packageName, activityName: app.activityName }] : [];
}

function getSelectedFolderRefs(layout = state.layout) {
  const folder = getActiveFolderItem(layout);
  if (!folder || state.folderAppSelection.size === 0) return [];
  return folder.appRefs.filter((ref) => state.folderAppSelection.has(getRefKey(ref)));
}

function getFolderDragRefs(key, layout = state.layout) {
  const folder = getActiveFolderItem(layout);
  if (!folder) return [];
  if (state.folderAppSelection.has(key)) {
    const selected = getSelectedFolderRefs(layout);
    if (selected.length > 1) return selected;
  }
  return folder.appRefs.filter((ref) => getRefKey(ref) === key);
}

function getSelectedRightSideEntries() {
  return getSelectedBoardItems()
    .map((item) => ({ area: "page", itemId: item.id, item }))
    .concat(getSelectedDockItems().map((item) => ({ area: "dock", itemId: item.id, item })));
}

function createRightSideSelectionDescriptor(triggerArea, triggerItem) {
  if (!triggerItem || itemToRefs(triggerItem).length === 0) return null;
  const selectedEntries = getSelectedRightSideEntries();
  const triggerIsSelected = selectedEntries.some((entry) => entry.area === triggerArea && entry.itemId === triggerItem.id);
  if (!triggerIsSelected || selectedEntries.length < 2) return null;

  const usableEntries = selectedEntries
    .map((entry) => ({ ...entry, refs: itemToRefs(entry.item) }))
    .filter((entry) => entry.refs.length > 0);

  if (usableEntries.length < 2) return null;

  return {
    kind: "refs",
    source: "rightMulti",
    refs: dedupeRefs(usableEntries.flatMap((entry) => entry.refs)),
    itemSources: usableEntries.map((entry) => ({ area: entry.area, itemId: entry.itemId })),
    title: `${usableEntries.length} selected items`,
    subtitle: "Drop selected items into folder, page, or dock"
  };
}

function getLinkedAppKeysFromBoardSelection() {
  const keys = new Set();
  getSelectedBoardItems().forEach((item) => {
    itemToRefs(item).forEach((ref) => keys.add(`${ref.packageName}/${ref.activityName}`));
  });
  getSelectedDockItems().forEach((item) => {
    itemToRefs(item).forEach((ref) => keys.add(`${ref.packageName}/${ref.activityName}`));
  });
  state.folderAppSelection.forEach((key) => keys.add(key));
  return keys;
}

function getLinkedBoardCellIdsFromCatalogSelection(pageIndex = state.activePage) {
  const selectedKeys = new Set([...state.catalogSelection, ...state.folderAppSelection]);
  const page = state.layout.pages[pageIndex];
  const linked = new Set();
  if (!page || selectedKeys.size === 0) return linked;

  page.items.forEach((item) => {
    if (itemToRefs(item).some((ref) => selectedKeys.has(`${ref.packageName}/${ref.activityName}`))) {
      getItemCellIds(pageIndex, item).forEach((cellId) => linked.add(cellId));
    }
  });

  return linked;
}

function getItemCellIds(pageIndex, item) {
  const ids = [];
  for (let y = item.y; y < item.y + item.spanY; y += 1) {
    for (let x = item.x; x < item.x + item.spanX; x += 1) {
      ids.push(getCellId(pageIndex, x, y));
    }
  }
  return ids;
}

function getLinkedDockItemIdsFromCatalogSelection() {
  const selectedKeys = new Set([...state.catalogSelection, ...state.folderAppSelection]);
  const linked = new Set();
  if (selectedKeys.size === 0) return linked;
  state.layout.dock.forEach((item) => {
    if (itemToRefs(item).some((ref) => selectedKeys.has(getRefKey(ref)))) {
      linked.add(item.id);
    }
  });
  return linked;
}

function removeEmptyFolderIfNeeded(layout, area, pageIndex, folderId) {
  if (area === "dock") {
    const folder = layout.dock.find((item) => item.id === folderId && item.type === "folder");
    if (!folder) return;
    if (folder.appRefs.length === 0) {
      layout.dock = layout.dock.filter((item) => item.id !== folderId);
      reindexDock(layout.dock);
    }
    return;
  }

  const page = layout.pages[pageIndex];
  const folder = page?.items.find((item) => item.id === folderId && item.type === "folder");
  if (!folder) return;
  if (folder.appRefs.length === 0) {
    page.items = page.items.filter((item) => item.id !== folderId);
  }
}

function syncActiveFolder() {
  const folder = getActiveFolderItem();
  if (!folder) {
    state.activeFolderArea = null;
    state.activeFolderId = null;
    state.folderAppSelection = new Set();
    return null;
  }

  const selectedFolderIds = new Set(
    (state.activeFolderArea === "dock" ? getSelectedDockItems() : getSelectedBoardItems())
      .filter((item) => item.type === "folder")
      .map((item) => item.id)
  );
  if (!selectedFolderIds.has(folder.id)) {
    state.activeFolderArea = null;
    state.activeFolderId = null;
    state.folderAppSelection = new Set();
    return null;
  }

  const validKeys = new Set(folder.appRefs.map(getRefKey));
  state.folderAppSelection = new Set([...state.folderAppSelection].filter((key) => validKeys.has(key)));
  return folder;
}

function itemToRefs(item) {
  if (item.type === "folder") {
    return dedupeRefs(item.appRefs);
  }
  if (item.type === "app" && item.packageName && item.activityName) {
    return [{ packageName: item.packageName, activityName: item.activityName }];
  }
  return [];
}

function createAppItem(app, target) {
  return {
    id: createId(),
    type: "app",
    x: target.x,
    y: target.y,
    spanX: 1,
    spanY: 1,
    packageName: app.packageName,
    activityName: app.activityName,
    title: null,
    appRefs: [],
    appWidgetId: null,
    widgetProvider: null
  };
}

function createFolderItem(target, refs, title) {
  return {
    id: createId(),
    type: "folder",
    x: target.x,
    y: target.y,
    spanX: 1,
    spanY: 1,
    packageName: null,
    activityName: null,
    title,
    appRefs: dedupeRefs(refs),
    appWidgetId: null,
    widgetProvider: null
  };
}

function reindexDock(dock) {
  const normalized = normalizeDock(dock);
  dock.splice(0, dock.length, ...normalized);
}

function getDockItemBySlot(layout, slot) {
  const normalizedSlot = clampDockSlot(slot);
  return layout.dock.find((item) => item.x === normalizedSlot) || null;
}

function isDockSlotOccupied(layout, slot, ignoreItemId = null) {
  const normalizedSlot = clampDockSlot(slot);
  return layout.dock.some((item) => item.x === normalizedSlot && item.id !== ignoreItemId);
}

function getDockAvailableSlots(layout, preferredStart = 0) {
  const slots = [];
  const start = clampDockSlot(preferredStart);
  for (let offset = 0; offset < DOCK_SLOT_COUNT; offset += 1) {
    const slot = (start + offset) % DOCK_SLOT_COUNT;
    if (!isDockSlotOccupied(layout, slot)) {
      slots.push(slot);
    }
  }
  return slots;
}

function setDockItemAtSlot(layout, item, slot) {
  const normalizedSlot = clampDockSlot(slot);
  layout.dock = layout.dock.filter((candidate) => candidate.id !== item.id && candidate.x !== normalizedSlot);
  item.x = normalizedSlot;
  item.y = 0;
  item.spanX = 1;
  item.spanY = 1;
  layout.dock.push(item);
  layout.dock.sort((a, b) => a.x - b.x);
}

function isAreaFree(page, x, y, spanX, spanY, ignoreId = null) {
  return page.items.every((item) => {
    if (ignoreId && item.id === ignoreId) return true;
    const intersectsX = x < item.x + item.spanX && x + spanX > item.x;
    const intersectsY = y < item.y + item.spanY && y + spanY > item.y;
    return !(intersectsX && intersectsY);
  });
}

function createDragDescriptorForCatalog(key) {
  const refs = getCatalogDragRefs(key);
  if (!refs.length) return null;
  return {
    kind: "refs",
    source: "catalog",
    refs,
    title: refs.length === 1 ? lookupAppLabel(refs[0].packageName, refs[0].activityName) : `${refs.length} apps`,
    subtitle: "Long-press drag to page, folder, or dock"
  };
}

function createDragDescriptorForFolder(key) {
  const refs = getFolderDragRefs(key);
  const activeFolder = getActiveFolderItem();
  if (!refs.length || !activeFolder) return null;
  return {
    kind: "refs",
    source: "folder",
    refs,
    sourceFolderArea: state.activeFolderArea,
    sourceFolderId: activeFolder.id,
    title: refs.length === 1 ? lookupAppLabel(refs[0].packageName, refs[0].activityName) : `${refs.length} folder apps`,
    subtitle: "Drop into another folder, page, or dock"
  };
}

function createDragDescriptorForBoard(cellId) {
  const item = getBoardDragItem(cellId);
  if (!item) return null;
  const selectionDescriptor = createRightSideSelectionDescriptor("page", item);
  if (selectionDescriptor) return selectionDescriptor;
  return {
    kind: "item",
    source: "board",
    itemId: item.id,
    itemType: item.type,
    title: itemDisplayName(item),
    subtitle: "Drop to move, folder, or dock"
  };
}

function createDragDescriptorForDock(itemId) {
  const item = getDockDragItem(itemId);
  if (!item) return null;
  const selectionDescriptor = createRightSideSelectionDescriptor("dock", item);
  if (selectionDescriptor) return selectionDescriptor;
  return {
    kind: "item",
    source: "dock",
    itemId: item.id,
    itemType: item.type,
    title: itemDisplayName(item),
    subtitle: "Drop to move, folder, or page"
  };
}

function applyDragDrop(descriptor, target) {
  if (!descriptor || !target) return;
  const updated = deepClone(state.layout);
  const result = descriptor.kind === "refs"
    ? applyRefsDrop(updated, descriptor, target)
    : applyItemDrop(updated, descriptor, target);

  if (!result?.ok) {
    if (result?.message) alert(result.message);
    return;
  }

  const finalResult = preserveOpenFolderAfterDrop(updated, descriptor, result);
  commitLayout(updated);
  applyPostDropState(finalResult);
  render();
}

function preserveOpenFolderAfterDrop(layout, descriptor, result) {
  if (!result?.ok || descriptor?.source !== "folder") return result;
  const sourceFolder = findFolderById(layout, descriptor.sourceFolderId, state.activePage);
  if (!sourceFolder) return result;
  return {
    ...result,
    focusFolderArea: sourceFolder.area,
    focusFolderId: sourceFolder.item.id
  };
}

function applyPostDropState(result) {
  state.catalogSelection = new Set();
  state.folderAppSelection = new Set();
  state.boardSelection = new Set();
  state.dockSelection = new Set();

  if (result?.focusFolderId) {
    state.activeFolderArea = result.focusFolderArea || "page";
    state.activeFolderId = result.focusFolderId;

    if (state.activeFolderArea === "dock") {
      state.dockSelection = new Set([result.focusFolderId]);
    } else {
      const page = state.layout.pages[state.activePage];
      const folder = page?.items?.find((item) => item.id === result.focusFolderId && item.type === "folder");
      if (folder) {
        state.boardSelection = new Set([getCellId(state.activePage, folder.x, folder.y)]);
      }
    }
    return;
  }

  state.activeFolderArea = null;
  state.activeFolderId = null;
}

function applyRefsDrop(layout, descriptor, target) {
  const refs = dedupeRefs(descriptor.refs);
  if (!refs.length) return { ok: false };

  if (target.kind === "board") {
    return dropRefsOnBoard(layout, descriptor, refs, target);
  }

  if (target.kind === "dock") {
    return dropRefsOnDock(layout, descriptor, refs, target);
  }

  if (target.kind === "folderList") {
    return dropRefsInFolderList(layout, descriptor, refs, target);
  }

  return { ok: false };
}

function applyItemDrop(layout, descriptor, target) {
  const dragged = detachDraggedItem(layout, descriptor);
  if (!dragged) return { ok: false };

  let result = { ok: false };
  if (target.kind === "board") {
    result = dropItemOnBoard(layout, dragged, descriptor, target);
  } else if (target.kind === "dock") {
    result = dropItemOnDock(layout, dragged, descriptor, target);
  } else if (target.kind === "folderList") {
    result = dropItemInFolderList(layout, dragged, descriptor, target);
  }

  if (!result?.ok) {
    attachDraggedItem(layout, dragged);
  }
  return result;
}

function resolveBoardTargetOccupant(layout, target) {
  const page = layout.pages[state.activePage];
  if (!page) return null;
  if (target.occupantId) {
    return page.items.find((item) => item.id === target.occupantId) || null;
  }
  const occupancy = buildOccupancyMap(page);
  return occupancy.get(`${target.x}:${target.y}`)?.item || null;
}

function resolveDockTargetItem(layout, target) {
  if (target.itemId) {
    return layout.dock.find((item) => item.id === target.itemId) || null;
  }
  return layout.dock.find((item) => item.x === target.slot) || null;
}

function descriptorContainsSourceItem(descriptor, area, itemId) {
  return descriptor.source === "rightMulti" &&
    Array.isArray(descriptor.itemSources) &&
    descriptor.itemSources.some((entry) => entry.area === area && entry.itemId === itemId);
}

function dropRefsOnBoard(layout, descriptor, refs, target) {
  const page = layout.pages[state.activePage];
  const occupant = resolveBoardTargetOccupant(layout, target);

  if (occupant?.type === "folder") {
    if (
      (descriptor.source === "folder" && descriptor.sourceFolderId === occupant.id) ||
      descriptorContainsSourceItem(descriptor, "page", occupant.id) ||
      descriptorContainsSourceItem(descriptor, "dock", occupant.id)
    ) {
      return { ok: false, message: "Drop onto a folder that is not part of the current selection." };
    }
    occupant.appRefs = dedupeRefs(occupant.appRefs.concat(refs));
    removeDraggedRefsFromSourceFolder(layout, descriptor);
    return { ok: true, focusFolderArea: "page", focusFolderId: occupant.id };
  }

  if (occupant?.type === "app") {
    if (descriptorContainsSourceItem(descriptor, "page", occupant.id) || descriptorContainsSourceItem(descriptor, "dock", occupant.id)) {
      return { ok: false, message: "Drop onto an app that is not part of the current selection." };
    }
    const folderRefs = dedupeRefs(itemToRefs(occupant).concat(refs));
    page.items = page.items.filter((item) => item.id !== occupant.id);
    const createdFolder = createFolderItem({ x: occupant.x, y: occupant.y }, folderRefs, "Folder");
    page.items.push(createdFolder);
    removeDraggedRefsFromSourceFolder(layout, descriptor);
    return { ok: true, focusFolderArea: "page", focusFolderId: createdFolder.id };
  }

  if (occupant?.type === "widget") {
    return { ok: false, message: "Widgets cannot receive dropped apps." };
  }

  if (!isAreaFree(page, target.x, target.y, 1, 1)) {
    return { ok: false, message: "That position is already occupied." };
  }

  const item = refs.length === 1
    ? createAppItem(refs[0], { x: target.x, y: target.y })
    : createFolderItem({ x: target.x, y: target.y }, refs, "Folder");
  page.items.push(item);
  removeDraggedRefsFromSourceFolder(layout, descriptor);
  return item.type === "folder"
    ? { ok: true, focusFolderArea: "page", focusFolderId: item.id }
    : { ok: true };
}

function dropRefsOnDock(layout, descriptor, refs, target) {
  const targetItem = resolveDockTargetItem(layout, target);
  if (targetItem?.type === "folder") {
    if (
      (descriptor.source === "folder" && descriptor.sourceFolderId === targetItem.id) ||
      descriptorContainsSourceItem(descriptor, "page", targetItem.id) ||
      descriptorContainsSourceItem(descriptor, "dock", targetItem.id)
    ) {
      return { ok: false, message: "Drop onto a folder that is not part of the current selection." };
    }
    targetItem.appRefs = dedupeRefs(targetItem.appRefs.concat(refs));
    removeDraggedRefsFromSourceFolder(layout, descriptor);
    return { ok: true, focusFolderArea: "dock", focusFolderId: targetItem.id };
  }

  if (targetItem?.type === "app") {
    if (descriptorContainsSourceItem(descriptor, "page", targetItem.id) || descriptorContainsSourceItem(descriptor, "dock", targetItem.id)) {
      return { ok: false, message: "Drop onto an app that is not part of the current selection." };
    }
    const slot = targetItem.x;
    const folder = createFolderItem({ x: slot, y: 0 }, itemToRefs(targetItem).concat(refs), "Folder");
    layout.dock = layout.dock.filter((item) => item.id !== targetItem.id);
    setDockItemAtSlot(layout, folder, slot);
    removeDraggedRefsFromSourceFolder(layout, descriptor);
    return { ok: true, focusFolderArea: "dock", focusFolderId: folder.id };
  }

  if (layout.dock.length >= DOCK_SLOT_COUNT) {
    return { ok: false, message: "Dock is full." };
  }

  if (isDockSlotOccupied(layout, target.slot)) {
    return { ok: false, message: "That dock slot is occupied." };
  }
  const item = refs.length === 1
    ? createAppItem(refs[0], { x: target.slot, y: 0 })
    : createFolderItem({ x: target.slot, y: 0 }, refs, "Folder");
  setDockItemAtSlot(layout, item, target.slot);
  removeDraggedRefsFromSourceFolder(layout, descriptor);
  return item.type === "folder"
    ? { ok: true, focusFolderArea: "dock", focusFolderId: item.id }
    : { ok: true };
}

function reorderFolderRefs(refs, movingRefs, targetKey, placement) {
  const movingKeys = new Set(movingRefs.map(getRefKey));
  const remaining = refs.filter((ref) => !movingKeys.has(getRefKey(ref)));
  let insertAt = targetKey ? remaining.findIndex((ref) => getRefKey(ref) === targetKey) : remaining.length;
  if (insertAt < 0) insertAt = remaining.length;
  if (targetKey && placement === "after") insertAt += 1;
  return remaining.slice(0, insertAt).concat(movingRefs, remaining.slice(insertAt));
}

function dropRefsInFolderList(layout, descriptor, refs, target) {
  const activeFolder = getActiveFolderItem(layout);
  if (!activeFolder) return { ok: false, message: "Open a folder first." };
  const activeFolderArea = state.activeFolderArea === "dock" ? "dock" : "page";
  if (descriptorContainsSourceItem(descriptor, activeFolderArea, activeFolder.id)) {
    return { ok: false, message: "Drop onto a folder that is not part of the current selection." };
  }
  const movingKeys = new Set(refs.map(getRefKey));
  if (target.key && movingKeys.has(target.key) && descriptor.source === "folder" && descriptor.sourceFolderId === activeFolder.id) {
    return { ok: true, focusFolderArea: state.activeFolderArea, focusFolderId: activeFolder.id };
  }
  if (descriptor.source === "folder" && descriptor.sourceFolderId === activeFolder.id) {
    activeFolder.appRefs = reorderFolderRefs(activeFolder.appRefs, refs, target.key, target.placement);
    return { ok: true, focusFolderArea: state.activeFolderArea, focusFolderId: activeFolder.id };
  }
  activeFolder.appRefs = reorderFolderRefs(activeFolder.appRefs, refs, target.key, target.placement);
  removeDraggedRefsFromSourceFolder(layout, descriptor);
  return { ok: true, focusFolderArea: state.activeFolderArea, focusFolderId: activeFolder.id };
}

function dropItemOnBoard(layout, dragged, descriptor, target) {
  const page = layout.pages[state.activePage];
  const occupant = resolveBoardTargetOccupant(layout, target);

  if (occupant?.id === dragged.item.id) {
    if (!isAreaFree(page, target.x, target.y, dragged.item.spanX, dragged.item.spanY, dragged.item.id)) {
      return { ok: false, message: "That position is already occupied." };
    }
    dragged.item.x = target.x;
    dragged.item.y = target.y;
    page.items.push(dragged.item);
    return { ok: true };
  }

  if (occupant?.type === "folder") {
    if (dragged.item.type === "widget") return { ok: false, message: "Widgets cannot be dropped into folders." };
    occupant.appRefs = dedupeRefs(occupant.appRefs.concat(itemToRefs(dragged.item)));
    removeEmptyFolderIfNeeded(layout, dragged.area, state.activePage, dragged.item.id);
    return { ok: true, focusFolderArea: "page", focusFolderId: occupant.id };
  }

  if (occupant?.type === "app") {
    if (dragged.item.type === "widget") return { ok: false, message: "Widgets cannot combine into folders." };
    const folder = createFolderItem({ x: occupant.x, y: occupant.y }, itemToRefs(occupant).concat(itemToRefs(dragged.item)), "Folder");
    page.items = page.items.filter((item) => item.id !== occupant.id);
    page.items.push(folder);
    return { ok: true, focusFolderArea: "page", focusFolderId: folder.id };
  }

  if (occupant?.type === "widget") {
    return { ok: false, message: "That spot is occupied by a widget." };
  }

  if (!isAreaFree(page, target.x, target.y, dragged.item.spanX, dragged.item.spanY, dragged.item.id)) {
    return { ok: false, message: "That position is already occupied." };
  }
  dragged.item.x = target.x;
  dragged.item.y = target.y;
  page.items.push(dragged.item);
  return { ok: true };
}

function dropItemOnDock(layout, dragged, descriptor, target) {
  const targetItem = resolveDockTargetItem(layout, target);
  if (dragged.item.type === "widget") {
    return { ok: false, message: "Widgets cannot be placed in the dock." };
  }

  if (descriptor.source === "dock" && targetItem && targetItem.id !== dragged.item.id) {
    const fromSlot = dragged.originalX;
    const toSlot = targetItem.x;
    targetItem.x = fromSlot;
    targetItem.y = 0;
    targetItem.spanX = 1;
    targetItem.spanY = 1;
    setDockItemAtSlot(layout, dragged.item, toSlot);
    layout.dock.sort((a, b) => a.x - b.x);
    return { ok: true };
  }

  if (targetItem?.type === "folder") {
    targetItem.appRefs = dedupeRefs(targetItem.appRefs.concat(itemToRefs(dragged.item)));
    return { ok: true, focusFolderArea: "dock", focusFolderId: targetItem.id };
  }

  if (targetItem?.type === "app") {
    const slot = targetItem.x;
    const folder = createFolderItem({ x: slot, y: 0 }, itemToRefs(targetItem).concat(itemToRefs(dragged.item)), "Folder");
    layout.dock = layout.dock.filter((item) => item.id !== targetItem.id);
    setDockItemAtSlot(layout, folder, slot);
    return { ok: true, focusFolderArea: "dock", focusFolderId: folder.id };
  }

  if (layout.dock.length >= DOCK_SLOT_COUNT) {
    return { ok: false, message: "Dock is full." };
  }

  if (isDockSlotOccupied(layout, target.slot, dragged.item.id)) {
    return { ok: false, message: "That dock slot is occupied." };
  }
  dragged.item.x = clampDockSlot(target.slot);
  dragged.item.y = 0;
  dragged.item.spanX = 1;
  dragged.item.spanY = 1;
  setDockItemAtSlot(layout, dragged.item, dragged.item.x);
  return { ok: true };
}

function dropItemInFolderList(layout, dragged, descriptor, target) {
  const activeFolder = getActiveFolderItem(layout);
  if (!activeFolder) return { ok: false, message: "Open a folder first." };
  if (dragged.item.type === "widget") {
    return { ok: false, message: "Widgets cannot be dropped into folders." };
  }
  if (activeFolder.id === dragged.item.id) {
    return { ok: false, message: "Drop that folder onto a different target." };
  }
  activeFolder.appRefs = reorderFolderRefs(activeFolder.appRefs, itemToRefs(dragged.item), target.key, target.placement);
  return { ok: true, focusFolderArea: state.activeFolderArea, focusFolderId: activeFolder.id };
}

function detachDraggedItem(layout, descriptor) {
  if (descriptor.source === "board") {
    const page = layout.pages[state.activePage];
    const item = page.items.find((candidate) => candidate.id === descriptor.itemId);
    if (!item) return null;
    page.items = page.items.filter((candidate) => candidate.id !== descriptor.itemId);
    return { area: "page", item, originalX: item.x, originalY: item.y };
  }

  if (descriptor.source === "dock") {
    const item = layout.dock.find((candidate) => candidate.id === descriptor.itemId);
    if (!item) return null;
    layout.dock = layout.dock.filter((candidate) => candidate.id !== descriptor.itemId);
    return { area: "dock", item, originalX: item.x };
  }

  return null;
}

function attachDraggedItem(layout, dragged) {
  if (dragged.area === "dock") {
    if (layout.dock.length >= DOCK_SLOT_COUNT && isDockSlotOccupied(layout, dragged.originalX, dragged.item.id)) return false;
    const fallbackSlot = getDockAvailableSlots(layout)[0] ?? null;
    const targetSlot = !isDockSlotOccupied(layout, dragged.originalX, dragged.item.id)
      ? clampDockSlot(dragged.originalX)
      : fallbackSlot;
    if (targetSlot === null) return false;
    setDockItemAtSlot(layout, dragged.item, targetSlot);
    return true;
  }
  dragged.item.x = dragged.originalX ?? dragged.item.x;
  dragged.item.y = dragged.originalY ?? dragged.item.y;
  layout.pages[state.activePage].items.push(dragged.item);
  return true;
}

function removeDraggedRefsFromSourceFolder(layout, descriptor) {
  if (descriptor.source === "folder") {
    const sourceFolder = findFolderById(layout, descriptor.sourceFolderId, state.activePage);
    if (!sourceFolder) return;
    const draggedKeys = new Set(descriptor.refs.map(getRefKey));
    sourceFolder.item.appRefs = sourceFolder.item.appRefs.filter((ref) => !draggedKeys.has(getRefKey(ref)));
    removeEmptyFolderIfNeeded(layout, sourceFolder.area, state.activePage, sourceFolder.item.id);
    return;
  }

  if (descriptor.source !== "rightMulti" || !Array.isArray(descriptor.itemSources)) return;

  const pageIds = new Set();
  const dockIds = new Set();
  descriptor.itemSources.forEach((entry) => {
    if (entry.area === "dock") {
      dockIds.add(entry.itemId);
    } else {
      pageIds.add(entry.itemId);
    }
  });

  if (pageIds.size > 0) {
    const page = layout.pages[state.activePage];
    if (page) {
      page.items = page.items.filter((item) => !pageIds.has(item.id));
    }
  }

  if (dockIds.size > 0) {
    layout.dock = layout.dock.filter((item) => !dockIds.has(item.id));
    reindexDock(layout.dock);
  }
}

function resolveNewFolderTarget(layout, pageIndex, selectedItems) {
  const page = layout.pages[pageIndex];
  const selectedCells = getSelectedBoardCells(pageIndex);

  for (const cell of selectedCells) {
    if (isAreaFree(page, cell.x, cell.y, 1, 1)) {
      return { x: cell.x, y: cell.y };
    }
  }

  for (const item of selectedItems) {
    if (isAreaFree(page, item.x, item.y, 1, 1)) {
      return { x: item.x, y: item.y };
    }
  }

  return findFirstAvailableArea(layout, pageIndex, 1, 1);
}

function findFirstAvailableArea(layout, pageIndex, spanX, spanY) {
  const page = layout.pages[pageIndex];
  for (let y = 0; y <= layout.grid.rows - spanY; y += 1) {
    for (let x = 0; x <= layout.grid.cols - spanX; x += 1) {
      if (isAreaFree(page, x, y, spanX, spanY)) {
        return { x, y };
      }
    }
  }
  return null;
}

function buildOccupancyMap(page) {
  const map = new Map();
  page.items.forEach((item) => {
    for (let y = item.y; y < item.y + item.spanY; y += 1) {
      for (let x = item.x; x < item.x + item.spanX; x += 1) {
        map.set(`${x}:${y}`, {
          item,
          anchor: x === item.x && y === item.y
        });
      }
    }
  });
  return map;
}

function render() {
  els.pageTitle.value = getActivePage().title || "";
  syncActiveFolder();
  syncGridInputs();
  renderTargetPageOptions();
  renderHomePageOptions();
  renderTargetDockSlotOptions();
  renderTargetFolderOptions();
  renderSummary();
  renderFolderAppList();
  renderPageTabs();
  renderBoard();
  renderDock();
  renderAppList();
}

function syncGridInputs() {
  els.gridCols.value = state.layout.grid.cols;
  els.gridRows.value = state.layout.grid.rows;
}

function renderTargetPageOptions() {
  els.targetPage.innerHTML = "";
  state.targetPage = clampPageIndex(state.targetPage);
  state.layout.pages.forEach((page, index) => {
    const option = document.createElement("option");
    option.value = String(index);
    option.textContent = formatPageLabel(page, index);
    option.selected = index === state.targetPage;
    els.targetPage.appendChild(option);
  });
}

function renderHomePageOptions() {
  els.homePage.innerHTML = "";
  state.layout.homePageIndex = clampPageIndex(state.layout.homePageIndex);
  state.layout.pages.forEach((page, index) => {
    const option = document.createElement("option");
    option.value = String(index);
    option.textContent = page.title?.trim() || `Page ${index + 1}`;
    option.selected = index === state.layout.homePageIndex;
    els.homePage.appendChild(option);
  });
}

function renderTargetDockSlotOptions() {
  els.targetDockSlot.innerHTML = "";
  state.targetDockSlot = clampDockSlot(state.targetDockSlot);
  for (let index = 0; index < DOCK_SLOT_COUNT; index += 1) {
    const option = document.createElement("option");
    option.value = String(index);
    option.textContent = `Slot ${index + 1}`;
    option.selected = index === state.targetDockSlot;
    els.targetDockSlot.appendChild(option);
  }
}

function renderSummary() {
  const activeFolder = getActiveFolderItem();
  const boardCells = getSelectedBoardCells();
  const boardItems = getSelectedBoardItems();
  const dockItems = getSelectedDockItems();
  const linkedApps = getLinkedAppKeysFromBoardSelection();
  const linkedCells = getLinkedBoardCellIdsFromCatalogSelection();
  const targetFolder = getAvailableTargetFolders().find((entry) => entry.item.id === state.targetFolderId) || null;

  els.selectionSummary.textContent =
    `List selected: ${state.catalogSelection.size} | Grid selected: ${boardCells.length} cells / ${boardItems.length} items | ` +
    `Dock selected: ${dockItems.length} items | ` +
    `Folder apps selected: ${state.folderAppSelection.size} | Linked list matches: ${linkedApps.size} | Linked grid matches: ${linkedCells.size}`;
  els.appCount.textContent = `${state.catalog.length} apps`;
  els.dockCount.textContent = `${state.layout.dock.length} / ${DOCK_SLOT_COUNT}`;

  const selectedAppLabels = getSelectedCatalogEntries().slice(0, 6).map((app) => app.label);
  const selectedBoardLabels = boardItems.slice(0, 6).map((item) => itemDisplayName(item));
  const selectedDockLabels = dockItems.slice(0, 6).map((item) => itemDisplayName(item));
  const lines = [
    `Current page: ${formatPageLabel(getActivePage(), state.activePage)}`,
    `Target page: ${formatPageLabel(state.layout.pages[state.targetPage], state.targetPage)}`,
    `Target dock slot: ${state.targetDockSlot + 1}`,
    `Home page: ${formatPageLabel(state.layout.pages[state.layout.homePageIndex], state.layout.homePageIndex)}`,
    `Destination folder: ${targetFolder ? formatFolderTargetLabel(targetFolder) : "Create new / use selected folder"}`,
    `Active folder: ${activeFolder ? itemDisplayName(activeFolder) : "None"}`,
    `Selected apps: ${selectedAppLabels.length ? selectedAppLabels.join(", ") : "None"}`,
    `Selected board items: ${selectedBoardLabels.length ? selectedBoardLabels.join(", ") : "None"}`,
    `Selected dock items: ${selectedDockLabels.length ? selectedDockLabels.join(", ") : "None"}`,
    `Selected folder apps: ${state.folderAppSelection.size ? getSelectedFolderRefs().map((ref) => lookupAppLabel(ref.packageName, ref.activityName)).slice(0, 6).join(", ") : "None"}`
  ];
  els.selectedItemDetails.textContent = lines.join("\n");
}

function renderFolderAppList() {
  const folder = getActiveFolderItem();
  els.folderAppList.innerHTML = "";

  if (!folder) {
    els.folderAppCount.textContent = "0";
    els.folderContext.textContent = "Click a folder on the board or in the dock to inspect and select the apps inside it.";
    if (state.lastLoadedFolderId !== null) {
      state.lastLoadedFolderId = null;
      els.folderTitle.value = "";
    }
    return;
  }

  if (state.lastLoadedFolderId !== folder.id) {
    els.folderTitle.value = folder.title || "";
    state.lastLoadedFolderId = folder.id;
  }

  els.folderAppCount.textContent = `${folder.appRefs.length} apps`;
  els.folderContext.textContent = state.activeFolderArea === "dock"
    ? `${itemDisplayName(folder)} in Dock`
    : `${itemDisplayName(folder)} on Page ${state.activePage + 1}`;

  folder.appRefs.forEach((ref) => {
    const key = getRefKey(ref);
    const row = document.createElement("div");
    row.className = "app-row folder-app-row";
    row.dataset.key = key;
    if (state.folderAppSelection.has(key)) row.classList.add("selected");
    if (state.catalogSelection.has(key)) row.classList.add("linked");
    bindLongPressDragTarget(row, (event) => createDragDescriptorForFolder(key));

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = state.folderAppSelection.has(key);
    checkbox.addEventListener("click", (event) => event.stopPropagation());
    checkbox.addEventListener("change", () => {
      const next = new Set(state.folderAppSelection);
      if (checkbox.checked) {
        next.add(key);
      } else {
        next.delete(key);
      }
      state.folderAppSelection = next;
      syncVisibleSelectionState();
    });

    const content = document.createElement("div");
    content.innerHTML = `<strong>${escapeHtml(lookupAppLabel(ref.packageName, ref.activityName))}</strong><small>${escapeHtml(ref.packageName)}</small>`;

    row.addEventListener("click", (event) => {
      if (shouldSuppressClick()) return;
      const additive = event.ctrlKey || event.metaKey;
      state.folderAppSelection = updateSelectionSet(state.folderAppSelection, key, additive);
      syncVisibleSelectionState();
    });

    row.append(checkbox, content);
    els.folderAppList.appendChild(row);
  });
}

function renderDock() {
  els.dockList.innerHTML = "";
  const linkedDockIds = getLinkedDockItemIdsFromCatalogSelection();
  const dockItemsBySlot = new Map(state.layout.dock.map((item) => [item.x, item]));
  const activeFolder = getActiveFolderItem();
  els.dockContext.textContent = activeFolder && state.activeFolderArea === "dock"
    ? `${itemDisplayName(activeFolder)} in Dock`
    : "Select dock items here, or choose a dock slot above to place apps and folders into the Android dock.";

  for (let slot = 0; slot < DOCK_SLOT_COUNT; slot += 1) {
    const item = dockItemsBySlot.get(slot) || null;
    const cell = document.createElement("button");
    cell.type = "button";
    cell.className = "dock-cell";
    cell.dataset.slot = String(slot);

    if (!item) {
      cell.classList.add("empty");
      if (slot === state.targetDockSlot) cell.classList.add("target-slot");
      cell.innerHTML = `<span class="cell-title">Empty Dock Slot</span><span class="cell-subtitle">Slot ${slot + 1}</span>`;
      cell.addEventListener("click", () => {
        state.targetDockSlot = slot;
        renderTargetDockSlotOptions();
        renderDock();
        renderSummary();
      });
      els.dockList.appendChild(cell);
      continue;
    }

    cell.dataset.itemId = item.id;
    if (item.type === "folder") cell.classList.add("folder");
    if (state.dockSelection.has(item.id)) cell.classList.add("selected");
    if (linkedDockIds.has(item.id)) cell.classList.add("linked");
    if (slot === state.targetDockSlot) cell.classList.add("target-slot");
    bindLongPressDragTarget(cell, () => createDragDescriptorForDock(item.id));

    const subtitle = item.type === "folder" ? `${item.appRefs.length} apps` : `Slot ${slot + 1}`;
    cell.innerHTML = `<span class="cell-title">${escapeHtml(itemDisplayName(item))}</span><span class="cell-subtitle">${escapeHtml(subtitle)}</span>`;
    cell.addEventListener("click", (event) => {
      if (shouldSuppressClick()) return;
      const additive = event.ctrlKey || event.metaKey;
      state.dockSelection = updateSelectionSet(state.dockSelection, item.id, additive);
      state.targetDockSlot = slot;
      if (item.type === "folder") {
        if (!additive || !state.activeFolderId || !state.dockSelection.has(item.id)) {
          state.activeFolderArea = "dock";
          state.activeFolderId = item.id;
        }
      }
      syncVisibleSelectionState();
    });
    els.dockList.appendChild(cell);
  }
}

function renderTargetFolderOptions() {
  els.targetFolder.innerHTML = "";
  state.targetFolderId = getValidTargetFolderId(state.targetFolderId);

  const noneOption = document.createElement("option");
  noneOption.value = "";
  noneOption.textContent = "Create new / use selected folder";
  noneOption.selected = !state.targetFolderId;
  els.targetFolder.appendChild(noneOption);

  getAvailableTargetFolders().forEach((folder) => {
    const option = document.createElement("option");
    option.value = folder.item.id;
    option.textContent = formatFolderTargetLabel(folder);
    option.selected = folder.item.id === state.targetFolderId;
    els.targetFolder.appendChild(option);
  });
}

function renderPageTabs() {
  els.pageTabs.innerHTML = "";
  state.layout.pages.forEach((page, index) => {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = formatPageLabel(page, index);
    if (index === state.activePage) button.classList.add("selected-tab");
    if (index === state.layout.homePageIndex) button.classList.add("home-tab");
    button.addEventListener("click", () => {
      state.activePage = index;
      clearBoardSelection();
      render();
    });
    els.pageTabs.appendChild(button);
  });
}

function renderBoard() {
  const cols = state.layout.grid.cols;
  const rows = state.layout.grid.rows;
  const page = getActivePage();
  const occupancy = buildOccupancyMap(page);
  const linkedCellIds = getLinkedBoardCellIdsFromCatalogSelection();

  els.board.innerHTML = "";
  els.board.style.gridTemplateColumns = `repeat(${cols}, minmax(0, 1fr))`;

  for (let y = 0; y < rows; y += 1) {
    for (let x = 0; x < cols; x += 1) {
      const cellId = getCellId(state.activePage, x, y);
      const cell = document.createElement("button");
      cell.type = "button";
      cell.className = "cell";
      cell.dataset.cellId = cellId;

      if (state.boardSelection.has(cellId)) cell.classList.add("selected");
      if (linkedCellIds.has(cellId)) cell.classList.add("linked");

      const occupant = occupancy.get(`${x}:${y}`);
      if (!occupant) {
        cell.classList.add("empty");
        cell.innerHTML = `<span class="cell-title">Empty</span><span class="cell-subtitle">${x}, ${y}</span>`;
      } else if (!occupant.anchor) {
        cell.classList.add("occupied");
        cell.innerHTML = `<span class="cell-title">${escapeHtml(itemDisplayName(occupant.item))}</span><span class="cell-subtitle">Occupied area</span>`;
      } else if (occupant.item.type === "folder") {
        cell.classList.add("folder", "has-item");
        cell.innerHTML = `<span class="cell-title">${escapeHtml(itemDisplayName(occupant.item))}</span><span class="cell-subtitle">${occupant.item.appRefs.length} apps</span>`;
      } else if (occupant.item.type === "widget") {
        cell.classList.add("widget", "has-item");
        cell.innerHTML = `<span class="cell-title">${escapeHtml(itemDisplayName(occupant.item))}</span><span class="cell-subtitle">${occupant.item.spanX}x${occupant.item.spanY}</span>`;
      } else {
        cell.classList.add("has-item");
        cell.innerHTML = `<span class="cell-title">${escapeHtml(itemDisplayName(occupant.item))}</span><span class="cell-subtitle">${x}, ${y}</span>`;
      }

      if (occupant) {
        bindLongPressDragTarget(cell, () => createDragDescriptorForBoard(cellId));
      }

      cell.addEventListener("click", (event) => {
        if (shouldSuppressClick()) return;
        const additive = event.ctrlKey || event.metaKey;
        state.boardSelection = updateSelectionSet(state.boardSelection, cellId, additive);
        if (occupant?.anchor && occupant.item.type === "folder") {
          if (!additive || !state.activeFolderId || !state.boardSelection.has(getCellId(state.activePage, occupant.item.x, occupant.item.y))) {
            state.activeFolderArea = "page";
            state.activeFolderId = occupant.item.id;
          }
        }
        syncVisibleSelectionState();
      });

      els.board.appendChild(cell);
    }
  }
}

function renderAppList() {
  const linkedAppKeys = getLinkedAppKeysFromBoardSelection();
  const query = state.search.trim();
  const apps = query
    ? state.catalog.filter((app) => {
        const haystack = `${app.label} ${app.packageName} ${app.activityName}`.toLowerCase();
        return haystack.includes(query);
      })
    : state.catalog;

  els.appList.innerHTML = "";

  apps.forEach((app) => {
    const key = getAppKey(app);
    const row = document.createElement("div");
    row.className = "app-row";
    row.dataset.key = key;
    if (state.catalogSelection.has(key)) row.classList.add("selected");
    if (linkedAppKeys.has(key)) row.classList.add("linked");
    bindLongPressDragTarget(row, () => createDragDescriptorForCatalog(key));

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = state.catalogSelection.has(key);
    checkbox.addEventListener("click", (event) => event.stopPropagation());
    checkbox.addEventListener("change", () => {
      const next = new Set(state.catalogSelection);
      if (checkbox.checked) {
        next.add(key);
      } else {
        next.delete(key);
      }
      state.catalogSelection = next;
      syncVisibleSelectionState();
    });

    const content = document.createElement("div");
    content.innerHTML = `<strong>${escapeHtml(app.label)}</strong><small>${escapeHtml(app.packageName)}</small>`;

    row.addEventListener("click", (event) => {
      if (shouldSuppressClick()) return;
      const additive = event.ctrlKey || event.metaKey;
      state.catalogSelection = updateSelectionSet(state.catalogSelection, key, additive);
      syncVisibleSelectionState();
    });

    row.append(checkbox, content);
    els.appList.appendChild(row);
  });
}

function syncVisibleSelectionState() {
  syncActiveFolder();
  renderTargetDockSlotOptions();
  renderFolderAppList();
  renderTargetFolderOptions();
  renderDock();
  syncVisibleAppListState();
  syncVisibleFolderListState();
  syncVisibleBoardState();
  renderSummary();
}

function bindLongPressDragTarget(element, descriptorFactory) {
  element.addEventListener("pointerdown", (event) => {
    if (event.target.closest('input[type="checkbox"]')) return;
    if (event.ctrlKey || event.metaKey) return;
    beginLongPressDrag(event, descriptorFactory(event));
  });
}

function syncVisibleAppListState() {
  const linkedAppKeys = getLinkedAppKeysFromBoardSelection();
  els.appList.querySelectorAll(".app-row").forEach((row) => {
    const key = row.dataset.key;
    row.classList.toggle("selected", state.catalogSelection.has(key));
    row.classList.toggle("linked", linkedAppKeys.has(key));
    const checkbox = row.querySelector('input[type="checkbox"]');
    if (checkbox) checkbox.checked = state.catalogSelection.has(key);
  });
}

function syncVisibleFolderListState() {
  els.folderAppList.querySelectorAll(".folder-app-row").forEach((row) => {
    const key = row.dataset.key;
    row.classList.toggle("selected", state.folderAppSelection.has(key));
    row.classList.toggle("linked", state.catalogSelection.has(key));
    const checkbox = row.querySelector('input[type="checkbox"]');
    if (checkbox) checkbox.checked = state.folderAppSelection.has(key);
  });
}

function syncVisibleBoardState() {
  const linkedCellIds = getLinkedBoardCellIdsFromCatalogSelection();
  els.board.querySelectorAll(".cell").forEach((cell) => {
    const cellId = cell.dataset.cellId;
    cell.classList.toggle("selected", state.boardSelection.has(cellId));
    cell.classList.toggle("linked", linkedCellIds.has(cellId));
  });

  const linkedDockIds = getLinkedDockItemIdsFromCatalogSelection();
  els.dockList.querySelectorAll(".dock-cell").forEach((cell) => {
    const itemId = cell.dataset.itemId;
    if (!itemId) return;
    cell.classList.toggle("selected", state.dockSelection.has(itemId));
    cell.classList.toggle("linked", linkedDockIds.has(itemId));
  });
}

function updateSelectionSet(current, key, additive) {
  if (additive) {
    const next = new Set(current);
    if (next.has(key)) {
      next.delete(key);
    } else {
      next.add(key);
    }
    return next;
  }
  return new Set([key]);
}

function itemDisplayName(item) {
  if (item.type === "folder") return item.title || "Folder";
  if (item.type === "widget") return item.title || "Widget";
  return lookupAppLabel(item.packageName, item.activityName);
}

function lookupAppLabel(packageName, activityName) {
  const key = `${packageName || ""}/${activityName || ""}`;
  return state.catalog.find((app) => getAppKey(app) === key)?.label || packageName || "Unknown";
}

function dedupeRefs(refs) {
  const seen = new Set();
  return refs
    .map((ref) => ({
      packageName: normalizeNullableText(ref?.packageName),
      activityName: normalizeNullableText(ref?.activityName)
    }))
    .filter((ref) => ref.packageName && ref.activityName)
    .filter((ref) => {
      const key = `${ref.packageName}/${ref.activityName}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .map((ref) => ({ packageName: ref.packageName, activityName: ref.activityName }));
}

function clampToInt(value, fallback) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(fallback, Math.trunc(parsed));
}

function clampNumber(value, min, max, fallback) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(min, Math.min(Math.trunc(parsed), max));
}

function deepClone(value) {
  if (window.structuredClone) return window.structuredClone(value);
  return JSON.parse(JSON.stringify(value));
}

function isTypingTarget(target) {
  return target instanceof Element && Boolean(target.closest("input, textarea, select, [contenteditable]"));
}

function createId() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID();
  return `item-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function downloadJson(filename, value) {
  const blob = new Blob([JSON.stringify(value, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatPageLabel(page, index) {
  const base = page?.title?.trim() || `Page ${index + 1}`;
  return index === state.layout.homePageIndex ? `Home - ${base}` : base;
}

function formatFolderTargetLabel(target) {
  return target.area === "dock"
    ? `${itemDisplayName(target.item)} (Dock)`
    : `${itemDisplayName(target.item)} (${formatPageLabel(state.layout.pages[state.activePage], state.activePage)})`;
}
