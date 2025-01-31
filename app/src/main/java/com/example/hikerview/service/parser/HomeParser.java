package com.example.hikerview.service.parser;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.constants.ArticleColTypeEnum;
import com.example.hikerview.model.MovieRule;
import com.example.hikerview.ui.base.BaseCallback;
import com.example.hikerview.ui.home.enums.HomeActionEnum;
import com.example.hikerview.ui.home.model.ArticleList;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.utils.FilterUtil;
import com.example.hikerview.utils.StringUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2018/10/22
 * 时间：At 14:57
 */
public class HomeParser {
    private static final String TAG = "HomeParser";

    private static boolean canAdd(String title) {
        return !FilterUtil.hasFilterWord(title);
    }

    public static void findList(String url, ArticleListRule movieChoose, String rule, String s, boolean newLoad, BaseCallback<ArticleList> callback) {
        if (rule.startsWith("js:")) {
            JsEngineBridge.parseHomeCallBack(url, movieChoose.getCol_type(), movieChoose, rule, s, newLoad, callback);
            return;
        }
        String originalRule = rule + "";
        String[] rules = originalRule.split("==>");
        rule = rules[0];
        String baseUrl = StringUtil.getBaseUrl(movieChoose.getUrl());
        MovieRule movieInfo = new MovieRule();
        movieInfo.setBaseUrl(baseUrl);
        movieInfo.setSearchUrl(movieChoose.getUrl());
        try {
            Document doc = Jsoup.parse(s);
            List<ArticleList> lists = new ArrayList<>();
            String[] ss = rule.split(";");
            //循环获取
            Elements elements = new Elements();
            String[] ss2 = ss[0].split("&&");
            Element element;
            element = CommonParser.getTrueElement(ss2[0], doc);
            for (int i = 1; i < ss2.length - 1; i++) {
                element = CommonParser.getTrueElement(ss2[i], element);
            }
            elements.addAll(CommonParser.selectElements(element, ss2[ss2.length - 1]));
            int fyIndex = 0;
            //获取详情
            for (Element elementt : elements) {
                String moreRule = "";
                rules = originalRule.split("==>");
                if (rules.length > 1) {
                    rules[1] = rules[1].replace("fyIndex", String.valueOf(fyIndex));
                    moreRule = StringUtil.arrayToString(rules, 1, "==>");
                }
                try {
                    ArticleList listBean = new ArticleList();
                    if (!TextUtils.isEmpty(movieChoose.getCol_type())) {
                        listBean.setType(movieChoose.getCol_type());
                    } else {
                        listBean.setType(ArticleColTypeEnum.MOVIE_3.getCode());
                    }
                    //获取名字
                    if (SettingConfig.developerMode) {
                        try {
                            getTitle(listBean, movieChoose, movieInfo, elementt, ss[1]);
                        } catch (Exception e) {
                            listBean.setTitle("");
                        }
                    } else {
                        getTitle(listBean, movieChoose, movieInfo, elementt, ss[1]);
                    }
                    //获取图片链接
                    if (SettingConfig.developerMode) {
                        try {
                            getPicUrl(listBean, movieChoose, movieInfo, elementt, ss[2]);
                        } catch (Exception e) {
                            listBean.setPic("");
                        }
                    } else {
                        getPicUrl(listBean, movieChoose, movieInfo, elementt, ss[2]);
                    }
                    //获取详情
                    if (SettingConfig.developerMode) {
                        try {
                            getDesc(listBean, movieChoose, movieInfo, elementt, ss[3]);
                        } catch (Exception e) {
                            listBean.setDesc("");
                        }
                    } else {
                        getDesc(listBean, movieChoose, movieInfo, elementt, ss[3]);
                    }
                    //获取链接
                    String urlRule = StringUtil.arrayToString(ss, 4, ss.length, "&&");
                    if (SettingConfig.developerMode) {
                        try {
                            getUrl(url, listBean, movieInfo, elementt, urlRule, ss, moreRule);
                        } catch (Exception e) {
                            listBean.setUrl("*");
                        }
                    } else {
                        getUrl(url, listBean, movieInfo, elementt, urlRule, ss, moreRule);
                    }
                    if (canAdd(listBean.getTitle())) {
                        lists.add(listBean);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                fyIndex++;
            }
            if (newLoad) {
                callback.bindArrayToView(HomeActionEnum.ARTICLE_LIST_NEW, lists);
            } else {
                callback.bindArrayToView(HomeActionEnum.ARTICLE_LIST, lists);
            }
        } catch (Exception e) {
            e.printStackTrace();
            callback.error("解析失败", e.toString(), "404", e);
            JSEngine.getInstance().log(e.getMessage(), JSON.toJSON(movieChoose));
        }
    }

    private static void getUrl(String url, ArticleList listBean, MovieRule movieInfo, Element elementt, String rule, String[] allRules, String moreRule) {
        if (allRules.length > 4) {
            if ("*".equals(rule)) {
                listBean.setUrl(url);
            } else {
                String[] ss6 = rule.split("&&");
                Element element5;
                if (ss6.length == 1) {
                    element5 = elementt;
                } else {
                    element5 = CommonParser.getTrueElement(ss6[0], elementt);
                }
                for (int i = 1; i < ss6.length - 1; i++) {
                    element5 = CommonParser.getTrueElement(ss6[i], element5);
                }
                listBean.setUrl(CommonParser.getUrl(element5, ss6[ss6.length - 1], movieInfo, movieInfo.getChapterUrl()));
            }
            if (StringUtil.isNotEmpty(moreRule)) {
//                Log.d(TAG, "getUrl: " + moreRule);
                listBean.setUrl(listBean.getUrl() + "@rule=" + moreRule);
            }
        }
    }

    private static void getDesc(ArticleList listBean, ArticleListRule movieChoose, MovieRule movieInfo, Element elementt, String rule) {
        String[] ss5 = rule.split("&&");
        Element element4;
        if (ss5.length == 1) {
            element4 = elementt;
        } else {
            element4 = CommonParser.getTrueElement(ss5[0], elementt);
        }
        for (int i = 1; i < ss5.length - 1; i++) {
            element4 = CommonParser.getTrueElement(ss5[i], element4);
        }
        listBean.setDesc(CommonParser.getText(element4, ss5[ss5.length - 1]));
    }

    private static void getPicUrl(ArticleList listBean, ArticleListRule movieChoose, MovieRule movieInfo, Element elementt, String rule) {
        if ("*".equals(rule)) {
            listBean.setPic("*");
            if (movieChoose.getCol_type().equals(ArticleColTypeEnum.MOVIE_1.getCode())) {
                listBean.setType(ArticleColTypeEnum.TEXT_1.getCode());
            }
        } else {
            String[] ss4 = rule.split("&&");
            Element element3;
            if (ss4.length == 1) {
                element3 = elementt;
            } else {
                element3 = CommonParser.getTrueElement(ss4[0], elementt);
            }
            for (int i = 1; i < ss4.length - 1; i++) {
                element3 = CommonParser.getTrueElement(ss4[i], element3);
            }
            listBean.setPic(CommonParser.getUrl(element3, ss4[ss4.length - 1], movieInfo, movieInfo.getChapterUrl()));
        }
    }

    private static void getTitle(ArticleList listBean, ArticleListRule movieChoose, MovieRule movieInfo, Element elementt, String rule) {
        String[] ss3 = rule.split("&&");
        Element element2;
        if (ss3.length == 1) {
            element2 = elementt;
        } else {
            element2 = CommonParser.getTrueElement(ss3[0], elementt);
        }
        for (int i = 1; i < ss3.length - 1; i++) {
            element2 = CommonParser.getTrueElement(ss3[i], element2);
        }
        listBean.setTitle(CommonParser.getText(element2, ss3[ss3.length - 1]));
    }
}
