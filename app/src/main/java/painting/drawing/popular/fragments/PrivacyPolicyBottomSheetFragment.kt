package painting.drawing.popular.fragments


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import painting.drawing.popular.MainActivity
import com.drawing.paint.R
import com.drawing.paint.databinding.PrivacyPolicyBottomSheetFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.lang.Exception


class PrivacyPolicyBottomSheetFragment : BottomSheetDialogFragment() {

    private var binding: PrivacyPolicyBottomSheetFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.privacy_policy_bottom_sheet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = PrivacyPolicyBottomSheetFragmentBinding.bind(view)

        binding!!.apply {
            tvContacts.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/dr_padding"))
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Make sure you have the Telegram app installed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as MainActivity).isFragmentShown = true
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).isFragmentShown = false
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}