package vcmsa.projects.buggybank

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import vcmsa.projects.buggybank.R

private const val ARG_IMAGE_RES = "imageRes"
private const val ARG_TEXT = "text"

/**
 * A simple Fragment subclass that displays one tutorial overlay (image + text + "OK" button).
 * Use newInstance(imageRes, text) to pass in exactly what you want to show.
 * When "OK" is tapped, the fragment simply removes itself.
 */
class TutorialFragment : Fragment() {
    
    private var imageRes: Int = 0
    private var messageText: String? = null
    
    private lateinit var mascotImage: ImageView
    private lateinit var tutorialText: TextView
    private lateinit var okButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageRes = it.getInt(ARG_IMAGE_RES)
            messageText = it.getString(ARG_TEXT)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate your overlay layout (must match the IDs below)
        val view = inflater.inflate(R.layout.fragment_tutorial, container, false)
        
        mascotImage = view.findViewById(R.id.mascotImage)
        tutorialText = view.findViewById(R.id.tutorialText)
        okButton = view.findViewById(R.id.nextButton)
        
        // Set image and text based on the arguments passed in
        mascotImage.setImageResource(imageRes)
        tutorialText.text = messageText
        
        // Change button label to "OK" (it was "Next"/"Finish" before)
        okButton.text = "OK"
        okButton.setOnClickListener {
            // Simply remove/dismiss this fragment
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
        
        return view
    }
    
    companion object {
        /**
         * Create a new instance of TutorialFragment with the given image and text.
         *
         * @param imageRes   Drawable resource ID for the mascot/image.
         * @param text       The explanatory text to display.
         */
        @JvmStatic
        fun newInstance(imageRes: Int, text: String): TutorialFragment {
            return TutorialFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_IMAGE_RES, imageRes)
                    putString(ARG_TEXT, text)
                }
            }
        }
    }
}
