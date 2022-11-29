package org.xbet.debugpartitionstorage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }


    fun cunshuquanxian(view: View) {

        val checkSelfPermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val checkSelfPermission2 =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (checkSelfPermission == PackageManager.PERMISSION_DENIED || checkSelfPermission2 == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1002
            )
        } else {
            Toast.makeText(this, "有权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1002) {
            val find = grantResults.find {
                it == PackageManager.PERMISSION_DENIED
            }
            if (find == null) {
                val gaid = "12345"
                val initData = SDIDManager.weHaveAccess(this, gaid, sExecutorService)
                App.toast("获取的值是：$initData")

                Toast.makeText(this, "有权限", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private val sExecutorService = ThreadPoolExecutor(
        1, 2,
        0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(8)
    ) { r, executor -> }

    fun login(view: View) {
        val gaid = "12345"
        val initData = SDIDManager.login(this, gaid, sExecutorService)
        App.toast("获取的值是：$initData")

    }

    fun cunshuquanxia2n(view: View) {
        val gaid = "12345"
        val initData = SDIDManager.weHaveAccess(this, gaid, sExecutorService)
        App.toast("获取的值是：$initData")
    }
}