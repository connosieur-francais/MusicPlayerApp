package main;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class MusicPlayer extends PlaybackListener {
	// this will be used to update isPaused more synchronously
	private static final Object playSignal = new Object();

	private MusicPlayerGUI musicPlayerGUI;
	// we will need a way to store our song's detials, so we will be creating a song
	// class
	private Song currentSong;

	public Song getCurrentSong() {
		return currentSong;
	}

	private ArrayList<Song> playlist;

	// we will need to keep track of the index of the song we are playing
	private int currentPlaylistIndex;

	// use JLayer library to create an AdvancedPlyaer obj which will handle playing
	// the music
	private AdvancedPlayer advancedPlayer;

	// pause boolean flag used to indicate whether the player has been paused;
	private boolean isPaused;

	// boolean flag used to tell if a song is finished or not
	private boolean songFinished;
	
	// boolean flag used to tell if buttons are pressed
	private boolean pressedNext, pressedPrev;

	// stores in the last frame when the playback is finished [used for pausing and
	// resuming]
	private int currentFrame;

	public void setCurrentFrame(int frame) {
		currentFrame = frame;
	}

	// track how many miliseconds has passed since playing the song. [used for
	// updating the slider]
	private int currentTimeInMilli;

	public void setCurrentTimeInMilli(int timeInMilli) {
		currentTimeInMilli = timeInMilli;
	}

	// Constructor
	public MusicPlayer(MusicPlayerGUI musicPlayerGUI) {
		this.musicPlayerGUI = musicPlayerGUI;
	}

	public void loadSong(Song song) {
		// stop song if possible
		stopSong();

		// reset playback slider
		musicPlayerGUI.setPlaybackSliderValue(0);
		currentTimeInMilli = 0;

		// start from the beggining frame;
		currentFrame = 0;

		// start song
		currentSong = song;

		if (currentSong != null) {
			playCurrentSong();
		}
	}

	public void loadPlaylist(File playlistFile) {
		playlist = new ArrayList<>();

		// store the paths from the text file into the playlist array list
		try {
			FileReader fileReader = new FileReader(playlistFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String songPath;
			while ((songPath = bufferedReader.readLine()) != null) {
				// create song object based on song path
				Song song = new Song(songPath);

				// add song to playlist array list
				playlist.add(song);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (playlist.size() > 0) {
			// reset playback slider
			musicPlayerGUI.setPlaybackSliderValue(0);
			currentTimeInMilli = 0;

			// update current song to the first song in the playlist
			currentSong = playlist.get(0);

			// start from the beggining frame;
			currentFrame = 0;

			// update GUI
			musicPlayerGUI.enablePauseButtonDisablePlayButton();
			musicPlayerGUI.updateSongTitleAndArtist(currentSong);
			musicPlayerGUI.updatePlaybackSlider(currentSong);

			// start current song
			playCurrentSong();

		}
	}

	public void pauseSong() {
		if (advancedPlayer != null) {
			// update isPaused flag
			isPaused = true;

			// then we want to stop the song.
			stopSong();
		}
	}

	public void stopSong() {
		if (advancedPlayer != null) {
			advancedPlayer.stop();
			advancedPlayer.close();
			advancedPlayer = null;
		}
	}

	public void prevSong() {
		// set boolean flag to true if button is pressed
		pressedPrev = true;
		
		if (playlist == null) {
			return;
		}

		if (currentPlaylistIndex - 1 < 0) {
			JOptionPane.showMessageDialog(musicPlayerGUI, "No more songs available in the playlist!",
					"Could not play previous song", JOptionPane.ERROR_MESSAGE);
			System.out.println("No more songs in the playlist ;)");
			return;
		}

		// stop song
		if (!songFinished) {
			stopSong();
		}

		// decrease current playlist index
		currentPlaylistIndex--;

		// update current song
		currentSong = playlist.get(currentPlaylistIndex);

		// reset frame
		currentFrame = 0;

		// reset playback slider
		musicPlayerGUI.setPlaybackSliderValue(0);
		currentTimeInMilli = 0;

		// update GUI
		musicPlayerGUI.enablePauseButtonDisablePlayButton();
		musicPlayerGUI.updateSongTitleAndArtist(currentSong);
		musicPlayerGUI.updatePlaybackSlider(currentSong);

		playCurrentSong();
	}

	public void nextSong() {
		// set boolean flag to true when button is pressed
		pressedNext = true;
		
		// no need to go to the next song if there is no playlist
		if (playlist == null)
			return;
		
		// check to see if we have reached the end of the playlist.
		if (currentPlaylistIndex + 1 > playlist.size() - 1) {
			JOptionPane.showMessageDialog(musicPlayerGUI, "No more songs available in the playlist!",
					"Could not play next song", JOptionPane.ERROR_MESSAGE);
			System.out.println("No more songs in the playlist ;)");
			return;
		}

		// stop the song if possible
		if (!songFinished) {
			stopSong();
		}

		// increase current playlist index
		currentPlaylistIndex++;
		System.out.println(currentPlaylistIndex);

		// update current song
		currentSong = playlist.get(currentPlaylistIndex);

		// reset frame;
		currentFrame = 0;

		// reset playback slider
		musicPlayerGUI.setPlaybackSliderValue(0);
		currentTimeInMilli = 0;

		// update GUI
		musicPlayerGUI.enablePauseButtonDisablePlayButton();
		musicPlayerGUI.updateSongTitleAndArtist(currentSong);
		musicPlayerGUI.updatePlaybackSlider(currentSong);

		// play current song
		playCurrentSong();

	}

	public void playCurrentSong() {
		if (currentSong == null)
			return;

		try {
			// read mpp3 audio data
			FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

			// create a new advanced player
			advancedPlayer = new AdvancedPlayer(bufferedInputStream);
			advancedPlayer.setPlayBackListener(this);

			// start music
			startMusicThread();

			// start playback slider thread

			startPlaybackSliderThread();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// create a thread that will handle playing the music
	private void startMusicThread() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (isPaused) {
						synchronized (playSignal) {
							// update flag
							isPaused = false;

							// notify the other thread to continue (makes sure that isPaused is updated to
							// false properly)
							playSignal.notify();
						}

						// resume music
						advancedPlayer.play(currentFrame, Integer.MAX_VALUE);
					} else {
						// play music from the beginning
						advancedPlayer.play();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	// Create a thread that will handle updating the slider
	public void startPlaybackSliderThread() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (isPaused) {
					try {
						// wait till it gets notified by other thread to continue
						// makes sure that isPaused boolean flag updates to false before continueing
						synchronized (playSignal) {
							playSignal.wait();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				while (!isPaused && !songFinished && !pressedNext && !pressedPrev) {
					try {
						// Increment current time in milliseconds
						currentTimeInMilli++;

						// Calculate into frame value
						int calculatedFrame = (int) ((double) currentTimeInMilli * 2.08
								* currentSong.getFrameRatePerMillisecond());

						// update GUI
						musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);

						// Mimic 1 millisecond using thread.sleep.
						Thread.sleep(1);

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	public void playbackStarted(PlaybackEvent evt) {
		// this method gets called when the song finishes or if the player gets closed
		System.out.println("Playback Started");
		songFinished = false;
	}

	@Override
	public void playbackFinished(PlaybackEvent evt) {
		// this method gets called in the beginning of the song
		System.out.println("Playback Finished");
		if (isPaused) {
			currentFrame += (int) ((double) evt.getFrame() * currentSong.getFrameRatePerMillisecond());
		} else {
			// when the song ends
			songFinished = true;
			
			if (playlist == null) {
				// update GUI
				musicPlayerGUI.enablePlayButtonDisablePauseButton();
			} else {
				// last song in the playlist
				if (currentPlaylistIndex == playlist.size() - 1) {
					// update GUI
					musicPlayerGUI.enablePlayButtonDisablePauseButton();
				} else {
					// if next or prev button is pressed, dont do anything
					if (pressedNext || pressedPrev) {
						pressedNext = false;
						pressedPrev = false;
						return;
					}
					// if not last song in the playlist, go to next song
					nextSong();
				}
			}
		}
	}

}
