package truesculpt.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import truesculpt.utils.Utils;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class UpdateManager extends BaseManager
{

	public enum EUpdateStatus
	{
		IS_A_BETA, UNDEFINED, UP_TO_DATE, UPDATE_NEEDED
	}

	private String mCurrentVersion = "";
	private String mLatestVersion = "";

	public UpdateManager(Context baseContext)
	{
		super(baseContext);
		// TODO Auto-generated constructor stub
	}

	public int getCurrentVersionCode()
	{
		int nCurrVersionCode = -1;

		PackageManager pm = getbaseContext().getPackageManager();
		try
		{
			PackageInfo info = pm.getPackageInfo(getbaseContext().getPackageName(), 0);
			nCurrVersionCode = info.versionCode;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}

		return nCurrVersionCode;
	}

	public String getCurrentVersion()
	{
		if (mCurrentVersion.length() == 0)
		{
			String strCurrVersion = "0.0";

			PackageManager pm = getbaseContext().getPackageManager();
			try
			{
				PackageInfo info = pm.getPackageInfo(getbaseContext().getPackageName(), 0);
				strCurrVersion = info.versionName;
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			mCurrentVersion = strCurrVersion;
		}

		return mCurrentVersion;
	}

	public String getLatestVersion()
	{
		if (mLatestVersion.length() == 0)
		{
			String strLatestVersion = "-1.0";

			try
			{
				// TODO check alternative url list (register url)
				URL url = new URL("http://code.google.com/p/truesculpt/wiki/Version");
				InputStream stream = url.openStream();
				InputStreamReader reader = new InputStreamReader(stream);
				BufferedReader buffReader = new BufferedReader(reader);
				String strTemp = buffReader.readLine();
				String strFileVersion = "";
				while (strTemp != null)
				{
					strFileVersion += strTemp;
					strTemp = buffReader.readLine();
				}
				buffReader.close();

				// <p>LATEST_STABLE_VERSION=0_1 </p>
				// LATEST_BETA_VERSION=0_1
				// UPDATE_BASE_URL=http://truesculpt.googlecode.com/files/TrueSculpt_

				Pattern p = Pattern.compile("<p>LATEST_STABLE_VERSION=[0-9]+_[0-9]+ </p>", Pattern.CASE_INSENSITIVE);

				Matcher m = p.matcher(strFileVersion);
				if (m.find())
				{
					String elem = m.group();
					elem = elem.replace("<p>LATEST_STABLE_VERSION=", "");
					elem = elem.replace("</p>", "");
					elem = elem.replace("_", ".");
					elem = elem.trim();

					strLatestVersion = elem;
				}
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			mLatestVersion = strLatestVersion;
		}
		return mLatestVersion;
	}

	public EUpdateStatus getUpdateStatus(String strCurrVersion, String strLatestVersion)
	{

		EUpdateStatus res = EUpdateStatus.UNDEFINED;

		int majCurr = -1;
		int minCurr = -1;
		int majLat = -1;
		int minLat = -1;

		try
		{
			String[] tempVer = strCurrVersion.split("\\.");
			if (tempVer.length == 2)
			{
				majCurr = Integer.parseInt(tempVer[0]);
				minCurr = Integer.parseInt(tempVer[1]);
			}
			tempVer = strLatestVersion.split("\\.");
			if (tempVer.length == 2)
			{
				majLat = Integer.parseInt(tempVer[0]);
				minLat = Integer.parseInt(tempVer[1]);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			res = EUpdateStatus.UNDEFINED;
			return res;
		}

		boolean bUpdateNeeded = false;
		boolean bIsBeta = false;

		if (majCurr >= 0 && minCurr >= 0 && majLat >= 0 && minLat >= 0)
		{
			if (majLat > majCurr)
			{
				bUpdateNeeded = true;
			}
			else if (majLat == majCurr)
			{
				if (minLat > minCurr)
				{
					bUpdateNeeded = true;
				}
				else if (minLat < minCurr)
				{
					bIsBeta = true;
				}
			}
			else if (majLat < majCurr)
			{
				bIsBeta = true;
			}
		}
		else
		{
			res = EUpdateStatus.UNDEFINED;
			return res;
		}

		if (bIsBeta)
		{
			res = EUpdateStatus.IS_A_BETA;
		}
		else
		{
			if (bUpdateNeeded)
			{
				res = EUpdateStatus.UPDATE_NEEDED;
			}
			else
			{
				res = EUpdateStatus.UP_TO_DATE;
			}
		}

		if (res != EUpdateStatus.UNDEFINED)
		{
			getManagers().getOptionsManager().updateLastSoftwareUpdateCheckDate();
		}

		return res;
	};

	// TODO Improve by redirecting to market and reading update needs from
	// market
	public void CheckUpdate(Context context)
	{
		boolean bStartUpdateActivity = false;
		if (getManagers().getOptionsManager().getCheckUpdateAtStartup() == true)
		{
			bStartUpdateActivity = true;
		}

		long timeOfLastUpdate = getManagers().getOptionsManager().getLastSoftwareUpdateCheckDate();
		long today = System.currentTimeMillis();
		long timeSinceLastUpdate = today - timeOfLastUpdate;

		long timeThresold = 31;
		timeThresold *= 24;
		timeThresold *= 3600;
		timeThresold *= 1000;// one month in millis
		if (timeSinceLastUpdate > timeThresold)
		{
			bStartUpdateActivity = true;// mandatory updates
		}

		if (bStartUpdateActivity)
		{
			Utils.StartMyActivity(context, truesculpt.ui.panels.UpdatePanel.class, true);
		}
	}

	@Override
	public void onCreate()
	{

	}

	@Override
	public void onDestroy()
	{

	}

}
