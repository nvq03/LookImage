package com.quy.lookimage

import android.app.Activity
import android.app.AlertDialog
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class HomeFragment : Fragment() {

    private val REQUEST_DELETE = 101
    private var lastPendingDeleteUri: Uri? = null
    private lateinit var adapter: LockedImageAdapter
    private lateinit var lockedDir: File
    private var pendingDeleteCallback: ((Boolean) -> Unit)? = null


    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d("PickImage", "Ảnh được chọn: $uri")
                val copiedFile = copyImageToPrivateStorage(uri)

                tryDeleteOriginal(uri) { success ->
                    if (success) {
                        adapter.updateData(getLockedImages())
                    } else {
                        // Nếu xóa thất bại, bạn có thể xóa ảnh đã copy để rollback
                        copiedFile.delete()
                    }
                }
            }
        }
    }


    private val deleteImageLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lastPendingDeleteUri?.let {
                Log.d("DeleteDebug", "Người dùng xác nhận — gọi lại delete(uri)")
                try {
                    requireContext().contentResolver.delete(it, null, null)
                    pendingDeleteCallback?.invoke(true)
                } catch (e: Exception) {
                    Log.e("DeleteDebug", "Lỗi khi xóa ảnh gốc sau khi người dùng xác nhận", e)
                    Toast.makeText(requireContext(), "Không thể xóa ảnh", Toast.LENGTH_SHORT).show()
                    pendingDeleteCallback?.invoke(false)
                }
                lastPendingDeleteUri = null
                pendingDeleteCallback = null
            }
        } else {
            Toast.makeText(requireContext(), "Không thể xóa ảnh", Toast.LENGTH_SHORT).show()
            pendingDeleteCallback?.invoke(false)
            pendingDeleteCallback = null
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lockedDir = File(requireContext().filesDir, "locked_images").apply {
            if (!exists()) mkdirs()
        }

        adapter = LockedImageAdapter(getLockedImages()) { file ->
            showImageDialog(file)
        }


        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewLocked)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter

        view.findViewById<Button>(R.id.btnPickImage).setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(pickIntent)
        }
    }

    private fun getLockedImages(): List<File> {
        return lockedDir.listFiles()?.toList() ?: emptyList()
    }

    private fun copyImageToPrivateStorage(uri: Uri): File {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val file = File(lockedDir, "${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun deleteOriginal(uri: Uri) {
        Log.d("DeleteDebug", "Yêu cầu xóa ảnh với URI: $uri")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                requireContext().contentResolver.delete(uri, null, null)
                Log.d("DeleteDebug", "Đã xóa ảnh gốc trực tiếp (API >= 29)")
            } catch (e: RecoverableSecurityException) {
                Log.w("DeleteDebug", "Cần yêu cầu người dùng xác nhận xóa ảnh")
                lastPendingDeleteUri = uri
                val intentSender = e.userAction.actionIntent.intentSender
                deleteImageLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } catch (e: Exception) {
                Log.e("DeleteDebug", "Lỗi khi xóa ảnh gốc", e)
                Toast.makeText(requireContext(), "Không thể xóa ảnh gốc", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                val deleted = requireContext().contentResolver.delete(uri, null, null)
                Log.d("DeleteDebug", "Xóa ảnh trực tiếp, kết quả: $deleted")
            } catch (e: Exception) {
                Log.e("DeleteDebug", "Lỗi khi xóa ảnh gốc", e)
                Toast.makeText(requireContext(), "Không thể xóa ảnh gốc", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreImageToGallery(file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            val outStream = resolver.openOutputStream(it)
            val inStream = FileInputStream(file)
            inStream.copyTo(outStream!!)
            inStream.close()
            outStream.close()

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, values, null, null)

            file.delete()
            adapter.updateData(getLockedImages())
        }
    }

    private fun showImageDialog(file: File) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_preview, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.dialog_image)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_delete)
        val btnRestore = dialogView.findViewById<Button>(R.id.btn_restore)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close)

        imageView.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnDelete.setOnClickListener {
            file.delete()
            adapter.updateData(getLockedImages())
            dialog.dismiss()
            Toast.makeText(requireContext(), "Image deleted", Toast.LENGTH_SHORT).show()
        }

        btnRestore.setOnClickListener {
            restoreImageToGallery(file)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Image restored", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)

    }

    private fun tryDeleteOriginal(uri: Uri, onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                requireContext().contentResolver.delete(uri, null, null)
                Log.d("DeleteDebug", "Đã xóa ảnh gốc trực tiếp (API >= 29)")
                onResult(true)
            } catch (e: RecoverableSecurityException) {
                Log.w("DeleteDebug", "Cần yêu cầu người dùng xác nhận xóa ảnh")
                lastPendingDeleteUri = uri
                pendingDeleteCallback = onResult
                val intentSender = e.userAction.actionIntent.intentSender
                deleteImageLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } catch (e: Exception) {
                Log.e("DeleteDebug", "Lỗi khi xóa ảnh gốc", e)
                Toast.makeText(requireContext(), "Không thể xóa ảnh", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        } else {
            try {
                val deleted = requireContext().contentResolver.delete(uri, null, null) > 0
                Log.d("DeleteDebug", "Xóa ảnh trực tiếp, kết quả: $deleted")
                onResult(deleted)
            } catch (e: Exception) {
                Log.e("DeleteDebug", "Lỗi khi xóa ảnh gốc", e)
                Toast.makeText(requireContext(), "Không thể xóa ảnh", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }


}
