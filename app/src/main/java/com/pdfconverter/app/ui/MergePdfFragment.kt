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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.pdfconverter.app.R
import com.pdfconverter.app.utils.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MergePdfFragment : Fragment() {

    private val pdfUris = mutableListOf<Uri>()
    private lateinit var recyclerFiles: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var mergeAdapter: MergeAdapter

    private val pdfPicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pdfUris.addAll(uris)
        mergeAdapter.notifyDataSetChanged()
        updateStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_merge_pdf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerFiles = view.findViewById(R.id.recyclerFiles)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)

        mergeAdapter = MergeAdapter(pdfUris) { index ->
            pdfUris.removeAt(index)
            mergeAdapter.notifyDataSetChanged()
            updateStatus()
        }
        recyclerFiles.layoutManager = LinearLayoutManager(requireContext())
        recyclerFiles.adapter = mergeAdapter

        view.findViewById<Button>(R.id.btnAddPdf).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        view.findViewById<Button>(R.id.btnConvert).setOnClickListener {
            startMerge()
        }
    }

    private fun updateStatus() {
        tvStatus.text = "已添加 ${pdfUris.size} 个PDF文件"
    }

    private fun startMerge() {
        if (pdfUris.size < 2) {
            Snackbar.make(requireView(), "请至少选择2个PDF文件", Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在合并..."
        tvStatus.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val outputDir = PdfUtils.getOutputDir(requireContext())
            val result = PdfUtils.mergePdfs(pdfUris.toList(), requireContext(), outputDir)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = { file ->
                        tvStatus.text = "合并成功!\n保存到: ${file.absolutePath}"
                        Snackbar.make(requireView(), "合并成功", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { e ->
                        tvStatus.text = "合并失败: ${e.message}"
                        Snackbar.make(requireView(), "合并失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

class MergeAdapter(
    private val uris: MutableList<Uri>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<MergeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text = uris[position].lastPathSegment ?: "PDF ${position + 1}"
        holder.itemView.setOnLongClickListener {
            onRemove(position)
            true
        }
    }

    override fun getItemCount() = uris.size
}
