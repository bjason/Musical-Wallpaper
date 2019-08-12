package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.CaseFormat;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;

import grabber.APlayerAPIGrabber;
import grabber.Grabber;
import grabber.SpotifyGrabber;
import grabber.YouTubeGrabber;
import util.ImageCollageCreator;
import util.InvalidPlaylistURLException;
import util.PlaylistIDManager;
import util.PropertiesManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Hashtable;

/// User interface for application
public class VisualizorUI extends JFrame {
	private static Font defaultFont = new Font("Segoe UI", Font.PLAIN, 18); // default
																			// font

	private JPanel[] panels;
	private int panelPosition = 0;

	public void setup() {
		setDefaultLookAndFeelDecorated(true);

		// setSize(400, 300);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setTitle("Spotify Playlist Visulizor");

		setVisible(true);
		setResizable(false);
		setLocationRelativeTo(null);

		UIManager.put("Button.font", defaultFont);
		UIManager.put("Label.font", defaultFont);
		UIManager.put("ComboBox.font", defaultFont);
		UIManager.put("TextField.font", defaultFont);
		UIManager.put("Slider.font", defaultFont);
		UIManager.put("ProgressBar.font", defaultFont);

		panels = new JPanel[] { new SelectPlaylistPanel(), new ArtworkSizePanel(), new LoadingScreenPanel(),
				new WallpaperSetPanel() };
		setupPanels(); // add empty borders to all panels

		// display the first panel
		add(panels[panelPosition]);

		pack();
	}

	private class SelectPlaylistPanel extends JPanel {
		@SuppressWarnings("unchecked")
		SelectPlaylistPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			// Box box = createVerticalBox();

			JLabel label = new JLabel("<html>Welcome to Spotify Playlist Visualizor"
					+ "<br>Make your favourite album covers<br>into your new favourite wallpaper!"
					+ "<br>Create or find a playlist with your"
					+ "<br>favourite albums then paste the<br>link below with Ctrl-V</html>");
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(label);
			add(Box.createVerticalStrut(20));

			JLabel label2 = new JLabel("Choose your playlist source:");
			label2.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(label2);
			add(Box.createVerticalStrut(20));

			// TODO add support for qmusic and kgmusic
			final JComboBox jComboBox = new JComboBox<>();
			jComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
			jComboBox.addItem("Spotify");
			jComboBox.addItem("163 Music");
			jComboBox.addItem("Youtube");
			jComboBox.addItem("QQ Music");
			jComboBox.addItem("KuGou Music");

			jComboBox.setPreferredSize(new Dimension(300, 30));
			// set select-able items
			DefaultListSelectionModel model = new DefaultListSelectionModel();
			model.addSelectionInterval(0, 1);
			model.addSelectionInterval(3, 4);
			
			EnabledJComboBoxRenderer enableRenderer = new EnabledJComboBoxRenderer(model);
			jComboBox.setRenderer(enableRenderer);
			
			add(jComboBox);

			add(Box.createVerticalStrut(10));

			String initialURL = "";
			int initialPos = 0;
			try {
				String pos = PropertiesManager.getProperty("sourceId");
				if (pos != null) {
					initialPos = Integer.parseInt(pos);
				}
				jComboBox.setSelectedIndex(initialPos);
				int selectedItem = jComboBox.getSelectedIndex();

				// use a playlist from spotify if this is the user's first time
				initialURL = setUrl(initialPos, selectedItem);
			} catch (IOException e) {
				showErrorMessage(e.getMessage());
			}
			final JTextField textField = new JTextField(initialURL);
			textField.setAlignmentX(Component.LEFT_ALIGNMENT);

			textField.setCaretPosition(0);
			textField.setPreferredSize(new Dimension(300, 30));
			// textField.setMaximumSize(new Dimension(300, 30));
			add(textField);

			jComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					int pos = jComboBox.getSelectedIndex();
					int item = jComboBox.getSelectedIndex();
					try {
						textField.setText(setUrl(pos, item));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			add(Box.createVerticalStrut(20));

			JButton button = new JButton("Go");
			button.setAlignmentX(Component.LEFT_ALIGNMENT);

			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						int sourceId = jComboBox.getSelectedIndex();
						PropertiesManager.setProperty("sourceId", sourceId);

						// try and get the playlistID and userID - if this works
						// then continue to next panel
						String[] IDs = new PlaylistIDManager().getPlaylistIDAndUserIDFromURL(textField.getText());
						PropertiesManager.setProperty(sourceId + "playlistId", IDs[0]);
						PropertiesManager.setProperty(sourceId + "userId", IDs[1]);

						PropertiesManager.setProperty(sourceId + "playlistURL", textField.getText());

						nextPanel();
					} catch (InvalidPlaylistURLException exception) {
						// an invalid URL was supplied, don't continue
						showErrorMessage("That's not a valid playlist URL");
					} catch (IOException exception) {
						showErrorMessage(exception.getMessage());
					}
				}
			});
			add(button);

			// this.add(box, BorderLayout.EAST);
		}

		private String setUrl(int initialPos, int selectedItem) throws IOException {
			String initialURL = PropertiesManager.getProperty(selectedItem + "playlistURL");

			// use a playlist from spotify if this is the user's first time
			if (initialURL == null || initialURL.equals("")) {
				initialURL = PlaylistIDManager.DEFAULT_URL[initialPos];
			}
			return initialURL;
		}
	}

	private class ArtworkSizePanel extends JPanel {
		ArtworkSizePanel() {
			// GUI
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(new JLabel("How many album covers per wallpaper?"));

			Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
			labelTable.put(new Integer(0), new JLabel("Few"));
			labelTable.put(new Integer(1), new JLabel("Some"));
			labelTable.put(new Integer(2), new JLabel("Many"));
			labelTable.put(new Integer(3), new JLabel("<html>Rank<br>Mode</html>"));
			labelTable.put(new Integer(4), new JLabel("<html>Zune<br>Mode</html>"));

			int initialValue = 0; // default in case of error
			try {
				initialValue = Integer.parseInt(PropertiesManager.getProperty("imageSizeCode", "0"));
			} catch (IOException e) {
				showErrorMessage("Could not load user preferences. " + e.getMessage());
			}

			final JSlider slider = new JSlider(0, 4, initialValue);
			slider.setPreferredSize(new Dimension(200, 100));
			slider.setLabelTable(labelTable);
			slider.setPaintLabels(true);
			slider.setPaintTicks(true);
			add(slider);
			slider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (!slider.getValueIsAdjusting()) {
						try {
							PropertiesManager.setProperty("imageSizeCode", String.valueOf(slider.getValue()));
						} catch (IOException exception) {
							showErrorMessage(exception.getMessage());
						}
					}
				}
			});

			JButton button = new JButton("Generate wallpapers");
			add(button);
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					nextPanel();
				}
			});
		}
	}

	private class LoadingScreenPanel extends JPanel {

		private final JProgressBar progressBar;
		private final JLabel trackProcessing;

		LoadingScreenPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			progressBar = new JProgressBar(0, 100);
			progressBar.setPreferredSize(new Dimension(300, 100));
			progressBar.setValue(0);
			add(progressBar);
			progressBar.setStringPainted(true);

			trackProcessing = new JLabel();
			add(trackProcessing);
		}

		// TODO add and modify trackProcessing label
		private void beginLoading() throws NumberFormatException, IOException {
			progressBar.setString("Downloading album art...");

			int sourceId = Integer.parseInt(PropertiesManager.getProperty("sourceId"));
			Grabber grabber = null;
			String transCode;

			switch (sourceId) {
			case 0:
				grabber = new SpotifyGrabber() {
					@Override
					protected void done() {
						// when the album art grabber is done, generate the
						// collages
						// and display that progress
						if (this.errorCode != null) {
							// there was an error downloading the images
							showErrorMessage(this.errorCode);
						} else {
							// no problems - go ahead and generate collages
							generateCollages(progressBar);
						}
					}
				};
				break;
			case 3:
			case 1:
			case 4:
				transCode = APlayerAPIGrabber.TransCode_INDEX[sourceId];

				grabber = new APlayerAPIGrabber(transCode) {
					@Override
					protected void done() {
						// when the album art grabber is done, generate the
						// collages
						// and display that progress
						if (this.errorCode != null) {
							// there was an error downloading the images
							showErrorMessage(this.errorCode);
						} else {
							// no problems - go ahead and generate collages
							generateCollages(progressBar);
						}
					}
				};
				break;
			case 2:
				grabber = new YouTubeGrabber() {
					@Override
					protected void done() {
						// when the album art grabber is done, generate the
						// collages
						// and display that progress
						if (this.errorCode != null) {
							// there was an error downloading the images
							showErrorMessage(this.errorCode);
						} else {
							// no problems - go ahead and generate collages
							generateCollages(progressBar);
						}
					}
				};
			}

			setupProgressBarChanging(grabber);
		}

		private void setupProgressBarChanging(final SwingWorker task) {
			task.execute();

			task.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getPropertyName().equals("progress")) {
						progressBar.setValue(task.getProgress());
					}
				}
			});
		}

		private void generateCollages(JProgressBar progressBar) {
			progressBar.setString("Generating collages...");
			progressBar.setValue(0);

			ImageCollageCreator imageCollageCreator = new ImageCollageCreator() {
				@Override
				protected void done() {
					// when the collages are generated, go the next pane
					if (this.errorCode != null) {
						// there was an error generating the collages
						showErrorMessage(this.errorCode);
					} else {
						// no problems
						nextPanel();
					}
				}
			};

			setupProgressBarChanging(imageCollageCreator);
		}
	}

	private class WallpaperSetPanel extends JPanel {
		WallpaperSetPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(new JLabel("<html>Your collages are ready!" + "<br>Now you just need to set them as your wallpaper"
					+ "<br>Click the button below, then:" + "<br>1. Set 'background' to 'slideshow'"
					+ "<br>2. Click 'browse', go to 'Spotify Playlist Visualizor'"
					+ "<br>3. Click on 'Collages' then click 'Choose folder'"
					+ "<br>And enjoy your favourite music, now your favourite wallpaper!"));
			add(Box.createVerticalStrut(20));
			JButton settingsButton = new JButton("Open wallpaper settings");
			add(settingsButton);
			settingsButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						Runtime.getRuntime().exec("cmd /c start ms-settings:personalization-background");
					} catch (IOException exception) {
						showErrorMessage(exception.getMessage());
					}
				}
			});
		}
	}

	private void setupPanels() {
		for (JPanel panel : panels) {
			panel.setBorder(new EmptyBorder(30, 30, 30, 30));
		}
	}

	protected void nextPanel() {
		swapPanel(panelPosition + 1);
	}

	protected void previousPanel() {
		swapPanel(panelPosition - 1);
	}

	private void swapPanel(final int newPanelPosition) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				remove(panels[panelPosition]);
				panelPosition = newPanelPosition;
				add(panels[panelPosition]);
				pack();
				invalidate();
				revalidate();

				// start the loading of collages if this is the progress screen
				if (panels[panelPosition] instanceof LoadingScreenPanel) {
					try {
						((LoadingScreenPanel) panels[panelPosition]).beginLoading();
					} catch (NumberFormatException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}

	private class EnabledJComboBoxRenderer extends BasicComboBoxRenderer {
		static final long serialVersionUID = -984932432414L;
		private final ListSelectionModel enabledItems;
		private Color disabledColor = Color.lightGray;

		public EnabledJComboBoxRenderer(ListSelectionModel enabled) {
			super();
			this.enabledItems = enabled;
		}

		public void setDisabledColor(Color disabledColor) {
			this.disabledColor = disabledColor;
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index,
				boolean isSelected,
				boolean cellHasFocus) {
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (!enabledItems.isSelectedIndex(index)) {// not enabled
				if (isSelected) {
					c.setBackground(UIManager.getColor("ComboBox.background"));
				} else {
					c.setBackground(super.getBackground());
				}
				c.setForeground(disabledColor);
			} else {
				c.setBackground(super.getBackground());
				c.setForeground(super.getForeground());
			}

			return c;
		}
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
		swapPanel(0); // return to the start of the process
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				VisualizorUI musicalWallpaperUI = new VisualizorUI();
				musicalWallpaperUI.setup();
			}
		});
	}
}
