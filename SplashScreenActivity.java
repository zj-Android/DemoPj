/**
 * 
 */
package cn.com.jiujiang.ui;

import java.io.File;
import java.io.InputStream;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import cn.com.core.entity.CacheUpdateInfo;
import cn.com.core.init.UpdateUtil;
import cn.com.core.init.UpdateUtil.CheckUpdateCallBack;
import cn.com.core.util.AlertUtil;
import cn.com.core.util.AlertUtil.AlertCallBack;
import cn.com.core.util.Constant;
import cn.com.core.util.LogUtil;
import cn.com.core.util.SharedPreUtil;
import cn.com.core.web.NativeCacheUtil;
import cn.com.core.web.WebActivity;
import cn.com.csii.mobile.cache.CacheControl;
import cn.com.csii.mobile.cache.util.CacheUtil;
import cn.com.csii.mobile.http.ResultInterface;
import cn.com.jiujiang.ui.R;
import cn.com.jiujiang.ui.base.BaseActivity;
import cn.jpush.android.api.JPushInterface;

/**
 * 客户端启动入口页面，执行内容包括开机动画、缓存检测、版本检测和版本更新操作。
 * 
 * @author 田裕杰
 * 
 */
public class SplashScreenActivity extends BaseActivity {

	private ImageView background;// 动画完成后图片（在开机动画无法播放的情况下可以用此图片作为loading图片）
	private SharedPreUtil sharedPreUtil;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.splashscreen);
		sharedPreUtil = new SharedPreUtil(this);
		background = (ImageView) findViewById(R.id.background);
		initApp();
	}
	// 初始化应用程序
	public void initApp() {
		// 判断是否是首次启动
		boolean isFirst = sharedPreUtil.getToggleState(Constant.IS_FIRST_START);

		if (Constant.isAllowNativeCache) {
			if (!isFirst) {
				System.out.println("是首次启动");
				// 设置首次启动标识
				sharedPreUtil.setToggleState(Constant.IS_FIRST_START, true);
				// 如果是首次启动则执行加载缓存操作
				NativeCacheUtil.initNativeCache(context);
				// 保存初始版本信息
				sharedPreUtil.setToggleString(Constant.VERSION_VX,
						Constant.VX_VERSION);
				sharedPreUtil.setToggleString(Constant.VERSION_HTMLS,
						Constant.HTMLS_VERSION);
			} else {
				System.out.println("不是首次启动");
				// 如果非首次启动则执行 缓存检测操作 检查缓存文件是否被意外删除
				NativeCacheUtil.checkNativeCache(context);
			}
		}
		if (!Constant.isDebug) {//加上！说明不走app版本检测更新和vx包下载流程，取消！说明走app更新和vx包下载流程
			System.out.println("调试模式isDebug:"+Constant.isDebug);
			// 跳转页面
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					InitComplete();
				}
			}, 2000);
		} else {
			System.out.println("检测版本");
			// app版本更新检测
			UpdateUtil.checkAppUpdate(baseAt, new CheckUpdateCallBack() {
				@Override
				public void onInitSuccess(boolean isSuccess, String errorMsg) {
					// TODO Auto-generated method stub
					if (isSuccess) {
						System.out.println("isSuccess为true");
						// 检测成功
						// 检测缓存版本
						final CacheUpdateInfo info = constant.getCacheUpdateInfo();
						//检测vx版本
						if (!sharedPreUtil.getToggleString(Constant.VERSION_VX).equals(info.getVxVersion())) {
							LogUtil.d(baseAt, "检测到vx缓存包新版本，开始下载！");
							baseAt.requestStream(info.getVxUrl(), null, new ResultInterface() {
								
								@Override
								public void onSuccess(Object arg0) {
									// TODO Auto-generated method stub
									LogUtil.d(baseAt, "vx缓存包新版本下载成功！");
									File file = new File(CacheUtil.getInstance().getCachePath(context)
											+ "/" + Constant.VX_NAME);
									CacheControl.getInstance().UpdateCacheFile(file, (InputStream)arg0);
//									sharedPreUtil.setToggleString(Constant.VERSION_VX,
//											info.getVxVersion());									
									LogUtil.d(baseAt, "更新本地htmls缓存包：file---"+file.getAbsolutePath());
									//检测htmls版本
									if (!sharedPreUtil.getToggleString(Constant.VERSION_HTMLS).equals(info.getHtmlsVersion())) {
										LogUtil.d(baseAt, "检测到htmls缓存包新版本，开始下载！");
										baseAt.requestStream(info.getHtmlsUrl(), null, new ResultInterface() {
											
											@Override
											public void onSuccess(Object arg0) {
												// TODO Auto-generated method stub
												LogUtil.d(baseAt, "htmls缓存包新版本下载成功！");
												File file = new File(CacheUtil.getInstance().getCachePath(context)
														+ "/" + Constant.HTMLS_NAME);
												CacheControl.getInstance().UpdateCacheFile(file, (InputStream)arg0);
//												sharedPreUtil.setToggleString(Constant.VERSION_HTMLS,
//														info.getHtmlsVersion());
												LogUtil.d(baseAt, "更新本地htmls缓存包：file---"+file.getAbsolutePath());
												UpdateSuccess();
											}
											
											@Override
											public void onError(Object arg0) {
												// TODO Auto-generated method stub
												UpdateField("更新失败！");
											}
										});
									}else {
										UpdateSuccess();
									}
								}
								
								@Override
								public void onError(Object arg0) {
									// TODO Auto-generated method stub
									UpdateField("更新失败！");
								}
							});
						}else {
							UpdateSuccess();
						}
//						// 跳转页面
//						InitComplete();
					} else {
						// 检测失败 弹出提示 退出客户端
						System.out.println("isSuccess为false");
						UpdateField(errorMsg);
					}
				}
			});
		}
	}
	public void UpdateSuccess(){
		// 跳转页面
		LogUtil.d(baseAt, "跳转到主页面！");
		InitComplete();
	}
	public void UpdateField(String errorMsg){
		// 检测失败 弹出提示 退出客户端
		AlertUtil.ShowHintDialog(baseAt, "退出", "提示", errorMsg,
				new AlertCallBack() {

					@Override
					public void onPositive() {
						// TODO Auto-generated method stub
						FinishActivity();
					}

					@Override
					public void onNegative() {
						// TODO Auto-generated method stub

					}
				});
	}
	@Override
	protected void onResume() {
		super.onResume();
		JPushInterface.onResume(this);
	}
	@Override
	protected void onPause() {
		super.onPause();
		JPushInterface.onPause(this);
	}
	/**
	 * @brief 初始化完成 跳转页面
	 * */
	public void InitComplete() {
		if (sharedPreUtil.getToggleState(Constant.IS_FIRST_START)) {
			// 首次登录
			StartActivity(MainActivity.class, null);
			//如果用户点击了推送且推送的是公告管理页面则执行
			if(constant.jpush_title.equals("1")){
				System.out.println("1跳转到推送页面-公告管理");
	            Intent intenta = new Intent(context, WebActivity.class);
	            intenta.putExtra(Constant.ACTIONID, "ForJiGuang");
	            intenta.putExtra(Constant.ACTIONNAME, "公告管理");
	    		context.startActivity(intenta);
	    		constant.jpush_title = "";
	    	//如果用户点击了推送且推送的是精彩活动页面则执行
			}if(constant.jpush_title.equals("2")){
				System.out.println("2跳转到推送页面-精彩活动");
	            Intent intentb = new Intent(context, WebActivity.class);
	            intentb.putExtra(Constant.ACTIONID, "ForJiGuang");
	            intentb.putExtra(Constant.ACTIONNAME, "精彩活动");
	    		context.startActivity(intentb);
	    		constant.jpush_title = "";
			}
			FinishActivity();
		} else {
			StartActivity(MainActivity.class, null);
			if(constant.jpush_title.equals("1")){
				System.out.println("3跳转到推送页面-公告管理");
	            Intent intenta = new Intent(context, WebActivity.class);
	            intenta.putExtra(Constant.ACTIONID, "ForJiGuang");
	            intenta.putExtra(Constant.ACTIONNAME, "公告管理");
	    		context.startActivity(intenta);
	    		constant.jpush_title = "";
			}if(constant.jpush_title.equals("2")){
				System.out.println("4跳转到推送页面-精彩活动");
	            Intent intentb = new Intent(context, WebActivity.class);
	            intentb.putExtra(Constant.ACTIONID, "ForJiGuang");
	            intentb.putExtra(Constant.ACTIONNAME, "精彩活动");
	    		context.startActivity(intentb);
	    		constant.jpush_title = "";
			}
			FinishActivity();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
}
