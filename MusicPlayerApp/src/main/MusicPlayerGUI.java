package main;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MusicPlayerGUI extends JFrame {

	private MusicPlayer musicPlayer;

	// allow us to use file explorer in our app

	private JFileChooser jFileChooser;

	private JLabel songTitle, songArtist;
	private JPanel playbackBtns;
	private JSlider playbackSlider;

	public MusicPlayerGUI() {
		// Calls JFrame constructor to configure out GUI and set the title header to
		// "Audio Player"
		super("Music Player");

		setSize(400, 600);

		// End process when app is closed
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// Launch the app at the center of the screen
		setLocationRelativeTo(null);

		// Prevent the app from being resized
		setResizable(false);

		// Set layout to null which allows us to control the (x, y,) coordinates of our
		// components.
		setLayout(null);

		// change the frame color
		getContentPane().setBackground(Constants.FRAME_COLOR);

		musicPlayer = new MusicPlayer(this);
		jFileChooser = new JFileChooser();

		// set a default path for the file explorer
		jFileChooser.setCurrentDirectory(new File(Constants.ASSETS_PACKAGE_PATH));

		// filter file chooser to only see .mp3 files
		jFileChooser.setFileFilter(new FileNameExtensionFilter("MP3", "mp3"));
		// jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("AAC",
		// "aac"));

		addGuiComponents();
	}

	private void addGuiComponents() {
		// add toolbar
		addToolbar();

		// Load record image
		JLabel songImage = new JLabel(loadImage(Constants.RECORD_IMAGE_PATH));
		songImage.setBounds(0, 50, getWidth() - 20, 225);
		add(songImage);

		// song title
		songTitle = new JLabel("Song Title");
		songTitle.setBounds(0, 285, getWidth() - 10, 30);
		songTitle.setFont(Constants.SONG_TITLE_FONT);
		songTitle.setForeground(Constants.TEXT_COLOR);
		songTitle.setHorizontalAlignment(SwingConstants.CENTER);
		add(songTitle);

		// song artist
		songArtist = new JLabel("Artist");
		songArtist.setBounds(0, 315, getWidth() - 10, 30);
		songArtist.setFont(Constants.SONG_ARTIST_FONT);
		songArtist.setForeground(Constants.TEXT_COLOR);
		songArtist.setHorizontalAlignment(SwingConstants.CENTER);
		add(songArtist);

		playbackSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		playbackSlider.setBounds(getWidth() / 2 - 300 / 2, 365, 300, 40);
		playbackSlider.setBackground(null);
		playbackSlider.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				// when the user is holding the tick, we want to pause the song
				musicPlayer.pauseSong();

				// toggle on play button and toggle off pause button
				enablePlayButtonDisablePauseButton();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// when the user drops the tick
				JSlider source = (JSlider) e.getSource();

				// get the frame value from where the user wants to playback to
				int frame = source.getValue();

				// update the current frame in the music player to this form
				musicPlayer.setCurrentFrame(frame);

				// update current time in milliseconds as well
				musicPlayer.setCurrentTimeInMilli(
						(int) (frame / (2.08 * musicPlayer.getCurrentSong().getFrameRatePerMillisecond())));

				// resume the song
				musicPlayer.playCurrentSong();

				// toggle on pause button and toggle off play button
				enablePauseButtonDisablePlayButton();
			}
		});
		add(playbackSlider);

		// playback buttons (i.e previous, play, next)
		addPlaybackBtns();
	}

	private void addToolbar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setBounds(0, 0, getWidth(), 20);

		// prevents toolbar from being moved
		toolbar.setFloatable(false);

		// add drop down menu
		JMenuBar menuBar = new JMenuBar();
		toolbar.add(menuBar);

		// now we will add a song menu where we will place the loading song option.
		JMenu songMenu = new JMenu("Song");
		menuBar.add(songMenu);

		// add the "load song" item in the songMenu
		JMenuItem loadSong = new JMenuItem("Load Song");
		loadSong.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// an integer is return to us to let us known what the user did
				int result = jFileChooser.showOpenDialog(MusicPlayerGUI.this);
				File selectedFile = jFileChooser.getSelectedFile();

				// this means that we are also checking to see if the user pressed the "open"
				// button
				if (result == JFileChooser.APPROVE_OPTION && selectedFile != null) {
					// create a song obj based on selected file.
					Song song = new Song(selectedFile.getPath());

					// load song in music player
					musicPlayer.loadSong(song);

					// update song title and artist
					updateSongTitleAndArtist(song);

					// update playback slider
					updatePlaybackSlider(song);

					// toggle on pause button and toggle off play button
					enablePauseButtonDisablePlayButton();
				}
			}
		});
		songMenu.add(loadSong);

		// Now we will add the playlist menu.
		JMenu playlistMenu = new JMenu("Playlist");
		menuBar.add(playlistMenu);

		// then add the items to the playlist menu
		JMenuItem createPlaylist = new JMenuItem("Create Playlist");
		createPlaylist.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Load music playlist dialog
				new MusicPlaylistDialog(MusicPlayerGUI.this).setVisible(true);
			}
		});
		playlistMenu.add(createPlaylist);

		JMenuItem loadPlaylist = new JMenuItem("Load Playlist");
		loadPlaylist.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.setFileFilter(new FileNameExtensionFilter("TXT", "txt"));
				jFileChooser.setCurrentDirectory(new File(Constants.ASSETS_PACKAGE_PATH));
				
				int result = jFileChooser.showOpenDialog(MusicPlayerGUI.this);
				File selectedFile = jFileChooser.getSelectedFile();
				
				if (result == JFileChooser.APPROVE_OPTION && selectedFile != null) {
					// stop the music
					musicPlayer.stopSong();
					
					// load playlist
					musicPlayer.loadPlaylist(selectedFile);
				}
			}
		});
		playlistMenu.add(loadPlaylist);

		add(toolbar);
	}

	private void addPlaybackBtns() {
		playbackBtns = new JPanel();
		playbackBtns.setBounds(0, 435, getWidth(), 80);
		playbackBtns.setBackground(null);

		// Previous button
		JButton prevButton = new JButton(loadImage(Constants.PREV_IMAGE_PATH));
		prevButton.setBorderPainted(false);
		prevButton.setBackground(null);
		prevButton.setFocusPainted(false);
		prevButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// go to the previous song
				musicPlayer.prevSong();
				
			}
		});
		playbackBtns.add(prevButton);

		// Play button
		JButton playButton = new JButton(loadImage(Constants.PLAY_IMAGE_PATH));
		playButton.setBorderPainted(false);
		playButton.setBackground(null);
		playButton.setFocusPainted(false);
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// toggle off play button and toggle on pause button
				enablePauseButtonDisablePlayButton();

				// play or resume song
				musicPlayer.playCurrentSong();
			}
		});
		playbackBtns.add(playButton);

		// pause button
		JButton pauseButton = new JButton(loadImage(Constants.PAUSE_IMAGE_PATH));
		pauseButton.setBorderPainted(false);
		pauseButton.setBackground(null);
		pauseButton.setFocusPainted(false);
		pauseButton.setVisible(false);
		pauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// toggle off pause button and toggle on play button
				enablePlayButtonDisablePauseButton();

				// pause the song
				musicPlayer.pauseSong();
			}
		});
		playbackBtns.add(pauseButton);

		// next button
		JButton nextButton = new JButton(loadImage(Constants.NEXT_IMAGE_PATH));
		nextButton.setBorderPainted(false);
		nextButton.setBackground(null);
		nextButton.setFocusPainted(false);
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// go to the next song
				musicPlayer.nextSong();
			}
		});
		playbackBtns.add(nextButton);

		add(playbackBtns);

	}

	// this will allow us to update our slider from the music player class.
	public void setPlaybackSliderValue(int frame) {
		playbackSlider.setValue(frame);
	}

	public void updateSongTitleAndArtist(Song song) {
		songTitle.setText(song.getSongTitle());
		songArtist.setText(song.getSongArtist());
	}

	public void updatePlaybackSlider(Song song) {
		// update max count for slider
		playbackSlider.setMaximum(song.getMp3File().getFrameCount());

		// Create the song length label
		Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

		// Beginning will be 00:00
		JLabel labelBeginning = new JLabel("00:00");
		labelBeginning.setFont(Constants.PLAYBACK_SLIDER_LABEL_FONT);
		labelBeginning.setForeground(Constants.TEXT_COLOR);

		// end will vary depending on the song
		JLabel labelEnd = new JLabel(song.getSongLength());
		labelEnd.setFont(Constants.PLAYBACK_SLIDER_LABEL_FONT);
		labelEnd.setForeground(Constants.TEXT_COLOR);

		labelTable.put(0, labelBeginning);
		labelTable.put(song.getMp3File().getFrameCount(), labelEnd);

		playbackSlider.setLabelTable(labelTable);
		playbackSlider.setPaintLabels(true);
	}

	public void enablePauseButtonDisablePlayButton() {
		// retrieve reference to play button from playbackBtns panel
		JButton playButton = (JButton) playbackBtns.getComponent(1);
		JButton pauseButton = (JButton) playbackBtns.getComponent(2);

		// turn off play button
		playButton.setVisible(false);
		playButton.setEnabled(false);

		// turn on pause button
		pauseButton.setVisible(true);
		pauseButton.setEnabled(true);
	}

	public void enablePlayButtonDisablePauseButton() {
		// retrieve reference to play button from playbackBtns panel
		JButton playButton = (JButton) playbackBtns.getComponent(1);
		JButton pauseButton = (JButton) playbackBtns.getComponent(2);

		// turn on play button
		playButton.setVisible(true);
		playButton.setEnabled(true);

		// turn off pause button
		pauseButton.setVisible(false);
		pauseButton.setEnabled(false);
	}

	private ImageIcon loadImage(String imagePath) {

		try {
			// Read the image file from the given path
			BufferedImage image = ImageIO.read(new File(imagePath));

			// returns the image icon so that our component can render the image
			return new ImageIcon(image);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// could not find resource
		System.out.println("loadImage -> Could not locate resource ' " + imagePath + " '.");
		return null;
	}
}
