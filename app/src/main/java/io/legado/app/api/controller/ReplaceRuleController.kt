package io.legado.app.api.controller

import io.legado.app.api.ReturnData
import io.legado.app.R
import splitties.init.appCtx
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.replace
import io.legado.app.utils.stackTraceStr

object ReplaceRuleController {

    val allRules: ReturnData
        get() {
            val rules = appDb.replaceRuleDao.all
            val returnData = ReturnData()
            returnData.setData(GSON.toJson(rules))
            return returnData
        }


    fun saveRule(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg(appCtx.getString(R.string.data_not_empty))
        val rule = GSON.fromJsonObject<ReplaceRule>(postData).getOrNull()
        if (rule == null) {
            returnData.setErrorMsg(appCtx.getString(R.string.invalid_format))
        } else {
            if (rule.order == Int.MIN_VALUE) {
                rule.order = appDb.replaceRuleDao.maxOrder + 1
            }
            appDb.replaceRuleDao.insert(rule)
        }
        return returnData
    }


    fun delete(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg(appCtx.getString(R.string.data_not_empty))
        val rule = GSON.fromJsonObject<ReplaceRule>(postData).getOrNull()
        if (rule == null) {
            returnData.setErrorMsg(appCtx.getString(R.string.invalid_format))
        } else {
            appDb.replaceRuleDao.delete(rule)
        }
        return returnData
    }

    /**
     * Pass in test data format
     * {
     *  rule: Replace,
     *  text: "xxx"
     * }
     */
    fun testRule(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg(appCtx.getString(R.string.data_not_empty))
        val map = GSON.fromJsonObject<Map<String, *>>(postData).getOrNull()
        if (map == null) {
            returnData.setErrorMsg(appCtx.getString(R.string.invalid_format))
        } else {
            val rule = map["rule"]?.let {
                if (it is String) {
                    GSON.fromJsonObject<ReplaceRule>(it).getOrNull()
                } else {
                    GSON.fromJsonObject<ReplaceRule>(GSON.toJson(it)).getOrNull()
                }
            }
            if (rule == null) {
                returnData.setErrorMsg(appCtx.getString(R.string.invalid_format))
                return returnData
            }
            if (rule.pattern.isEmpty()) {
                returnData.setErrorMsg(appCtx.getString(R.string.replace_rule_not_empty))
            }
            val text = map["text"] as String
            val content = try {
                if (rule.isRegex) {
                    text.replace(
                        rule.pattern.toRegex(),
                        rule.replacement,
                        rule.getValidTimeoutMillisecond()
                    )
                } else {
                    text.replace(rule.pattern, rule.replacement)
                }
            } catch (e: Exception) {
                e.stackTraceStr
            }
            returnData.setData(content)
        }
        return returnData
    }

}