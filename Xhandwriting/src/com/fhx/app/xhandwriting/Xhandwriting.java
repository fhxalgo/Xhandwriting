package com.fhx.app.xhandwriting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import hanzidict.HanziDict;
import hanzilookup.data.CharacterDescriptor;
import hanzilookup.data.CharacterTypeRepository;
import hanzilookup.data.MatcherThread;
import hanzilookup.data.MemoryStrokesStreamProvider;
import hanzilookup.data.StrokesDataSource;
import hanzilookup.data.StrokesMatcher;
import hanzilookup.data.MatcherThread.ResultsHandler;
import hanzilookup.ui.WrittenCharacter;
import hanzilookup.ui.WrittenCharacter.WrittenStroke;


public class Xhandwriting extends Activity implements OnTouchListener {

	private static String TAG = Xhandwriting.class.getName();
	final public int ABOUT = 0;
	
	DrawPanel dp;
	private List<Path> pointsToDraw = new ArrayList<Path>();
	private Paint mPaint;
	Path path;
	
	private ListView listView;  // show list of matched words
	private TextView editText;  // show added words panel
	
	// HanziLookup
	// The current type of character we're looking for; set by the type radio buttons.
	// Should be one of the type constants defined in CharacterTypeRepository.
	private int searchType;
	// StrokesDataSource abstracts access to a strokes data stream.
	private StrokesDataSource strokesDataSource;
	
	// Some settings with default initial values
	private boolean autoLookup	= true;		// whether to run a lookup automatically with each new stroke
	private double looseness 	= 0.25;		// the "looseness" of lookup, 0-1, higher == looser, looser more computationally intensive
	private int numResults		= 15;		// maximum number of results to return with each lookup
	
	// The matching Thread instance for running comparisons in a separate Thread so not to
	// lock up the event dispatch Thread.  We reuse a single Thread instance over the liftime
	// of the app, putting it to sleep when there's no comparison to be done.
	private MatcherThread matcherThread;
	
	// List of components that receive the results of this lookup widget.
	// Use a LinkedHashSet since it iterates in order.
	private Set characterHandlers = new LinkedHashSet();
	private List candidatesList;
	private List<Character> matchedList = new ArrayList<Character>();  // match list
	private ArrayAdapter<Character> adapter;
	
	///// CharacterCanvas
	// The WrittenCharacter that is operated on as mouse input is recorded.
	private WrittenCharacter inputCharacter = new WrittenCharacter();
	
	// We collect a current stroke of input and add whole strokes at a time to the inputCharacter.
	private WrittenCharacter.WrittenStroke currentStroke;
	// Need to keep track of the previous point as we are building a new WrittenStroke.
	private WrittenCharacter.WrittenPoint previousPoint;

	public HanziDict dictionary;
	
	private MediaPlayer player;
	private MediaPlayer mp = new MediaPlayer();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// set up Paint
		// set up paint
		mPaint = new Paint();
		mPaint.setDither(true);
		mPaint.setColor(Color.GREEN);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(12);
		
		// add DrawPanel
		FrameLayout fl = (FrameLayout) findViewById(R.id.drawView1);
		dp = new DrawPanel(this);	
		dp.setOnTouchListener(this);
		
		fl.addView(dp);
		Log.i(TAG, "added DrawPanel");
		
		// load dictionary data
		this.dictionary = new HanziDict(this.getAssets());
		this.dictionary.init();
				
		adapter = new ArrayAdapter<Character>(this,
				android.R.layout.simple_list_item_1, matchedList);
		listView = (ListView) findViewById(R.id.listView1);
		//txtView.setBackgroundColor(Color.WHITE);
		listView.setAdapter(adapter);

		editText = (TextView) findViewById(R.id.editText1);
		editText.setBackgroundColor(Color.WHITE);
		
		// add word to TextArea
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				Toast.makeText(getApplicationContext(),
						"Click ListItem Number " + position, Toast.LENGTH_LONG).show();
				
				Character item = adapter.getItem(position);
				//editText.setText(item + " : " + new Date());
				
				String def = dictionary.getDefinitionData(item.charValue());
				editText.setText(def);
				
				view.setSelected(true);
			}

		});
		
		// long click: show dictionary!
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {

				// do sound 
				Toast.makeText(getApplicationContext(),
						"Sound for " + position, Toast.LENGTH_LONG).show();
								
				Character item = adapter.getItem(position);
				List<String> audioFiles = dictionary.getPinyinFiles(item);
				
				editText.setText(item + " : " + Arrays.toString(audioFiles.toArray()));
				
				// look up the pinyin audio files
				for (String af : audioFiles) {
					playMedia(af);
				}
				
				view.setSelected(true);
				return true;
			}
		});
		
		// handwriting init
		loadStrokeDataFile();
		
		// load mp3 pin yin audio files - zip file 
		//loadPinyinAudioFiles();
		
		// start the matcher thread
		initMatcherThread();		

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
    	menu.add(0,ABOUT,0,"About");
    	return true;		
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		dp.pause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		dp.resume();
	}

	// The minimum pixel distance the mouse must be dragged before a new point is added.
	// Making this too small will result in lots of points that need to be analyzed.
	// Making this too large results in halting mouse input with characters composed of lines.
	static private final double MIN_STROKE_SEGMENT_LENGTH = 5.0;
	
	public boolean onTouch(View v, MotionEvent me) {
		// TODO Auto-generated method stub
		synchronized (pointsToDraw) {
			if (me.getAction() == MotionEvent.ACTION_DOWN) {
				path = new Path();
				path.moveTo(me.getX(), me.getY());
				// path.lineTo(me.getX(), me.getY());
				pointsToDraw.add(path);
				
				// create input Char
				this.previousPoint = this.inputCharacter.new WrittenPoint(me.getX(), me.getY());
				Log.i(TAG, "onTouch: MotionEvent.ACTION_DOWN");
				
			} else if (me.getAction() == MotionEvent.ACTION_MOVE) {
				path.lineTo(me.getX(), me.getY());
				
				Log.i(TAG, "onTouch: MotionEvent.ACTION_MOVE");
				// create WrittenCharacter
				WrittenCharacter.WrittenPoint nextPoint = this.inputCharacter.new WrittenPoint(me.getX(), me.getY());
				
				if(null != this.previousPoint && this.previousPoint.distance(nextPoint) >= MIN_STROKE_SEGMENT_LENGTH) {
					// If the mouse has not been dragged a minimal distance, then we ignore this event.
					
					if(null == this.currentStroke) {
						// If the current stroke is null, that means that the this is the second point of the stroke.
						// The first point is stored this.previousPoint.
						// Now that we know that there is a second point we can add both points to a newly initialized stroke.
						this.currentStroke = this.inputCharacter.new WrittenStroke();
						this.currentStroke.addPoint(this.previousPoint);
					}
					
					// Add the new point to the WrittenStroke, and cycle the previousPoint.
					this.currentStroke.addPoint(nextPoint);
					this.previousPoint = nextPoint;
					
					Log.i(TAG, "onTouch: currentStroke.addPoint: " + nextPoint);
				}
				
			} else if (me.getAction() == MotionEvent.ACTION_UP) {
				// path.lineTo(me.getX(), me.getY());
				
				Log.i(TAG, "onTouch: MotionEvent.ACTION_UP");
				// do lookup
				if(null != this.currentStroke) {
					// The current stroke will still be null if the mouse wasn't dragged far enough for a new stroke.
					
					// Add the new WrittenStroke to the WrittenCharacter, and reset input variables.
					this.inputCharacter.addStroke(this.currentStroke);
					this.previousPoint = null;
					this.currentStroke = null;
				}
				
				// trigger lookup
				//this.notifyStrokesListeners();
				runLookup();
			}
		}
		return true;
	}
	
	public class DrawPanel extends SurfaceView implements Runnable {

		Thread t = null;
		SurfaceHolder holder;
		boolean isItOk = false;

		public DrawPanel(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			holder = getHolder();
		}

		@SuppressLint("WrongCall")
		public void run() {
			// TODO Auto-generated method stub
			while (isItOk == true) {

				if (!holder.getSurface().isValid()) {
					continue;
				}

				Canvas c = holder.lockCanvas();
				c.drawARGB(255, 0, 0, 0);
				onDraw(c);
				holder.unlockCanvasAndPost(c);
			}
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			super.onDraw(canvas);
			synchronized (pointsToDraw) {
				for (Path path : pointsToDraw) {
					canvas.drawPath(path, mPaint);
				}
			}
		}

		public void pause() {
			isItOk = false;
			while (true) {
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			}
			t = null;
		}

		public void resume() {
			isItOk = true;
			t = new Thread(this);
			t.start();

		}
	}
	
	public void onClear(View v) {
		synchronized(this.pointsToDraw) {
			pointsToDraw.clear();
		}
		
		// clear handwriting
		this.inputCharacter.clear();
		this.currentStroke = null;
		
		dp.invalidate();
		
		// also reset the word view
		this.editText.setText("");
		this.adapter.clear();
	}
	
	public void onUndo(View v) {
		synchronized(this.pointsToDraw) {
			pointsToDraw.remove(pointsToDraw.size()-1);
		}
		
		// remove the last character stroke
		List<WrittenStroke> strokeList = this.inputCharacter.getStrokeList();
		if (strokeList.size() > 0) {
			strokeList.remove(strokeList.size() - 1);
		}
		
		if (strokeList.size() > 0 && this.autoLookup && this.inputCharacter.getStrokeList().size() > 0) {			
			this.runLookup();	
		}
		else {
			this.adapter.clear();
			this.editText.setText("");
		}
		
		dp.invalidate();
	}
	
	public void loadStrokeDataFile() {
		try {
			//Font initialFont = ChineseFontFinder.getChineseFont();
			
			AssetManager am = this.getAssets();
			InputStream compiledData = am.open("strokes.dat");
//			InputStreamReader inputStreamReader = new InputStreamReader(is,
//					"UTF-8");
//			BufferedReader reader = new BufferedReader(inputStreamReader);

			this.strokesDataSource = new StrokesDataSource(new MemoryStrokesStreamProvider(compiledData));
			Log.i(TAG, "loaded StrokesDataSource " + this.strokesDataSource.hashCode());
			
//			String line;
//			while ((line = reader.readLine()) != null) {
//				Pattern pat = Pattern.compile("([^\\s])\\t(.+)"); //$NON-NLS-1$
//				Matcher m = pat.matcher(line);
//				if (m.find()) {
//					String character = m.group(1);
//					String strokeData = m.group(2);
//					strokeDataMap.put(character, strokeData);
//				}
//			}
//
//			Log.i(TAG, "loaded stroke characters: "
//					+ strokeDataMap.keySet().size());
//			Log.i(TAG,
//					"loaded chinese characters: "
//							+ Arrays.toString(strokeDataMap.keySet().toArray()));
		} catch (Exception io) {

		}
	}
	
	/**
	 * Init the Thread that does comparisons.
	 * The thread sits idle waiting until it needs to do a comparison.
	 */
	private void initMatcherThread() {
		this.matcherThread = new MatcherThread();
		this.matcherThread.addResultsHandler(new ResultsHandler() {
			public void handleResults(Character[] results) {
				Xhandwriting.this.handleMatchedWords(results);
			}
		});
		
		// no sense in holding up app shutdown, so make it a daemon Thread.
		this.matcherThread.setDaemon(true);
		// NORM_PRIORITY so it doesn't compete with event dispatch
		this.matcherThread.setPriority(Thread.NORM_PRIORITY);
		
		// Start it up.  It will immediately go to sleep waiting for a comparison.
		this.matcherThread.start();
		
		Log.i(TAG, "initMatcherThread: matherThread started.");
	}
	
	/**
	 * Load the given characters into the selection window.
	 * @param results the results to load
	 */
	private void handleMatchedWords(final Character[] results) {
		
		// the normal Thread doesn't work
		this.runOnUiThread(new Runnable() {
			public void run() {
				Log.i(TAG, "handleMatchedWords: " + Arrays.toString(results));
				//txtView.setText(Arrays.toString(results).replaceAll(",", "").substring(1).replace(']', ' '));
				adapter.clear();
				for (Character c: results) {
					adapter.add(c);
				}
			}
		});
	}
	
	/**
	 * Run matching in the separate matching Thread.
	 * The thread will load the results into the results window if it finishes before interrupted.
	 */
	void runLookup() {
	    Log.i(TAG, "runLookup(begin)");
	    
		WrittenCharacter writtenCharacter = this.getCharacter();
	    Log.i(TAG, "writtenCharacter: " + writtenCharacter);
	    Log.i(TAG, "writtenCharacter.getStrokeList().size(): " + writtenCharacter.getStrokeList().size());
	    
		if(writtenCharacter.getStrokeList().size() == 0) {
			// Don't bother doing anything if nothing has been input yet (number of strokes == 0).
			this.handleMatchedWords(new Character[0]);
			return;
		}
			
		CharacterDescriptor inputDescriptor = writtenCharacter.buildCharacterDescriptor();
		Log.i(TAG, "inputDescriptor: " + inputDescriptor);
	    Log.i(TAG, "inputDescriptor: strokeCount " + inputDescriptor.getStrokeCount());
	    Log.i(TAG, "inputDescriptor: subStrokeCount" + inputDescriptor.getSubStrokeCount());
	    
		
    	boolean searchTraditional = this.searchType == CharacterTypeRepository.GENERIC_TYPE || this.searchType == CharacterTypeRepository.TRADITIONAL_TYPE;
        boolean searchSimplified = this.searchType  == CharacterTypeRepository.GENERIC_TYPE || this.searchType == CharacterTypeRepository.SIMPLIFIED_TYPE;
    	
        StrokesMatcher matcher = new StrokesMatcher(inputDescriptor,
	            							     searchTraditional,
	            							     searchSimplified,
	            							     this.looseness,
	            							     this.numResults,
	            							     this.strokesDataSource);
	    
        // If the Thread is currently running, setting a new StrokesMatcher
        // will cause the Thread to fall out of its current matching loop
        // discarding any accumulated results.  It will then start processing
        // the newly set StrokesMatcher.
        this.matcherThread.setStrokesMatcher(matcher);
        
        Log.i(TAG, "runLookup(end)");
	}
	
	/**
	 * Getter for the WrittenCharacter.
	 * Other components that actually do the analysis will need access to it.
	 * 
	 * @return the WrittenCharacter operated on by this Canvas
	 */
	public WrittenCharacter getCharacter() {
		return this.inputCharacter;
	}
	
	private void strokeFinished(StrokeEvent e) {
	    if(this.autoLookup) {
	        this.runLookup();
	    }
	}
	
	public class StrokeEvent extends EventObject {
	    private StrokeEvent() {
	        // the source is always this canvas.
	        super(Xhandwriting.this);
	    }
	}

	// mp3 stuff: which way is better, add to res/raw or use zip?? 
	// measure performance on this
	public void playMP3 (int mp3FileId) {
		
		player = MediaPlayer.create(this, mp3FileId);
		//player.setVolume(volume_level, volume_level);
		player.start();
		player.setOnCompletionListener(new OnCompletionListener() {			
			@Override
			public void onCompletion(MediaPlayer arg0) {
				//volume_level += volume_incr;
				//player.setVolume(volume_level, volume_level);
				/*try {
					player.prepare();
				} catch (IOException e) {
					// Should be a CANTHAPPEN since was previously prepared!
					Log.i(LOG_TAG, "Unexpected IOException " + e);
				}*/
				player.stop();
			}
		});	
	}
	
	public void loadPinyinAudioFiles() {
		try {
			Log.i(TAG, "loading loadPinyinAudioFiles...");
			
			Log.i(TAG, "loaded Mandarin_sound.zip file.");
		} catch (Exception io) {

		}
	}
	
	private void playMedia(String fileName) {
		// should probably do it in a seperate thread, like matcherThread
		
		try {
			AssetManager am = this.getAssets();
			InputStream is = am.open("Mandarin_sounds.zip");
			ZipInputStream zis = new ZipInputStream(is);
			
			ZipEntry ze = null;
			File mp3File = null;
			FileOutputStream out = null;
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.getName().equals(fileName + ".mp3")) {
					// Toast.makeText(this, "Found", 2).show();
					mp3File = File.createTempFile(fileName, ".mp3");
					out = new FileOutputStream(mp3File);
					copyFile(zis, out);
					break;
				}
			}

			if (mp3File == null || !mp3File.canRead()) {
				Toast.makeText(this, "CANNOT READ " + fileName,
						Toast.LENGTH_SHORT).show();
				return;
			}
			
			mp.reset();  // reset the play to ready state
			mp.setDataSource(mp3File.getAbsolutePath());
			mp.prepare();
			Toast.makeText(this, "Start play", Toast.LENGTH_SHORT).show();
			mp.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					mp.stop();
					Toast.makeText(Xhandwriting.this, "Media Play Complete",
							Toast.LENGTH_SHORT).show();
				}
			});

			mp.start();
			Toast.makeText(this, "Started OK", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public void copyFile(InputStream inStream, OutputStream outStream) {	
  
    	try{

    	    byte[] buffer = new byte[1024];
 
    	    int length;
    	    //copy the file content in bytes 
    	    while ((length = inStream.read(buffer)) > 0){
 
    	    	outStream.write(buffer, 0, length); 
    	    }
 
    	    inStream.close();
    	    outStream.close();
 
    	    Log.i(TAG, "File is copied successful!");
 
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ABOUT:
			AboutDialog about = new AboutDialog(this);
			about.setTitle("XHandwriting demo");
			about.show();

			break;

		}
		return true;
	}
}
