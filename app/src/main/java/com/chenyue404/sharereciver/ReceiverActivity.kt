package com.chenyue404.sharereciver

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.util.*

private const val OPEN_DOCUMENT_REQUEST_CODE = 0x33

class ReceiverActivity : Activity() {

    private var preAddText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        val shareText: String? =
            intent.run {
                if (action == Intent.ACTION_SEND
                    && type == "text/plain"
                ) {
                    getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
        shareText?.let {
            log("收到$it")
            preAddText = shareText
        } ?: kotlin.run {
            log("没读取到任何内容")
            finish()
            return
        }

        val fromAlias = intent.component?.className?.contains("alias", true) == true

        val persistedUriPermissions = contentResolver.persistedUriPermissions

        val uri = persistedUriPermissions.firstOrNull()?.uri
        uri?.let {
            addText(it, preAddText ?: "")
        } ?: kotlin.run {
            Toast.makeText(this, "请选择一个文件用于保存", Toast.LENGTH_SHORT).show()
            pickFile()
        }
    }

    private fun log(str: String) {
        Log.d("ReceiverActivity", str)
    }

    private fun pickFile() {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }.let {
            startActivityForResult(it, OPEN_DOCUMENT_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode != OPEN_DOCUMENT_REQUEST_CODE
            || resultCode != RESULT_OK
        ) {
            finish()
            return
        }

        resultData?.data?.also {
            log("uri=$it")
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            addText(it, preAddText ?: return)
        }
    }

    private fun addText(uri: Uri, str: String) {
        val currentTime = DateFormat.getDateTimeInstance().format(Date())
        val finalStr = "$currentTime\n$str\n\n"

        fun saveFailed(errStr: String) {
            Toast.makeText(this, errStr, Toast.LENGTH_SHORT).show()
        }

        try {
            contentResolver.openFileDescriptor(uri, "wa")?.use { parcelFileDescriptor ->
                FileOutputStream(parcelFileDescriptor.fileDescriptor).use {
                    it.write(finalStr.toByteArray())
                }
            }
            preAddText = null
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            saveFailed(e.toString())
            e.printStackTrace()
        } catch (e: IOException) {
            saveFailed(e.toString())
            e.printStackTrace()
        } finally {
            finish()
        }
    }
}