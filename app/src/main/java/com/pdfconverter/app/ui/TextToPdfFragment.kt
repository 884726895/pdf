package com.pdfconverter.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.pdfconverter.app.R
import com.pdfconverter.app.utils.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextToPdfFragment : Fragment() {

    private lateinit var etTextContent: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_text_to_pdf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etTextContent = view.findViewById(R.id.etTextContent)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)

        view.findViewById<Button>(R.id.btnConvert).setOnClickListener {
            startConversion()
        }
    }

    private fun startConversion() {
        val text = etTextContent.text?.toString()?.trim()
        if (text.isNullOrEmpty()) {
            Snackbar.make(requireView(), "请输入要转换的文本内容", Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在生成PDF..."
        tvStatus.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val outputDir = PdfUtils.getOutputDir(requireContext())
            val result = PdfUtils.textToPdf(text, outputDir)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = { file ->
                        tvStatus.text = "PDF生成成功!\n保存到: ${file.absolutePath}"
                        Snackbar.make(requireView(), "生成成功", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { e ->
                        tvStatus.text = "生成失败: ${e.message}"
                        Snackbar.make(requireView(), "生成失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}
