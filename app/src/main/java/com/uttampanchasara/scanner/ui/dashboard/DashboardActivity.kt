package com.uttampanchasara.scanner.ui.dashboard

import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.View
import android.widget.ListPopupWindow
import com.uttampanchasara.scanner.R
import com.uttampanchasara.scanner.data.DataManager
import com.uttampanchasara.scanner.data.repository.record.RecordData
import com.uttampanchasara.scanner.di.component.ActivityComponent
import com.uttampanchasara.scanner.getDate
import com.uttampanchasara.scanner.getTime
import com.uttampanchasara.scanner.shareRecord
import com.uttampanchasara.scanner.ui.addnew.AddNewActivity
import com.uttampanchasara.scanner.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_dashboard.*
import javax.inject.Inject


class DashboardActivity : BaseActivity(), DashboardView,
        Observer<List<RecordData>>,
        SearchView.OnQueryTextListener,
        DateListAdapter.DateSelectionListener,
        RecordClickListener {

    override fun getLayout(): Int {
        return R.layout.activity_dashboard
    }

    companion object {
        val TAG = "DashboardActivity"
    }

    @Inject
    lateinit var mDataManager: DataManager

    @Inject
    lateinit var mViewModel: DashboardViewModel

    lateinit var mListPopupWindow: ListPopupWindow

    var mSelectedDate: String? = ""

    override fun injectComponents(mActivityComponent: ActivityComponent) {
        mActivityComponent.inject(this)
    }

    lateinit var mAdapter: RecordListAdapter
    lateinit var mDateAdapter: DateListAdapter

    override fun setUp() {
        mViewModel.onAttachView(this)
        setToolbar(toolbar, "Records", false)
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddNewActivity::class.java))
        }

        mDateAdapter = DateListAdapter(this, this)
        mListPopupWindow = ListPopupWindow(this)
        mListPopupWindow.setAdapter(mDateAdapter)
        mListPopupWindow.anchorView = toolbar
        mListPopupWindow.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, android.R.color.transparent)))

        txtSelectDate.setOnClickListener {
            mListPopupWindow.show()
        }

        mAdapter = RecordListAdapter(this)
        rvRecords.adapter = mAdapter
        rvRecords.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        mDataManager.getAllDates().observeForever {
            mDateAdapter.setDataList(it)
        }
        mSelectedDate = getDate(System.currentTimeMillis())
        fetchRecords()
    }

    override fun onPause() {
        mDataManager.getRecords().removeObserver(this)
        mViewModel.onDetachView()
        dismissDialog()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissDialog()
    }

    fun dismissDialog() {
        //dismiss dialog if showing on pause/destroy to avoid window leak issue
        if (mListPopupWindow.isShowing) {
            mListPopupWindow.dismiss()
        }
    }

    override fun onDateSelected(date: String?) {
        mSelectedDate = date
        fetchRecords()

        dismissDialog()
    }

    private fun fetchRecords() {
        txtSelectDate.text = mSelectedDate
        mDataManager.getRecordsFromDate(mSelectedDate).observe(this, this)
    }

    override fun onChanged(records: List<RecordData>?) {
        setDataList(records)
    }

    override fun onRecordShare(recordData: RecordData) {

        val shareData: String?
        val address: String? = if (recordData.address.isNotEmpty()) {
            "Address: " + recordData.address
        } else {
            "Address: NA"
        }
        val number: String? = if (recordData.number.isNotEmpty()) {
            "Number: " + recordData.number
        } else {
            "Number: NA"
        }

        shareData = "Name: " + recordData.name +
                "\nCode: " + recordData.code +
                "\n" + number +
                "\n" + address +
                "\nTime: " + getTime(recordData.time)

        shareRecord(shareData)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.menu_dashboard, menu)

        val searchItem = menu.findItem(R.id.action_search)

        var searchView: SearchView? = null
        if (searchItem != null) {
            searchView = searchItem.actionView as SearchView
            searchView.setOnQueryTextListener(this)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(query: String?): Boolean {
        mViewModel.searchRecord("%$query%", mSelectedDate)
        return true
    }

    override fun onSearchResult(records: List<RecordData>?) {
        setDataList(records)
    }

    private fun setDataList(records: List<RecordData>?) {
        val list = records!!
        if (list.isEmpty()) {
            placeholder.visibility = View.VISIBLE
        } else {
            placeholder.visibility = View.GONE
        }
        mAdapter.setData(list)
    }
}
