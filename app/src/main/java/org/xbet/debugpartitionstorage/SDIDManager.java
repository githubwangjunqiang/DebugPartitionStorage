package org.xbet.debugpartitionstorage;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Android-小强 on 2022/11/16.
 * mailbox:980766134@qq.com
 * description:
 */
public class SDIDManager {
    private static final String TAG = "12345";
    /**
     * 标识后后缀
     */
    private static final String suffix = "GAPPX";
    /**
     * 密钥原文，加密时会将此值 md5
     */
    private static final String secretKey = "GAPPX";
    /**
     * 存储的异步线程
     */
    private static FutureTask<Boolean> mSaveFutureTask;

    /**
     * 外部 登录时调用
     *
     * @return
     */
    public static String login(Activity activity, String gaid, ThreadPoolExecutor sExecutorService) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            //大于Android10
            String s = Utils.loadSdId(activity.getApplicationContext());
            if (TextUtils.isEmpty(s)) {
                Utils.saveSdId(activity.getApplicationContext(), gaid, sExecutorService);
            }
            return s;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //先判断权限 Android 6-10
            int read = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            int write = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (PackageManager.PERMISSION_GRANTED == read && PackageManager.PERMISSION_GRANTED == write) {
                //有权限
                String s = Utils.loadSdId(activity.getApplicationContext());
                if (TextUtils.isEmpty(s)) {
                    Utils.saveSdId(activity.getApplicationContext(), gaid, sExecutorService);
                }
                return s;
            } else {
                //没有权限
                return null;
            }
        } else {
            //Android 5
            String s = Utils.loadSdId(activity.getApplicationContext());
            if (TextUtils.isEmpty(s)) {
                Utils.saveSdId(activity.getApplicationContext(), gaid, sExecutorService);
            }
            return s;
        }
    }

//    /**
//     * 权限被申请了 需要触发一下
//     */
//    public static String weHaveAccess(Activity activity, String gaid, ThreadPoolExecutor sExecutorService) {
//
//    }

    private static class Utils {
        /**
         * 存入 外部磁盘文件
         *
         * @param context
         * @param gaid    谷歌广告id
         */
        public static final void saveSdId(Context context, String gaid, ThreadPoolExecutor executor) {

            if (mSaveFutureTask != null && !mSaveFutureTask.isDone()) {
                mSaveFutureTask.cancel(true);
            }
            mSaveFutureTask = new FutureTask<>(() -> {
                while (true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (saveFile10(context, gaid)) {
                            break;
                        }
                    } else {
                        if (saveFile9(context, gaid)) {
                            break;
                        }
                    }
                    Thread.sleep(3000);
                }
                return true;
            });
            executor.execute(mSaveFutureTask);

        }

        /**
         * 获取 sdid
         *
         * @param context
         */
        public static final String loadSdId(Context context) {
            String loadGaid = null;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
//                13及以上
                loadGaid = loadFile9(context);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
//                12
                loadGaid = loadFile9(context);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
//                11
                loadGaid = loadFile9(context);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
//                10
                loadGaid = loadFile9(context);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                6 以下
                loadGaid = loadFile9(context);
            } else {
                //                6-9
                loadGaid = loadFile9(context);
            }
            return loadGaid;

        }

        /**
         * 获取 sdid
         *
         * @param context
         * @return
         */
        @RequiresApi(api = Build.VERSION_CODES.Q)
        private static CharSequence loadFile10(Context context) {
            Cursor cursor = null;
            try {
                ContentResolver resolver = context.getContentResolver();
                Uri external = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                String[] projection = new String[]{MediaStore.Downloads._ID};
                cursor = resolver.query(external, null, null, null, null);
                Uri imageUri = null;
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        imageUri = ContentUris.withAppendedId(external, cursor.getLong(0));

                    } while (cursor.moveToNext());
                } else {
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            return null;
        }

        /**
         * 获取 sdid
         *
         * @param context
         * @return
         */
        private static String loadFile9(Context context) {
            String success = null;
            BufferedReader reader = null;
            try {
                File externalStoragePublicDirectory =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!externalStoragePublicDirectory.exists()) {
                    externalStoragePublicDirectory.mkdirs();
                }
                List<String> list = new ArrayList<>();
                externalStoragePublicDirectory.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        list.add(pathname.getName());
                        return false;
                    }
                });

                if (!list.isEmpty()) {
                    long time = 0;
                    for (String data : list) {
                        if (TextUtils.isEmpty(data)) {
                            continue;
                        }
                        try {
                            String md5Suffix = md5Data(suffix);
                            if (data.length() > md5Suffix.length() && data.endsWith(md5Suffix)) {
                                String substring = data.substring(0, data.length() - md5Suffix.length());
                                String[] strings = AESUtils.loadKeyAndVector(secretKey);
                                String msg = AESUtils.decryptData(strings[0], strings[1], substring);
                                String[] split = msg.split(suffix);
                                String oldSdid = split[0];
                                String didTime = split[1];
                                long lDidTime = Long.parseLong(didTime);
                                if (lDidTime > time) {
                                    success = oldSdid;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return success;
        }


        /**
         * Android10以及以上存储
         *
         * @param context
         * @param gaid
         * @return
         */
        @RequiresApi(api = Build.VERSION_CODES.Q)
        private static boolean saveFile10(Context context, String gaid) {
            boolean success = false;
            Uri imgUri = null;
            OutputStream os = null;

            try {
                String content = encodingGAID(context, gaid);
                long timeMillis = System.currentTimeMillis();
                ContentValues values = new ContentValues();

                values.put(MediaStore.Downloads.DISPLAY_NAME, content + "");
                values.put(MediaStore.Downloads.MIME_TYPE, "txt/*");
                values.put(MediaStore.Downloads.TITLE, content);
                values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/" + content
                );
                Uri external = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                ContentResolver resolver = context.getContentResolver();

                Uri insertUri = resolver.insert(external, values);
                Log.d(TAG, "saveFile10: " + insertUri);
                if (insertUri != null) {
                    os = resolver.openOutputStream(insertUri);
                }
                if (os != null) {
                    os.write(content.getBytes(StandardCharsets.UTF_8));
                    imgUri = insertUri;
                    success = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "fail: " + e.getLocalizedMessage());
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "fail in close:: " + e.getCause());
                }
            }

            return success;
        }

        /**
         * Android10以下存储
         *
         * @param context
         * @param gaid
         * @return
         */
        private static boolean saveFile9(Context context, String gaid) {
            boolean success = false;
            FileOutputStream fileOutputStream = null;
            try {
                File externalStoragePublicDirectory =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!externalStoragePublicDirectory.exists()) {
                    externalStoragePublicDirectory.mkdirs();
                }
                String content = encodingGAID(context, gaid);
                File file = new File(externalStoragePublicDirectory, content);
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.flush();
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return success;
        }


//    /**
//     * 文件夹名称
//     *
//     * @param context
//     * @return
//     */
//    private static String loadFileDocName(Context context) {
//        String packageName = context.getPackageName();
//        return MD5Utils.md5Data(packageName);
//    }

        /**
         * 生成-sdid
         *
         * @param context
         * @param gaid
         * @return
         */
        private static String encodingGAID(Context context, String gaid) {
            long currentTimeMillis = System.currentTimeMillis();
            String[] loadKeyAndVector = AESUtils.loadKeyAndVector(secretKey);
            String s = AESUtils.encryptData(
                    loadKeyAndVector[0],
                    loadKeyAndVector[1],
                    gaid + suffix + "" + currentTimeMillis
            ).trim() + md5Data(suffix);
            Log.d("12345", "encodingGAID: " + s);
            return s;
        }

        /**
         * @param psd MD5 要加密的对象
         * @return MD5加密后市返回一个32位数的字符串，返回“”，代表加密异常
         */
        private static String md5Data(String psd) {
            try {
                // 1，获取加密算法对象，单利设计模式
                MessageDigest instance = MessageDigest.getInstance("MD5");
                // 2，通过加密算法操作，对psd进行哈希加密操作
                byte[] digest = instance.digest(psd.getBytes());
                StringBuffer sb = new StringBuffer();
                // 循环16次
                for (byte b : digest) {
                    // 获取b的后8位
                    int i = b & 0xff;
                    // 将10进制数，转化为16进制
                    String hexString = Integer.toHexString(i);
                    // 容错处理，长度小于2的，自动补0
                    if (hexString.length() < 2) {
                        hexString = "0" + hexString;
                    }
                    // 把生成的32位字符串添加到stringBuffer中
                    sb.append(hexString);
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        /**
         * base64 转换成 byte数组
         *
         * @param base64String base64编码数据
         * @return 返回 byte数组
         */
        private static byte[] decode(String base64String) {
            try {
                byte[] decode = Base64.decode(base64String, Base64.URL_SAFE);
                return decode;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * byte数组 转换成 base64
         *
         * @param bytes byte数组
         * @return 返回 base64编码
         */
        private static String encode(byte[] bytes) {
            try {
                byte[] encode = Base64.encode(bytes, Base64.URL_SAFE);
                return new String(encode);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    private static class AESUtils {


        /**
         * 默认的加密方式
         */
        public static final String AES_CODE = "AES";
        /**
         * 默认的加密方式 填充方式
         */
        public static final String AES_CBD_CODE = "AES/CBC/PKCS5Padding";

        /**
         * 产生随机密钥(这里产生密钥必须是16位)
         */
        public static String generateKey() {
            String key = UUID.randomUUID().toString();
            // 替换掉-号
            key = key.replace("-", "").substring(0, 16);
            return key;
        }

        /**
         * 获取 AES 加密的 key 和向量
         *
         * @return
         */
        public static String[] loadKeyAndVector(String name) {
            String md5Code = Utils.md5Data(name);
            StringBuilder builderKey = new StringBuilder();
            StringBuilder builderVector = new StringBuilder();
            for (int i = 0; i < md5Code.length(); i++) {
                if (i % 2 == 0) {
                    builderKey.append(md5Code.charAt(i));
                } else {
                    builderVector.append(md5Code.charAt(i));
                }
            }
            return new String[]{builderKey.toString(), builderVector.toString()};
        }

        /**
         * 加密数据
         *
         * @param key     密钥
         * @param vector  向量
         * @param content 内容
         * @return base64 串
         */
        public static String encryptData(String key, String vector, String content) {
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(vector) || TextUtils.isEmpty(content)) {
                return "";
            }
            byte[] encryptedBytes = new byte[0];
            try {
                byte[] byteContent = content.getBytes();
                byte[] enCodeFormat = key.getBytes();
                SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, AES_CODE);
                byte[] initParam = vector.getBytes();
                IvParameterSpec ivParameterSpec = new IvParameterSpec(initParam);
                Cipher cipher = Cipher.getInstance(AES_CBD_CODE);
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
                encryptedBytes = cipher.doFinal(byteContent);
                return Utils.encode(encryptedBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 解密
         *
         * @param key     密钥
         * @param vector  向量
         * @param content 内容
         * @return
         */
        public static String decryptData(String key, String vector, String content) {
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(vector) || TextUtils.isEmpty(content)) {
                return "";
            }
            try {
                byte[] encryptedBytes = Utils.decode(content);
                byte[] enCodeFormat = key.getBytes();
                SecretKeySpec secretKey = new SecretKeySpec(enCodeFormat, AES_CODE);
                byte[] initParam = vector.getBytes();
                IvParameterSpec ivParameterSpec = new IvParameterSpec(initParam);
                Cipher cipher = Cipher.getInstance(AES_CBD_CODE);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
                byte[] result = cipher.doFinal(encryptedBytes);
                return new String(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }


}
