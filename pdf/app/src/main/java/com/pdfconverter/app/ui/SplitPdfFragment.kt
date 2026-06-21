package com.pdfconverter.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.pdfconverter.app.R
import com.pdfconverter.app.utils.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplitPdfFragment : Fragment() {

    private var selectedPdfUri: Uri? = null
    private lateinit var tvSelectedFile: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var radioGroupSplitMode: RadioGroup
    private lateinit var radioSplitAll: RadioButton
    private lateinit var etSplitRanges: EditText

    private val pdfPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedPdfUri = it
            tvSelectedFile.text = it.lastPathSegment ?: "已选择文件"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_split_pdf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)
        radioGroupSplitMode = view.findViewById(R.id.radioGroupSplitMode)
        radioSplitAll = view.findViewById(R.id.radioSplitAll)
        etSplitRanges = view.findViewById(R.id.etSplitRanges)

        view.findViewById<Button>(R.id.btnSelectPdf).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        radioGroupSplitMode.setOnCheckedChangeListener { _, checkedId ->
            etSplitRanges.visibility =
                if (checkedId == R.id.radioSplitRange) View.VISIBLE else View.GONE
        }

        view.findViewById<Button>(R.id.btnConvert).setOnClickListener {
            startSplit()
        }
    }

    private fun startSplit() {
        val uri = selectedPdfUri ?: run {
            Snackbar.make(requireView(), "请先选择PDF文件", Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在拆分..."
        tvStatus.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val outputDir = PdfUtils.getOutputDir(requireContext())
            val ranges = if (radioSplitAll.isChecked) {
                // Will be determined after loading doc
                null
            } else {
                parseSplitRanges(etSplitRanges.text.toString())
            }

            // Calculate ranges
            val finalRanges: List<IntRange>
            if (ranges != null) {
                finalRanges = ranges
            } else {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                val pageCount = doc.numberOfPages
                doc.close()
                inputStream?.close()
                finalRanges = (0 until pageCount).map { it..it }
            }

            val result = PdfUtils.splitPdf(requireContext(), uri, outputDir, finalRanges)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = { files ->
                        tvStatus.text = "拆分成功! 共生成 ${files.size} 个文件\n保存在: ${outputDir.absolutePath}"
                        Snackbar.make(requireView(), "拆分成功", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { e ->
                        tvStatus.text = "拆分失败: ${e.message}"
                        Snackbar.make(requireView(), "拆分失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun parseSplitRanges(input: String): List<IntRange>? {
        return try {
            input.split(",").map { part ->
                val range = part.trim().split("-")
                if (range.size == 2) {
                    (range[0].toInt() - 1)..(range[1].toInt() - 1)
                } else {
                    (range[0].toInt() - 1)..(range[0].toInt() - 1)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
