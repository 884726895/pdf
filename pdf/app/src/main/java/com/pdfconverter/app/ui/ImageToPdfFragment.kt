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

class ImageToPdfFragment : Fragment() {

    private val imageUris = mutableListOf<Uri>()
    private lateinit var tvImageCount: TextView
    private lateinit var recyclerImages: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var imageAdapter: ImageAdapter

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        imageUris.clear()
        imageUris.addAll(uris)
        updateImageCount()
        imageAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_image_to_pdf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvImageCount = view.findViewById(R.id.tvImageCount)
        recyclerImages = view.findViewById(R.id.recyclerImages)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)

        imageAdapter = ImageAdapter(imageUris)
        recyclerImages.layoutManager = LinearLayoutManager(requireContext())
        recyclerImages.adapter = imageAdapter

        view.findViewById<Button>(R.id.btnSelectImages).setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        view.findViewById<Button>(R.id.btnConvert).setOnClickListener {
            startConversion()
        }
    }

    private fun updateImageCount() {
        tvImageCount.text = "已选择 ${imageUris.size} 张图片"
    }

    private fun startConversion() {
        if (imageUris.isEmpty()) {
            Snackbar.make(requireView(), "请先选择图片", Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在转换..."
        tvStatus.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val outputDir = PdfUtils.getOutputDir(requireContext())
            val result = PdfUtils.imageToPdf(imageUris.toList(), requireContext(), outputDir)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = { file ->
                        tvStatus.text = "转换成功!\n保存到: ${file.absolutePath}"
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
}

class ImageAdapter(private val uris: List<Uri>) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text = uris[position].lastPathSegment ?: "Image ${position + 1}"
    }

    override fun getItemCount() = uris.size
}
