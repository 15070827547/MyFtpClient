package com.example.myftpclient;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;

import com.example.myftpclient.ftp.FTP;
import com.example.myftpclient.ftp.Result;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private EditText et_servename;
	private EditText et_username;
	private EditText et_password;

	private Button btn_login;
	private Button btn_logout;
	private Button btn_local_refresh;
	private Button btn_remote_refresh;

	private ListView lv_ftp_files;
	private ListView lv_local_files;

	private FTP ftp;
	private List<FTPFile> remoteFile;
	private List<File> localFile;
	private static final String LOCAL_PATH = "/mnt/sdcard/";

	private RemoteAdapter remoteAdapter;
	private LocalAdapter localAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et_servename = (EditText) findViewById(R.id.et_servename);
		et_username = (EditText) findViewById(R.id.et_username);
		et_password = (EditText) findViewById(R.id.et_password);

		btn_login = (Button) findViewById(R.id.btn_login);
		btn_logout = (Button) findViewById(R.id.btn_logout);
		btn_local_refresh = (Button) findViewById(R.id.btn_local_refresh);
		btn_remote_refresh = (Button) findViewById(R.id.btn_remote_refresh);

		lv_ftp_files = (ListView) findViewById(R.id.lv_ftp_files);
		lv_local_files = (ListView) findViewById(R.id.lv_local_files);

		setListener();

		remoteFile = new ArrayList<FTPFile>();
		remoteAdapter = new RemoteAdapter(MainActivity.this, remoteFile);
		lv_ftp_files.setAdapter(remoteAdapter);

		localFile = new ArrayList<File>();
		localAdapter = new LocalAdapter(MainActivity.this, localFile);
		lv_local_files.setAdapter(localAdapter);
	}

	private void setListener() {
		btn_login.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				loginFtp();
			}
		});
		btn_logout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				logoutFtp();
			}
		});
		btn_local_refresh.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				refreshLocalFiles();
			}

		});
		btn_remote_refresh.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				refreshRemoteFiles();
			}
			
		});
		lv_ftp_files.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				// 通过AlertDialog.Builder这个类来实例化我们的一个AlertDialog的对象
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				// 设置Title的图标
				builder.setIcon(R.drawable.ic_launcher);
				// 设置Title的内容
				builder.setTitle("下载确认");
				// 设置Content来显示一个信息
				builder.setMessage("点击确定将开始下载此文件!!!");
				// 设置一个PositiveButton
				builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						downFtpFile(remoteFile.get(position));
					}
				});
				// 设置一个NegativeButton
				builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});
				// 显示出该对话框
				builder.show();
			}
		});

		lv_local_files.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				// 通过AlertDialog.Builder这个类来实例化我们的一个AlertDialog的对象
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				// 设置Title的图标
				builder.setIcon(R.drawable.ic_launcher);
				// 设置Title的内容
				builder.setTitle("上传确认");
				// 设置Content来显示一个信息
				builder.setMessage("点击确定将开始上传此文件,如果文件过大请确认是否是Wifi状态!!!");
				// 设置一个PositiveButton
				builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						uploadLocalFile(localFile.get(position));
					}
				});
				// 设置一个NegativeButton
				builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});
				// 显示出该对话框
				builder.show();
			}
		});
	}

	/**
	 * 加载本地目录
	 */
	private void getFileDir(String filePath) {
		// 获取根目录
		File f = new File(filePath);
		// 获取根目录下所有文件
		File[] files = f.listFiles();
		// 循环添加到本地列表
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isHidden() || file.getName().equals("LOST.DIR")) {
				continue;
			}
			localFile.add(file);
			localAdapter.refresh(localFile);
		}
	}

	private void refreshLocalFiles() {
		getFileDir(LOCAL_PATH);
	}

	private void refreshRemoteFiles() {
		if (ftp!=null) {
			try {
				remoteFile = ftp.listFiles(FTP.REMOTE_PATH);
				if (remoteFile!=null) {
					remoteAdapter.refresh(remoteFile);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 下载FTP服务器文件
	 */
	private void downFtpFile(final FTPFile ftpFile) {
		if (ftpFile != null) {
			new Thread() {
				public void run() {
					Result result = null;
					try {
						// 下载
						result = ftp.download(FTP.REMOTE_PATH, ftpFile.getName(), LOCAL_PATH);
						if (result.isSucceed()) {
							Log.e("TAG",
									"download ok...time:" + result.getTime() + " and size:" + result.getResponse());
							MainActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
								}
							});
						} else {
							MainActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
								}
							});
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				};
			}.start();
		}
	}

	/**
	 * 上传本地文件到FTP
	 */
	private void uploadLocalFile(final File file) {
		if (file != null) {
			new Thread() {
				public void run() {
					Result result = null;
					try {
						// 上传
						result = ftp.uploading(file, FTP.REMOTE_PATH);
						if (result.isSucceed()) {
							Log.e("TAG",
									"uploading ok...time:" + result.getTime() + " and size:" + result.getResponse());
							MainActivity.this.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
								}
							});
						} else {
							MainActivity.this.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
								}
							});
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				};
			}.start();
		}
	}

	/**
	 * 注销FTP
	 */
	private void logoutFtp() {
		new Thread() {
			public void run() {
				if (ftp != null) {
					try {
						// 关闭FTP服务
						ftp.closeConnect();
						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								remoteAdapter.clear();
							}
						});
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	/**
	 * 登录FTP
	 */
	private void loginFtp() {

		new Thread() {
			public void run() {
				try {

					if (ftp != null) {
						// 关闭FTP服务
						try {
							ftp.closeConnect();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					// 初始化FTP
					String hostName = et_servename.getText().toString();
					String userName = et_username.getText().toString();
					String passwrod = et_password.getText().toString();

					ftp = new FTP(hostName, userName, passwrod);

					// 打开FTP服务
					boolean openConnect = ftp.openConnect();
					// 加载FTP列表
					if (ftp != null) {

					}
					remoteFile = ftp.listFiles(FTP.REMOTE_PATH);
					if (remoteFile != null) {
						if (openConnect) {
							MainActivity.this.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									Toast.makeText(MainActivity.this, "登录成功", 1).show();
									remoteAdapter.refresh(remoteFile);
								}
							});
						} else {
							Toast.makeText(MainActivity.this, "登录失败", 1).show();
						}
					}
				} catch (UnknownHostException hostException) {
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							remoteAdapter.clear();
							Toast.makeText(MainActivity.this, "No address associated with hostname", 1).show();
						}
					});
				} catch (final IOException e) {
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							remoteAdapter.clear();
							Toast.makeText(MainActivity.this, e.getMessage(), 1).show();
						}
					});
				}
			};
		}.start();
	}
}
