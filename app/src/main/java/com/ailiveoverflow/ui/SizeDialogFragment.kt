package com.ailiveoverflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ailiveoverflow.core.StateManager

/** 尺寸调整弹窗 */
class SizeDialogFragment(private val stateManager: StateManager) : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = LayoutInflater.from(context).inflate(
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            container, false
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("调整悬浮窗尺寸")
            .setView(LayoutInflater.from(requireContext()).inflate(androidx.appcompat.R.layout.alert_dialog, null))
            .create()

        dialog.setOnShowListener {
            val dialogView = dialog.findViewById<View>(android.R.id.content) as? ViewGroup
                ?: return@setOnShowListener

            val width = stateManager.overlayWidth
            val height = stateManager.overlayHeight

            val seekWidth = SeekBar(context).apply {
                max = 400
                progress = width
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        stateManager.overlayWidth = progress
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            val seekHeight = SeekBar(context).apply {
                max = 400
                progress = height
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        stateManager.overlayHeight = progress
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            dialogView.addView(TextView(context).apply {
                text = "宽度：$width dp"
                textSize = 14f
            })
            dialogView.addView(seekWidth)
            dialogView.addView(TextView(context).apply {
                text = "高度：$height dp"
                textSize = 14f
            })
            dialogView.addView(seekHeight)
        }

        dialog.show()
        return view
    }
}