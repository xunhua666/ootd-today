package com.ootd.today.data.repository

import com.ootd.today.domain.stylist.StylistRequest

/**
 * 大模型数据源。
 *
 * 与其它 Provider 一样，接口只描述「要什么」，不描述「怎么拿到」。
 * 具体是哪家服务商由实现类决定，换服务商只需替换实现。
 *
 * 刻意只返回**原始文本**而不在这里解析：解析逻辑属于领域层
 * （[com.ootd.today.domain.stylist.StylistAdviceParser]），
 * 这样解析规则能独立单测，也不会因为换服务商而重写一遍。
 */
interface StylistProvider {

    /**
     * 是否已具备调用条件（通常是配了 API Key）。
     *
     * 未配置时 UI 应引导用户去设置，而不是发一个必然失败的请求。
     *
     * 做成 suspend 而不是普通函数：判断是否配置需要读 DataStore，
     * 而 DataStore 读盘不能阻塞主线程。
     */
    suspend fun isConfigured(): Boolean

    /**
     * 请求一次建议。
     *
     * @return 模型返回的原始文本；网络失败、鉴权失败、超时都返回 null，
     *   由调用方回退到本地算法。**不抛异常** —— 大模型不可用是常态。
     */
    suspend fun complete(request: StylistRequest): String?
}

/**
 * 未配置或未启用时使用的空实现。
 *
 * 用一个显式的「什么都不做」对象，比在各处判空要好：
 * 好处是注入点永远是同一个类型，UI 只需问 [isConfigured]。
 */
object DisabledStylistProvider : StylistProvider {
    override suspend fun isConfigured(): Boolean = false
    override suspend fun complete(request: StylistRequest): String? = null
}