package net.jejer.hipda.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.jejer.hipda.async.AvatarUrlTask;
import net.jejer.hipda.utils.HiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AvatarUrlCache {

	private final static String LOG_TAG = AvatarUrlCache.class.getSimpleName();

	private final static String PREFS_NAME = "AvatarPrefsFile";
	private final static String AVATAR_URL_PREFIX = HiUtils.BaseUrl + "uc_server/data/avatar";

	private Context mCtx;

	private final static int MAX_ENTRIES = 4096;
	private final static String INDEX_KEY = "INDEX";
	private final static String DELIMITER = ";";

	private SharedPreferences avatarPrefs;
	private LRUCache<String, String> avatarMap;


	private Map<String, String> mDirtyUids = new ConcurrentHashMap<String, String>();

	private boolean mUpdated = false;
	private long lastSaveTime = 0;

	private static boolean FETCH_AVATAR_URL_BY_UID = false;


	private static AvatarUrlCache ourInstance = new AvatarUrlCache();

	public static AvatarUrlCache getInstance() {
		return ourInstance;
	}

	private AvatarUrlCache() {
	}


	public void init(Context ctx) {
		mCtx = ctx;

		avatarMap = new LRUCache<String, String>(MAX_ENTRIES);
		avatarPrefs = mCtx.getSharedPreferences(PREFS_NAME, 0);

		load();
	}

	private void load() {
		long start = System.currentTimeMillis();
		String index = avatarPrefs.getString(INDEX_KEY, "");
		if (index.length() > 0) {
			String[] indexes = index.split(DELIMITER);
			for (int i = 0; i < indexes.length; i++) {
				String key = indexes[i];
				if (key.length() > 0)
					avatarMap.put(key, avatarPrefs.getString(key, ""));
			}
		} else {
			Map<String, ?> keys = avatarPrefs.getAll();
			for (Map.Entry<String, ?> entry : keys.entrySet()) {
				avatarMap.put(entry.getKey(), entry.getValue().toString());
			}
		}
		Log.v(LOG_TAG, "load avatarPrefs, size=" + avatarMap.usedEntries() + ", time used : " + (System.currentTimeMillis() - start) + " ms");
	}

	public void put(String uid, String url) {
		if (url == null)
			url = "";
		if (url.startsWith(AVATAR_URL_PREFIX)) {
			url = url.substring(AVATAR_URL_PREFIX.length());
		}
		avatarMap.put(uid, url);
		mDirtyUids.remove(uid);
		mUpdated = true;
	}

	public String get(String uid) {
		if (!avatarMap.containsKey(uid)) {
			return "";
		}
		String url = avatarMap.get(uid);
		if (url.startsWith("/")) {
			url = AVATAR_URL_PREFIX + url;
		}
		return url;
	}

	public void markDirty(String uid) {
		if (!avatarMap.containsKey(uid) && !mDirtyUids.containsKey(uid)) {
			mDirtyUids.put(uid, uid);
		}
	}

	public void fetchAvatarUrls() {
		if (mDirtyUids.size() > 0) {
			Set<String> uids = mDirtyUids.keySet();
			Collection<String> fetchlist = new ArrayList<String>();
			for (String uid : uids) {
				fetchlist.add(uid);
				mDirtyUids.remove(uid);
			}
			if (FETCH_AVATAR_URL_BY_UID)
				new AvatarUrlTask(mCtx).fetch(fetchlist);
		}
	}

	public boolean contains(String uid) {
		return avatarMap.containsKey(uid);
	}

	public boolean isUpdated() {
		return mUpdated;
	}

	public void setUpdated(boolean updated) {
		mUpdated = updated;
	}

	public void save() {
		long start = System.currentTimeMillis();
		if (start - lastSaveTime > 5 * 1000) {
			StringBuilder stringBuilder = new StringBuilder();
			Collection<Map.Entry<String, String>> entries = avatarMap.getAll();
			SharedPreferences.Editor prefsWriter = avatarPrefs.edit();
			prefsWriter.clear();
			for (Map.Entry<String, String> entry : entries) {
				stringBuilder.append(entry.getKey()).append(DELIMITER);
				prefsWriter.putString(entry.getKey(), entry.getValue());
			}
			prefsWriter.putString(INDEX_KEY, stringBuilder.toString());
			prefsWriter.apply();
			lastSaveTime = System.currentTimeMillis();
			Log.v(LOG_TAG, "save avatarPrefs, size=" + avatarMap.usedEntries() + ", time used : " + (System.currentTimeMillis() - start) + " ms");
		}
	}


}