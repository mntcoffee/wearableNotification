package com.example.wearablenotification.setup

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.wearablenotification.R
import com.example.wearablenotification.main.MainActivity.Companion.SPEED_01
import com.example.wearablenotification.main.MainActivity.Companion.SPEED_02
import kotlinx.android.synthetic.main.fragment_first_navigation.view.*


class FirstNavigationFragment : Fragment() {

    private lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_first_navigation, container, false)

        // enable scroll text view
        view.text_view_navigation_first.movementMethod = ScrollingMovementMethod()

        view.next_button_first_fragment.setOnClickListener {
            var ringerMode = audioManager.ringerMode
            when(ringerMode) {
                AudioManager.RINGER_MODE_VIBRATE -> {
                    // manner
                    Toast.makeText(activity,
                            "マナーモードを解除してください．",
                            Toast.LENGTH_SHORT).show()

                }
                AudioManager.RINGER_MODE_SILENT -> {
                    // silent
                    Toast.makeText(activity,
                            "サイレントモードを解除してください．",
                            Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // normal
                    findNavController().navigate(R.id.action_firstNavigationFragment_to_secondNavigationFragment)
                }
            }
        }

        view.test_mode_switch_first_fragment.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                SPEED_01 = 20.0
                SPEED_02 = 5.0
            } else {
                SPEED_01 = 45.0
                SPEED_02 = 10.0
            }
        }

        return view
    }

}