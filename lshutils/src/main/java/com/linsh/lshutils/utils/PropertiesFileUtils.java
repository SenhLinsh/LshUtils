package com.linsh.lshutils.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.linsh.utilseverywhere.FileUtils;
import com.linsh.lshutils.base.JsonBean;

import java.io.File;

/**
 * <pre>
 *    author : Senh Linsh
 *    github : https://github.com/SenhLinsh
 *    date   : 2017/11/10
 *    desc   : 工具类: 快速存储配置信息到外部储存
 *             注意  : 如果存储的是JsonBean, 则需要对其进行免混淆, 否则会解析错误无法存取
 * </pre>
 */
public class PropertiesFileUtils {

    /**
     * 获取配置文件所处文件夹
     */
    private static File getPropertyDir() {
        return FileManagerUtils.getDir("properties");
    }

    /**
     * 获取配置文件
     *
     * @param classOfT 配置对应的类
     * @return 配置文件
     */
    public static <T extends JsonBean> File getPropertyFile(Class<T> classOfT) {
        return FileManagerUtils.getFile(getPropertyDir(), classOfT.getSimpleName());
    }

    /**
     * 获取配置文件
     *
     * @param fileName 配置文件名
     * @return 配置文件
     */
    public static File getPropertyFile(String fileName) {
        return FileManagerUtils.getFile(getPropertyDir(), fileName);
    }

    /**
     * 储存 JsonObject 到配置文件中
     * <p>配置文件的名称为 JsonObject 的类名
     *
     * @param jsonBean JsonObject
     */
    public static <T extends JsonBean> void putObject(T jsonBean) {
        if (jsonBean == null) return;
        String json = new Gson().toJson(jsonBean);
        putString(jsonBean.getClass().getSimpleName(), json);
    }

    /**
     * 储存文本到配置文件中
     *
     * @param fileName 文件名
     * @param content  文本内容
     */
    public static void putString(String fileName, String content) {
        File file = getPropertyFile(fileName);
        if (content != null && content.length() > 0) {
            try {
                FileUtils.writeFile(file, content, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取 JsonObject 对应的配置
     *
     * @param classOfT JsonObject 类
     * @return JsonObject
     */
    public static <T extends JsonBean> T getObject(Class<T> classOfT) {
        T t = null;
        String json = getString(classOfT.getSimpleName());
        if (json == null || json.length() == 0)
            return null;
        try {
            t = new Gson().fromJson(json, classOfT);
        } catch (Exception e) {
            Log.w("PropertiesFileUtils", "解析Json出错");
        }
        return t;
    }

    /**
     * 获取配置文件中的文本
     *
     * @param fileName 配置文件名
     * @return 配置文本
     */
    public static String getString(String fileName) {
        File file = getPropertyFile(fileName);
        if (!file.exists()) return null;
        StringBuilder stringBuilder = FileUtils.readFile(file);
        return stringBuilder == null ? null : stringBuilder.toString();
    }
}
