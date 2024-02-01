package com.ztftrue.music.utils.textToolbar

import android.content.Intent
import android.view.ActionMode
import android.view.View
import androidx.annotation.DoNotInline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.core.view.allViews
import com.ztftrue.music.sqlData.model.DictionaryApp


internal class CustomTextToolbar(
    private val view: View,
    private val customApp: List<DictionaryApp>,
) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val textActionModeCallback: TextActionModeCallback = TextActionModeCallback()

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?

// Before 1.2.0 ActionCallback has to be defined like this:
// typealias ActionCallback = () -> Unit
//
//  onCopyRequested: ActionCallback?,
//  onPasteRequested: ActionCallback?,
//  onCutRequested: ActionCallback?,
//  onSelectAllRequested: ActionCallback?
    ) {
        textActionModeCallback.rect = rect
        textActionModeCallback.onCopyRequested = onCopyRequested
        textActionModeCallback.onCutRequested = onCutRequested
        textActionModeCallback.onPasteRequested = onPasteRequested
        textActionModeCallback.onSelectAllRequested = onSelectAllRequested
        textActionModeCallback.customProcessTextApp = customApp
        textActionModeCallback.onProcessAppItemClick = {
            var min = 0
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
            val intent = Intent()
            intent.setAction(Intent.ACTION_PROCESS_TEXT)
            intent.setClassName(
                it.packageName,
                it.name
            )
            intent.putExtra(
                Intent.EXTRA_PROCESS_TEXT,
                actionMode?.title
            )
            view.context.startActivity(intent)
        }
        if (actionMode == null) {
            status = TextToolbarStatus.Shown
            actionMode =
                TextToolbarHelperMethods.startActionMode(
                    view,
                    FloatingTextActionModeCallback(textActionModeCallback),
                    ActionMode.TYPE_FLOATING
                )
        } else {
            actionMode?.invalidate()
        }
    }


}

internal object TextToolbarHelperMethods {
    @DoNotInline
    fun startActionMode(
        view: View,
        actionModeCallback: ActionMode.Callback,
        type: Int
    ): ActionMode {
        return view.startActionMode(
            actionModeCallback,
            type
        )
    }

    fun invalidateContentRect(actionMode: ActionMode) {
        actionMode.invalidateContentRect()
    }
}


