package com.ztftrue.music.utils.textToolbar

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Rect
import com.ztftrue.music.sqlData.model.DictionaryApp

internal class TextActionModeCallback(
    var rect: Rect = Rect.Zero,
    val onActionModeDestroy: (() -> Unit)? = null,
    var onCopyRequested: (() -> Unit)? = null,
    var onPasteRequested: (() -> Unit)? = null,
    var onCutRequested: (() -> Unit)? = null,
    var onSelectAllRequested: (() -> Unit)? = null,
    var customProcessTextApp: List<DictionaryApp>? = null,
    var onProcessAppItemClick: ((DictionaryApp) -> Unit)? = null,
    var textToolbar: CustomTextToolbar?=null
) {
    fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)

        onCopyRequested?.let {
            addMenuItem(menu, MenuItemOption.Copy)
        }
        customProcessTextApp?.forEachIndexed { index, it ->
            addMenuItemDictionaryApp(menu, it, index)
        }
        onPasteRequested?.let {
            addMenuItem(menu, MenuItemOption.Paste)
        }
        onCutRequested?.let {
            addMenuItem(menu, MenuItemOption.Cut)
        }
        onSelectAllRequested?.let {
            addMenuItem(menu, MenuItemOption.SelectAll)
        }
        textToolbar?.let {
            addMenuItem(menu, MenuItemOption.Close)
        }
        return true
    }

    // this method is called to populate new menu items when the actionMode was invalidated
    fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        if (mode == null || menu == null) return false
        updateMenuItems(menu)
        // should return true so that new menu items are populated
        return true
    }

    fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        if (item != null) {
            if (item.groupId == 0) {
                try {
                    when (item.itemId) {
                        MenuItemOption.Copy.id -> onCopyRequested?.invoke()
                        MenuItemOption.Paste.id -> onPasteRequested?.invoke()
                        MenuItemOption.Cut.id -> onCutRequested?.invoke()
                        MenuItemOption.SelectAll.id -> onSelectAllRequested?.invoke()
                        MenuItemOption.Close.id ->textToolbar?.hideAll()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (item.groupId == 1) {
                val app = customProcessTextApp?.get(item.order)
                if (app != null) {

//                    var min = 0
//                    var max: Int = view.getText().length()
//                    if (mTextView.isFocused()) {
//                        val selStart: Int = mTextView.getSelectionStart()
//                        val selEnd: Int = mTextView.getSelectionEnd()
//                        min = Math.max(0, Math.min(selStart, selEnd))
//                        max = Math.max(0, Math.max(selStart, selEnd))
//                    }
//                    // Perform your definition lookup with the selected text
//                    // Perform your definition lookup with the selected text
//                    val selectedText: CharSequence = mTextView.getText().subSequence(min, max)
                    onProcessAppItemClick?.invoke(app)
                }
            }
        }
        mode?.finish()
        return true
    }

    fun onDestroyActionMode() {
        onActionModeDestroy?.invoke()
    }

    @VisibleForTesting
    internal fun updateMenuItems(menu: Menu) {
        addOrRemoveMenuItem(menu, MenuItemOption.Copy, onCopyRequested)
        addOrRemoveMenuItem(menu, MenuItemOption.Paste, onPasteRequested)
        addOrRemoveMenuItem(menu, MenuItemOption.Cut, onCutRequested)
        addOrRemoveMenuItem(menu, MenuItemOption.SelectAll, onSelectAllRequested)
        customProcessTextApp?.forEachIndexed { index, it ->
            addOrRemoveMenuAppItem(menu, it, index, onProcessAppItemClick)
        }
    }

    internal fun addMenuItem(menu: Menu, item: MenuItemOption) {
        menu.add(0, item.id, item.order, item.titleResource)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    internal fun addMenuItemDictionaryApp(menu: Menu, item: DictionaryApp, index: Int) {
        menu.add(1, index + 10, index, item.label)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    private fun addOrRemoveMenuItem(
        menu: Menu,
        item: MenuItemOption,
        callback: (() -> Unit)?
    ) {
        when {
            callback != null && menu.findItem(item.id) == null -> addMenuItem(menu, item)
            callback == null && menu.findItem(item.id) != null -> menu.removeItem(item.id)
        }
    }

    private fun addOrRemoveMenuAppItem(
        menu: Menu,
        item: DictionaryApp,
        index: Int,
        callback: ((d: DictionaryApp) -> Unit)?
    ) {
        // TODO magic number, because this default max number is 4.
        when {
            callback != null && menu.findItem(index + 10) == null -> addMenuItemDictionaryApp(
                menu,
                item,
                index
            )

            callback == null && menu.findItem(index + 10) != null -> menu.removeItem(index + 10)
        }
    }
}


internal enum class MenuItemOption(val id: Int) {
    Copy(0),
    Paste(1),
    Cut(2),
    SelectAll(3),
    Close(4);

    val titleResource: Int
        get() = when (this) {
            Copy -> android.R.string.copy
            Paste -> android.R.string.paste
            Cut -> android.R.string.cut
            SelectAll -> android.R.string.selectAll
            Close -> android.R.string.cancel
        }

    /**
     * This item will be shown before all items that have order greater than this value.
     */
    val order = id
}