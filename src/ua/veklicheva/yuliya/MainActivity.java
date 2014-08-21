package ua.veklicheva.yuliya;

import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static long back_pressed;

	public static final String MIME_TEXT_PLAIN = "text/plain";
	public static final String TAG = "NfcDemo";

	private TextView mTextView;
	private NfcAdapter mNfcAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTextView = (TextView) findViewById(R.id.textView_explanation);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		if (mNfcAdapter == null) {
			// Stop here, we definitely need NFC
			Toast.makeText(this, "This device doesn't support NFC.",
					Toast.LENGTH_LONG).show();
			finish();
			return;

		}

		if (!mNfcAdapter.isEnabled()) {
			mTextView.setText("NFC is disabled.");
		} else {
			mTextView.setText(R.string.explanation);
		}

		handleIntent(getIntent());
	}

	@Override
	protected void onResume() {
		super.onResume();

		/**
		 * It's important, that the activity is in the foreground (resumed).
		 * Otherwise an IllegalStateException is thrown.
		 */
		setupForegroundDispatch(this, mNfcAdapter);
	}

	@Override
	protected void onPause() {
		/**
		 * Call this before onPause, otherwise an IllegalArgumentException is
		 * thrown as well.
		 */
		stopForegroundDispatch(this, mNfcAdapter);

		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		/**
		 * This method gets called, when a new Intent gets associated with the
		 * current activity instance. Instead of creating a new activity,
		 * onNewIntent will be called. For more information have a look at the
		 * documentation.
		 * 
		 * In our case this method gets called, when the user attaches a Tag to
		 * the device.
		 */
		handleIntent(intent);
	}

	/**
	 * @param activity
	 *            The corresponding {@link Activity} requesting the foreground
	 *            dispatch.
	 * @param adapter
	 *            The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void setupForegroundDispatch(final Activity activity,
			NfcAdapter adapter) {
		final Intent intent = new Intent(activity.getApplicationContext(),
				activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final PendingIntent pendingIntent = PendingIntent.getActivity(
				activity.getApplicationContext(), 0, intent, 0);

		IntentFilter[] filters = new IntentFilter[1];
		String[][] techList = new String[][] {};

		// Notice that this is the same filter as in our manifest.
		filters[0] = new IntentFilter();
		filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
		try {
			filters[0].addDataType(MIME_TEXT_PLAIN);
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Check your mime type.");
		}

		adapter.enableForegroundDispatch(activity, pendingIntent, filters,
				techList);
	}

	/**
	 * @param activity
	 *            The corresponding {@link BaseActivity} requesting to stop the
	 *            foreground dispatch.
	 * @param adapter
	 *            The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void stopForegroundDispatch(final Activity activity,
			NfcAdapter adapter) {
		adapter.disableForegroundDispatch(activity);
	}

	@Override
	public void onBackPressed() {
		if (back_pressed + 2000 > System.currentTimeMillis()) {
			super.onBackPressed();
		} else {
			Toast.makeText(getBaseContext(), "Нажмите еще раз чтобы выйти",
					Toast.LENGTH_SHORT).show();
		}
		back_pressed = System.currentTimeMillis();
	}

	private void handleIntent(Intent intent) {
		String action = intent.getAction();

		if ((NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))) {
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			new MifareClassicReaderTask().execute(tag);
		}
	}

	/**
	 * Background task for reading the data. Do not block the UI thread while
	 * reading.
	 * 
	 * @author Ralf Wondratschek
	 * 
	 */
	private class MifareClassicReaderTask extends AsyncTask<Tag, Void, String> {

		@Override
		protected String doInBackground(Tag... params) {
			Tag tag = params[0];

			MifareClassic mfc = MifareClassic.get(tag);
			String cardData = "";
			byte[] data;

			try {
				byte[] baReadUID = new byte[] { (byte) 0xa0, (byte) 0xb0,
						(byte) 0xc0, (byte) 0xd0, (byte) 0xe0, (byte) 0xf0 };
				byte[] baReadUID2 = new byte[] { (byte) 0xa1, (byte) 0xb1,
						(byte) 0xc1, (byte) 0xd1, (byte) 0xe1, (byte) 0xf1 };

				mfc.connect();
				int secCount = mfc.getSectorCount();
				for (int j = 0; j < secCount; j++) {

					cardData += "sector " + j + ": ";

					if (mfc.authenticateSectorWithKeyA(j, baReadUID)) {

						int bCount = mfc.getBlockCountInSector(j);
						int bIndex = mfc.sectorToBlock(j);
						for (int i = 0; i < bCount; i++) {
							data = mfc.readBlock(bIndex);
							cardData += Arrays.toString(data) + "; ";
							bIndex++;
						}
					} else if (mfc.authenticateSectorWithKeyB(j, baReadUID)) {

						int bCount = mfc.getBlockCountInSector(j);
						int bIndex = mfc.sectorToBlock(j);
						for (int i = 0; i < bCount; i++) {
							data = mfc.readBlock(bIndex);
							cardData += Arrays.toString(data) + "; ";
							bIndex++;
						}
					} else if (mfc.authenticateSectorWithKeyA(j, baReadUID2)) {

						int bCount = mfc.getBlockCountInSector(j);
						int bIndex = mfc.sectorToBlock(j);
						for (int i = 0; i < bCount; i++) {
							data = mfc.readBlock(bIndex);
							cardData += Arrays.toString(data) + "; ";
							bIndex++;
						}
					} else if (mfc.authenticateSectorWithKeyB(j, baReadUID2)) {

						int bCount = mfc.getBlockCountInSector(j);
						int bIndex = mfc.sectorToBlock(j);
						for (int i = 0; i < bCount; i++) {
							data = mfc.readBlock(bIndex);
							cardData += Arrays.toString(data) + "; ";
							bIndex++;
						}
					} else if (mfc.authenticateSectorWithKeyA(j,
							MifareClassic.KEY_DEFAULT)) {

						int bCount = mfc.getBlockCountInSector(j);
						int bIndex = mfc.sectorToBlock(j);
						for (int i = 0; i < bCount; i++) {
							data = mfc.readBlock(bIndex);
							cardData += Arrays.toString(data) + "; ";
							bIndex++;
						}
					} else {
						cardData += "error; ";
					}
				}
			} catch (IOException e) {
				Log.e(TAG, e.getLocalizedMessage());
			}

			return cardData;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				mTextView.setText("Read content: " + result);
			}
		}
	}
}