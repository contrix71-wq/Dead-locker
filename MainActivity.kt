package com.deadlock.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {
    private val pink = Color.rgb(255, 43, 145)
    private val black = Color.rgb(5, 5, 5)
    private var entered = ""
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showPinScreen()
    }

    private fun immersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    private fun base(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(black)
        }
        return root
    }

    private fun label(text: String, size: Float, color: Int = Color.WHITE): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    }

    private fun button(text: String, action: () -> Unit): Button = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35, 25, 32)))
        textSize = 16f
        setOnClickListener { action() }
    }

    private fun showPinScreen() {
        immersive()
        entered = ""
        base()
        root.addView(label("DEAD LOCK", 32f, pink))
        root.addView(label("ВВЕДИТЕ КОД ДОСТУПА", 14f), LinearLayout.LayoutParams(-1, 64))
        val digits = label("••••", 28f, pink)
        root.addView(digits, LinearLayout.LayoutParams(-1, 64))
        val grid = GridLayout(this).apply { columnCount = 3 }
        for (n in 1..9) grid.addView(key(n.toString(), digits), GridLayout.LayoutParams().apply {
            width = 0; height = 76; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        })
        grid.addView(key("⌫", digits) { if (entered.isNotEmpty()) entered = entered.dropLast(1); digits.text = "•".repeat(entered.length).ifEmpty { "••••" } }, GridLayout.LayoutParams().apply { width = 0; height = 76; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) })
        grid.addView(key("0", digits), GridLayout.LayoutParams().apply { width = 0; height = 76; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) })
        grid.addView(key("✓", digits) { if (entered == "4928") playIntro() else { entered = ""; digits.text = "НЕВЕРНЫЙ КОД" } }, GridLayout.LayoutParams().apply { width = 0; height = 76; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) })
        root.addView(grid, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun key(value: String, display: TextView, custom: (() -> Unit)? = null): TextView = TextView(this).apply {
        text = value; textSize = 25f; gravity = Gravity.CENTER; setTextColor(pink)
        setOnClickListener {
            if (custom != null) custom.invoke() else if (entered.length < 4) {
                entered += value
                display.text = "•".repeat(entered.length)
                if (entered.length == 4) {
                    if (entered == "4928") playIntro()
                    else { entered = ""; display.text = "НЕВЕРНЫЙ КОД" }
                }
            }
        }
    }

    private fun playIntro() {
        immersive()
        val video = VideoView(this)
        video.setBackgroundColor(Color.BLACK)
        setContentView(video)
        video.setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.intro}"))
        video.setOnCompletionListener { showHome() }
        video.setOnErrorListener { _, _, _ -> showHome(); true }
        video.start()
    }

    private fun showHome() {
        immersive()
        base()
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("DEAD LOCK", 24f, pink), LinearLayout.LayoutParams(0, 64, 1f))
        top.addView(button("⋮") { showMenu() }, LinearLayout.LayoutParams(64, 56))
        root.addView(top)
        root.addView(label("ПРИЛОЖЕНИЯ", 13f, pink), LinearLayout.LayoutParams(-1, 48))
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        for (app in apps) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(8, 8, 8, 8) }
            val icon = ImageView(this).apply { setImageDrawable(app.loadIcon(pm)) }; row.addView(icon, LinearLayout.LayoutParams(48, 48))
            val name = TextView(this).apply { text = app.loadLabel(pm); textSize = 17f; setTextColor(Color.WHITE); setPadding(16, 0, 0, 0) }
            row.addView(name, LinearLayout.LayoutParams(0, 56, 1f))
            row.setOnClickListener { try { startActivity(pm.getLaunchIntentForPackage(app.activityInfo.packageName)) } catch (_: Exception) {} }
            list.addView(row)
        }
        setContentView(root)
    }

    private fun showMenu() {
        val options = arrayOf("Помилование", "Настройки Android")
        android.app.AlertDialog.Builder(this)
            .setTitle("DEAD LOCK")
            .setItems(options) { _, which -> if (which == 0) showPardon() else startActivity(Intent(Settings.ACTION_SETTINGS)) }
            .setNegativeButton("Закрыть", null).show()
    }

    private fun showPardon() {
        val input = EditText(this).apply {
            hint = "Код разблокировки"
            inputType = 2
            setTextColor(Color.WHITE)
        }
        val wrap = LinearLayout(this).apply { setPadding(24, 8, 24, 0); addView(input) }
        android.app.AlertDialog.Builder(this)
            .setTitle("Помилование")
            .setMessage("Введите код, чтобы открыть настройки выбора штатного лаунчера.")
            .setView(wrap)
            .setPositiveButton("Разблокировать") { _, _ ->
                if (input.text.toString() == "7610") {
                    try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
                } else Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null).show()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() { super.onBackPressed() }
}
