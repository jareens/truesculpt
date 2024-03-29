package truesculpt.ui.panels;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.zip.GZIPOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import truesculpt.main.Managers;
import truesculpt.main.R;
import truesculpt.main.TrueSculptApp;
import truesculpt.managers.UtilsManager;
import truesculpt.managers.WebManager;
import truesculpt.ui.adapters.JavaScriptInterface;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.ParseException;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

//TODO thread all waiting phases
public class WebFilePanel extends Activity
{
	private final String mStrBaseWebSite = WebManager.GetBaseWebLibraryAdress();

	private Button mPublishToWebBtn;
	private WebView mWebView;

	private class MyWebViewClient extends WebViewClient
	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			view.loadUrl(url);
			return true;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// Check if the key event was the BACK key and if there's history
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack())
		{
			mWebView.goBack();
			return true;
		}
		// If it wasn't the BACK key or there's no web page history, bubble up
		// to the default
		// system behavior (probably exit the activity)
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getManagers().getUtilsManager().updateFullscreenWindowStatus(getWindow());

		setContentView(R.layout.webfile);

		getManagers().getUsageStatisticsManager().TrackEvent("OpenFromWeb", "", 1);

		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.setWebViewClient(new MyWebViewClient());
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		mWebView.addJavascriptInterface(new JavaScriptInterface(this, getManagers()), "Android");

		int nVersionCode = getManagers().getUpdateManager().getCurrentVersionCode();
		mWebView.loadUrl(mStrBaseWebSite + "?version=" + nVersionCode);

		mPublishToWebBtn = (Button) findViewById(R.id.publish_to_web);
		mPublishToWebBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final String name = getManagers().getMeshManager().getName();
				final File imagefile = new File(getManagers().getFileManager().GetImageFileName());
				final File objectfile = new File(getManagers().getFileManager().GetObjectFileName());

				getManagers().getUsageStatisticsManager().TrackEvent("PublishToWeb", name, 1);

				if (imagefile.exists() && objectfile.exists())
				{
					try
					{
						final File zippedObject = File.createTempFile("object", "zip");
						zippedObject.deleteOnExit();

						BufferedReader in = new BufferedReader(new FileReader(objectfile));
						BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(zippedObject)));
						System.out.println("Compressing file");
						int c;
						while ((c = in.read()) != -1)
						{
							out.write(c);
						}
						in.close();
						out.close();

						long size = 0;

						size = new FileInputStream(imagefile).getChannel().size();
						size += new FileInputStream(zippedObject).getChannel().size();
						size /= 1000;

						final SpannableString msg = new SpannableString("You will upload your latest saved version of this scupture representing " + size + " ko of data\n\n" + "When clicking the yes button you accept to publish your sculpture under the terms of the creative commons share alike, non commercial license\n" + "http://creativecommons.org/licenses/by-nc-sa/3.0" + "\n\nDo you want to proceed ?");
						Linkify.addLinks(msg, Linkify.ALL);

						AlertDialog.Builder builder = new AlertDialog.Builder(WebFilePanel.this);
						builder.setMessage(msg).setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int id)
							{
								PublishPicture(imagefile, zippedObject, name);
							}
						}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int id)
							{

							}
						});
						AlertDialog dlg = builder.create();
						dlg.show();

						// Make the textview clickable. Must be called after
						// show()
						((TextView) dlg.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(WebFilePanel.this);
					builder.setMessage("File has not been saved, you need to save it before publishing\nDo you want to proceed to save window ?").setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int id)
						{
							((FileSelectorPanel) getParent()).getTabHost().setCurrentTab(2);
						}
					}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int id)
						{
						}
					});
					builder.show();
				}
			}
		});
	}

	public void PublishPicture(File imagefile, File zipobjectfile, String name)
	{
		String strUploadURL = "";
		try
		{
			String myHeader = "uploadUrl";
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(mStrBaseWebSite + "/upload");
			HttpResponse httpResponse = httpClient.execute(httpPost);
			Header[] headers = httpResponse.getHeaders(myHeader);
			for (int i = 0; i < headers.length; i++)
			{
				Header header = headers[i];
				if (header.getName().equals(myHeader))
				{
					strUploadURL = header.getValue();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// upload saved file
		try
		{
			uploadPicture(imagefile, zipobjectfile, strUploadURL, name, "");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void uploadPicture(File imagefile, File zipobjectfile, String uploadURL, String title, String description) throws ParseException, IOException, URISyntaxException
	{
		HttpClient httpclient = new DefaultHttpClient();

		HttpPost httppost = new HttpPost(uploadURL);
		httppost.addHeader("title", title);
		httppost.addHeader("description", description);
		httppost.addHeader("installationID", UtilsManager.Installation.id(this));

		MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.STRICT);
		ContentBody cbImageFile = new FileBody(imagefile, "image/png");
		ContentBody cbObjectFile = new FileBody(zipobjectfile, "application/zip");

		mpEntity.addPart("imagefile", cbImageFile);
		mpEntity.addPart("objectfile", cbObjectFile);

		httppost.setEntity(mpEntity);

		System.out.println("executing request " + httppost.getRequestLine());
		HttpResponse httpResponse = httpclient.execute(httppost);

		String myHeader = "displayURL";
		Header[] headers = httpResponse.getHeaders(myHeader);
		for (int i = 0; i < headers.length; i++)
		{
			Header header = headers[i];
			if (header.getName().equals(myHeader))
			{
				String newURL = mStrBaseWebSite + header.getValue();
				Log.d("WEB", "Loading web site " + newURL);
				mWebView.loadUrl(newURL);
			}
		}

	}

	public Managers getManagers()
	{
		return ((TrueSculptApp) getApplicationContext()).getManagers();
	}
}
