package com.quy.lookimage

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PasswordActivity : AppCompatActivity() {

    private lateinit var edtPassword: EditText
    private lateinit var edtConfirm: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)


        if (PasswordHelper.hasPassword(this)) {
            showPasswordDialog()
        }

        edtPassword = findViewById(R.id.edit_password)
        edtConfirm = findViewById(R.id.edt_comfirm_password)
        btnSave = findViewById(R.id.btn_save_password)


        edtPassword.setupPasswordToggle(
            visibleIcon = R.drawable.ic_show_password,
            hiddenIcon = R.drawable.ic_hide_password
        )

        edtConfirm.setupPasswordToggle(
            visibleIcon = R.drawable.ic_show_password,
            hiddenIcon = R.drawable.ic_hide_password
        )


        btnSave.setOnClickListener {
            val pass = edtPassword.text.toString()
            val confirm = edtConfirm.text.toString()

            if (pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PasswordHelper.setPassword(this, pass)
            Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    private fun EditText.setupPasswordToggle(visibleIcon: Int, hiddenIcon: Int) {
        var isPasswordVisible = false

        setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = compoundDrawablesRelative[2] // drawableEnd = index 2
                if (drawableEnd != null) {
                    val drawableWidth = drawableEnd.bounds.width()
                    if (event.rawX >= (right - drawableWidth - paddingEnd)) {
                        isPasswordVisible = !isPasswordVisible

                        inputType = if (isPasswordVisible) {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        } else {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }

                        // Giữ nguyên con trỏ khi thay đổi inputType
                        setSelection(text?.length ?: 0)

                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            compoundDrawablesRelative[0], // drawableStart
                            null,
                            ContextCompat.getDrawable(context, if (isPasswordVisible) visibleIcon else hiddenIcon),
                            null
                        )

                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun showPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_password, null)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edit_password_dialog)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_open_password)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        dialog.show() // PHẢI gọi show() trước khi chỉnh window

        // Làm mờ nền và bo góc sau khi show()
        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
            attributes = attributes.apply {
                dimAmount = 0.95f // Làm mờ nền 50%
            }
        }

        edtPassword.setupPasswordToggle(
            visibleIcon = R.drawable.ic_show_password,
            hiddenIcon = R.drawable.ic_hide_password
        )


        btnConfirm.setOnClickListener {
            val input = edtPassword.text.toString()
            val savedPassword = PasswordHelper.getPassword(this)

            if (input == savedPassword) {
                val i  =Intent(this, MainActivity::class.java)
                startActivity(i)
                finish()
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
            }
        }
    }



}