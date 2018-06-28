package com.rod.flowlayoutmanager.sticky

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import com.rod.flowlayoutmanager.common.DataFactory
import org.jetbrains.anko.*

/**
 * No pains, no gains.
 *
 * Created by Rod on 2018/6/28.
 */
class StickyActivity : AppCompatActivity() {

    private lateinit var mListView: ListView
    private lateinit var mStickyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
    }

    private fun initView() {
        frameLayout {
            mListView = listView {
                adapter = ArrayAdapter<String>(this@StickyActivity, android.R.layout.simple_list_item_1, android.R.id.text1, DataFactory.getStringList(50))
            }

            mStickyContainer = linearLayout {
                lparams(matchParent, wrapContent)
                orientation = LinearLayout.VERTICAL
            }
        }
    }
}