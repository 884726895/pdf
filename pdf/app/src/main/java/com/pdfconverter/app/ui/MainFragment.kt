package com.pdfconverter.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.pdfconverter.app.R

class MainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.cardPdfToImage).setOnClickListener {
            it.findNavController().navigate(R.id.action_to_pdfToImage)
        }
        view.findViewById<View>(R.id.cardImageToPdf).setOnClickListener {
            it.findNavController().navigate(R.id.action_to_imageToPdf)
        }
        view.findViewById<View>(R.id.cardPdfToText).setOnClickListener {
            it.findNavController().navigate(R.id.action_to_pdfToText)
        }
        view.findViewById<View>(R.id.cardTextToPdf).setOnClickListener {
            it.findNavController().navigate(R.id.action_to_textToPdf)
        }
        view.findViewById<View>(R.id.cardMergePdf).setOnClickListener {
            it.findNavController().navigate(R.id.action_to_mergePdf)
        }
        view.findViewById<View>(R.id.cardSplitPdf).setOnClickListener {
            it.findNavController().navigate(R.id.action_to_splitPdf)
        }
        view.findViewById<View>(R.id.cardCompressPdf).setOnClickListener {
            it.findNavController().navigate(R.id.action_to_compressPdf)
        }
    }
}
