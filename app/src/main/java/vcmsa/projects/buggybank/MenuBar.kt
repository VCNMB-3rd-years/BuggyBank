package vcmsa.projects.buggybank



import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.Visibility
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.io.FileOutputStream

private val FragReport = ReportFragment()
private val FragAnalysis = AnalysisFragment()
private val FragDashboard = MainPageFragment()
private val FragCreatePopUp = CreatPopUpFragment()
private val FragTransactionRecords = TransactionRecords()
private val FragSetABudget = SetBudgetFragment()
private val FragCalculator = CalculatorFragment()
private val FragCurrencyConvertor = CurrencyConverterFragment()
private val FragProf = FragProfile()
//private val FragSettings = SettingsFragment()
private val FragLogout = logoutFragment()
private val FragBudgetBuddyWelcome = BudgetBuddyWelcomeFragment()
private val FragProgress = BudgetProgressBar()


class MenuBar : AppCompatActivity() {

    lateinit var navToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menubar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val sideNavView: NavigationView = findViewById(R.id.sideMenubar)


        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.bringToFront()

        replaceFrag(FragDashboard)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        sideNavView.bringToFront()
        drawerLayout.requestLayout()

//Bottom menu bar nav code
        val bottomBar = findViewById<BottomNavigationView>(R.id.NavBar)

        // navigation for bottom nav bar
        bottomBar.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.ic_home -> {
                    val fadeOut = AlphaAnimation(1f, 0f).apply {
                        duration = 150
                        fillAfter = true
                    }
                    val fadeIn = AlphaAnimation(0f, 1f).apply {
                        duration = 150
                        fillAfter = true
                        startOffset = 150
                    }
                    FragDashboard.view?.startAnimation(fadeOut)
                    FragDashboard.view?.postDelayed({ FragDashboard.view?.startAnimation(fadeIn) }, 150)
                    replaceFrag(FragDashboard)
                }
                R.id.ic_analysis -> {
                    val fadeOut = AlphaAnimation(1f, 0f).apply {
                        duration = 150
                        fillAfter = true
                    }
                    val fadeIn = AlphaAnimation(0f, 1f).apply {
                        duration = 150
                        fillAfter = true
                        startOffset = 150
                    }
                    FragAnalysis.view?.startAnimation(fadeOut)
                    FragAnalysis.view?.postDelayed({ FragAnalysis.view?.startAnimation(fadeIn) }, 150)
                    replaceFrag(FragAnalysis)
                }
                R.id.ic_create -> {
                    val showPopUp = FragCreatePopUp
                    showPopUp.show(supportFragmentManager, "showPopUp")
                }

                R.id.ic_transactions -> {
                    val fadeOut = AlphaAnimation(1f, 0f).apply {
                        duration = 150
                        fillAfter = true
                    }
                    val fadeIn = AlphaAnimation(0f, 1f).apply {
                        duration = 150
                        fillAfter = true
                        startOffset = 150
                    }
                    FragTransactionRecords.view?.startAnimation(fadeOut)
                    FragTransactionRecords.view?.postDelayed({ FragTransactionRecords.view?.startAnimation(fadeIn) }, 150)
                    replaceFrag(FragTransactionRecords)
                }
                R.id.ic_trophies -> Toast.makeText(this, "Trophies coming soon", Toast.LENGTH_LONG).show()
            }

            true
        }

        //Side nav menu bar code
        navToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        navToggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.dark_green)
        drawerLayout.addDrawerListener(navToggle)
        navToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

            sideNavView.setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.is_setABudget -> {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragSetABudget.view?.startAnimation(fadeOut)
                        FragSetABudget.view?.postDelayed({ FragSetABudget.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragSetABudget)
                    }
                    R.id.is_reports -> {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragReport.view?.startAnimation(fadeOut)
                        FragReport.view?.postDelayed({ FragReport.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragReport)
                    }
                    R.id.is_progress -> {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragCalculator.view?.startAnimation(fadeOut)
                        FragCalculator.view?.postDelayed({ FragCalculator.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragProgress)
                    }
                    R.id.is_budgetBuddy -> {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragReport.view?.startAnimation(fadeOut)
                        FragReport.view?.postDelayed({ FragReport.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragBudgetBuddyWelcome)
                    }
                    R.id.is_calculator -> {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragCalculator.view?.startAnimation(fadeOut)
                        FragCalculator.view?.postDelayed({ FragCalculator.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragCalculator)
                  }
                    R.id.is_Profile -> {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragProf.view?.startAnimation(fadeOut)
                        FragProf.view?.postDelayed({ FragProf.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragProf)
                    }
                    R.id.is_currencyConvertor -> {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragCurrencyConvertor.view?.startAnimation(fadeOut)
                        FragCurrencyConvertor.view?.postDelayed({ FragCurrencyConvertor.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragCurrencyConvertor)
                    }
                    R.id.is_switchAccount -> Toast.makeText(this, "Switch account coming soon", Toast.LENGTH_LONG).show()
                    R.id.is_budgetBuddy ->
                    {
                        val fadeOut = AlphaAnimation(1f, 0f).apply {
                            duration = 150
                            fillAfter = true
                        }
                        val fadeIn = AlphaAnimation(0f, 1f).apply {
                            duration = 150
                            fillAfter = true
                            startOffset = 150
                        }
                        FragCurrencyConvertor.view?.startAnimation(fadeOut)
                        FragCurrencyConvertor.view?.postDelayed({ FragCurrencyConvertor.view?.startAnimation(fadeIn) }, 150)
                        replaceFrag(FragBudgetBuddyWelcome)}

                R.id.is_logut -> {


                    val fadeOut = AlphaAnimation(1f, 0f).apply {
                        duration = 150
                        fillAfter = true
                    }
                    val fadeIn = AlphaAnimation(0f, 1f).apply {
                        duration = 150
                        fillAfter = true
                        startOffset = 150
                    }
                    FragLogout.view?.startAnimation(fadeOut)
                    FragLogout.view?.postDelayed({ FragLogout.view?.startAnimation(fadeIn) }, 150)
                    val showPopUp = FragLogout
                    showPopUp.show(supportFragmentManager,"showPopUp")
                    replaceFrag(FragLogout)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true

        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {

                    finish()
                }
            }
        })

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)

            val bottomBar = findViewById<BottomNavigationView>(R.id.NavBar)

            when (currentFragment) {
                is MainPageFragment -> bottomBar.menu.findItem(R.id.ic_home).isChecked = true
                is AnalysisFragment -> bottomBar.menu.findItem(R.id.ic_analysis).isChecked = true
                is TransactionRecords -> bottomBar.menu.findItem(R.id.ic_transactions).isChecked = true

            }


            val btnBack = findViewById<ImageButton>(R.id.btnBack)
            if (currentFragment is MainPageFragment) {
                btnBack.visibility = ImageButton.GONE
            } else {
                btnBack.visibility = ImageButton.VISIBLE
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (navToggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun replaceFrag(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)

//ensures that we don't load the same page multiple times and filling the backstack with the same page
        if (currentFragment != null && currentFragment::class == fragment::class) return

        val transaction = supportFragmentManager.beginTransaction()
        lifecycleScope.launch {
            transaction.replace(R.id.fragmentContainerView, fragment)
                .addToBackStack(null) // Adds new page to the back stack
                .commit()
        }

        supportFragmentManager.executePendingTransactions()

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        if (fragment == FragDashboard) {
            btnBack.visibility = ImageButton.GONE
        } else {
            btnBack.visibility = ImageButton.VISIBLE
        }
    }


    fun createPDF(transactions: List<Transaction>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        paint.textSize = 12f

        var y = 30f
        canvas.drawText("BuggyBank Expense Report", 10f, y, paint)
        y += 20f
//loop each transaction within db
        transactions.forEach {
            canvas.drawText(
                "${it.date}: ${it.description} - R${it.amount}",
                10f,
                y,
                paint
            )
            y += 20
        }


        pdfDocument.finishPage(page)

        val docsFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (docsFolder != null && !docsFolder.exists()) {
            docsFolder.mkdirs()
        }

        val file = File(docsFolder, "Report.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(this, "PDF saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()

            val uri: Uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                file
            )

            val openIntent = Intent(Intent.ACTION_VIEW)
            openIntent.setDataAndType(uri, "application/pdf")
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(openIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
