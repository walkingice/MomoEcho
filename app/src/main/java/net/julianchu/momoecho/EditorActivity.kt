package net.julianchu.momoecho

import androidx.fragment.app.Fragment
import net.julianchu.momoecho.editclip.VisualEditorFragment

class EditorActivity : SingleFragmentActivity() {

    override fun createFragment(): Fragment {
        return VisualEditorFragment.createFragment()
    }
}
