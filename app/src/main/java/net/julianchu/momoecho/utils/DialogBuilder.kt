package net.julianchu.momoecho.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog

/**
 * The layout should consists of at least three view
 * 1. confirm button, with android.R.id.button1
 * 2. cancel button, with android.R.id.closeButton
 * 3. edit editText, with android.R.id.edit
 */
fun showSingleInputDialog(
    context: Context,
    @LayoutRes layoutRes: Int,
    confirmCallback: (String?) -> Unit
): EditText {
    val factory = LayoutInflater.from(context)
    val view = factory.inflate(layoutRes, null)
    val dialog = AlertDialog.Builder(context).create()
    val edit = view.findViewById<EditText>(android.R.id.edit)
    dialog.setView(view)

    view.findViewById<Button>(android.R.id.closeButton).setOnClickListener {
        dialog.dismiss()
    }

    view.findViewById<Button>(android.R.id.button1).setOnClickListener {
        dialog.dismiss()
        confirmCallback(edit.text.toString())
    }

    dialog.show()
    return edit
}

fun showConfirmDialog(
    context: Context,
    msg: CharSequence,
    confirmCallback: () -> Unit
) {
    AlertDialog.Builder(context)
        .setMessage(msg)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            confirmCallback()
        }
        .show()
}