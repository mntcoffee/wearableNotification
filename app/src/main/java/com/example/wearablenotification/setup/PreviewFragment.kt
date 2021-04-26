package com.example.wearablenotification.setup

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.wearablenotification.MainActivity
import com.example.wearablenotification.R
import kotlinx.android.synthetic.main.fragment_preview.view.*


class PreviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_preview, container, false)

        view.back_button_preview_fragment.setOnClickListener {
            findNavController().popBackStack()
        }

        view.start_button_preview_fragment.setOnClickListener {
            /**
             * [TODO] check setup
             */
            activity.apply {
                val intent = Intent(this, MainActivity::class.java)

                startActivity(intent)
            }

        }

        return view
    }

}