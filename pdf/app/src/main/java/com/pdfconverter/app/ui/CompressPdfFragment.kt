package com.pdfconverter.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
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

class CompressPdfFragment : Fragment() {

    private var selectedPdfUri: Uri? = null
    private var quality = 70

    private lateinit var tvSelectedFile: TextView
    private lateinit var tvFileInfo: TextView
    private lateinit var tvQualityLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var seekBarQuality: SeekBar

    private val pdfPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedPdfUri = it
            tvSelectedFile.text = it.lastPathSegment ?: "已选择文件"
            updateFileInfo()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_compress_pdf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)
        tvFileInfo = view.findViewById(R.id.tvFileInfo)
        tvQualityLabel = view.findViewById(R.id.tvQualityLabel)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)
        seekBarQuality = view.findViewById(R.id.seekBarQuality)

        view.findViewById<Button>(R.id.btnSelectPdf).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        seekBarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                quality = progress.coerceIn(1, 100)
                tvQualityLabel.text = "压缩质量: $quality%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        view.findViewById<Button>(R.id.btnConvert).setOnClickListener {
            startCompression()
        }
    }

    private fun updateFileInfo() {
        val uri = selectedPdfUri ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                val pageCount = doc.numberOfPages
                doc.close()
                inputStream?.close()
                withContext(Dispatchers.Main) {
                    tvFileInfo.text = "页数: $pageCount"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvFileInfo.text = ""
                }
            }
        }
    }

    private fun startCompression() {
        val uri = selectedPdfUri ?: run {
            Snackbar.make(requireView(), "请先选择PDF文件", Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在压缩..."
        tvStatus.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val outputDir = PdfUtils.getOutputDir(requireContext())
            val result = PdfUtils.compressPdf(requireContext(), uri, outputDir, quality)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = { file ->
                        tvStatus.text = "压缩成功!\n保存到: ${file.absolutePath}"
                        Snackbar.make(requireView(), "压缩成功", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { e ->
                        tvStatus.text = "压缩失败: ${e.message}"
                        Snackbar.make(requireView(), "压缩失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}
