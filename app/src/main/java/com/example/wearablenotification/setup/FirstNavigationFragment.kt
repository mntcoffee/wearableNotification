package com.example.wearablenotification.setup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.wearablenotification.R
import kotlinx.android.synthetic.main.fragment_first_navigation.view.*


class FirstNavigationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_first_navigation, container, false)

        view.next_button_first_fragment.setOnClickListener {
            findNavController().navigate(R.id.action_firstNavigationFragment_to_secondNavigationFragment)
        }

        return view
    }

}