package com.pdfconverter.app.ui

import android.graphics.Bitmap
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

class PdfToImageFragment : Fragment() {

    private var selectedPdfUri: Uri? = null
    private var outputFormat = Bitmap.CompressFormat.PNG

    private lateinit var tvSelectedFile: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var etPageRange: EditText
    private lateinit var radioGroupPages: RadioGroup
    private lateinit var radioAllPages: RadioButton

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
        return inflater.inflate(R.layout.fragment_pdf_to_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)
        etPageRange = view.findViewById(R.id.etPageRange)
        radioGroupPages = view.findViewById(R.id.radioGroupPages)
        radioAllPages = view.findViewById(R.id.radioAllPages)

        view.findViewById<Button>(R.id.btnSelectPdf).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        val radioGroupFormat = view.findViewById<RadioGroup>(R.id.radioGroupFormat)
        radioGroupFormat.setOnCheckedChangeListener { _, checkedId ->
            outputFormat = when (checkedId) {
                R.id.radioJpg -> Bitmap.CompressFormat.JPEG
                else -> Bitmap.CompressFormat.PNG
            }
        }

        radioGroupPages.setOnCheckedChangeListener { _, checkedId ->
            etPageRange.visibility = if (checkedId == R.id.radioCustomPages) View.VISIBLE else View.GONE
        }

        view.findViewById<Button>(R.id.btnConvert).setOnClickListener {
            startConversion()
        }
    }

    private fun startConversion() {
        val uri = selectedPdfUri ?: run {
            Snackbar.make(requireView(), "请先选择PDF文件", Snackbar.LENGTH_SHORT).show()
            return
        }

        val pageRange = if (radioAllPages.isChecked) {
            null
        } else {
            parsePageRange(etPageRange.text.toString())
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在转换..."
        tvStatus.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val outputDir = PdfUtils.getOutputDir(requireContext())
            val result = PdfUtils.pdfToImage(
                requireContext(), uri, outputDir, outputFormat, 90, pageRange
            )

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = { files ->
                        tvStatus.text = "转换成功! 共生成 ${files.size} 个文件\n保存在: ${outputDir.absolutePath}"
                        Snackbar.make(requireView(), "转换成功", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { e ->
                        tvStatus.text = "转换失败: ${e.message}"
                        Snackbar.make(requireView(), "转换失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun parsePageRange(input: String): IntRange? {
        return try {
            val parts = input.split("-")
            if (parts.size == 2) {
                (parts[0].trim().toInt() - 1)..(parts[1].trim().toInt() - 1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
