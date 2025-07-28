package com.qateam.lookimage

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog


class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Khai báo các view
        val linearShare = view.findViewById<LinearLayout>(R.id.linear_share)
        val linearRate = view.findViewById<LinearLayout>(R.id.linear_rate)
        val linearPrivacy = view.findViewById<LinearLayout>(R.id.linear_privacy_policy)
        val linearContact = view.findViewById<LinearLayout>(R.id.linear_contact)

        // SHARE
        linearShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
                putExtra(Intent.EXTRA_TEXT, "Try this amazing image locker app: https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        }

        // RATE + PRIVACY → show dialog
        val dialogMessage = "Our application has nothing to do with users' personal information images, and if users want to delete data or applications, please carefully review the images that have been put into the application to avoid losing images."

        linearRate.setOnClickListener {
            try {
                val uri = Uri.parse("market://details?id=${requireContext().packageName}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.android.vending") // Mở bằng Play Store
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Nếu không có Play Store, mở bằng trình duyệt
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

        linearPrivacy.setOnClickListener {
            showNoteDialog("Note", dialogMessage)
        }

        // CONTACT
        linearContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:tubepayteam@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No email app installed", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showNoteDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
