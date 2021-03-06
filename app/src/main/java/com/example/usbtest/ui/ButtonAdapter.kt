package com.example.usbtest.ui

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.example.usbtest.R

class ButtonAdapter : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_btn) {

    override fun convert(helper: BaseViewHolder, item: String?) {
        helper.setText(R.id.bt_compat, item)
    }
}