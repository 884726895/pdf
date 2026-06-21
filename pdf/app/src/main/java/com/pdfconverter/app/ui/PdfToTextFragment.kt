package com.pdfconverter.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.pdfconverter.app.R
import com.pdfconverter.app.utils.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfToTextFragment : Fragment() {

    private var selectedPdfUri: Uri? = null
    private lateinit var tvSelectedFile: TextView
    private lateinit var tvPreview: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var etPassword: TextInputEditText

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
        return inflater.inflate(R.layout.fragment_pdf_to_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)
        tvPreview = view.findViewById(R.id.tvPreview)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)
        etPassword = view.findViewById(R.id.etPassword)

        view.findViewById<Button>(R.id.btnSelectPdf).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        view.findViewById<Button>(R.id.btnConvert).setOnClickListener {
            startExtraction()
        }
    }

    private fun startExtraction() {
        val uri = selectedPdfUri ?: run {
            Snackbar.make(requireView(), "请先选择PDF文件", Snackbar.LENGTH_SHORT).show()
            return
        }

        val password = etPassword.text?.toString()?.takeIf { it.isNotEmpty() }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在提取文本..."
        tvStatus.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val result = PdfUtils.pdfToText(requireContext(), uri, password)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = { text ->
                        tvPreview.text = text
                        if (text.isNotBlank()) {
                            saveTextToFile(text)
                        }
                        tvStatus.text = "文本提取完成，共 ${text.length} 字符"
                        Snackbar.make(requireView(), "提取成功", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { e ->
                        tvStatus.text = "提取失败: ${e.message}"
                        Snackbar.make(requireView(), "提取失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun saveTextToFile(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outputDir = PdfUtils.getOutputDir(requireContext())
                val file = File(outputDir, PdfUtils.generateFileName("extracted", "txt"))
                FileOutputStream(file).use { it.write(text.toByteArray()) }
                withContext(Dispatchers.Main) {
                    tvStatus.text = "${tvStatus.text}\n已保存到: ${file.absolutePath}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "${tvStatus.text}\n保存文件失败: ${e.message}"
                }
            }
        }
    }
}
